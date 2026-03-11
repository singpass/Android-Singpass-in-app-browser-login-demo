package sg.ndi.sample.utility

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import sg.ndi.sample.BuildConfig

object RetrofitHelper {

    @Volatile private var INSTANCE: Retrofit? = null

    fun getInstance(): Retrofit {

        val networkJson = Json { ignoreUnknownKeys = true }

        return INSTANCE ?: synchronized(this) {
            val baseUrl =
//                "http://10.10.3.82:5001/gcci01kev4zvb32mbn80hn45v2dqjx/asia-southeast1/"
              "https://asia-southeast1-gcci01kev4zvb32mbn80hn45v2dqjx.cloudfunctions.net"
            INSTANCE ?: Retrofit.Builder().baseUrl(baseUrl)
                .client(createOKHttpClient())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(networkJson.asConverterFactory("application/json; charset=utf-8".toMediaType()))
                .build()
                .also { INSTANCE = it }
        }
    }

    private fun createOKHttpClient(): OkHttpClient {

        val okhttpLogger = createHttpLoggingInterceptor()

        return OkHttpClient.Builder().apply {
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            okhttpLogger?.run {
                addInterceptor(this)
            }
        }.build()
    }

    private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor? {
        return if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor { message -> Log.d("OKHTTP interceptor", message) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        } else null
    }
}
