package com.thinh.aistudybuddy.data.network

import android.util.Log
import android.util.Base64
import com.thinh.aistudybuddy.data.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val HOST = "10.0.2.2"
    private const val DEFAULT_PORT = 3001
    private const val MAX_PORT = 3020
    private const val CONNECT_TIMEOUT_MS = 500
    private const val API_CONNECT_TIMEOUT_SEC = 10L
    private const val API_WRITE_TIMEOUT_SEC = 30L
    private const val API_READ_TIMEOUT_SEC = 90L
    private const val API_CALL_TIMEOUT_SEC = 120L

    var authToken: String? = null
    @Volatile private var baseUrlOverride: String? = null
    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedService: ApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                val isNoAuth = original.header("No-Auth") == "true"

                if (isNoAuth) {
                    requestBuilder.removeHeader("No-Auth")
                } else {
                    authToken?.let {
                        requestBuilder.addHeader("Authorization", "Bearer $it")
                    }
                }

                val response = chain.proceed(requestBuilder.build())
                if (!isNoAuth && response.code == 401) {
                    logAuthTokenDiagnostics("HTTP 401 for ${original.method} ${original.url.encodedPath}")
                }
                response
            }
            .connectTimeout(API_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(API_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(API_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(API_CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    val instance: ApiService
        get() {
            val activeBaseUrl = resolveBaseUrl()
            val cached = cachedService
            if (cached != null && cachedBaseUrl == activeBaseUrl) return cached

            return synchronized(this) {
                val syncedBaseUrl = resolveBaseUrl()
                val syncedCached = cachedService
                if (syncedCached != null && cachedBaseUrl == syncedBaseUrl) return@synchronized syncedCached

                Log.d(TAG, "Using backend baseUrl=$syncedBaseUrl")
                val service = Retrofit.Builder()
                    .baseUrl(syncedBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
                cachedBaseUrl = syncedBaseUrl
                cachedService = service
                service
            }
        }

    fun setBaseUrlOverride(baseUrl: String?) {
        baseUrlOverride = baseUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
            if (it.endsWith("/")) it else "$it/"
        }
        cachedBaseUrl = null
        cachedService = null
    }

    fun resetBaseUrlOverride() {
        setBaseUrlOverride(null)
    }

    fun hasUsableAuthToken(): Boolean = !authToken.isNullOrBlank() && !isAuthTokenExpired()

    fun authTokenLength(): Int = authToken?.length ?: 0

    fun authTokenExpiryEpochSeconds(): Long? {
        val token = authToken?.trim().orEmpty()
        if (token.isBlank()) return null

        val parts = token.split(".")
        if (parts.size < 2) return null

        return runCatching {
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val json = JSONObject(payload)
            if (json.has("exp")) json.getLong("exp") else null
        }.getOrNull()
    }

    fun isAuthTokenExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val exp = authTokenExpiryEpochSeconds() ?: return false
        return exp <= nowEpochSeconds
    }

    fun logAuthTokenDiagnostics(reason: String) {
        val length = authTokenLength()
        val exp = authTokenExpiryEpochSeconds()
        val expired = isAuthTokenExpired()
        Log.w(TAG, "Auth diagnostics: reason=$reason tokenLength=$length exp=$exp expired=$expired")
    }

    private fun resolveBaseUrl(): String {
        baseUrlOverride?.let { return it }

        val openPort = (DEFAULT_PORT..MAX_PORT).firstOrNull { port ->
            runCatching {
                Log.d(TAG, "Probing backend $HOST:$port")
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(HOST, port), CONNECT_TIMEOUT_MS)
                    true
                }
            }.getOrDefault(false)
        } ?: DEFAULT_PORT

        val baseUrl = "http://$HOST:$openPort/"
        Log.d(TAG, "Resolved backend baseUrl=$baseUrl")
        return baseUrl
    }
}