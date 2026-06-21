package com.listasmart.cupons.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.listasmart.cupons.R;
import com.listasmart.cupons.adapters.LeaderboardAdapter;
import com.listasmart.cupons.models.LeaderboardUser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Ranking global completo, aberto a partir do Perfil via Shared Element
 * Transition (o card "ranking_card" cresce para ocupar a tela). Somente
 * leitura: lista todos os colaboradores em ordem decrescente de pontos e
 * destaca o ranking do usuário atual no formato "Nome - #posição".
 */
public class RankingActivity extends AppCompatActivity {

    /** Extra: JSON da lista completa do ranking (List&lt;LeaderboardUser&gt;). */
    public static final String EXTRA_RANKING_JSON = "ranking_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> supportFinishAfterTransition());

        List<LeaderboardUser> ranking = parseRanking(getIntent().getStringExtra(EXTRA_RANKING_JSON));

        RecyclerView list = findViewById(R.id.listFullRanking);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new LeaderboardAdapter(this, ranking));

        renderUserSummary(ranking);
    }

    /** Destaca o ranking do usuário atual: "Nome - #posição". */
    private void renderUserSummary(List<LeaderboardUser> ranking) {
        TextView summary = findViewById(R.id.rankingUserSummary);
        for (int i = 0; i < ranking.size(); i++) {
            LeaderboardUser u = ranking.get(i);
            if (u.isCurrentUser()) {
                summary.setText(getString(R.string.ranking_user_summary, u.getName(), i + 1));
                summary.setVisibility(android.view.View.VISIBLE);
                return;
            }
        }
    }

    private List<LeaderboardUser> parseRanking(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<LeaderboardUser>>() {}.getType();
        List<LeaderboardUser> list = new Gson().fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }
}
