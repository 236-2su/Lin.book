package com.example.myapplication.api

import com.example.myapplication.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        private var apiService: ApiService? = null
        
        @JvmStatic
        fun getApiService(): ApiService {
            if (apiService == null) {
                // 로깅 인터셉터 (디버깅용)
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
                
                // OkHttpClient 설정 (SSL 검증 비활성화 + 긴 타임아웃 - 개발용)
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .hostnameVerifier { _, _ -> true }
                    .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(90, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
                
                // Retrofit 인스턴스 생성
                val retrofit = Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                apiService = retrofit.create(ApiService::class.java)
            }
            return apiService!!
        }
        
        // SSL 검증 비활성화 헬퍼 메소드들 (개발용)
        private fun getUnsafeTrustManager(): X509TrustManager {
            return object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            }
        }
        
        private fun getUnsafeSSLSocketFactory(): SSLSocketFactory {
            return try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf<TrustManager>(getUnsafeTrustManager()), java.security.SecureRandom())
                return sslContext.socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        
        // HTTP 폴백을 위한 OkHttpClient (Retrofit과 별도)
        fun createUnsafeOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            
            return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
