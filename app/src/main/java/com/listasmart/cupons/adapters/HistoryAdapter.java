package com.listasmart.cupons.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.listasmart.cupons.R;
import com.listasmart.cupons.helpers.DateHelper;
import com.listasmart.cupons.models.Contribution;

import java.util.List;

/**
 * Adapter do histórico de contribuições (ListView da tela de Perfil).
 * Diferencia visualmente leituras de QR e cadastros manuais.
 */
public class HistoryAdapter extends BaseAdapter {

    private final Context context;
    private final List<Contribution> items;

    public HistoryAdapter(Context context, List<Contribution> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_history, parent, false);
        }

        Contribution c = items.get(position);

        FrameLayout iconBox = convertView.findViewById(R.id.histIconBox);
        ImageView icon = convertView.findViewById(R.id.histIcon);
        TextView title = convertView.findViewById(R.id.histTitle);
        TextView detail = convertView.findViewById(R.id.histDetail);
        TextView purchaseDate = convertView.findViewById(R.id.histPurchaseDate);
        TextView date = convertView.findViewById(R.id.histDate);
        TextView points = convertView.findViewById(R.id.histPoints);

        boolean isQr = Contribution.TYPE_QR.equals(c.getType());

        // QR e manual exibem as MESMAS informações: produto, mercado, preço e
        // data da compra. (No QR não mostramos mais o link do SEFAZ.) Apenas o
        // ícone/título diferenciam a origem do registro.
        if (isQr) {
            iconBox.setBackgroundResource(R.drawable.bg_icon_green);
            icon.setImageResource(R.drawable.ic_qr);
            title.setText(R.string.hist_qr_title);
        } else {
            iconBox.setBackgroundResource(R.drawable.bg_icon_blue);
            icon.setImageResource(R.drawable.ic_edit);
            title.setText(R.string.hist_manual_title);
        }

        detail.setVisibility(View.VISIBLE);
        detail.setText(context.getString(R.string.hist_manual_detail,
                c.getProduct(), c.getMarket(), c.getPrice()));

        // Data da compra (campo "date" da contribuição).
        if (c.getDate() != null && !c.getDate().isEmpty()) {
            purchaseDate.setVisibility(View.VISIBLE);
            purchaseDate.setText(context.getString(R.string.hist_purchase_date,
                    DateHelper.formatDate(c.getDate())));
        } else {
            purchaseDate.setVisibility(View.GONE);
        }

        points.setText(context.getString(R.string.hist_points, c.getPoints()));
        // Momento da inclusão do registro (mantido).
        date.setText(context.getString(R.string.hist_submitted_at,
                DateHelper.formatTimestamp(c.getSubmittedAt())));

        return convertView;
    }
}
