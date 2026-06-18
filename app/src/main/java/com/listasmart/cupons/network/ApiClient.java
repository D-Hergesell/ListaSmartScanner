package com.listasmart.cupons.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Configuração única do Retrofit (Singleton).
 *
 * BASE_URL aponta para um projeto MockAPI (https://mockapi.io).
 * Troque o subdomínio pelo endpoint gerado no seu painel da MockAPI.
 */
public class ApiClient {

    // Substitua "SEU_PROJETO" pelo identificador do seu projeto MockAPI.
    public static final String BASE_URL = "https://SEU_PROJETO.mockapi.io/api/v1/";

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
