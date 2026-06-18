package com.listasmart.cupons.network;

/**
 * Corpo de POST /contributions e PUT /contributions/{id}.
 *
 * type = "manual": product + market + price + date.
 * type = "qr": rawData (conteúdo bruto do QR Code).
 */
public class ContributionRequest {

    private String type;
    private String product;
    private String market;
    private Double price;   // Double (nullable) para não enviar 0 em QR
    private String date;    // "yyyy-MM-dd"
    private String rawData;

    public ContributionRequest() {
    }

    /** Atalho para contribuição manual. */
    public static ContributionRequest manual(String product, String market,
                                             double price, String date) {
        ContributionRequest r = new ContributionRequest();
        r.type = "manual";
        r.product = product;
        r.market = market;
        r.price = price;
        r.date = date;
        return r;
    }

    /** Atalho para contribuição via QR. */
    public static ContributionRequest qr(String rawData) {
        ContributionRequest r = new ContributionRequest();
        r.type = "qr";
        r.rawData = rawData;
        return r;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
}
