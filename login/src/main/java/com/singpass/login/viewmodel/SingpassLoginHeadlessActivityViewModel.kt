package com.singpass.login.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.browser.auth.AuthTabColorSchemeParams
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.singpass.login.BuildConfig
import com.singpass.login.model.AuthState
import com.singpass.login.model.LoginParams
import com.singpass.login.util.SingpassLoginDatastore
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
import org.json.JSONException

internal class SingpassLoginHeadlessActivityViewModel(val app: Application) : AndroidViewModel(app) {

    companion object {
        val browserBlackList = listOf(
            "com.microsoft.emmx",
            "com.huawei.browser",
            "com.huawei.search",
            "com.huawei.hwsearch",
            "com.UCMobile.intl",
            "com.microsoft.bingintl"
        )
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.INITIAL)
    val authState: StateFlow<AuthState>
        get() = _authState

    private var singpassLoginParam: LoginParams? = null

    fun setAuthorizationParams(singpassLoginParam: LoginParams) {
        this.singpassLoginParam = singpassLoginParam
        viewModelScope.launch(Dispatchers.IO) {
            SingpassLoginDatastore.saveSingpassLoginParam(getApplication(), singpassLoginParam)
        }
    }

    fun getAuthorizationParams(): LoginParams? {
        return if (singpassLoginParam == null) {
            singpassLoginParam = SingpassLoginDatastore.getSingpassLoginParam(getApplication())
            singpassLoginParam
        } else singpassLoginParam
    }

    fun clearAuthorizationParams() {
        viewModelScope.launch(Dispatchers.IO) {
            SingpassLoginDatastore.clearDataStore(getApplication())
        }
    }

    var customTabColorSchemes: Pair<CustomTabColorSchemeParams, CustomTabColorSchemeParams> = internalCreateCustomTabColorSchemes()
    var authTabColorSchemes: Pair<AuthTabColorSchemeParams, AuthTabColorSchemeParams> = internalCreateAuthTabColorSchemes()

    private var _authTabNotSupportedOnDevice = true
    val authTabNotSupportedOnDevice
        get() = _authTabNotSupportedOnDevice

