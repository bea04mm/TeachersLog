package pt.ipt.dama.TeachersLog.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import pt.ipt.dama.TeachersLog.BuildConfig

/**
 * Singleton object to provide and configure the Retrofit network client.
 */
object NetworkClient {
    private const val BASE_URL = BuildConfig.API_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Only log network details in Debug builds to protect user privacy
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    /**
     * Instance of the defined [ApiService].
     */
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
