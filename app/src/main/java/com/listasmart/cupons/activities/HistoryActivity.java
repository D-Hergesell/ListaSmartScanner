package com.listasmart.cupons.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.listasmart.cupons.R;
import com.listasmart.cupons.adapters.HistoryAdapter;
import com.listasmart.cupons.helpers.DateHelper;
import com.listasmart.cupons.models.Contribution;
import com.listasmart.cupons.network.ApiClient;
import com.listasmart.cupons.network.ContributionRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lista completa de registros do usuário (mais recente -> mais antigo), aberta
 * a partir do Perfil via Shared Element Transition (o card "history_card"
 * cresce para ocupar a tela). Permite editar (sem gerar pontos) ou excluir um
 * registro (estorna os pontos no backend). Retorna RESULT_OK quando houve
 * alteração, para o Perfil recarregar pontos/contagem/ranking.
 */
public class HistoryActivity extends AppCompatActivity {

    /** Extra: JSON da lista completa de contribuições (List&lt;Contribution&gt;). */
    public static final String EXTRA_HISTORY_JSON = "history_json";

    private final List<Contribution> items = new ArrayList<>();
    private HistoryAdapter adapter;
    private boolean changed; // marca se algo foi editado/excluído

    private ListView list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithResult());

        list = findViewById(R.id.listFullHistory);
        empty = findViewById(R.id.emptyRecords);

        items.addAll(parseHistory(getIntent().getStringExtra(EXTRA_HISTORY_JSON)));
        adapter = new HistoryAdapter(this, items);
        list.setAdapter(adapter);

        // Toque em um registro abre as ações (editar / excluir).
        list.setOnItemClickListener((parent, view, position, id) ->
                showActionsDialog(items.get(position)));

        refreshEmptyState();
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }

    private void finishWithResult() {
        setResult(changed ? RESULT_OK : RESULT_CANCELED);
        supportFinishAfterTransition();
    }

    private void refreshEmptyState() {
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showActionsDialog(Contribution c) {
        // Registros via QR Code não podem ser editados nem excluídos
        // (origem autoritativa no cupom fiscal).
        if (Contribution.TYPE_QR.equals(c.getType())) {
            Toast.makeText(this, R.string.qr_not_editable, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.record_actions_title)
                .setItems(new CharSequence[]{
                        getString(R.string.btn_edit),
                        getString(R.string.btn_delete)
                }, (d, which) -> {
                    if (which == 0) showEditDialog(c);
                    else confirmDelete(c);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    // ---------------------------------------------------------------
    // Edição (PUT) — não gera pontos
    // ---------------------------------------------------------------

    private void showEditDialog(Contribution c) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_edit_contribution, null);
        EditText editProduct = form.findViewById(R.id.editProduct);
        EditText editMarket = form.findViewById(R.id.editMarket);
        EditText editPrice = form.findViewById(R.id.editPrice);
        TextView editDate = form.findViewById(R.id.editDate);

        editProduct.setText(c.getProduct());
        editMarket.setText(c.getMarket());
        if (c.getPrice() > 0) editPrice.setText(String.valueOf(c.getPrice()));

        final String[] dateIso = {
                c.getDate() != null && !c.getDate().isEmpty() ? c.getDate() : DateHelper.todayIso()
        };
        editDate.setText(dateIso[0]);
        editDate.setOnClickListener(v -> openDatePicker(dateIso[0], iso -> {
            dateIso[0] = iso;
            editDate.setText(iso);
        }));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_record_title)
                .setView(form)
                .setPositiveButton(R.string.btn_save, null) // sobrescrito p/ validar sem fechar
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String product = editProduct.getText().toString().trim();
                    String market = editMarket.getText().toString().trim();
                    String priceStr = editPrice.getText().toString().trim().replace(",", ".");

                    if (product.isEmpty()) {
                        editProduct.setError(getString(R.string.error_product_required));
                        return;
                    }
                    if (market.isEmpty()) {
                        editMarket.setError(getString(R.string.error_market_required));
                        return;
                    }
                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        editPrice.setError(getString(R.string.error_price_invalid));
                        return;
                    }
                    if (price <= 0) {
                        editPrice.setError(getString(R.string.error_price_zero));
                        return;
                    }
                    if (price > 100000) {
                        editPrice.setError(getString(R.string.error_price_too_high));
                        return;
                    }
                    submitEdit(c, product, market, price, dateIso[0], dialog);
                }));
        dialog.show();
    }

    private void submitEdit(Contribution c, String product, String market,
                            double price, String dateIso, AlertDialog dialog) {
        // clientUuid não é exigido na edição, mas mantemos o padrão da API.
        ContributionRequest req = ContributionRequest.manual(
                product, market, price, dateIso, UUID.randomUUID().toString());
        // Preserva o tipo original (QR/manual): a edição nunca altera o tipo.
        req.setType(c.getType());

        ApiClient.getApiService().updateContribution(c.getId(), req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Contribution> call,
                                   @NonNull Response<Contribution> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Contribution updated = response.body();
                    int idx = indexOf(c.getId());
                    if (idx >= 0) items.set(idx, updated);
                    adapter.notifyDataSetChanged();
                    changed = true;
                    dialog.dismiss();
                    Toast.makeText(HistoryActivity.this, R.string.edit_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HistoryActivity.this, R.string.record_action_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Contribution> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, R.string.record_action_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------
    // Exclusão (DELETE) — estorna os pontos
    // ---------------------------------------------------------------

    private void confirmDelete(Contribution c) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message)
                .setPositiveButton(R.string.btn_delete, (d, w) -> submitDelete(c))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void submitDelete(Contribution c) {
        ApiClient.getApiService().deleteContribution(c.getId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    int idx = indexOf(c.getId());
                    if (idx >= 0) items.remove(idx);
                    adapter.notifyDataSetChanged();
                    refreshEmptyState();
                    changed = true;
                    Toast.makeText(HistoryActivity.this, R.string.delete_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HistoryActivity.this, R.string.record_action_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, R.string.record_action_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private int indexOf(long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) return i;
        }
        return -1;
    }

    private interface OnDatePicked {
        void onPicked(String iso);
    }

    private void openDatePicker(String currentIso, OnDatePicked cb) {
        Calendar cal = Calendar.getInstance();
        Date current = DateHelper.parseIso(currentIso);
        if (current != null) cal.setTime(current);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> cb.onPicked(DateHelper.formatIso(year, month, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // sem datas futuras
        dialog.show();
    }

    private List<Contribution> parseHistory(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<Contribution>>() {}.getType();
        List<Contribution> list = new Gson().fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }
}
