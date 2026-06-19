package com.listasmart.cupons.models;

import com.listasmart.cupons.network.ContributionRequest;

/**
 * Contribuição pendente de envio (fila offline / outbox).
 *
 * <p>Cada entrada carrega a chave de idempotência ({@code clientUuid}) gerada no
 * momento da criação. Enquanto não for confirmada pelo backend, fica guardada no
 * SQLite e é reenviada quando há conexão. Como o servidor deduplica pela chave,
 * reenviar a mesma entrada NUNCA duplica os dados na nuvem.
 */
public class OutboxEntry {

    private long id;            // id local (autoincrement no SQLite)
    private String clientUuid;  // chave de idempotência
    private String type;        // "qr" ou "manual"
    private String product;
    private String market;
    private double price;
    private String date;        // yyyy-MM-dd
    private String rawData;

    public OutboxEntry() {
    }

    /** Cria uma entrada de cadastro manual. */
    public static OutboxEntry manual(String clientUuid, String product, String market,
                                     double price, String date) {
        OutboxEntry e = new OutboxEntry();
        e.clientUuid = clientUuid;
        e.type = "manual";
        e.product = product;
        e.market = market;
        e.price = price;
        e.date = date;
        return e;
    }

    /** Cria uma entrada de leitura de QR. */
    public static OutboxEntry qr(String clientUuid, String rawData) {
        OutboxEntry e = new OutboxEntry();
        e.clientUuid = clientUuid;
        e.type = "qr";
        e.rawData = rawData;
        return e;
    }

    /** Converte a entrada no corpo da requisição (com a chave de idempotência). */
    public ContributionRequest toRequest() {
        if ("qr".equals(type)) {
            return ContributionRequest.qr(rawData, clientUuid);
        }
        return ContributionRequest.manual(product, market, price, date, clientUuid);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }

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
}
