package com.listasmart.cupons.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.listasmart.cupons.R;
import com.listasmart.cupons.models.LeaderboardUser;

import java.util.List;

/**
 * Adapter do ranking de colaboradores (ListView da tela de Perfil).
 */
public class LeaderboardAdapter extends BaseAdapter {

    private final Context context;
    private final List<LeaderboardUser> users;

    public LeaderboardAdapter(Context context, List<LeaderboardUser> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_leaderboard, parent, false);
        }

        LeaderboardUser user = users.get(position);

        LinearLayout root = convertView.findViewById(R.id.rowRoot);
        TextView position_ = convertView.findViewById(R.id.rankPosition);
        TextView avatar = convertView.findViewById(R.id.rankAvatar);
        TextView name = convertView.findViewById(R.id.rankName);
        TextView contribs = convertView.findViewById(R.id.rankContribs);
        TextView points = convertView.findViewById(R.id.rankPoints);

        position_.setText(String.valueOf(position + 1));
        avatar.setText(user.getAvatar());
        name.setText(user.getName());
        contribs.setText(context.getString(R.string.contributions_count, user.getContributions()));
        points.setText(String.valueOf(user.getPoints()));

        // Destaque visual para o usuário atual
        if (user.isCurrentUser()) {
            root.setBackgroundResource(R.drawable.bg_row_current);
            name.setTextColor(ContextCompat.getColor(context, R.color.indigo));
            avatar.setBackgroundResource(R.drawable.bg_icon_circle);
            avatar.setTextColor(Color.WHITE);
        } else {
            root.setBackgroundResource(R.drawable.bg_row);
            name.setTextColor(ContextCompat.getColor(context, R.color.gray_900));
            avatar.setBackgroundResource(R.drawable.bg_avatar_gray);
            avatar.setTextColor(ContextCompat.getColor(context, R.color.gray_800));
        }

        return convertView;
    }
}
