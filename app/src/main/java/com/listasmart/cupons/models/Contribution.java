package com.listasmart.cupons.models;

/**
 * Representa uma contribuição feita pelo usuário (leitura de QR ou cadastro manual).
 * É a entidade principal persistida no SQLite.
 */
public class Contribution {

    public static final String TYPE_QR = "qr";
    public static final String TYPE_MANUAL = "manual";

    private long id;
    private String type;      // "qr" ou "manual"
    private String product;   // nome do produto (manual)
    private String market;    // nome do mercado (manual)
    private double price;     // preço informado (manual)
    private String date;      // data da compra (yyyy-MM-dd)
    private String rawData;   // conteúdo bruto do QR Code (qr)
    private long submittedAt; // timestamp de envio
    private int points;       // pontos ganhos

    public Contribution() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
