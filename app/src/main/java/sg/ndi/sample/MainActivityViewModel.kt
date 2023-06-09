package sg.ndi.sample

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.BrowserDenyList
import net.openid.appauth.browser.Browsers
import net.openid.appauth.browser.VersionRange
import net.openid.appauth.browser.VersionedBrowserMatcher
import org.json.JSONException

class MainActivityViewModel(private val app: Application) : AndroidViewModel(app) {

    private var authService: AuthorizationService? = null
    private var serviceConfiguration: AuthorizationServiceConfiguration? = null
    private var authorizationRequest: AuthorizationRequest? = null

    var authIntent: Intent? = null

    private var sessionVerifier: String = ""
    private var pkceSessionParameters: PkceSessionParameters? = null

    private val _launchAuthorizationWebPage: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val launchAuthorizationWebPage: StateFlow<Boolean>
        get() = _launchAuthorizationWebPage

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

    var _pkceEnabledState = mutableStateOf(true)
        private set
    val pkceEnabledState: State<Boolean>
        get() = _pkceEnabledState

    fun pkceCheckBoxClicked(checked: Boolean) {
        _pkceEnabledState.value = checked
    }

    override fun onCleared() {
        super.onCleared()
        authService?.dispose()
    }

    fun updateAuthCode(authCode: String?) {
        _authCodeState.value = if (authCode.isNullOrBlank()) {
            DEFAULT_AUTH_CODE_TEXT
        } else {
            authCode
        }
    }

    fun consumeAuthorizationWebPageTrigger() {
        _launchAuthorizationWebPage.value = false
    }

    fun enableBackButtons() {
        _buttonEnabledState.value = true
    }

