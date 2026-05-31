package com.nathan.djavarp.game.ui.tab;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;
import com.nathan.djavarp.game.SAMP;

public class TabManager {

    private View rootView;
    private RecyclerView listView;
    private TabAdapter adapter;
    private TextView totalPlayers;
    private boolean visible;

    public TabManager(SAMP activity) {
        rootView = activity.findViewById(R.id.tab_view);
        if (rootView == null) return;

        listView = rootView.findViewById(R.id.tab_list);
        listView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new TabAdapter();
        listView.setAdapter(adapter);

        totalPlayers = rootView.findViewById(R.id.tab_total_players);

        rootView.setOnClickListener(v -> {});
    }

    public void show() {
        if (rootView != null) {
            rootView.setVisibility(View.VISIBLE);
            visible = true;
        }
    }

    public void hide() {
        if (rootView != null) {
            rootView.setVisibility(View.GONE);
            visible = false;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void clear() {
        if (adapter != null) {
            adapter.clear();
        }
        if (totalPlayers != null) {
            totalPlayers.setText("0");
        }
    }

    public void setStat(int id, int color, String name, int score, int ping) {
        if (adapter != null) {
            adapter.addItem(new PlayerData(id, color, name, score, ping));
            if (totalPlayers != null) {
                totalPlayers.setText(String.valueOf(adapter.items.size()));
            }
        }
    }
}
