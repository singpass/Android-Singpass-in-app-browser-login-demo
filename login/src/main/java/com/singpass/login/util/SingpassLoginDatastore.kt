package com.singpass.login.util

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.dataStore
import com.singpass.login.model.LoginParams
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.singpassLoginParamDataStore: DataStore<LoginParams?> by dataStore(
    fileName = "SingpassLoginParam.json",
    serializer = LoginParams.LoginParamsSerializer
)

object SingpassLoginDatastore {

    internal var authCodeStateMemoryStore: Pair<String, String>? = null
    fun getMemoryCodeState(): Pair<String, String>? {
        return authCodeStateMemoryStore?.run {
            this.copy()
        }.also {
            authCodeStateMemoryStore = null
        }
    }

    internal fun getSingpassLoginParam(context: Context): LoginParams? {
        return try {
            runBlocking {
                context.singpassLoginParamDataStore.data.first()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    internal suspend fun saveSingpassLoginParam(context: Context, singpassLoginParam: LoginParams): Boolean {
        return try {
            context.singpassLoginParamDataStore.updateData { singpassLoginParam }
            true
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    internal suspend fun clearDataStore(context: Context): Boolean {
        return try {
            context.singpassLoginParamDataStore.updateData {
                LoginParams.SingpassLoginParam(
                    nonce = "",
                    state = "",
                    codeChallenge = "",
                    codeChallengeMethod = "",
                    scope = "",
                    clientId = "",
                    redirectUri = "www.singpass.gov.sg".toUri(),
                    useAppAuthAlways = false
                )
            }
            true
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}