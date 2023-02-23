package sg.ndi.sample

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "id_token") val idToken: String? = null,
    @Json(name = "refresh_token") val refresh_token: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null
)
