package com.listasmart.cupons.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.listasmart.cupons.R;
import com.listasmart.cupons.adapters.HistoryAdapter;
import com.listasmart.cupons.adapters.LeaderboardAdapter;
import com.listasmart.cupons.database.DBHelper;
import com.listasmart.cupons.helpers.DateHelper;
import com.listasmart.cupons.helpers.GamificationHelper;
import com.listasmart.cupons.helpers.SessionManager;
import com.listasmart.cupons.models.Contribution;
import com.listasmart.cupons.models.LeaderboardUser;
import com.listasmart.cupons.models.Market;
import com.listasmart.cupons.models.Product;
import com.listasmart.cupons.network.ApiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela principal: header fixo + três abas (Escanear, Manual, Perfil) alternadas
 * por visibilidade, e bottom navigation. Concentra a navegação interna,
 * o consumo da MockAPI (Retrofit), o cache/CRUD no SQLite e a gamificação.
 */
public class MainActivity extends AppCompatActivity {

    private DBHelper db;
    private SessionManager session;

    // Conteúdos das abas
    private View contentScan, contentManual, contentProfile;

    // Tabs (bottom nav)
    private LinearLayout tabScan, tabManual, tabProfile;
    private TextView tabScanLabel, tabManualLabel, tabProfileLabel;
    private ImageView tabScanIcon, tabManualIcon, tabProfileIcon;

    // Aba manual
    private Spinner spinnerProduct, spinnerMarket;
    private EditText inputProductOther, inputMarketOther, inputPrice;
    private TextView inputDate;
    private LinearLayout manualSuccessAlert;
    private String selectedDateIso;

    // Aba scan
    private LinearLayout scanSuccessAlert, scanResultBox;
    private TextView scanResultData, scanSuccessText;

    private static final String OTHER = "Outro";

    // Launcher do scanner (recebe o resultado do QR)
    private final ActivityResultLauncher<Intent> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String qr = result.getData().getStringExtra(ScannerActivity.EXTRA_QR_RESULT);
                            onQrScanned(qr);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        session = new SessionManager(this);

