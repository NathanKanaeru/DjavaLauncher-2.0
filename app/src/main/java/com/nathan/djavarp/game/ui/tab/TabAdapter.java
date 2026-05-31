package com.nathan.djavarp.game.ui.tab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;

import java.util.ArrayList;
import java.util.List;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {

    public List<PlayerData> items = new ArrayList<>();

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void addItem(PlayerData data) {
        items.add(data);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tab_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PlayerData data = items.get(position);
        holder.id.setText(String.valueOf(data.id));
        holder.name.setText(data.name);
        holder.name.setTextColor(data.color);
        holder.score.setText(String.valueOf(data.score));
        holder.ping.setText(String.valueOf(data.ping));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView id;
        TextView name;
        TextView score;
        TextView ping;

        ViewHolder(View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.player_id);
            name = itemView.findViewById(R.id.player_name);
            score = itemView.findViewById(R.id.player_score);
            ping = itemView.findViewById(R.id.player_ping);
        }
    }
}
