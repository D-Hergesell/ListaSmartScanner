package com.listasmart.cupons.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.listasmart.cupons.R;
import com.listasmart.cupons.helpers.DateHelper;
import com.listasmart.cupons.models.Contribution;

import java.util.List;

/**
 * Adapter do histórico de contribuições (RecyclerView). Diferencia visualmente
 * leituras de QR e cadastros manuais. O clique em um item é opcional: o Perfil
 * usa apenas para exibição; a tela de histórico completo registra um listener
 * para abrir as ações de editar/excluir.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Contribution c);
    }

    private final Context context;
    private final List<Contribution> items;
    private OnItemClickListener listener;

    public HistoryAdapter(Context context, List<Contribution> items) {
        this.context = context;
        this.items = items;
    }

    /** Define o callback de clique (null = itens não interativos, como no Perfil). */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Contribution c = items.get(position);

        boolean isQr = Contribution.TYPE_QR.equals(c.getType());

        // QR e manual exibem as MESMAS informações: produto, mercado, preço e
        // data da compra. Apenas o ícone/título diferenciam a origem do registro.
        if (isQr) {
            h.iconBox.setBackgroundResource(R.drawable.bg_icon_green);
            h.icon.setImageResource(R.drawable.ic_qr);
            h.title.setText(R.string.hist_qr_title);
        } else {
            h.iconBox.setBackgroundResource(R.drawable.bg_icon_blue);
            h.icon.setImageResource(R.drawable.ic_edit);
            h.title.setText(R.string.hist_manual_title);
        }

        h.detail.setVisibility(View.VISIBLE);
        h.detail.setText(context.getString(R.string.hist_manual_detail,
                c.getProduct(), c.getMarket(), c.getPrice()));

        // Data da compra (campo "date" da contribuição).
        if (c.getDate() != null && !c.getDate().isEmpty()) {
            h.purchaseDate.setVisibility(View.VISIBLE);
            h.purchaseDate.setText(context.getString(R.string.hist_purchase_date,
                    DateHelper.formatDate(c.getDate())));
        } else {
            h.purchaseDate.setVisibility(View.GONE);
        }

        h.points.setText(context.getString(R.string.hist_points, c.getPoints()));
        // Momento da inclusão do registro.
        h.date.setText(context.getString(R.string.hist_submitted_at,
                DateHelper.formatTimestamp(c.getSubmittedAt())));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        final FrameLayout iconBox;
        final ImageView icon;
        final TextView title, detail, purchaseDate, date, points;

        VH(@NonNull View v) {
            super(v);
            iconBox = v.findViewById(R.id.histIconBox);
            icon = v.findViewById(R.id.histIcon);
            title = v.findViewById(R.id.histTitle);
            detail = v.findViewById(R.id.histDetail);
            purchaseDate = v.findViewById(R.id.histPurchaseDate);
            date = v.findViewById(R.id.histDate);
            points = v.findViewById(R.id.histPoints);
        }
    }
}
