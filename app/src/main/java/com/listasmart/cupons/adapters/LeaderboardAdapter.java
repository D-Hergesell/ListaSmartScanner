package com.listasmart.cupons.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.listasmart.cupons.R;
import com.listasmart.cupons.models.LeaderboardUser;

import java.util.List;

/**
 * Adapter do ranking de colaboradores (RecyclerView). Usado tanto no preview
 * do Perfil quanto na tela de ranking completo, com reciclagem de views.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    private final Context context;
    private final List<LeaderboardUser> users;

    public LeaderboardAdapter(Context context, List<LeaderboardUser> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaderboardUser user = users.get(position);

        // Usa o ranking real quando informado (ex.: usuário fora do top 10);
        // caso contrário, a posição na lista exibida.
        int shownRank = user.getRank() > 0 ? user.getRank() : position + 1;
        h.position.setText(String.valueOf(shownRank));
        h.avatar.setText(user.getAvatar());
        h.name.setText(user.getName());
        // Subtítulo: Selo de Confiabilidade vindo do backend (/ranking).
        h.contribs.setText(user.getBadge() != null ? user.getBadge() : "");
        h.points.setText(String.valueOf(user.getPoints()));

        // Destaque visual para o usuário atual
        if (user.isCurrentUser()) {
            h.root.setBackgroundResource(R.drawable.bg_row_current);
            h.name.setTextColor(ContextCompat.getColor(context, R.color.indigo));
            h.avatar.setBackgroundResource(R.drawable.bg_icon_circle);
            h.avatar.setTextColor(Color.WHITE);
        } else {
            h.root.setBackgroundResource(R.drawable.bg_row);
            h.name.setTextColor(ContextCompat.getColor(context, R.color.gray_900));
            h.avatar.setBackgroundResource(R.drawable.bg_avatar_gray);
            h.avatar.setTextColor(ContextCompat.getColor(context, R.color.gray_800));
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final TextView position, avatar, name, contribs, points;

        VH(@NonNull View v) {
            super(v);
            root = v.findViewById(R.id.rowRoot);
            position = v.findViewById(R.id.rankPosition);
            avatar = v.findViewById(R.id.rankAvatar);
            name = v.findViewById(R.id.rankName);
            contribs = v.findViewById(R.id.rankContribs);
            points = v.findViewById(R.id.rankPoints);
        }
    }
}
