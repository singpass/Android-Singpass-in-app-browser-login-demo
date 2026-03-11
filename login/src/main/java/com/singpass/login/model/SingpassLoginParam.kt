package com.singpass.login.model

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.datastore.core.Serializer
import com.singpass.login.util.UriSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.io.OutputStream

@Serializable
@Parcelize
sealed class LoginParams : Parcelable {

    @Keep
    @Serializable
    data class SingpassLoginParam(
//    these are for non-fapi flow
        val nonce: String,
        val state: String,
        val codeChallenge: String,
        val codeChallengeMethod: String,
        val scope: String,
        val clientId: String,
        @Serializable(with = UriSerializer::class) val redirectUri: Uri,
        val useAppAuthAlways: Boolean = false,
        @param:ColorInt val customTabAppBarColor: Int? = null,
        @param:ColorInt val customTabAppBarColorDark: Int? = null,
        @param:ColorInt val customTabNavigationBarColor: Int? = null,
        @param:ColorInt val customTabNavigationBarColorDark: Int? = null
    ) : LoginParams()

    @Keep
    @Serializable
    data class SingpassFapiLoginParam(
        val useAppAuthAlways: Boolean = false,
        val requestUri: String,
        val clientId: String,
        @Serializable(with = UriSerializer::class) val redirectUri: Uri = "www.singpass.gov.sg".toUri(),
        @param:ColorInt val customTabAppBarColor: Int? = null,
        @param:ColorInt val customTabAppBarColorDark: Int? = null,
        @param:ColorInt val customTabNavigationBarColor: Int? = null,
        @param:ColorInt val customTabNavigationBarColorDark: Int? = null
    ) : LoginParams()

    object LoginParamsSerializer : Serializer<LoginParams?> {
        val Json = Json { ignoreUnknownKeys = true }

        override val defaultValue: SingpassFapiLoginParam = SingpassFapiLoginParam(false, "", "", "www.singpass.gov.sg".toUri())

        @OptIn(ExperimentalSerializationApi::class)
        override suspend fun readFrom(input: InputStream): LoginParams? {
            return try {
                Json.decodeFromStream(serializer(), input)
            } catch (serialization: SerializationException) {
                Log.w("SingpassLoginParamSerializer", "Unable to read LoginParams: ${serialization.message}")
                //            throw CorruptionException("Unable to read SingpassLoginParam", serialization)
                null
            }
        }

        override suspend fun writeTo(t: LoginParams?, output: OutputStream) {
            output.write(
                Json.encodeToString(t)
                    .encodeToByteArray()
            )
        }
    }

}

