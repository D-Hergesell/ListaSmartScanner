package com.listasmart.cupons.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.listasmart.cupons.R;
import com.listasmart.cupons.adapters.HistoryAdapter;
import com.listasmart.cupons.adapters.LeaderboardAdapter;
import com.listasmart.cupons.database.DBHelper;
import com.listasmart.cupons.helpers.DateHelper;
import com.listasmart.cupons.helpers.SessionManager;
import com.listasmart.cupons.models.Contribution;
import com.listasmart.cupons.models.LeaderboardUser;
import com.listasmart.cupons.models.Market;
import com.listasmart.cupons.models.OutboxEntry;
import com.listasmart.cupons.models.Product;
import com.listasmart.cupons.models.UserMe;
import com.listasmart.cupons.network.ApiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela principal: header fixo + três abas (Escanear, Manual, Perfil) alternadas
 * por visibilidade, e bottom navigation. Concentra a navegação interna,
 * o consumo da API (Retrofit), o cache/CRUD no SQLite e a gamificação.
 */
public class MainActivity extends AppCompatActivity {

    private DBHelper db;
    private SessionManager session;

    // Conteúdos das abas
    private View contentScan, contentManual, contentProfile;

    // Tabs (bottom nav)
    private LinearLayout tabScan, tabManual, tabProfile;
    private TextView tabScanLabel, tabManualLabel, tabProfileLabel;

    // Aba manual
    private Spinner spinnerProduct, spinnerMarket;
    private EditText inputProductOther, inputMarketOther, inputPrice;
    private TextView inputDate;
    private LinearLayout manualSuccessAlert;
    private String selectedDateIso;

    // Aba scan
    private LinearLayout scanSuccessAlert, scanResultBox;
    private TextView scanResultData, scanSuccessText;

    // Pull-to-refresh do Perfil
    private SwipeRefreshLayout profileSwipeRefresh;

    // Valor da opção "Outro" nos spinners (carregado de strings.xml em onCreate)
    private String other;

    // Limite de itens exibidos no preview do Perfil (ranking/registros).
    private static final int PROFILE_PREVIEW_LIMIT = 10;

    // Últimas listas completas carregadas, passadas às telas "ver tudo".
    private List<LeaderboardUser> fullLeaderboard = new ArrayList<>();
    private List<Contribution> fullHistory = new ArrayList<>();

