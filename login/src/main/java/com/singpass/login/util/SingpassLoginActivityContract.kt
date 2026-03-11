package com.singpass.login.util

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import com.singpass.login.activity.SingpassLoginHeadlessActivity
import com.singpass.login.model.LoginParams
import com.singpass.login.model.SingpassLoginResult

class SingpassLoginActivityContract: ActivityResultContract<LoginParams, SingpassLoginResult>() {

    override fun createIntent(context: Context, input: LoginParams): Intent {
        return Intent(context, SingpassLoginHeadlessActivity::class.java).apply {
            putExtra(SingpassLoginHeadlessActivity.SINGPASS_LOGIN_PARAM, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SingpassLoginResult {
        if (intent == null) {
            val codeState = SingpassLoginDatastore.getMemoryCodeState()
            return if (codeState == null) {
                SingpassLoginResult.createErrorObject("Result intent is null!")
            } else {
                SingpassLoginResult(state = codeState.second, code = codeState.first)
            }
        }
        val data = IntentCompat.getParcelableExtra(
            intent,
            SingpassLoginHeadlessActivity.SINGPASS_AUTH_RESULT,
            SingpassLoginResult::class.java)

        return data ?: SingpassLoginResult.createErrorObject("Intent parcelable extra is null!")
    }
}
