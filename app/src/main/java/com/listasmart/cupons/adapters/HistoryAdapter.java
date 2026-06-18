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
import java.util.Locale;

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
        TextView date = convertView.findViewById(R.id.histDate);
        TextView points = convertView.findViewById(R.id.histPoints);

        boolean isQr = Contribution.TYPE_QR.equals(c.getType());

        if (isQr) {
            iconBox.setBackgroundResource(R.drawable.bg_icon_green);
            icon.setImageResource(R.drawable.ic_qr);
            title.setText("Leitura de QR Code");
            detail.setVisibility(c.getRawData() != null ? View.VISIBLE : View.GONE);
            detail.setText(c.getRawData());
        } else {
            iconBox.setBackgroundResource(R.drawable.bg_icon_blue);
            icon.setImageResource(R.drawable.ic_edit);
            title.setText("Cadastro Manual");
            detail.setVisibility(View.VISIBLE);
            detail.setText(String.format(Locale.getDefault(),
                    "%s - %s · R$ %.2f", c.getProduct(), c.getMarket(), c.getPrice()));
        }

        points.setText(String.format(Locale.getDefault(), "+%d pts", c.getPoints()));
        date.setText(DateHelper.formatTimestamp(c.getSubmittedAt()));

        return convertView;
    }
}
