package com.listasmart.cupons.network;

import com.listasmart.cupons.models.LeaderboardUser;
import com.listasmart.cupons.models.Market;
import com.listasmart.cupons.models.Product;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Endpoints da MockAPI consumidos via Retrofit.
 * Os recursos "products", "markets" e "leaderboard" devem existir
 * no projeto MockAPI configurado em ApiClient.BASE_URL.
 */
public interface ApiService {

    @GET("products")
    Call<List<Product>> getProducts();

    @GET("markets")
    Call<List<Market>> getMarkets();

    @GET("leaderboard")
    Call<List<LeaderboardUser>> getLeaderboard();
}
