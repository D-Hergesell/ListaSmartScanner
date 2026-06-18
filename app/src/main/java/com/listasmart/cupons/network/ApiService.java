package com.listasmart.cupons.network;

import com.listasmart.cupons.models.Contribution;
import com.listasmart.cupons.models.LeaderboardUser;
import com.listasmart.cupons.models.Market;
import com.listasmart.cupons.models.Product;
import com.listasmart.cupons.models.UserMe;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Endpoints do backend Lista Smart (Spring Boot).
 * O token Bearer é injetado automaticamente pelo AuthInterceptor (ApiClient),
 * exceto em /auth/** e nos catálogos, que são públicos.
 */
public interface ApiService {

    // ---- Autenticação (público) ----
    @POST("auth/register")
    Call<AuthResponse> register(@Body AuthRequest body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body AuthRequest body);

    // ---- Perfil (autenticado) ----
    @GET("users/me")
    Call<UserMe> getMe();

    // ---- Contribuições (autenticado) ----
    /** POST pode retornar VÁRIAS contribuições (1 por item, no caso de QR). */
    @POST("contributions")
    Call<List<Contribution>> createContribution(@Body ContributionRequest body);

    @GET("contributions/user/{id}")
    Call<List<Contribution>> getUserContributions(@Path("id") long userId);

    @PUT("contributions/{id}")
    Call<Contribution> updateContribution(@Path("id") long id, @Body ContributionRequest body);

    @DELETE("contributions/{id}")
    Call<Void> deleteContribution(@Path("id") long id);

    // ---- Catálogo (público) ----
    @GET("products")
    Call<List<Product>> getProducts();

    @GET("markets")
    Call<List<Market>> getMarkets();

    // ---- Ranking (autenticado) ----
    @GET("ranking")
    Call<List<LeaderboardUser>> getRanking();
}