        bindViews();
        setupTabs();
        setupScan();
        setupManual();
        loadCatalog();        // Retrofit -> SQLite (apenas na 1ª carga)
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
            new AlertDialog.Builder(this)
                    .setTitle("Leitura inválida")
                    .setMessage("Não foi possível ler o conteúdo do QR Code. Tente novamente.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Persiste a contribuição no SQLite
        Contribution c = new Contribution();
        c.setType(Contribution.TYPE_QR);
        c.setRawData(rawValue.trim());
        c.setSubmittedAt(System.currentTimeMillis());
        c.setPoints(GamificationHelper.POINTS_QR);
        db.insertContribution(c);

        // Feedback visual
        scanResultData.setText(rawValue.trim());
        scanResultBox.setVisibility(View.VISIBLE);
        scanSuccessText.setText("Cupom lido com sucesso! +" + GamificationHelper.POINTS_QR + " pontos");
        scanSuccessAlert.setVisibility(View.VISIBLE);
        scanSuccessAlert.startAnimation(android.view.animation.AnimationUtils
                .loadAnimation(this, android.R.anim.fade_in));
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
                inputProductOther.setVisibility(OTHER.equals(value) ? View.VISIBLE : View.GONE);
            }
        });
        spinnerMarket.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onSelected(String value) {
                inputMarketOther.setVisibility(OTHER.equals(value) ? View.VISIBLE : View.GONE);
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
        if (OTHER.equals(product)) {
            product = inputProductOther.getText().toString().trim();
        }
        // Resolve mercado
        String market = String.valueOf(spinnerMarket.getSelectedItem());
        if (OTHER.equals(market)) {
            market = inputMarketOther.getText().toString().trim();
        }
        String priceStr = inputPrice.getText().toString().trim().replace(",", ".");

        // Validações de campo
        if (product.isEmpty() || product.equals("null")) {
            showError("Selecione ou informe o produto.");
            return;
        }
        if (market.isEmpty() || market.equals("null")) {
            showError("Selecione ou informe o mercado.");
            return;
        }
        if (priceStr.isEmpty()) {
            showError("Informe o preço do produto.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showError("Preço inválido. Use o formato 10.50.");
            return;
        }

        // Validação básica para evitar dados incoerentes
        if (price <= 0) {
            showError("O preço deve ser maior que zero.");
            return;
        }
        if (price > 100000) {
            showError("Preço muito alto. Verifique o valor informado.");
            return;
        }

        Date selected = DateHelper.parseIso(selectedDateIso);
        Date today = new Date();
        if (selected == null || selected.after(today)) {
            showError("A data da compra não pode ser no futuro.");
            return;
        }

        // Persiste no SQLite
        Contribution c = new Contribution();
        c.setType(Contribution.TYPE_MANUAL);
        c.setProduct(product);
        c.setMarket(market);
        c.setPrice(price);
        c.setDate(selectedDateIso);
        c.setSubmittedAt(System.currentTimeMillis());
        c.setPoints(GamificationHelper.POINTS_MANUAL);
        db.insertContribution(c);

        // Feedback + reset
        manualSuccessAlert.setVisibility(View.VISIBLE);
        manualSuccessAlert.startAnimation(android.view.animation.AnimationUtils
                .loadAnimation(this, android.R.anim.fade_in));
        resetManualForm();

        manualSuccessAlert.postDelayed(() ->
                manualSuccessAlert.setVisibility(View.GONE), 3000);
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
                .setTitle("Dados incompletos")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------------------------------------------------------------
    // Catálogo: Retrofit (MockAPI) -> cache no SQLite -> Spinners
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
        ApiClient.getApiService().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.replaceProducts(response.body());
                }
                fetchMarketsFromApi();
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Sem internet/API: cai para um catálogo padrão local.
                seedDefaultCatalog();
                fillSpinnersFromDb();
                Toast.makeText(MainActivity.this,
                        "Sem conexão com a API. Usando catálogo local.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMarketsFromApi() {
        ApiClient.getApiService().getMarkets().enqueue(new Callback<List<Market>>() {
            @Override
            public void onResponse(Call<List<Market>> call, Response<List<Market>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    db.replaceMarkets(response.body());
                }
                fillSpinnersFromDb();
            }

            @Override
            public void onFailure(Call<List<Market>> call, Throwable t) {
                seedDefaultCatalog();
                fillSpinnersFromDb();
            }
        });
    }

    /** Catálogo padrão (fallback) gravado no SQLite quando a API não responde. */
    private void seedDefaultCatalog() {
        List<Product> products = new ArrayList<>();
        String[] p = {"Arroz", "Feijão", "Açúcar", "Café", "Óleo", "Leite", "Pão", "Carne", "Frango", "Ovos"};
        for (int i = 0; i < p.length; i++) products.add(new Product(String.valueOf(i + 1), p[i]));
        db.replaceProducts(products);

        List<Market> markets = new ArrayList<>();
        String[] m = {"Carrefour", "Pão de Açúcar", "Extra", "Dia Supermercado", "Atacadão", "Assaí"};
        for (int i = 0; i < m.length; i++) markets.add(new Market(String.valueOf(i + 1), m[i]));
        db.replaceMarkets(markets);
    }

    private void fillSpinnersFromDb() {
        List<String> productNames = new ArrayList<>();
        for (Product p : db.getAllProducts()) productNames.add(p.getName());
        productNames.add(OTHER);

        List<String> marketNames = new ArrayList<>();
        for (Market m : db.getAllMarkets()) marketNames.add(m.getName());
        marketNames.add(OTHER);

        spinnerProduct.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, productNames));
        spinnerMarket.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, marketNames));
    }

    // ---------------------------------------------------------------
    // Aba Perfil (gamificação + ranking + histórico do SQLite)
    // ---------------------------------------------------------------

    private void refreshProfile() {
        int points = db.sumPoints();
        int contribCount = db.countContributions();

        TextView profileName = contentProfile.findViewById(R.id.profileName);
        TextView profileAvatar = contentProfile.findViewById(R.id.profileAvatar);
        TextView profilePoints = contentProfile.findViewById(R.id.profilePoints);
        TextView profileContribCount = contentProfile.findViewById(R.id.profileContribCount);
        TextView profileRank = contentProfile.findViewById(R.id.profileRank);

        profileName.setText(session.getUserName());
        profileAvatar.setText(session.getInitials());
        profilePoints.setText(String.valueOf(points));
        profileContribCount.setText(String.valueOf(contribCount));

        // Selo de confiabilidade
        TextView badgeLevel = contentProfile.findViewById(R.id.badgeLevel);
        TextView badgeCount = contentProfile.findViewById(R.id.badgeCount);
        TextView progressLabel = contentProfile.findViewById(R.id.progressLabel);
        ProgressBar progressBar = contentProfile.findViewById(R.id.progressBar);

        badgeLevel.setText(GamificationHelper.getBadgeLevel(contribCount));
        badgeCount.setText(contribCount + " contribuições");
        int target = GamificationHelper.getNextLevelTarget(contribCount);
        progressLabel.setText(contribCount + "/" + target);
        progressBar.setProgress(GamificationHelper.getProgressPercent(contribCount));

        // Ranking: usuário atual + concorrentes (da API ou fallback)
        buildLeaderboard(points, contribCount, profileRank);

        // Histórico
        buildHistory();
    }

    private void buildLeaderboard(int points, int contribCount, TextView profileRank) {
        final List<LeaderboardUser> list = new ArrayList<>();
        list.add(new LeaderboardUser(session.getUserName(), points, contribCount,
                session.getInitials(), true));

        ApiClient.getApiService().getLeaderboard().enqueue(new Callback<List<LeaderboardUser>>() {
            @Override
            public void onResponse(Call<List<LeaderboardUser>> call,
                                   Response<List<LeaderboardUser>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    list.addAll(response.body());
                } else {
                    list.addAll(defaultCompetitors());
                }
                renderLeaderboard(list, profileRank);
            }

            @Override
            public void onFailure(Call<List<LeaderboardUser>> call, Throwable t) {
                list.addAll(defaultCompetitors());
                renderLeaderboard(list, profileRank);
            }
        });
    }

    private List<LeaderboardUser> defaultCompetitors() {
        List<LeaderboardUser> l = new ArrayList<>();
        l.add(new LeaderboardUser("Carlos Silva", 450, 45, "CS", false));
        l.add(new LeaderboardUser("Maria Santos", 380, 38, "MS", false));
        l.add(new LeaderboardUser("João Oliveira", 320, 32, "JO", false));
        l.add(new LeaderboardUser("Ana Costa", 210, 21, "AC", false));
        l.add(new LeaderboardUser("Pedro Lima", 180, 18, "PL", false));
        return l;
    }

    private void renderLeaderboard(List<LeaderboardUser> list, TextView profileRank) {
        Collections.sort(list, new Comparator<LeaderboardUser>() {
            @Override
            public int compare(LeaderboardUser a, LeaderboardUser b) {
                return Integer.compare(b.getPoints(), a.getPoints());
            }
        });

        int rank = 1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isCurrentUser()) {
                rank = i + 1;
                break;
            }
        }
        profileRank.setText("#" + rank);

        ListView listLeaderboard = contentProfile.findViewById(R.id.listLeaderboard);
        listLeaderboard.setAdapter(new LeaderboardAdapter(this, list));
    }

    private void buildHistory() {
        List<Contribution> history = db.getAllContributions();
        ListView listHistory = contentProfile.findViewById(R.id.listHistory);
        TextView emptyHistory = contentProfile.findViewById(R.id.emptyHistory);

        if (history.isEmpty()) {
            emptyHistory.setVisibility(View.VISIBLE);
            listHistory.setAdapter(null);
        } else {
            emptyHistory.setVisibility(View.GONE);
            listHistory.setAdapter(new HistoryAdapter(this, history));
        }

        // Logout
        Button btnLogout = contentProfile.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setTitle("Sair")
                .setMessage("Deseja encerrar a sessão?")
                .setPositiveButton("Sair", (d, w) -> {
                    session.logout();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
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
