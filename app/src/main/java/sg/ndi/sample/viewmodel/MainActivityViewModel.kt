package sg.ndi.sample.viewmodel

import android.app.Application
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appstractive.jwt.JWT
import com.appstractive.jwt.audience
import com.appstractive.jwt.expiresAt
import com.appstractive.jwt.from
import com.appstractive.jwt.signatures.es256
import com.appstractive.jwt.verify
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.datastorage.getOrDefault
import com.singpass.login.model.LoginParams
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.openid.appauth.AuthorizationService
import sg.ndi.sample.App
import sg.ndi.sample.R
import sg.ndi.sample.dataStore
import sg.ndi.sample.model.AUTH_TYPE
import sg.ndi.sample.model.LaunchAuthState
import sg.ndi.sample.model.SessionParametersResponse
import sg.ndi.sample.utility.NdiOidcService
import sg.ndi.sample.utility.PackageUtils
import sg.ndi.sample.utility.RetrofitHelper
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import kotlin.time.toJavaInstant

class MainActivityViewModel(
    private val app: Application,
    private val state: SavedStateHandle,
) : AndroidViewModel(app) {

    private var authService: AuthorizationService? = null

    private var pkceSessionParametersResponse: SessionParametersResponse? = null

    private val _authState: MutableStateFlow<LaunchAuthState> =
        MutableStateFlow(LaunchAuthState.INITIAL)
    val authState: StateFlow<LaunchAuthState>
        get() = _authState

    var _spmInstalledState = mutableStateOf(false)
        private set
    val spmInstalledState: State<Boolean>
        get() = _spmInstalledState

    fun updateSpmInstalledState() {
        _spmInstalledState.value = PackageUtils.isSpmInstalled(app)
    }

    private val ndiOidcService: NdiOidcService by lazy {
        RetrofitHelper.getInstance().create(NdiOidcService::class.java)
    }

    var _authCodeState = mutableStateOf(DEFAULT_AUTH_CODE_TEXT)
        private set
    val authCodeState: State<String>
        get() = _authCodeState

    var _idTokenState = mutableStateOf(DEFAULT_ID_TOKEN_TEXT)
        private set
    val idTokenState: State<String>
        get() = _idTokenState

    var _buttonEnabledState = mutableStateOf(true)
        private set
    val buttonEnabledState: State<Boolean>
        get() = _buttonEnabledState

    private var currentAuthType: AUTH_TYPE = AUTH_TYPE.SINGPASS

    private val sessionTokenKey = stringPreferencesKey("sessionToken")
    private val unknownUser = "Unknown User"

    private suspend fun verifySessionToken(sessionToken: String): String? {

        val jwt = JWT.from(sessionToken)
        val verified = jwt.verify {
            es256 { pem(app.getString(R.string.rp_pubkey_pem)) }
        }

        return if (verified) {
            val userId = jwt.audience ?: unknownUser

            val date = jwt.expiresAt?.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Date.from(this.toJavaInstant())
                } else {
                    Date().apply {
                        time = this.time
                    }
                }
            }

            val loginMessage = if (date == null) {
                userId
            } else {
                "$userId\n\nLogin session expires at\n$date"
            }

            loginMessage
        } else {
            null
        }
    }

    init {

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->

            Log.e("init", throwable.message, throwable)

            App.toastMessage("Exception log out", getApplication())
            logout()
            _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _authState.tryEmit(LaunchAuthState.INITIAL)

            val sessionToken = app.dataStore.data.firstOrNull()?.getOrDefault(sessionTokenKey, "")

            if (sessionToken.isNullOrBlank()) {
                _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
            } else {
                Log.d("init", sessionToken)

                val appCheckToken = Firebase.appCheck.getAppCheckToken(false).await()

                val response = ndiOidcService.getAmIStilloggedIn(
                    appCheckToken = appCheckToken.token,
                    bearerToken = "Bearer $sessionToken"
                )

                if (response.isSuccessful) {

                    val verifyTokenResult = verifySessionToken(sessionToken)

                    if (!verifyTokenResult.isNullOrBlank()) {
                        _authState.tryEmit(LaunchAuthState.LOGIN_SUCCESS(verifyTokenResult))
                    } else {
                        _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
                        _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("RP session token signature verification failed!")
                    }
                } else {
                    if (response.code() == 403) {
                        App.toastMessage("Remote log out", getApplication())
                        logout()
                        _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
                    } else {
                        val jwt = JWT.from(sessionToken)
                        jwt.expiresAt?.epochSeconds?.run {
                            if (this < System.currentTimeMillis()) {
                                App.toastMessage("Local login", getApplication())
                                val verifyTokenResult = verifySessionToken(sessionToken)
                                if (!verifyTokenResult.isNullOrBlank()) {
                                    _authState.tryEmit(LaunchAuthState.LOGIN_SUCCESS(verifyTokenResult))
                                } else {
                                    _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
                                    _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("RP session token verification failed!")
                                }
                            } else {
                                App.toastMessage("Local log out", getApplication())
                                logout()
                                _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
                            }
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            app.dataStore.edit { it.clear() }
            reset()
            _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
        }
    }

    override fun onCleared() {
        super.onCleared()
        authService?.dispose()
        currentAuthType = AUTH_TYPE.SINGPASS
    }

    fun updateAuthCode(authCode: String?) {
        _authCodeState.value = if (authCode.isNullOrBlank()) {
            DEFAULT_AUTH_CODE_TEXT
        } else {
            authCode
        }
    }

    fun consumeAuthorizationWebPageTrigger() {
        when (val state = _authState.value) {
            LaunchAuthState.LOGGED_OUT,
            is LaunchAuthState.LOGIN_SUCCESS,
            LaunchAuthState.INITIAL -> Unit
            is LaunchAuthState.LAUNCH -> {
                _authState.tryEmit(
                    LaunchAuthState.LAUNCH(
                        singpassLoginParam = state.singpassLoginParam,
                        launched = true
                    )
                )
            }
        }
    }

    fun enableBackButtons() {
        _buttonEnabledState.value = true
    }

    fun sendAuthCodeToBackend(
        code: String,
        state: String
    ) {

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, ex ->
            Log.e("sendAuthCodeToBackend", "error occurred: ${ex.message}", ex)
            _idTokenState.value = ERROR_ID_TOKEN_TEXT.format(ex.message)
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _idTokenState.value = WAITING_ID_TOKEN_TEXT
            val appCheckToken = Firebase.appCheck.getAppCheckToken(false).await()

            val response = when (val sessionParams = pkceSessionParametersResponse) {
                is SessionParametersResponse.FapiPkceSessionParametersResponse -> {
                    ndiOidcService.postFAPIAuthCode(
                        appCheckToken = appCheckToken.token,
                        code = code,
                        state = sessionParams.state,
                        nonce = sessionParams.nonce,
                        redirectUri = app.getString(R.string.redirect_uri)
                    )
                }
                is SessionParametersResponse.PkceSessionParametersResponse -> {
                    ndiOidcService.postAuthCode(
                        appCheckToken = appCheckToken.token,
                        code = code,
                        state = state,
                        nonce = sessionParams.nonce,
                        redirectUri = app.getString(R.string.redirect_uri)
                    )
                }
                null -> null
            }

            if (response?.isSuccessful == true) {

                val responseObj = response.body()

                if (responseObj == null) {
                    _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("error occurred: Empty response! - ${response.code()}")
                } else  {

                    val stringBuilder = StringBuilder()

                    stringBuilder.append("API response\n\n uid: ${responseObj.uid}")

                    responseObj.sessionToken.let {
                        stringBuilder.append("\n\n")
                        stringBuilder.append(("session token: ${responseObj.sessionToken}"))
                    }

                    responseObj.userInfo?.let {
                        stringBuilder.append("\n\n")
                        stringBuilder.append("user_info: ${responseObj.userInfo}")
                    }

                    responseObj.userInfoError?.let {
                        stringBuilder.append("\n\n")
                        stringBuilder.append("userInfoError: ${responseObj.userInfoError}")
                    }

                    val outputString = if (currentAuthType == AUTH_TYPE.USERINFO) {
                        USER_INFO_OBTAINED_TEXT.format(stringBuilder.toString())
                    } else {
                        ID_TOKEN_OBTAINED_TEXT.format(stringBuilder.toString())
                    }

                    _idTokenState.value = "RP Session token retrieved!"

                    app.dataStore.updateData {
                        it.toMutablePreferences().also { preferences ->
                            preferences[sessionTokenKey] = responseObj.sessionToken
                        }
                    }
                    val verifyTokenResult = verifySessionToken(responseObj.sessionToken)

                    if (!verifyTokenResult.isNullOrBlank()) {
                        val finalResult = "$verifyTokenResult\n\n $outputString"
                        _authState.tryEmit(LaunchAuthState.LOGIN_SUCCESS(finalResult))
                    } else {
                        _authState.tryEmit(LaunchAuthState.LOGGED_OUT)
                        _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("RP session token verification failed!")
                    }
                }
            } else {
                _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("error occurred: ${response?.errorBody()?.string()} - ${response?.code()}")
            }
            _buttonEnabledState.value = true
        }
    }

    private fun getCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(
            code,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun getCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun getFapiRequestUri(authType: AUTH_TYPE = AUTH_TYPE.SINGPASS, appAuthOnly: Boolean) {

        currentAuthType = authType

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(
                "getFapiRequestUri",
                throwable.message ?: "error occurred during createAuthorizationServiceIntent",
                throwable
            )
            _buttonEnabledState.value = true
            updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Error createAuthorizationServiceIntent - (${throwable.message})"))
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _buttonEnabledState.value = false

            updateAuthCode("Getting FAPI PKCE params...")
            _idTokenState.value = DEFAULT_ID_TOKEN_TEXT

//            val codeVerifier = getCodeVerifier()
//            val codeChallenge = getCodeChallenge(codeVerifier)
            val appCheckToken = Firebase.appCheck.getAppCheckToken(false).await()
            val clientId: String

            val response = when (authType) {
                AUTH_TYPE.USERINFO -> {

                    clientId = app.getString(R.string.userinfo_client_id)

                    ndiOidcService.getFAPIRequestUri(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.USERINFO
                    )
                }
                AUTH_TYPE.SINGPASS -> {

                    clientId = app.getString(R.string.client_id)

                    ndiOidcService.getFAPIRequestUri(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.SINGPASS
                    )
                }
                AUTH_TYPE.SFV -> {

                    clientId = app.getString(R.string.sfv_client_id)

                    ndiOidcService.getFAPIRequestUri(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.SFV
                    )
                }
            }

            if (response.isSuccessful) {
                val _fapiPkceSessionParametersResponse = SessionParametersResponse.FapiPkceSessionParametersResponse.createInstance(
                    fapiPkceSessionParametersResponse = response.body(),
                    authType = authType
                )

                pkceSessionParametersResponse = _fapiPkceSessionParametersResponse

                _fapiPkceSessionParametersResponse?.run {
                    _authState.tryEmit(
                        LaunchAuthState.LAUNCH(
                            singpassLoginParam = LoginParams.SingpassFapiLoginParam(
                                useAppAuthAlways = appAuthOnly,
                                requestUri = requestUri,
                                clientId = clientId,
                                redirectUri = app.getString(R.string.redirect_uri).toUri(),
                                customTabAppBarColor = ContextCompat.getColor(getApplication(), R.color.appbarcolor),
                                customTabAppBarColorDark = ContextCompat.getColor(getApplication(), R.color.appbarcolordark),
                                customTabNavigationBarColor = ContextCompat.getColor(getApplication(), R.color.appbarcolor),
                                customTabNavigationBarColorDark = ContextCompat.getColor(getApplication(), R.color.appbarcolordark)
                            )
                        )

                    )
                }
            } else {
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Unable to get FAPI request URI! - (${response.code()})"))
            }
        }
    }

    fun getPkceParams(authType: AUTH_TYPE = AUTH_TYPE.SINGPASS, appAuthOnly: Boolean) {

        currentAuthType = authType

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(
                "createAuthorizationServ",
                throwable.message ?: "error occurred during createAuthorizationServiceIntent",
                throwable
            )
            _buttonEnabledState.value = true
            updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Error createAuthorizationServiceIntent - (${throwable.message})"))
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _buttonEnabledState.value = false

            updateAuthCode("Getting PKCE params...")
            _idTokenState.value = DEFAULT_ID_TOKEN_TEXT

//            val sessionVerifier = getCodeVerifier()
//            val sessionChallenge = getCodeChallenge(sessionVerifier)
            val clientId: String
            val scope: String
            val appCheckToken = Firebase.appCheck.getAppCheckToken(false).await()

            val response = when (authType) {
                AUTH_TYPE.USERINFO -> {

                    clientId = app.getString(R.string.userinfo_client_id)
                    scope = app.getString(R.string.userinfo_scope)

                    ndiOidcService.getPkceSessionParameters(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.USERINFO
                    )
                }
                AUTH_TYPE.SINGPASS -> {

                    clientId = app.getString(R.string.client_id)
                    scope = app.getString(R.string.auth_scope)

                    ndiOidcService.getPkceSessionParameters(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.SINGPASS
                    )
                }
                AUTH_TYPE.SFV -> {

                    clientId = app.getString(R.string.sfv_client_id)
                    scope = app.getString(R.string.sfv_scope)

                    ndiOidcService.getPkceSessionParameters(
                        appCheckToken = appCheckToken.token,
                        authType = AUTH_TYPE.SFV
                    )
                }
            }

            if (response.isSuccessful) {

                val _pkceSessionParametersResponse = SessionParametersResponse.PkceSessionParametersResponse.createInstance(
                    pkceSessionParametersResponse = response.body(),
                    authType = authType
                )

                pkceSessionParametersResponse = _pkceSessionParametersResponse

                _pkceSessionParametersResponse?.run {
                    _authState.tryEmit(
                        LaunchAuthState.LAUNCH(
                            singpassLoginParam = LoginParams.SingpassLoginParam(
                                nonce = nonce,
                                state = state,
                                codeChallenge = codeChallenge,
                                codeChallengeMethod = codeChallengeMethod,
                                clientId = clientId,
                                scope = scope,
                                redirectUri = app.getString(R.string.redirect_uri).toUri(),
                                useAppAuthAlways = appAuthOnly,
                                customTabAppBarColor = ContextCompat.getColor(getApplication(), R.color.appbarcolor),
                                customTabAppBarColorDark = ContextCompat.getColor(getApplication(), R.color.appbarcolordark),
                                customTabNavigationBarColor = ContextCompat.getColor(getApplication(), R.color.appbarcolor),
                                customTabNavigationBarColorDark = ContextCompat.getColor(getApplication(), R.color.appbarcolordark)
                            )
                        )
                    )
                }
            } else {
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Unable to get PKCE params! - (${response.code()})"))
            }
        }
    }

    fun reset() {
        _buttonEnabledState.value = true
        _authCodeState.value = DEFAULT_AUTH_CODE_TEXT
        _idTokenState.value = DEFAULT_ID_TOKEN_TEXT
        authService = null
        pkceSessionParametersResponse = null
    }

    companion object {
        const val DEFAULT_AUTH_CODE_TEXT = "No authCode obtained yet!"
        const val AUTH_CODE_OBTAINED_TEXT = "AuthCode :\n%s"
        const val ERROR_AUTH_CODE_TEXT = "Error :\n%s"

        const val DEFAULT_ID_TOKEN_TEXT = "No token response obtained yet!"
        const val ID_TOKEN_OBTAINED_TEXT = "Singpass login response :\n\n%s"
        const val USER_INFO_OBTAINED_TEXT = "User info response:\n\n%s"

        const val WAITING_ID_TOKEN_TEXT = "Sending authCode back to backend and waiting for response..."
        const val ERROR_ID_TOKEN_TEXT = "Error :\n%s"
    }
}