package com.listasmart.cupons.models;

import com.google.gson.annotations.SerializedName;

/**
 * Mercado retornado pela MockAPI via Retrofit e armazenado no SQLite.
 */
public class Market {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public Market() {
    }

    public Market(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}
