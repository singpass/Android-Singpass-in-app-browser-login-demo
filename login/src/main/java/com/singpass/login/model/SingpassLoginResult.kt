package com.singpass.login.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SingpassLoginResult(
    val state: String,
    val code: String,
    val error: String? = null,
    val userinfoError: String? = null
): Parcelable {
    companion object {
        fun createErrorObject(error: String): SingpassLoginResult {
            return SingpassLoginResult("", "",  error)
        }
    }
}
