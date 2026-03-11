package com.singpass.login.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.singpass.login.model.AuthState
import com.singpass.login.model.LoginParams
import com.singpass.login.model.SingpassLoginResult
import com.singpass.login.util.SingpassLoginDatastore
import com.singpass.login.viewmodel.SingpassLoginHeadlessActivityViewModel
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class SingpassLoginHeadlessActivity : ComponentActivity() {

    private val viewModel: SingpassLoginHeadlessActivityViewModel by viewModels()

    private lateinit var authActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var authTabActivityLauncher: ActivityResultLauncher<Intent>

    private fun checkIfIntentIsAuthTab() {

        val redirectUriHost = when(val params = viewModel.getAuthorizationParams()) {
            is LoginParams.SingpassFapiLoginParam -> params.redirectUri.host
            is LoginParams.SingpassLoginParam -> params.redirectUri.host
            null -> null
        }

        if (intent != null && !redirectUriHost.isNullOrBlank()) {
            val data = intent.data ?: return
            val host = data.host ?: return
            if (host.startsWith(redirectUriHost)) {
                handleAuthTabUri(data, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.enableEdgeToEdge(window)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets -> insets }

        checkIfIntentIsAuthTab()

        createAuthTabActivityResultLauncher()
        createAppAuthActivityResultLauncher()
        createCustomTabActivityResultLauncher()

        val singpassLoginParams = IntentCompat.getParcelableExtra(intent,SINGPASS_LOGIN_PARAM, LoginParams::class.java)

        viewModel.createCustomTabColorSchemes(singpassLoginParams)
        viewModel.createAuthTabColorSchemes(singpassLoginParams)

        if (singpassLoginParams == null) {
            finishWithError("Invalid params")
        } else {
            viewModel.setAuthorizationParams(singpassLoginParams)

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.authState.collect {
                        when (it) {
                            is AuthState.AppAuth if (!it.consumed) -> {
                                viewModel.consumeAuthEvents()
                                authActivityLauncher.launch(it.customTabsIntent)
                            }
                            is AuthState.AuthTab if (!it.consumed) -> {
                                viewModel.consumeAuthEvents()
                                it.authTabIntent.launch(
                                    authTabActivityLauncher,
                                    it.authorizationEndpointUri,
                                    it.redirectUri.host ?: "",
                                    it.redirectUri.path ?: ""
                                )
                            }
                            is AuthState.Error -> finishWithError(it.error)
                            AuthState.INITIAL -> Unit
                            AuthState.STARTED -> Unit
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun createAuthTabActivityResultLauncher() {
        authTabActivityLauncher = AuthTabIntent.registerActivityResultLauncher(this) { authResult ->
            when (authResult.resultCode) {
                AuthTabIntent.RESULT_OK -> {
                    val uri = authResult.resultUri
                    if (uri == null) {
                        finishWithError("redirect Uri is null!")
                        return@registerActivityResultLauncher
                    }
                    handleAuthTabUri(uri)
                }
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> {
                    finishWithError("Timeout!")
                }
                AuthTabIntent.RESULT_CANCELED -> {
                    finishWithError("User cancelled!")
                }
                AuthTabIntent.RESULT_UNKNOWN_CODE -> {
                    finishWithError("Unknown error!")
                }
                AuthTabIntent.RESULT_VERIFICATION_FAILED -> {
                    finishWithError("Verification failed!")
                }
                else -> {
                    finishWithError("Unknown result code (${authResult.resultCode})!")
                }
            }
        }
    }

    private fun createAppAuthActivityResultLauncher() {
        authActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                val resp = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                if (ex != null) {
                    Log.d("ActivityResult", "exception message = ${ex.message}")
                    Log.d("ActivityResult", "exception code = ${ex.code}")
                    Log.d("ActivityResult", "exception errorDescription = ${ex.errorDescription}")
                    Log.d("ActivityResult", "exception errorUri = ${ex.errorUri}")
                    Log.d("ActivityResult", "exception type = ${ex.type}")
                    Log.d("ActivityResult", "exception toJsonString = ${ex.toJsonString()}")

                    finishWithError(ex.errorDescription ?: ex.message ?: ex.error ?: "error code: ${ex.code}, type: ${ex.type}")
                    return@registerForActivityResult
                }

                if (resp != null) {

                    val code = resp.authorizationCode

                    if (code.isNullOrBlank()) {
                        finishWithError("code is missing!")
                        return@registerForActivityResult
                    }

                    val state = resp.state

                    if (state.isNullOrBlank()) {
                        finishWithError("state is missing!")
                        return@registerForActivityResult
                    }

                    finishWithSuccess(
                        state = state,
                        code = code
                    )
                } else {
                    finishWithError("Response is null!")
                }
            }
        }
    }

    private fun createCustomTabActivityResultLauncher() {
        authActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                val resp = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                if (ex != null) {
                    Log.d("ActivityResult", "exception message = ${ex.message}")
                    Log.d("ActivityResult", "exception code = ${ex.code}")
                    Log.d("ActivityResult", "exception errorDescription = ${ex.errorDescription}")
                    Log.d("ActivityResult", "exception errorUri = ${ex.errorUri}")
                    Log.d("ActivityResult", "exception type = ${ex.type}")
                    Log.d("ActivityResult", "exception toJsonString = ${ex.toJsonString()}")

                    finishWithError(ex.errorDescription ?: ex.message ?: ex.error ?: "error code: ${ex.code}, type: ${ex.type}")
                    return@registerForActivityResult
                }

                if (resp != null) {

                    val code = resp.authorizationCode

                    if (code.isNullOrBlank()) {
                        finishWithError("code is missing!")
                        return@registerForActivityResult
                    }

                    val state = resp.state

                    if (state.isNullOrBlank()) {
                        finishWithError("state is missing!")
                        return@registerForActivityResult
                    }

                    finishWithSuccess(
                        state = state,
                        code = code
                    )

                } else {
                    finishWithError("Response is null!")
                }
            }
        }
    }

    private fun finishWithError(error: String) {
        val resultIntent = Intent().apply {
            putExtra(SINGPASS_AUTH_RESULT, SingpassLoginResult.createErrorObject(error))
        }
        viewModel.clearAuthorizationParams()
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("onDestroy", this::class.java.simpleName)
    }

    private fun finishWithSuccess(state: String, code: String) {
        val resultIntent = Intent().apply {
            putExtra(SINGPASS_AUTH_RESULT, SingpassLoginResult(state = state, code = code))
        }

        Log.d("finishWithSuccess", "${resultIntent.hashCode()}")

        viewModel.clearAuthorizationParams()
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startSingpassAuthorizationFlow()
    }

    private fun handleAuthTabUri(uri: Uri, fromOnCreate: Boolean = false) {
        val code = uri.getQueryParameter("code")

        if (code.isNullOrBlank()) {
            finishWithError("code is missing!")
            return
        }

        val state = uri.getQueryParameter("state")

        if (state.isNullOrBlank()) {
            finishWithError("state is missing!")
            return
        }

        if (fromOnCreate) {
            SingpassLoginDatastore.authCodeStateMemoryStore = code to state
        }

        finishWithSuccess(
            state = state,
            code = code
        )
    }

    companion object {
        internal const val SINGPASS_LOGIN_PARAM = "SINGPASS_LOGIN_PARAM"
        internal const val SINGPASS_AUTH_RESULT = "SINGPASS_AUTH_RESULT"
    }
}