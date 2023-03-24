package sg.ndi.sample

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NdiOidcService {

    @GET(BuildConfig.generatePkceParamsPath)
    suspend fun getPkceSessionParameters(
        @Header("Cache-Control") cacheCtl: String = "no-cache",
        @Query("session_challenge") session_challenge: String,
        @Query("session_challenge_method") session_challenge_method: String = "S256",
        @Query("myinfo") myinfo: Boolean = false
    ): Response<PkceSessionParameters>

    @POST(BuildConfig.receiveAuthCodeParams)
    @FormUrlEncoded
    suspend fun postAuthCode(
        @Header("Cache-Control") cacheCtl: String = "no-cache",
        @Field("state") state: String?,
        @Field("code") code: String,
        @Field("session_id") sessionId: String,
        @Field("session_verifier") session_verifier: String,
        @Field("redirect_uri") redirect_uri: String
    ): Response<String>
}
