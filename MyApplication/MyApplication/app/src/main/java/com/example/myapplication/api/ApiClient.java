package com.example.myapplication.api;

import com.example.myapplication.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import javax.net.ssl.*;
import java.security.cert.CertificateException;

public class ApiClient {
    private static ApiService apiService;
    
    public static ApiService getApiService() {
        if (apiService == null) {
            // 로깅 인터셉터 (디버깅용)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // OkHttpClient 설정 (SSL 검증 비활성화 - 개발용)
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                    .build();
            
            // Retrofit 인스턴스 생성
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
    
    // SSL 검증 비활성화 헬퍼 메소드들 (개발용)
    private static X509TrustManager getUnsafeTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
            
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
            
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
        };
    }
    
    private static SSLSocketFactory getUnsafeSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{getUnsafeTrustManager()}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}