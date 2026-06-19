package com.listasmart.cupons.network;

import androidx.annotation.Nullable;

import com.listasmart.cupons.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Configuração única do Retrofit (Singleton) apontando para o backend
 * Lista Smart (Spring Boot). Injeta automaticamente o header
 * {@code Authorization: Bearer <token>} quando há sessão ativa.
 */
public class ApiClient {

    // URL do backend NÃO é mais hardcoded aqui: vem de BuildConfig.BASE_URL,
    // que é definido em app/build.gradle a partir de local.properties (API_BASE_URL).
    // Para trocar de ambiente, edite local.properties — nunca este arquivo.
    public static final String BASE_URL = BuildConfig.BASE_URL;

    /** Token JWT da sessão atual; definido pelo SessionManager. */
    @Nullable
    private static volatile String authToken;

    private static Retrofit retrofit;

    private ApiClient() {
    }

    /** Atualiza/limpa o token usado nas próximas requisições. */
    public static void setAuthToken(@Nullable String token) {
        authToken = token;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        okhttp3.Request original = chain.request();
                        String token = authToken;
                        if (token == null || token.isEmpty()) {
                            return chain.proceed(original);
                        }
                        okhttp3.Request authed = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(authed);
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
