package com.singpass.login.model

import android.content.Intent
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent

sealed class AuthState {
    data object STARTED : AuthState()
    data object INITIAL : AuthState()
    data class AuthTab(
        val authTabIntent: AuthTabIntent,
        val authorizationEndpointUri: Uri,
        val redirectUri: Uri,
        val consumed: Boolean = false
    ) : AuthState()
    data class AppAuth(
        val customTabsIntent: Intent,
        val consumed: Boolean = false
    ) : AuthState()

    data class Error(
        val error: String
    ) : AuthState()
}
