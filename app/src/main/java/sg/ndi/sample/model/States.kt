package sg.ndi.sample.model

import com.singpass.login.model.LoginParams

enum class AUTH_TYPE {
    USERINFO, SINGPASS, SFV
}

enum class ListItems {
    SFV_BUTTON, USERINFO_BUTTON, SINGPASS_BUTTON, INSTRUCTION, AUTHCODE, IDTOKEN, APPAUTH_CHECKBOX, FAPI_CHECKBOX
}

sealed class LaunchAuthState {

    data object INITIAL: LaunchAuthState()

    data object LOGGED_OUT: LaunchAuthState()
    data class LAUNCH(val singpassLoginParam: LoginParams, val launched: Boolean = false): LaunchAuthState()
    data class LOGIN_SUCCESS(val uid: String) : LaunchAuthState()
}