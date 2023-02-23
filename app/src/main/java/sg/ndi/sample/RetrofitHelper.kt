package sg.ndi.sample

import android.util.Log
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitHelper {

    @Volatile private var INSTANCE: Retrofit? = null

    fun getInstance(): Retrofit {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Retrofit.Builder().baseUrl(BuildConfig.backendBaseUrl)
                .client(createOKHttpClient())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .also { INSTANCE = it }
        }
    }

    private fun createOKHttpClient(): OkHttpClient  {

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