    // Launcher do scanner (recebe o resultado do QR)
    private final ActivityResultLauncher<Intent> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String qr = result.getData().getStringExtra(ScannerActivity.EXTRA_QR_RESULT);
                            onQrScanned(qr);
                        }
                    });

    // Tela "ver todos os registros": ao voltar com alteração (edição/exclusão),
    // recarrega o Perfil para refletir pontos/contagem/ranking atualizados.
    private final ActivityResultLauncher<Intent> historyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            refreshProfile();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        session = new SessionManager(this);
        other = getString(R.string.spinner_other);

        bindViews();
        setupTabs();
        setupScan();
        setupManual();
        setupProfileActions();
        loadCatalog();        // Retrofit -> SQLite (apenas na 1ª carga)
        flushOutbox();        // reenvia contribuições pendentes (fila offline)
        showTab(0);           // inicia na aba Escanear
    }

    private void bindViews() {
        contentScan = findViewById(R.id.contentScan);
        contentManual = findViewById(R.id.contentManual);
        contentProfile = findViewById(R.id.contentProfile);

        tabScan = findViewById(R.id.tabScan);
        tabManual = findViewById(R.id.tabManual);
        tabProfile = findViewById(R.id.tabProfile);
        tabScanLabel = findViewById(R.id.tabScanLabel);
        tabManualLabel = findViewById(R.id.tabManualLabel);
        tabProfileLabel = findViewById(R.id.tabProfileLabel);
    }

    // ---------------------------------------------------------------
    // Navegação entre abas
    // ---------------------------------------------------------------

    private void setupTabs() {
        tabScan.setOnClickListener(v -> showTab(0));
        tabManual.setOnClickListener(v -> showTab(1));
        tabProfile.setOnClickListener(v -> showTab(2));
    }

    private void showTab(int index) {
        contentScan.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        contentManual.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        contentProfile.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        // Reset visual das tabs
        int active = ContextCompat.getColor(this, R.color.indigo);
        int inactive = ContextCompat.getColor(this, R.color.gray_600);

        tabScan.setBackground(null);
        tabManual.setBackground(null);
        tabProfile.setBackground(null);
        tabScanLabel.setTextColor(inactive);
        tabManualLabel.setTextColor(inactive);
        tabProfileLabel.setTextColor(inactive);

        if (index == 0) {
            tabScan.setBackgroundResource(R.drawable.bg_badge);
            tabScanLabel.setTextColor(active);
        } else if (index == 1) {
            tabManual.setBackgroundResource(R.drawable.bg_badge);
            tabManualLabel.setTextColor(active);
        } else {
            tabProfile.setBackgroundResource(R.drawable.bg_badge);
            tabProfileLabel.setTextColor(active);
            refreshProfile();   // recarrega dados do SQLite ao abrir o perfil
        }
    }

    // ---------------------------------------------------------------
    // Aba Escanear (QR Code via ScannerActivity + ML Kit)
    // ---------------------------------------------------------------

    private void setupScan() {
        scanSuccessAlert = contentScan.findViewById(R.id.scanSuccessAlert);
        scanSuccessText = contentScan.findViewById(R.id.scanSuccessText);
        scanResultBox = contentScan.findViewById(R.id.scanResultBox);
        scanResultData = contentScan.findViewById(R.id.scanResultData);

        Button btnStart = contentScan.findViewById(R.id.btnStartCamera);
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
            scannerLauncher.launch(intent);
        });
    }

    private void onQrScanned(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            showReadError();
            return;
        }
        final String raw = rawValue.trim();

        // Enfileira na outbox (com chave de idempotência) e tenta enviar agora.
        OutboxEntry entry = OutboxEntry.qr(UUID.randomUUID().toString(), raw);
        entry.setId(db.enqueueOutbox(entry));

        postFromOutbox(entry, new ContribUi() {
            @Override
            public void onSuccess(List<Contribution> created) {
                int gained = 0;
                for (Contribution c : created) gained += c.getPoints();
                // Mostra os dados legíveis do cupom (produto, mercado, preço e
                // data da compra) por item, em vez do link bruto do SEFAZ.
                scanResultData.setText(formatScanResult(created));
                scanResultBox.setVisibility(View.VISIBLE);
                scanSuccessText.setText(getString(R.string.scan_success, gained));
                scanSuccessAlert.setVisibility(View.VISIBLE);
                scanSuccessAlert.startAnimation(android.view.animation.AnimationUtils
                        .loadAnimation(MainActivity.this, android.R.anim.fade_in));
            }

            @Override
            public void onQueued() {
                showQueuedOffline();
            }
        });
    }

    /**
     * Monta os dados legíveis do cupom (uma linha por item): produto, mercado,
     * preço e data da compra — as mesmas informações de um cadastro manual.
     */
    private String formatScanResult(List<Contribution> created) {
        StringBuilder sb = new StringBuilder();
        for (Contribution c : created) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(getString(R.string.hist_manual_detail,
                    c.getProduct(), c.getMarket(), c.getPrice()));
            if (c.getDate() != null && !c.getDate().isEmpty()) {
                sb.append("\n").append(getString(R.string.hist_purchase_date,
                        DateHelper.formatDate(c.getDate())));
            }
        }
        return sb.toString();
    }

    private void showReadError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_invalid_read_title)
                .setMessage(R.string.error_invalid_read_message)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    private void showContributionError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_incomplete_title)
                .setMessage(R.string.error_contribution_failed)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    private void showQueuedOffline() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.queued_offline_title)
                .setMessage(R.string.queued_offline_message)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    /** Callback de UI para uma tentativa de envio de contribuição. */
    private interface ContribUi {
        void onSuccess(List<Contribution> created);
        void onQueued();
    }

    /**
     * Envia uma pendência da outbox ao backend. A chave de idempotência garante
     * que reenvios não dupliquem. Em sucesso (ou rejeição 4xx do servidor) a
     * pendência sai da fila; em falha de rede, permanece para retry.
     * {@code ui} nulo = envio silencioso (flush em segundo plano).
     */
    private void postFromOutbox(OutboxEntry entry, ContribUi ui) {
        ApiClient.getApiService().createContribution(entry.toRequest()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Contribution>> call,
                                   @NonNull Response<List<Contribution>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.deleteOutbox(entry.getId());            // confirmado pela nuvem
                    if (ui != null) ui.onSuccess(response.body());
                } else {
                    // Rejeição do servidor (ex.: validação): não adianta retentar.
                    db.deleteOutbox(entry.getId());
                    if (ui != null) showContributionError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Contribution>> call, @NonNull Throwable t) {
                // Sem conexão: mantém na fila para reenvio posterior (flushOutbox).
                if (ui != null) ui.onQueued();
            }
        });
    }

    /** Reenvia, em silêncio, todas as contribuições pendentes da fila offline. */
    private void flushOutbox() {
        for (OutboxEntry entry : db.getOutbox()) {
            postFromOutbox(entry, null);
        }
    }

    // ---------------------------------------------------------------
    // Aba Manual (formulário + validação + AlertDialog + SQLite)
    // ---------------------------------------------------------------

    private void setupManual() {
        spinnerProduct = contentManual.findViewById(R.id.spinnerProduct);
        spinnerMarket = contentManual.findViewById(R.id.spinnerMarket);
        inputProductOther = contentManual.findViewById(R.id.inputProductOther);
        inputMarketOther = contentManual.findViewById(R.id.inputMarketOther);
        inputPrice = contentManual.findViewById(R.id.inputPrice);
        inputDate = contentManual.findViewById(R.id.inputDate);
        manualSuccessAlert = contentManual.findViewById(R.id.manualSuccessAlert);

        // Data padrão = hoje
        selectedDateIso = DateHelper.todayIso();
        inputDate.setText(selectedDateIso);
        inputDate.setOnClickListener(v -> openDatePicker());

        // Mostra/oculta campo "Outro"
        spinnerProduct.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onSelected(String value) {
                inputProductOther.setVisibility(other.equals(value) ? View.VISIBLE : View.GONE);
            }
        });
        spinnerMarket.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onSelected(String value) {
                inputMarketOther.setVisibility(other.equals(value) ? View.VISIBLE : View.GONE);
            }
        });

        Button btnRegister = contentManual.findViewById(R.id.btnRegisterProduct);
        btnRegister.setOnClickListener(v -> submitManual());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        Date current = DateHelper.parseIso(selectedDateIso);
        if (current != null) c.setTime(current);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDateIso = DateHelper.formatIso(year, month, day);
                    inputDate.setText(selectedDateIso);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // sem datas futuras
        dialog.show();
    }

    private void submitManual() {
        // Resolve produto
        String product = String.valueOf(spinnerProduct.getSelectedItem());
        if (other.equals(product)) {
            product = inputProductOther.getText().toString().trim();
        }
        // Resolve mercado
        String market = String.valueOf(spinnerMarket.getSelectedItem());
        if (other.equals(market)) {
            market = inputMarketOther.getText().toString().trim();
        }
        String priceStr = inputPrice.getText().toString().trim().replace(",", ".");

        // Validações de campo
        if (product.isEmpty() || product.equals("null")) {
            showError(getString(R.string.error_product_required));
            return;
        }
        if (market.isEmpty() || market.equals("null")) {
            showError(getString(R.string.error_market_required));
            return;
        }
        if (priceStr.isEmpty()) {
            showError(getString(R.string.error_price_required));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showError(getString(R.string.error_price_invalid));
            return;
        }

        // Validação básica para evitar dados incoerentes
        if (price <= 0) {
            showError(getString(R.string.error_price_zero));
            return;
        }
        if (price > 100000) {
            showError(getString(R.string.error_price_too_high));
            return;
        }

        Date selected = DateHelper.parseIso(selectedDateIso);
        Date today = new Date();
        if (selected == null || selected.after(today)) {
            showError(getString(R.string.error_date_future));
            return;
        }

        // Enfileira na outbox (com chave de idempotência) e tenta enviar agora.
        OutboxEntry entry = OutboxEntry.manual(UUID.randomUUID().toString(),
                product, market, price, selectedDateIso);
        entry.setId(db.enqueueOutbox(entry));

        postFromOutbox(entry, new ContribUi() {
            @Override
            public void onSuccess(List<Contribution> created) {
                manualSuccessAlert.setVisibility(View.VISIBLE);
                manualSuccessAlert.startAnimation(android.view.animation.AnimationUtils
                        .loadAnimation(MainActivity.this, android.R.anim.fade_in));
                resetManualForm();
                manualSuccessAlert.postDelayed(() ->
                        manualSuccessAlert.setVisibility(View.GONE), 3000);
            }

            @Override
            public void onQueued() {
                resetManualForm();   // dado salvo na fila; libera o formulário
                showQueuedOffline();
            }
        });
    }

    private void resetManualForm() {
        if (spinnerProduct.getAdapter() != null && spinnerProduct.getAdapter().getCount() > 0) {
            spinnerProduct.setSelection(0);
        }
        if (spinnerMarket.getAdapter() != null && spinnerMarket.getAdapter().getCount() > 0) {
            spinnerMarket.setSelection(0);
        }
        inputProductOther.setText("");
        inputProductOther.setVisibility(View.GONE);
        inputMarketOther.setText("");
        inputMarketOther.setVisibility(View.GONE);
        inputPrice.setText("");
        selectedDateIso = DateHelper.todayIso();
        inputDate.setText(selectedDateIso);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_incomplete_title)
                .setMessage(message)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    // ---------------------------------------------------------------
    // Catálogo: Retrofit (API) -> cache no SQLite -> Spinners
    // ---------------------------------------------------------------

    private void loadCatalog() {
        if (db.hasCachedCatalog()) {
            // Já há cache local: usa o SQLite e não depende de internet.
            fillSpinnersFromDb();
            return;
        }
        fetchProductsFromApi();
    }

    private void fetchProductsFromApi() {
        ApiClient.getApiService().getProducts().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.replaceProducts(response.body());
                }
                fetchMarketsFromApi();
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                // Sem internet/API: usa o cache local (se houver) e avisa.
                fillSpinnersFromDb();
                Toast.makeText(MainActivity.this,
                        R.string.toast_no_api, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMarketsFromApi() {
        ApiClient.getApiService().getMarkets().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Market>> call, @NonNull Response<List<Market>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.replaceMarkets(response.body());
                }
                fillSpinnersFromDb();
            }

            @Override
            public void onFailure(@NonNull Call<List<Market>> call, @NonNull Throwable t) {
                fillSpinnersFromDb();
            }
        });
    }

    private void fillSpinnersFromDb() {
        List<String> productNames = new ArrayList<>();
        for (Product p : db.getAllProducts()) productNames.add(p.getName());
        productNames.add(other);

        List<String> marketNames = new ArrayList<>();
        for (Market m : db.getAllMarkets()) marketNames.add(m.getName());
        marketNames.add(other);

        spinnerProduct.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, productNames));
        spinnerMarket.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, marketNames));
    }

    // ---------------------------------------------------------------
    // Aba Perfil (gamificação + ranking + histórico do SQLite)
    // ---------------------------------------------------------------

    private void refreshProfile() {
        // Nome e avatar sempre da sessão local (rápido, sem rede).
        TextView profileName = contentProfile.findViewById(R.id.profileName);
        TextView profileAvatar = contentProfile.findViewById(R.id.profileAvatar);
        profileName.setText(session.getUserName());
        profileAvatar.setText(session.getInitials());

        setupLogout();

        // Pontos/contagem/ranking/selo vêm do backend (fonte da verdade).
        // Sem rede ou sem sessão válida, exibe estado indisponível.
        ApiClient.getApiService().getMe().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserMe> call, @NonNull Response<UserMe> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderProfileFromCloud(response.body());
                } else {
                    showProfileOffline();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserMe> call, @NonNull Throwable t) {
                showProfileOffline();
            }
        });

        loadHistory();
        loadLeaderboard();
    }

    /** Perfil a partir do backend (autoritativo). */
    private void renderProfileFromCloud(UserMe me) {
        TextView profilePoints = contentProfile.findViewById(R.id.profilePoints);
        TextView profileContribCount = contentProfile.findViewById(R.id.profileContribCount);
        TextView profileRank = contentProfile.findViewById(R.id.profileRank);
        TextView badgeLevel = contentProfile.findViewById(R.id.badgeLevel);
        TextView badgeCount = contentProfile.findViewById(R.id.badgeCount);
        TextView progressLabel = contentProfile.findViewById(R.id.progressLabel);
        ProgressBar progressBar = contentProfile.findViewById(R.id.progressBar);

        profilePoints.setText(String.valueOf(me.getPoints()));
        profileContribCount.setText(String.valueOf(me.getContributions()));
        profileRank.setText(getString(R.string.profile_rank, me.getRankingPosition()));
        badgeCount.setText(getString(R.string.contributions_count, me.getContributions()));

        UserMe.Badge badge = me.getBadge();
        if (badge != null) {
            badgeLevel.setText(badge.getCurrentRank());
            progressBar.setProgress(badge.getProgressPercent());
            if (badge.getNextThreshold() != null) {
                progressLabel.setText(getString(R.string.progress_fraction,
                        badge.getPoints(), badge.getNextThreshold()));
            } else {
                progressLabel.setText(badge.getCurrentRank()); // rank máximo
            }
        }
    }

    /**
     * Sem conexão / sessão inválida: não exibe dados de gamificação (o backend
     * é a fonte da verdade). Mostra um indicador de indisponível e avisa.
     */
    private void showProfileOffline() {
        ((TextView) contentProfile.findViewById(R.id.profilePoints)).setText(R.string.placeholder_dash);
        ((TextView) contentProfile.findViewById(R.id.profileContribCount)).setText(R.string.placeholder_dash);
        ((TextView) contentProfile.findViewById(R.id.profileRank)).setText(R.string.placeholder_dash);
        Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
    }

    private void loadLeaderboard() {
        ApiClient.getApiService().getRanking().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<LeaderboardUser>> call,
                                   @NonNull Response<List<LeaderboardUser>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    renderLeaderboardPreview(response.body());
                } else {
                    renderLeaderboardPreview(new ArrayList<>()); // sem ranking disponível
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<LeaderboardUser>> call, @NonNull Throwable t) {
                renderLeaderboardPreview(new ArrayList<>()); // sem conexão: ranking vazio
            }
        });
    }

    /**
     * Preenche o card de ranking com os 10 primeiros colocados. Se o usuário
     * atual estiver fora do top 10, acrescenta sua linha (com o ranking real).
     * O container cresce conforme a quantidade de colaboradores; quando há mais
     * de 10, exibe o atalho "Ver ranking completo".
     */
    private void renderLeaderboardPreview(List<LeaderboardUser> full) {
        fullLeaderboard = full;

        RecyclerView rv = contentProfile.findViewById(R.id.leaderboardContainer);
        TextView seeFull = contentProfile.findViewById(R.id.btnSeeFullRanking);
        if (rv.getLayoutManager() == null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setNestedScrollingEnabled(false); // rola junto com o ScrollView do perfil
        }

        int currentUserIndex = -1;
        for (int i = 0; i < full.size(); i++) {
            if (full.get(i).isCurrentUser()) {
                currentUserIndex = i;
                break;
            }
        }

        int topCount = Math.min(PROFILE_PREVIEW_LIMIT, full.size());
        List<LeaderboardUser> preview = new ArrayList<>(full.subList(0, topCount));
        if (currentUserIndex >= topCount) {
            LeaderboardUser user = full.get(currentUserIndex);
            user.setRank(currentUserIndex + 1); // mantém o ranking verdadeiro
            preview.add(user);
        }

        rv.setAdapter(new LeaderboardAdapter(this, preview));
        seeFull.setVisibility(full.size() > PROFILE_PREVIEW_LIMIT ? View.VISIBLE : View.GONE);
    }

    /** Histórico do backend, com sincronização do cache local (SQLite). */
    private void loadHistory() {
        ApiClient.getApiService().getUserContributions(session.getUserId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Contribution>> call,
                                   @NonNull Response<List<Contribution>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.replaceContributions(response.body()); // espelha a nuvem no cache local
                    renderHistory(response.body());
                } else {
                    renderHistory(db.getAllContributions()); // cache offline
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Contribution>> call, @NonNull Throwable t) {
                renderHistory(db.getAllContributions());
            }
        });
    }

    /**
     * Preenche o card de histórico com os 10 registros mais recentes. O
     * container cresce conforme a quantidade de registros; quando há mais de
     * 10, exibe o botão "Ver todos os registros".
     */
    private void renderHistory(List<Contribution> history) {
        fullHistory = history;

        // Encerra o spinner do pull-to-refresh (o histórico é a última carga visível).
        if (profileSwipeRefresh != null) profileSwipeRefresh.setRefreshing(false);

        RecyclerView rv = contentProfile.findViewById(R.id.historyContainer);
        TextView emptyHistory = contentProfile.findViewById(R.id.emptyHistory);
        Button seeAll = contentProfile.findViewById(R.id.btnSeeAllHistory);

        if (rv.getLayoutManager() == null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setNestedScrollingEnabled(false); // rola junto com o ScrollView do perfil
            DividerItemDecoration divider =
                    new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            android.graphics.drawable.Drawable d =
                    ContextCompat.getDrawable(this, R.drawable.divider_history);
            if (d != null) divider.setDrawable(d);
            rv.addItemDecoration(divider);
        }

        if (history.isEmpty()) {
            emptyHistory.setVisibility(View.VISIBLE);
            seeAll.setVisibility(View.GONE);
            rv.setAdapter(new HistoryAdapter(this, new ArrayList<>()));
            return;
        }

        emptyHistory.setVisibility(View.GONE);

        int count = Math.min(PROFILE_PREVIEW_LIMIT, history.size());
        List<Contribution> preview = new ArrayList<>(history.subList(0, count));
        rv.setAdapter(new HistoryAdapter(this, preview));

        seeAll.setVisibility(history.size() > PROFILE_PREVIEW_LIMIT ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------
    // Telas "ver tudo" (Shared Element Transition)
    // ---------------------------------------------------------------

    private void setupProfileActions() {
        TextView seeFullRanking = contentProfile.findViewById(R.id.btnSeeFullRanking);
        Button seeAllHistory = contentProfile.findViewById(R.id.btnSeeAllHistory);
        seeFullRanking.setOnClickListener(v -> openFullRanking());
        seeAllHistory.setOnClickListener(v -> openFullHistory());

        // Pull-to-refresh: a raiz incluída do perfil é o SwipeRefreshLayout.
        profileSwipeRefresh = (SwipeRefreshLayout) contentProfile;
        profileSwipeRefresh.setColorSchemeResources(R.color.indigo);
        profileSwipeRefresh.setOnRefreshListener(this::refreshProfileWithFlush);
    }

    /**
     * Pull-to-refresh do Perfil: primeiro reenvia a fila offline (outbox) e, só
     * quando todas as pendências forem processadas, recarrega os dados da nuvem.
     * Assim, contribuições feitas offline aparecem na mesma atualização, sem
     * precisar reabrir o app. O spinner é encerrado ao fim do recarregamento
     * do histórico (ver {@link #renderHistory(java.util.List)}).
     */
    private void refreshProfileWithFlush() {
        List<OutboxEntry> pending = db.getOutbox();
        if (pending.isEmpty()) {
            refreshProfile();
            return;
        }
        final int[] remaining = {pending.size()};
        for (OutboxEntry entry : pending) {
            ApiClient.getApiService().createContribution(entry.toRequest()).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<Contribution>> call,
                                       @NonNull Response<List<Contribution>> response) {
                    db.deleteOutbox(entry.getId()); // servidor respondeu: sai da fila
                    onOneSettled();
                }

                @Override
                public void onFailure(@NonNull Call<List<Contribution>> call, @NonNull Throwable t) {
                    onOneSettled(); // sem conexão: permanece na fila para retry
                }

                private void onOneSettled() {
                    if (--remaining[0] == 0) refreshProfile();
                }
            });
        }
    }

    private void openFullRanking() {
        View card = contentProfile.findViewById(R.id.rankingCard);
        Intent intent = new Intent(this, RankingActivity.class);
        intent.putExtra(RankingActivity.EXTRA_RANKING_JSON, new Gson().toJson(fullLeaderboard));
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, card, "ranking_card");
        startActivity(intent, options.toBundle());
    }

    private void openFullHistory() {
        View card = contentProfile.findViewById(R.id.historyCard);
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra(HistoryActivity.EXTRA_HISTORY_JSON, new Gson().toJson(fullHistory));
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, card, "history_card");
        historyLauncher.launch(intent, options);
    }

    private void setupLogout() {
        Button btnLogout = contentProfile.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.btn_logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.btn_logout, (d, w) -> {
                    session.logout();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show());
    }

    /**
     * Listener simplificado para Spinner que entrega apenas o valor selecionado.
     */
    private abstract static class SimpleItemSelected
            implements android.widget.AdapterView.OnItemSelectedListener {
        public abstract void onSelected(String value);

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                   int position, long id) {
            onSelected(String.valueOf(parent.getItemAtPosition(position)));
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