    private fun internalCreateCustomTabColorSchemes(
        @ColorInt appBarColor: Int? = null,
        @ColorInt appBarColorDark: Int? = null,
        @ColorInt navBarColor: Int? = null,
        @ColorInt navBarColorDark: Int? = null,
    ): Pair<CustomTabColorSchemeParams, CustomTabColorSchemeParams> {

        val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
            appBarColor?.run {
                setToolbarColor(this)
                Log.d("createCustomTabColorSchemes", "appbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
            navBarColor?.run {
                setNavigationBarColor(this)
                Log.d("createCustomTabColorSchemes", "navbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
        }.build()

        val darkCustomTabColorSchemeParams = CustomTabColorSchemeParams.Builder().apply {
            appBarColorDark?.run {
                setToolbarColor(this)
                Log.d("createCustomTabColorSchemes", "dark appbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
            navBarColorDark?.run {
                setNavigationBarColor(this)
                Log.d("createCustomTabColorSchemes", "dark navbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
        }.build()

        customTabColorSchemes = customTabColorSchemeParams to darkCustomTabColorSchemeParams
        return customTabColorSchemes
    }

    private fun internalCreateAuthTabColorSchemes(
        @ColorInt appBarColor: Int? = null,
        @ColorInt appBarColorDark: Int? = null,
        @ColorInt navBarColor: Int? = null,
        @ColorInt navBarColorDark: Int? = null
    ): Pair<AuthTabColorSchemeParams, AuthTabColorSchemeParams>  {

        val authTabColorSchemeParams = AuthTabColorSchemeParams.Builder().apply {
            appBarColor?.run {
                setToolbarColor(this)
                Log.d("createCustomTabColorSchemes", "appbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
            navBarColor?.run {
                setNavigationBarColor(this)
                Log.d("createCustomTabColorSchemes", "navbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
        }.build()

        val darkAuthTabColorSchemeParams = AuthTabColorSchemeParams.Builder().apply {
            appBarColorDark?.run {
                setToolbarColor(this)
                Log.d("createCustomTabColorSchemes", "dark appbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
            navBarColorDark?.run {
                setNavigationBarColor(this)
                Log.d("createCustomTabColorSchemes", "dark navbar color: ${String.format("#%06X", (0xFFFFFF and this))}")
            }
        }.build()

        return authTabColorSchemeParams to darkAuthTabColorSchemeParams
    }

    fun createCustomTabColorSchemes(loginParams: LoginParams?) {

        if(loginParams == null) return

        when (loginParams) {
            is LoginParams.SingpassFapiLoginParam -> {
                customTabColorSchemes = internalCreateCustomTabColorSchemes(
                    appBarColor = loginParams.customTabAppBarColor,
                    appBarColorDark = loginParams.customTabAppBarColorDark,
                    navBarColor = loginParams.customTabNavigationBarColor,
                    navBarColorDark = loginParams.customTabNavigationBarColorDark
                )
            }
            is LoginParams.SingpassLoginParam -> {
                customTabColorSchemes = internalCreateCustomTabColorSchemes(
                    appBarColor = loginParams.customTabAppBarColor,
                    appBarColorDark = loginParams.customTabAppBarColorDark,
                    navBarColor = loginParams.customTabNavigationBarColor,
                    navBarColorDark = loginParams.customTabNavigationBarColorDark
                )
            }
        }
    }
    fun createAuthTabColorSchemes(loginParams: LoginParams?) {

        if(loginParams == null) return

        when (loginParams) {
            is LoginParams.SingpassFapiLoginParam -> {
                authTabColorSchemes = internalCreateAuthTabColorSchemes(
                    appBarColor = loginParams.customTabAppBarColor,
                    appBarColorDark = loginParams.customTabAppBarColorDark,
                    navBarColor = loginParams.customTabNavigationBarColor,
                    navBarColorDark = loginParams.customTabNavigationBarColorDark
                )
            }
            is LoginParams.SingpassLoginParam -> {
                authTabColorSchemes = internalCreateAuthTabColorSchemes(
                    appBarColor = loginParams.customTabAppBarColor,
                    appBarColorDark = loginParams.customTabAppBarColorDark,
                    navBarColor = loginParams.customTabNavigationBarColor,
                    navBarColorDark = loginParams.customTabNavigationBarColorDark
                )
            }
        }
    }

    private var authService: AuthorizationService? = null

    fun enableDisableActivityAlias(useAuthTab: Boolean) {

        val (appAuthEnableState, authTabEnableState) = if (useAuthTab) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED to PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED to PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (useAuthTab) {
            val appAuthCompName = ComponentName(app.packageName, BuildConfig.LIBRARY_PACKAGE_NAME + ".AppAuthActivityAlias");
            app.packageManager.setComponentEnabledSetting(
                appAuthCompName,
                appAuthEnableState,
                PackageManager.DONT_KILL_APP
            )

            val authTabCompName = ComponentName(app.packageName, BuildConfig.LIBRARY_PACKAGE_NAME + ".AuthTabActivityAlias");
            app.packageManager.setComponentEnabledSetting(
                authTabCompName,
                authTabEnableState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun getAuthTabBrowsers(): String? {

//                    auth tab only works on > android 9, android 9 and below crashes
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val isMotorolaDevice = Build.MANUFACTURER.lowercase() == "motorola"
        if (isMotorolaDevice) return null

        // Get all apps that can handle VIEW intents and Custom Tab service connections.
        val activityIntent = Intent(Intent.ACTION_VIEW, "http://www.example.com".toUri())
        val packageManager = getApplication<Application>().packageManager
        val resolveInfos = packageManager.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)
        val defaultResolveInfo = packageManager.resolveActivity(activityIntent, PackageManager.MATCH_DEFAULT_ONLY)

        // Extract package names from ResolveInfo objects
        val packageNames = mutableListOf<String>()

        resolveInfos.forEach {
            Log.d("getAuthTabBrowsers, Browsers:", it.activityInfo.packageName)
            packageNames.add(it.activityInfo.packageName)
        }

        val filtered = packageNames.filter {
            val authTabSupported = CustomTabsClient.isAuthTabSupported(getApplication(), it)
            val customTabSupported = !CustomTabsClient.getPackageName(getApplication(), listOf(it)).isNullOrBlank()
            val isBlackListed = browserBlackList.contains(it)
            Log.d("getAuthTabBrowsers", "packageName: $it, customtab: $customTabSupported, authtab: $authTabSupported, blacklisted: $isBlackListed")
            customTabSupported && authTabSupported && !isBlackListed
        }

        if (filtered.isNotEmpty()) {
            Log.d("getAuthTabBrowsers, authtab supported:", filtered.joinToString(","))
        } else {
            Log.d("getAuthTabBrowsers, authtab supported:", "false")
        }

        return if (defaultResolveInfo != null) {
            filtered.find { it == defaultResolveInfo.activityInfo.packageName } ?: filtered.firstOrNull()
        } else {
            filtered.firstOrNull()
        }
    }

    fun startSingpassAuthorizationFlow() {

        if (_authState.value !is AuthState.INITIAL) {
            return
        }

        _authState.tryEmit(AuthState.STARTED)

        when (val loginParam = singpassLoginParam) {
            is LoginParams.SingpassFapiLoginParam -> {
                viewModelScope.launch(Dispatchers.IO) {

                    val packageName: String? = getAuthTabBrowsers()
                    _authTabNotSupportedOnDevice = packageName.isNullOrBlank()

                    if (!loginParam.useAppAuthAlways && !packageName.isNullOrBlank()) {

                        Log.d("startSingpassAuthorizationFlow", "using auth tab")

                        val authorizationEndpointUri = "https://stg-id.singpass.gov.sg/fapi/auth".toUri().buildUpon()
                            .appendQueryParameter("client_id", loginParam.clientId)
                            .appendQueryParameter("request_uri", loginParam.requestUri)
                            .appendQueryParameter("redirect_uri_https_type", "app_claimed_https")
                            .build()

                        Log.d("startSingpassAuthorizationFlow", "SingpassFapiLoginParam: $authorizationEndpointUri")

                        val authTabIntent = AuthTabIntent.Builder().apply {
                            authTabColorSchemes.run {
                                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, second)
                                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, first)
                            }
                        }
                            .setEphemeralBrowsingEnabled(!_authTabNotSupportedOnDevice)
                            .build().apply { intent.setPackage(packageName) }

                        val authTab = AuthState.AuthTab(
                            authTabIntent = authTabIntent,
                            authorizationEndpointUri = authorizationEndpointUri,
                            redirectUri = loginParam.redirectUri
                        )

                        enableDisableActivityAlias(true)

                        _authState.tryEmit(authTab)
                    } else {

                        Log.d("startSingpassAuthorizationFlow", "using appAuth")

                        try {
                            val json_config = "{" +
                                "\"issuer\":\"https://stg-id.singpass.gov.sg\"," +
                                "\"authorizationEndpoint\":\"https://stg-id.singpass.gov.sg/fapi/auth\"," +
                                "\"tokenEndpoint\":\"https://stg-id.singpass.gov.sg/token\"" +
                                "}"

                            enableDisableActivityAlias(false)

                            launchAppAuthFapiAuthorization(
                                serviceConfig = AuthorizationServiceConfiguration.fromJson(json_config),
                                client_id = loginParam.clientId,
                                request_uri = loginParam.requestUri,
                                redirect_uri = loginParam.redirectUri
                            )
                        } catch (ex: JSONException) {
                            ex.printStackTrace()
                            _authState.tryEmit(AuthState.Error("Unable to launch browser(AppAuth): ${ex.message}"))
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            _authState.tryEmit(AuthState.Error("Unable to launch browser(AppAuth): ${ex.message}"))
                        }
                    }
                }
            }
            is LoginParams.SingpassLoginParam -> {
                viewModelScope.launch(Dispatchers.IO) {

                    val packageName: String? = getAuthTabBrowsers()
                    _authTabNotSupportedOnDevice = packageName.isNullOrBlank()

                    if (
                        !loginParam.useAppAuthAlways &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        !packageName.isNullOrBlank()
                    ) {

                        Log.d("startSingpassAuthorizationFlow", "using auth tab")

                        val authorizationEndpointUri = "https://stg-id.singpass.gov.sg/auth".toUri().buildUpon()
                            .appendQueryParameter("client_id", loginParam.clientId)
                            .appendQueryParameter("scope", loginParam.scope)
                            .appendQueryParameter("response_type", "code")
                            .appendQueryParameter("redirect_uri", loginParam.redirectUri.toString())
                            .appendQueryParameter("nonce", loginParam.nonce)
                            .appendQueryParameter("state", loginParam.state)
                            .appendQueryParameter("code_challenge", loginParam.codeChallenge)
                            .appendQueryParameter("code_challenge_method", loginParam.codeChallengeMethod)
                            .appendQueryParameter("redirect_uri_https_type", "app_claimed_https")
                            .build()

                        Log.d("createAuthTabServiceIntent", authorizationEndpointUri.toString())

                        val authTabIntent = AuthTabIntent.Builder().apply {
                            authTabColorSchemes.run{
                                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, second)
                                setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, first)
                            }
                        }
                            .setEphemeralBrowsingEnabled(!_authTabNotSupportedOnDevice)
                            .build().apply { intent.setPackage(packageName) }

                        val authTab = AuthState.AuthTab(
                            authTabIntent = authTabIntent,
                            authorizationEndpointUri = authorizationEndpointUri,
                            redirectUri = loginParam.redirectUri
                        )

                        enableDisableActivityAlias(true)

                        _authState.tryEmit(authTab)
                    } else {

                        Log.d("startSingpassAuthorizationFlow", "using appAuth")

                        try {
                            val json_config = "{" +
                                "\"issuer\":\"https://stg-id.singpass.gov.sg\"," +
                                "\"authorizationEndpoint\":\"https://stg-id.singpass.gov.sg/auth\"," +
                                "\"tokenEndpoint\":\"https://stg-id.singpass.gov.sg/token\"" +
                                "}"

                            enableDisableActivityAlias(false)

                            launchAppAuthAuthorization(
                                serviceConfig = AuthorizationServiceConfiguration.fromJson(json_config),
                                nonce = loginParam.nonce,
                                state = loginParam.state,
                                code_challenge = loginParam.codeChallenge,
                                code_challenge_method = loginParam.codeChallengeMethod,
                                client_id = loginParam.clientId,
                                redirect_uri = loginParam.redirectUri,
                                scope = loginParam.scope
                            )
                        } catch (ex: JSONException) {
                            ex.printStackTrace()
                            _authState.tryEmit(AuthState.Error("Unable to launch browser(AppAuth): ${ex.message}"))
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            _authState.tryEmit(AuthState.Error("Unable to launch browser(AppAuth): ${ex.message}"))
                        }
                    }
                }
            }
            null -> _authState.tryEmit(AuthState.Error("Login params are missing!"))
        }

        singpassLoginParam?.run {
        } ?: _authState.tryEmit(AuthState.Error("Login params are missing!"))
    }

    private suspend fun launchAppAuthFapiAuthorization(
        serviceConfig: AuthorizationServiceConfiguration,
        client_id: String,
        request_uri: String,
        redirect_uri: Uri
    ) {

        val authorizationRequest = AuthorizationRequest.Builder(
            serviceConfig, // discovery doc service config
            client_id, // client_id
            ResponseTypeValues.CODE, // responseType
            redirect_uri // redirect_uri
        ).apply {
            val additionalParams = mutableMapOf<String, String>()
            additionalParams["request_uri"] = request_uri
            setState(null)
            setNonce(null)
            setResponseMode(null)
            setCodeVerifier(null, null, null)
            if (additionalParams.isNotEmpty()) {
                setAdditionalParameters(additionalParams)
            }
        }.build()

        Log.d("createAuthServiceIntent", authorizationRequest.toUri().toString())

        val appAuthConfig = AppAuthConfiguration.Builder()
//            .setBrowserMatcher(
//                BrowserAllowList(
//                    VersionedBrowserMatcher(
//                        "com.microsoft.emmx",
//                        setOf("Ivy-Rk6ztai_IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy_M5mOPpfjZrd3vOx3t-cA6fVQ=="),
//                        true,
//                        VersionRange.ANY_VERSION
//                    )
//                ),
////                BrowserDenyList(
////                    VersionedBrowserMatcher(
////                        "com.microsoft.emmx",
////                        setOf("Ivy-Rk6ztai_IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy_M5mOPpfjZrd3vOx3t-cA6fVQ=="),
////                        true,
////                        VersionRange.ANY_VERSION
////                    ),
////                    VersionedBrowserMatcher(
////                        Browsers.SBrowser.PACKAGE_NAME,
////                        Browsers.SBrowser.SIGNATURE_SET,
////                        true,
////                        VersionRange.ANY_VERSION
////                    )
////                )
//            )
            .setBrowserMatcher { browser ->
                !browserBlackList.contains(browser.packageName)
            }
            .build()

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

        val _authService = AuthorizationService(app, appAuthConfig)
        authService = _authService

        try {
            // Create the custom tabs intent with CustomTabsIntent.Builder
            // Modify how you want the custom tabs to look using the androidx.browser api
            // This builder will also function to warm up the custom tabs in the background for faster custom tabs launching
            val customTabsIntent = _authService.customTabManager.createTabBuilder(authorizationRequest.toUri()).apply {
                customTabColorSchemes.run{
                    setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, second)
                    setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, first)
                }
                setShowTitle(true)
                setStartAnimations(app, android.R.anim.slide_in_left, android.R.anim.fade_out)
                setExitAnimations(app, android.R.anim.fade_in, android.R.anim.slide_out_right)
                setInitialActivityHeightPx(400)
                setInitialActivityWidthPx(400)
                setActivitySideSheetBreakpointDp(200)
                setEphemeralBrowsingEnabled(false)
            }.build()
            val intent = _authService.getAuthorizationRequestIntent(authorizationRequest, customTabsIntent)
            _authState.tryEmit(AuthState.AppAuth(intent))
        } catch (e: ActivityNotFoundException) {
            _authState.tryEmit(AuthState.Error("No suitable web browser found!"))
            withContext(Dispatchers.Main) {
                Toast.makeText(app, "No suitable web browser found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun launchAppAuthAuthorization(
        serviceConfig: AuthorizationServiceConfiguration,
        nonce: String,
        state: String,
        code_challenge: String,
        code_challenge_method: String,
        client_id: String,
        redirect_uri: Uri,
        scope: String
    ) {

//        _buttonEnabledState.value = false

        val authorizationRequest = AuthorizationRequest.Builder(
            serviceConfig, // discovery doc service config
            client_id, // client_id
            ResponseTypeValues.CODE, // responseType
            redirect_uri // redirect_uri
        ).apply {

            val additionalParams = mutableMapOf<String, String>()

            setScope(scope)
            setState(state)
            setNonce(nonce)
            additionalParams["redirect_uri_https_type"] = "app_claimed_https"
//            additionalParams["app_launch_url"] = "DO-NOT-PUT-THIS-FOR-ANDROID"

//            Set code_challenge for code_verifier as appauth library
//            does NOT natively support externally generated code_verifier
            setCodeVerifier(code_challenge, code_challenge, code_challenge_method)

            if (additionalParams.isNotEmpty()) {
                setAdditionalParameters(additionalParams)
            }
        }.build()

        Log.d("createAuthServiceIntent", authorizationRequest.toUri().toString())

        val appAuthConfig = AppAuthConfiguration.Builder()
            .setBrowserMatcher { browser ->
                !browserBlackList.contains(browser.packageName)
            }
            .build()

        val _authService = AuthorizationService(app, appAuthConfig)
        authService = _authService

        try {
            // Create the custom tabs intent with CustomTabsIntent.Builder
            // Modify how you want the custom tabs to look using the androidx.browser api
            // This builder will also function to warm up the custom tabs in the background for faster custom tabs launching
            val customTabsIntent = _authService.customTabManager.createTabBuilder(authorizationRequest.toUri()).apply {
                customTabColorSchemes?.run{
                    setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, second)
                    setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, first)
                }
                setShowTitle(true)
                setStartAnimations(app, android.R.anim.slide_in_left, android.R.anim.fade_out)
                setExitAnimations(app, android.R.anim.fade_in, android.R.anim.slide_out_right)
                setInitialActivityHeightPx(400)
                setInitialActivityWidthPx(400)
                setActivitySideSheetBreakpointDp(200)
                setEphemeralBrowsingEnabled(false)
            }.build()
            val intent = _authService.getAuthorizationRequestIntent(authorizationRequest, customTabsIntent)
            _authState.tryEmit(AuthState.AppAuth(intent))
        } catch (e: ActivityNotFoundException) {
            _authState.tryEmit(AuthState.Error("No suitable web browser found!"))
            withContext(Dispatchers.Main) {
                Toast.makeText(app, "No suitable web browser found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun consumeAuthEvents() {
        when(val value = _authState.value) {
            is AuthState.AppAuth -> {
                _authState.tryEmit(
                    AuthState.AppAuth(
                        customTabsIntent = value.customTabsIntent,
                        consumed = true
                    )
                )
            }
            is AuthState.AuthTab -> {
                _authState.tryEmit(
                    AuthState.AuthTab(
                        authTabIntent = value.authTabIntent,
                        authorizationEndpointUri = value.authorizationEndpointUri,
                        redirectUri = value.redirectUri,
                        consumed = true
                    )
                )
            }
            else -> Unit
        }
    }
}