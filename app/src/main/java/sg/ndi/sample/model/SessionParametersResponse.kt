package sg.ndi.sample.model

import kotlinx.serialization.Serializable

@Serializable
sealed class SessionParametersResponse {

    @Serializable
    data class FapiPkceSessionParametersResponse(
        val state: String,
        val nonce: String,
        val requestUri: String,
        val expiresIn: Int,
        val authType: AUTH_TYPE? = AUTH_TYPE.SINGPASS,
    ) : SessionParametersResponse() {
        companion object {
            fun createInstance(
                fapiPkceSessionParametersResponse: FapiPkceSessionParametersResponse?,
                authType: AUTH_TYPE,
            ): FapiPkceSessionParametersResponse? {

                if (fapiPkceSessionParametersResponse == null) return null

                return FapiPkceSessionParametersResponse(
                    state = fapiPkceSessionParametersResponse.state,
                    nonce = fapiPkceSessionParametersResponse.nonce,
                    authType = authType,
                    requestUri = fapiPkceSessionParametersResponse.requestUri,
                    expiresIn = fapiPkceSessionParametersResponse.expiresIn
                )
            }
        }
    }

    @Serializable
    data class PkceSessionParametersResponse(
        val codeChallenge: String,
        val codeChallengeMethod: String,
        val nonce: String,
        val state: String,
        val authType: AUTH_TYPE? = AUTH_TYPE.SINGPASS
    ) : SessionParametersResponse() {
        companion object {
            fun createInstance(
                pkceSessionParametersResponse: PkceSessionParametersResponse?,
                authType: AUTH_TYPE
            ): PkceSessionParametersResponse? {

                if (pkceSessionParametersResponse == null) return null

                return PkceSessionParametersResponse(
                    codeChallenge = pkceSessionParametersResponse.codeChallenge,
                    codeChallengeMethod = pkceSessionParametersResponse.codeChallengeMethod,
                    nonce = pkceSessionParametersResponse.nonce,
                    state = pkceSessionParametersResponse.state,
                    authType = authType
                )
            }
        }
    }
}

