package com.listasmart.cupons.models;

import com.google.gson.annotations.SerializedName;

/**
 * Produto retornado pela API via Retrofit e armazenado no SQLite.
 */
public class Product {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public Product() {
    }

    public Product(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Usado pelo ArrayAdapter do Spinner para exibir o nome.
    @Override
    public String toString() {
        return name;
    }
}
