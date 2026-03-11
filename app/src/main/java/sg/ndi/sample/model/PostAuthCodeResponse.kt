package sg.ndi.sample.model

import kotlinx.serialization.Serializable

@Serializable
data class PostAuthCodeResponse(
    val uid: String? = null,
    val sessionToken: String,
    val userInfo: String? = null,
    val userInfoError: String? = null
)