    fun sendAuthCodeToBackend(code: String, state: String?) {

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, ex ->
            Log.e("sendAuthCodeToBackend", "error occurred: ${ex.message}", ex)
            _idTokenState.value = ERROR_ID_TOKEN_TEXT.format(ex.message)
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _idTokenState.value = WAITING_ID_TOKEN_TEXT

            val response = ndiOidcService.postAuthCode(
                code = code,
                state = state,
                sessionId = pkceSessionParameters?.session_id ?: "",
                session_verifier = sessionVerifier,
                redirect_uri = getRedirectUri()
            )

            if (response.isSuccessful) {
                val decToken = response.body() ?: "Empty response!"
                _idTokenState.value =
                    if (state.isNullOrBlank())
                        ACCESS_TOKEN_OBTAINED_TEXT.format(decToken)
                    else ID_TOKEN_OBTAINED_TEXT.format(decToken)
            } else {
                _idTokenState.value = ERROR_ID_TOKEN_TEXT.format("error occurred: ${response.errorBody()?.string()} - ${response.code()}")
            }
            _buttonEnabledState.value = true
        }
    }

    private val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
        val toolbarColor = ContextCompat.getColor(app, R.color.primary)
        setToolbarColor(toolbarColor)
        setSecondaryToolbarColor(toolbarColor)
    }.build()

    private val darkCustomTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
        val toolbarColor = ContextCompat.getColor(app, R.color.grey60)
        setToolbarColor(toolbarColor)
        setSecondaryToolbarColor(toolbarColor)
    }.build()

    private fun getSessionVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(
            code,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun getSessionChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun createAuthorizationServiceIntent(myInfo: Boolean = false) {

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("createAuthorizationServ", throwable.message ?: "error occurred during createAuthorizationServiceIntent")
            _buttonEnabledState.value = true
            updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Error createAuthorizationServiceIntent - (${throwable.message})"))
        }

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {

            _buttonEnabledState.value = false

            updateAuthCode("Getting PKCE params...")
            _idTokenState.value = DEFAULT_ID_TOKEN_TEXT

            sessionVerifier = getSessionVerifier()
            val sessionChallenge = getSessionChallenge(sessionVerifier)

            val response = ndiOidcService.getPkceSessionParameters(
                session_challenge = sessionChallenge,
                myinfo = myInfo,
                requirePkce = _pkceEnabledState.value
            )

            if (response.isSuccessful) {
                pkceSessionParameters = response.body()

                if (pkceSessionParameters != null) {
                    pkceSessionParameters?.isMyInfo = myInfo
                    pkceSessionParameters?.requirePkce = _pkceEnabledState.value
                    pkceSessionParameters?.run {
                        if (myInfo) {
                            createMyInfoAuthServiceIntent(this)
                        } else {
                            createSingpassAuthServiceIntent(this)
                        }
                    }
                } else {
                    _buttonEnabledState.value = true
                    updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Unable to get PKCE params! - (${response.code()})"))
                }
            } else {
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format("Unable to get PKCE params!! - (${response.code()})"))
            }
        }
    }

    private suspend fun setupServiceConfigs(
        serviceConfig: AuthorizationServiceConfiguration,
        pkceSessionParameters: PkceSessionParameters
    ) {

        serviceConfiguration = serviceConfig

        authorizationRequest = createAuthRequest(serviceConfig, pkceSessionParameters)

        Log.d("createAuthServiceIntent", authorizationRequest?.toUri().toString())

        val appAuthConfig = AppAuthConfiguration.Builder()
            .setBrowserMatcher(
                BrowserDenyList(
                    VersionedBrowserMatcher(
                        "com.microsoft.emmx",
                        setOf("Ivy-Rk6ztai_IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy_M5mOPpfjZrd3vOx3t-cA6fVQ=="),
                        true,
                        VersionRange.ANY_VERSION
                    ),
                    VersionedBrowserMatcher(
                        Browsers.SBrowser.PACKAGE_NAME,
                        Browsers.SBrowser.SIGNATURE_SET,
                        true,
                        VersionRange.ANY_VERSION
                    )
                )
            ).build()

//        // code to generate the signature hash set for a browser
//        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//            app.packageManager.getPackageInfo(Browsers.Firefox.PACKAGE_NAME, PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
//        else app.packageManager.getPackageInfo(Browsers.Firefox.PACKAGE_NAME, PackageManager.GET_SIGNING_CERTIFICATES)
//
//        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
//            BrowserDescriptor.generateSignatureHashes(packageInfo.signingInfo.apkContentsSigners)
//        else BrowserDescriptor.generateSignatureHashes(packageInfo.signatures)
//
//        Log.d("edge package info", signatures.joinToString(","))

        authorizationRequest?.let { authRequest ->
            authService = AuthorizationService(app, appAuthConfig).also {
                createAuthIntent(it, authRequest)
            }
        }
    }

    private fun createMyInfoAuthServiceIntent(pkceSessionParameters: PkceSessionParameters) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val (apiVersion, auth) = if (pkceSessionParameters.requirePkce) "v4" to "authorize" else "v3" to "authorise"

                val jsonConfig = "{" +
                    "\"issuer\":\"https://test.api.myinfo.gov.sg\"," +
                    "\"authorizationEndpoint\":\"https://test.api.myinfo.gov.sg/com/$apiVersion/$auth\"," +
                    "\"tokenEndpoint\":\"https://test.api.myinfo.gov.sg/com/$apiVersion/token\"" +
                    "}"

                setupServiceConfigs(
                    serviceConfig = AuthorizationServiceConfiguration.fromJson(jsonConfig),
                    pkceSessionParameters = pkceSessionParameters
                )
            } catch (ex: JSONException) {
                ex.printStackTrace()
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format(ex.message))
            } catch (ex: Exception) {
                ex.printStackTrace()
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format(ex.message))
            }
        }
    }

    private fun createSingpassAuthServiceIntent(pkceSessionParameters: PkceSessionParameters) {

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val json_config = "{" +
                    "\"issuer\":\"https://stg-id.singpass.gov.sg\"," +
                    "\"authorizationEndpoint\":\"https://stg-id.singpass.gov.sg/auth\"," +
                    "\"tokenEndpoint\":\"https://test.api.myinfo.gov.sg/com/v4/token\"" +
                    "}"

                setupServiceConfigs(
                    serviceConfig = AuthorizationServiceConfiguration.fromJson(json_config),
                    pkceSessionParameters = pkceSessionParameters
                )
            } catch (ex: JSONException) {
                ex.printStackTrace()
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format(ex.message))
            } catch (ex: Exception) {
                ex.printStackTrace()
                _buttonEnabledState.value = true
                updateAuthCode(ERROR_AUTH_CODE_TEXT.format(ex.message))
            }
        }
    }

    private var _useCustomSchemeRedirectUri = true
    private fun getRedirectUri(): String {
        return if (_useCustomSchemeRedirectUri)
            app.getString(R.string.custom_scheme_redirect_uri)
        else app.getString(R.string.redirect_uri)
    }

    val radioButtonList = listOf("app scheme", "https scheme")
    val radioButtonState = mutableStateOf(radioButtonList[0])

    fun useHttpsRedirectUri(radioButtonText: String) {
        radioButtonState.value = radioButtonText
        _useCustomSchemeRedirectUri = radioButtonText == radioButtonList[0]
    }

    private suspend fun createAuthRequest(
        serviceConfig: AuthorizationServiceConfiguration,
        pkceSessionParameters: PkceSessionParameters
    ): AuthorizationRequest {

        _buttonEnabledState.value = false

        val client_id = if (pkceSessionParameters.isMyInfo) {
            if (pkceSessionParameters.requirePkce)
                app.getString(R.string.myinfo_client_id)
            else app.getString(R.string.myinfo_client_id_v3)
        } else {
            app.getString(R.string.client_id)
        }

        return AuthorizationRequest.Builder(
            serviceConfig, // discovery doc service config
            client_id, // client_id
            ResponseTypeValues.CODE, // responseType
            Uri.parse(getRedirectUri()) // redirect_uri
        ).apply {

            val additionalParams = mutableMapOf<String, String>()

            if (pkceSessionParameters.isMyInfo) {
                if (pkceSessionParameters.requirePkce) {
                    setScope(app.getString(R.string.myinfo_scope))
                    additionalParams["purpose_id"] = "demonstration"
                    setNonce(null)
                    setState(null)
                } else {
                    setScope(null)
                    setNonce(null)
                    setState(pkceSessionParameters.state)
                    additionalParams["purpose"] = "demonstrating MyInfo APIs"
                    additionalParams["attributes"] = app.getString(R.string.myinfo_scope)
                }
            } else {
                setScope(app.getString(R.string.auth_scope))
                setState(pkceSessionParameters.state)
                setNonce(pkceSessionParameters.nonce)
                if (!_useCustomSchemeRedirectUri) {
                    additionalParams["redirect_uri_https_type"] = "app_claimed_https"
                }
            }

//            additionalParams["app_launch_url"] = "DO-NOT-PUT-THIS-FOR-ANDROID"

            pkceSessionParameters.run {
//                Set code_challenge for code_verifier as appauth library
//                does NOT natively support externally generated code_verifier
                if (requirePkce)
                    setCodeVerifier(code_challenge, code_challenge, code_challenge_method)
                else setCodeVerifier(null, null, null)
            }

            if (additionalParams.isNotEmpty()) {
                setAdditionalParameters(additionalParams)
            }
        }.build()
    }

    private suspend fun createAuthIntent(
        authService: AuthorizationService,
        authRequest: AuthorizationRequest
    ) {
        try {
            // Create the custom tabs intent with CustomTabsIntent.Builder
            // Modify how you want the custom tabs to look using the androidx.browser api
            // This builder will also function to warm up the custom tabs in the background for faster custom tabs launching
            val customTabsIntent = authService.customTabManager.createTabBuilder(authRequest.toUri()).apply {
                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkCustomTabColorSchemeParams)
                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, customTabColorSchemeParams)
                setShowTitle(true)
                setStartAnimations(app, android.R.anim.slide_in_left, android.R.anim.fade_out)
                setExitAnimations(app, android.R.anim.fade_in, android.R.anim.slide_out_right)
            }.build()
            authIntent = authService.getAuthorizationRequestIntent(authRequest, customTabsIntent)
            _launchAuthorizationWebPage.tryEmit(true)
        } catch (e: ActivityNotFoundException) {
            updateAuthCode(ERROR_AUTH_CODE_TEXT.format("No suitable web browser found!"))
            withContext(Dispatchers.Main) {
                Toast.makeText(app, "No suitable web browser found!", Toast.LENGTH_SHORT).show()
            }
            _buttonEnabledState.value = true
        }
    }

    fun reset() {
        _buttonEnabledState.value = true

        _authCodeState.value = DEFAULT_AUTH_CODE_TEXT
        _idTokenState.value = DEFAULT_ID_TOKEN_TEXT

        authService = null
        serviceConfiguration = null
        authorizationRequest = null

        authIntent = null

        sessionVerifier = ""
        pkceSessionParameters = null
    }

    companion object {
        const val DEFAULT_AUTH_CODE_TEXT = "No authCode obtained yet!"
        const val AUTH_CODE_OBTAINED_TEXT = "AuthCode :\n%s"
        const val WAITING_AUTH_CODE_TEXT = "Waiting for authCode..."
        const val ERROR_AUTH_CODE_TEXT = "Error :\n%s"

        const val DEFAULT_ID_TOKEN_TEXT = "No token response obtained yet!"
        const val ID_TOKEN_OBTAINED_TEXT = "ID Token :\n%s"
        const val ACCESS_TOKEN_OBTAINED_TEXT = "Access Token :\n%s"
        const val WAITING_ID_TOKEN_TEXT = "Sending authCode back to backend and waiting for response..."
        const val ERROR_ID_TOKEN_TEXT = "Error :\n%s"
    }
}
