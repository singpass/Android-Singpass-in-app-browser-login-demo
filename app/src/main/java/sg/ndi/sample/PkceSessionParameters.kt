package sg.ndi.sample

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PkceSessionParameters(
    val session_id: String,
    val code_challenge: String,
    val code_challenge_method: String,
    val nonce: String? = null,
    val state: String? = null
) {
    var isMyInfo: Boolean = false
}
