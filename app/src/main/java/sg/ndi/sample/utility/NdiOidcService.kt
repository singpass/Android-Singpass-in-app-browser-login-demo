package sg.ndi.sample.utility

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import sg.ndi.sample.model.AUTH_TYPE
import sg.ndi.sample.model.PostAuthCodeResponse
import sg.ndi.sample.model.SessionParametersResponse

interface NdiOidcService {

    @GET("/rpSampleGeneratePkceCode")
    suspend fun getPkceSessionParameters(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Query("authType") authType: AUTH_TYPE = AUTH_TYPE.SINGPASS
    ): Response<SessionParametersResponse.PkceSessionParametersResponse>

    @POST("/rpSampleReceiveAuthCode")
    @FormUrlEncoded
    suspend fun postAuthCode(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Field("state") state: String,
        @Field("code") code: String,
        @Field("nonce") nonce: String,
        @Field("redirectUri") redirectUri: String
    ): Response<PostAuthCodeResponse>

    @GET("/rpSampleFapiGenerateRequestUri")
    suspend fun getFAPIRequestUri(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Query("authType") authType: AUTH_TYPE = AUTH_TYPE.SINGPASS
    ): Response<SessionParametersResponse.FapiPkceSessionParametersResponse>

    @POST("/rpSampleFapiReceiveAuthCode")
    @FormUrlEncoded
    suspend fun postFAPIAuthCode(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Field("code") code: String,
        @Field("state") state: String,
        @Field("nonce") nonce: String,
        @Field("redirectUri") redirectUri: String
    ): Response<PostAuthCodeResponse>

    @GET("/rpSampleAmIStillLoggedIn")
    suspend fun getAmIStilloggedIn(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Header("Authorization") bearerToken: String
    ): Response<ResponseBody>

}