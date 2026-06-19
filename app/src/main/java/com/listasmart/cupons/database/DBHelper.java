package com.listasmart.cupons.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.listasmart.cupons.models.Contribution;
import com.listasmart.cupons.models.Market;
import com.listasmart.cupons.models.OutboxEntry;
import com.listasmart.cupons.models.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper central do SQLite. Concentra criação das tabelas e o CRUD
 * de contribuições, produtos e mercados.
 *
 * Tabelas:
 *  - contributions: contribuições do usuário (entidade principal do app)
 *  - products / markets: cache local dos dados vindos da MockAPI (Retrofit)
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "lista_smart.db";
    // v2: tabela outbox (fila offline com chave de idempotência).
    private static final int DB_VERSION = 2;

    // Tabela de contribuições
    public static final String T_CONTRIB = "contributions";
    public static final String C_ID = "id";
    public static final String C_TYPE = "type";
    public static final String C_PRODUCT = "product";
    public static final String C_MARKET = "market";
    public static final String C_PRICE = "price";
    public static final String C_DATE = "date";
    public static final String C_RAW = "raw_data";
    public static final String C_SUBMITTED = "submitted_at";
    public static final String C_POINTS = "points";

    // Tabela de produtos (cache da API)
    public static final String T_PRODUCTS = "products";
    public static final String P_ID = "id";
    public static final String P_NAME = "name";

    // Tabela de mercados (cache da API)
    public static final String T_MARKETS = "markets";
    public static final String M_ID = "id";
    public static final String M_NAME = "name";

    // Fila offline (outbox): contribuições aguardando envio ao backend.
    public static final String T_OUTBOX = "outbox";
    public static final String O_ID = "id";
    public static final String O_UUID = "client_uuid";   // chave de idempotência
    public static final String O_TYPE = "type";
    public static final String O_PRODUCT = "product";
    public static final String O_MARKET = "market";
    public static final String O_PRICE = "price";
    public static final String O_DATE = "date";
    public static final String O_RAW = "raw_data";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_CONTRIB + " (" +
                C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_TYPE + " TEXT, " +
                C_PRODUCT + " TEXT, " +
                C_MARKET + " TEXT, " +
                C_PRICE + " REAL, " +
                C_DATE + " TEXT, " +
                C_RAW + " TEXT, " +
                C_SUBMITTED + " INTEGER, " +
                C_POINTS + " INTEGER)");

        db.execSQL("CREATE TABLE " + T_PRODUCTS + " (" +
                P_ID + " TEXT PRIMARY KEY, " +
                P_NAME + " TEXT)");

        db.execSQL("CREATE TABLE " + T_MARKETS + " (" +
                M_ID + " TEXT PRIMARY KEY, " +
                M_NAME + " TEXT)");

        db.execSQL("CREATE TABLE " + T_OUTBOX + " (" +
                O_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                O_UUID + " TEXT, " +
                O_TYPE + " TEXT, " +
                O_PRODUCT + " TEXT, " +
                O_MARKET + " TEXT, " +
                O_PRICE + " REAL, " +
                O_DATE + " TEXT, " +
                O_RAW + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_CONTRIB);
        db.execSQL("DROP TABLE IF EXISTS " + T_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_MARKETS);
        db.execSQL("DROP TABLE IF EXISTS " + T_OUTBOX);
        onCreate(db);
    }

    // ---------------------------------------------------------------
    // CRUD - Contribuições
    // ---------------------------------------------------------------

    /** CREATE: insere uma nova contribuição e devolve o id gerado. */
    public long insertContribution(Contribution c) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(C_TYPE, c.getType());
        v.put(C_PRODUCT, c.getProduct());
        v.put(C_MARKET, c.getMarket());
        v.put(C_PRICE, c.getPrice());
        v.put(C_DATE, c.getDate());
        v.put(C_RAW, c.getRawData());
        v.put(C_SUBMITTED, c.getSubmittedAt());
        v.put(C_POINTS, c.getPoints());
        long id = db.insert(T_CONTRIB, null, v);
        db.close();
        return id;
    }

    /**
     * Substitui o cache local pelo histórico vindo da nuvem (fonte da verdade).
     * Preserva o id do servidor para que edição/exclusão usem o mesmo identificador.
     */
    public void replaceContributions(List<Contribution> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_CONTRIB, null, null);
        for (Contribution c : items) {
            ContentValues v = new ContentValues();
            v.put(C_ID, c.getId());            // id canônico do backend
            v.put(C_TYPE, c.getType());
            v.put(C_PRODUCT, c.getProduct());
            v.put(C_MARKET, c.getMarket());
            v.put(C_PRICE, c.getPrice());
            v.put(C_DATE, c.getDate());
            v.put(C_RAW, c.getRawData());
            v.put(C_SUBMITTED, c.getSubmittedAt());
            v.put(C_POINTS, c.getPoints());
            db.insert(T_CONTRIB, null, v);
        }
        db.close();
    }

    /** READ: lista todas as contribuições, mais recentes primeiro. */
    public List<Contribution> getAllContributions() {
        List<Contribution> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_CONTRIB, null, null, null, null, null,
                C_SUBMITTED + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Contribution c = new Contribution();
                c.setId(cursor.getLong(cursor.getColumnIndexOrThrow(C_ID)));
                c.setType(cursor.getString(cursor.getColumnIndexOrThrow(C_TYPE)));
                c.setProduct(cursor.getString(cursor.getColumnIndexOrThrow(C_PRODUCT)));
                c.setMarket(cursor.getString(cursor.getColumnIndexOrThrow(C_MARKET)));
                c.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(C_PRICE)));
                c.setDate(cursor.getString(cursor.getColumnIndexOrThrow(C_DATE)));
                c.setRawData(cursor.getString(cursor.getColumnIndexOrThrow(C_RAW)));
                c.setSubmittedAt(cursor.getLong(cursor.getColumnIndexOrThrow(C_SUBMITTED)));
                c.setPoints(cursor.getInt(cursor.getColumnIndexOrThrow(C_POINTS)));
                list.add(c);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /** READ: contagem total de contribuições. */
    public int countContributions() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + T_CONTRIB, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    /** READ: soma de pontos do usuário. */
    public int sumPoints() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(" + C_POINTS + "),0) FROM " + T_CONTRIB, null);
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    /** UPDATE: atualiza uma contribuição existente. */
    public int updateContribution(Contribution c) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(C_PRODUCT, c.getProduct());
        v.put(C_MARKET, c.getMarket());
        v.put(C_PRICE, c.getPrice());
        v.put(C_DATE, c.getDate());
        int rows = db.update(T_CONTRIB, v, C_ID + "=?",
                new String[]{String.valueOf(c.getId())});
        db.close();
        return rows;
    }

    /** DELETE: remove uma contribuição pelo id. */
    public int deleteContribution(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(T_CONTRIB, C_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // ---------------------------------------------------------------
    // CRUD - Produtos / Mercados (cache da MockAPI)
    // ---------------------------------------------------------------

    /** Substitui o cache de produtos pelos dados recebidos da API. */
    public void replaceProducts(List<Product> products) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_PRODUCTS, null, null);
        for (Product p : products) {
            ContentValues v = new ContentValues();
            v.put(P_ID, p.getId());
            v.put(P_NAME, p.getName());
            db.insert(T_PRODUCTS, null, v);
        }
        db.close();
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_PRODUCTS, null, null, null, null, null, P_NAME + " ASC");
        if (cursor.moveToFirst()) {
            do {
                list.add(new Product(
                        cursor.getString(cursor.getColumnIndexOrThrow(P_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(P_NAME))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void replaceMarkets(List<Market> markets) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_MARKETS, null, null);
        for (Market m : markets) {
            ContentValues v = new ContentValues();
            v.put(M_ID, m.getId());
            v.put(M_NAME, m.getName());
            db.insert(T_MARKETS, null, v);
        }
        db.close();
    }

    public List<Market> getAllMarkets() {
        List<Market> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_MARKETS, null, null, null, null, null, M_NAME + " ASC");
        if (cursor.moveToFirst()) {
            do {
                list.add(new Market(
                        cursor.getString(cursor.getColumnIndexOrThrow(M_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(M_NAME))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public boolean hasCachedCatalog() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + T_PRODUCTS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count > 0;
    }

    // ---------------------------------------------------------------
    // Fila offline (outbox)
    // ---------------------------------------------------------------

    /** Enfileira uma contribuição pendente e devolve o id local gerado. */
    public long enqueueOutbox(OutboxEntry e) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(O_UUID, e.getClientUuid());
        v.put(O_TYPE, e.getType());
        v.put(O_PRODUCT, e.getProduct());
        v.put(O_MARKET, e.getMarket());
        v.put(O_PRICE, e.getPrice());
        v.put(O_DATE, e.getDate());
        v.put(O_RAW, e.getRawData());
        long id = db.insert(T_OUTBOX, null, v);
        db.close();
        return id;
    }

    /** Lista as contribuições pendentes, mais antigas primeiro (ordem de envio). */
    public List<OutboxEntry> getOutbox() {
        List<OutboxEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_OUTBOX, null, null, null, null, null, O_ID + " ASC");
        if (cursor.moveToFirst()) {
            do {
                OutboxEntry e = new OutboxEntry();
                e.setId(cursor.getLong(cursor.getColumnIndexOrThrow(O_ID)));
                e.setClientUuid(cursor.getString(cursor.getColumnIndexOrThrow(O_UUID)));
                e.setType(cursor.getString(cursor.getColumnIndexOrThrow(O_TYPE)));
                e.setProduct(cursor.getString(cursor.getColumnIndexOrThrow(O_PRODUCT)));
                e.setMarket(cursor.getString(cursor.getColumnIndexOrThrow(O_MARKET)));
                e.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(O_PRICE)));
                e.setDate(cursor.getString(cursor.getColumnIndexOrThrow(O_DATE)));
                e.setRawData(cursor.getString(cursor.getColumnIndexOrThrow(O_RAW)));
                list.add(e);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /** Remove uma pendência da outbox (após confirmação do backend). */
    public void deleteOutbox(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_OUTBOX, O_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
