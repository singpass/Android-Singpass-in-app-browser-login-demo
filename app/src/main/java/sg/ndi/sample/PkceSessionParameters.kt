package sg.ndi.sample

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PkceSessionParameters(
    val session_id: String,
    val code_challenge: String? = null,
    val code_challenge_method: String? = null,
    val nonce: String? = null,
    val state: String? = null
) {
    var isMyInfo: Boolean = false
    var requirePkce: Boolean = true
}
