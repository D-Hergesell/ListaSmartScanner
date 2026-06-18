package com.listasmart.cupons.network;

import androidx.annotation.Nullable;

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

    // Backend Lista Smart. Em produção, a URL pública do Render
    // (ex.: https://lista-smart-api.onrender.com/). Mantenha a barra final.
    // Para emulador acessando um backend local use http://10.0.2.2:8080/
    public static final String BASE_URL = "https://lista-smart-api.onrender.com/";

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
