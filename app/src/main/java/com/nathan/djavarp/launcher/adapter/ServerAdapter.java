package com.nathan.djavarp.launcher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.model.ServerInfo;

import java.util.List;

/**
 * RecyclerView adapter that renders the featured server (large card) at the
 * top and ordinary servers (compact card) underneath.
 */
public class ServerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_FEATURED = 0;
    public static final int VIEW_TYPE_NORMAL = 1;

    public interface OnServerActionListener {
        void onOpenDetails(ServerInfo server);
        void onDelete(ServerInfo server);
    }

    private final List<ServerInfo> servers;
    private final OnServerActionListener listener;

    public ServerAdapter(List<ServerInfo> servers, OnServerActionListener listener) {
        this.servers = servers;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return servers.get(position).isFeatured ? VIEW_TYPE_FEATURED : VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_FEATURED) {
            View v = inflater.inflate(R.layout.card_server_featured, parent, false);
            return new FeaturedVH(v);
        }
        View v = inflater.inflate(R.layout.card_server_item, parent, false);
        return new NormalVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ServerInfo s = servers.get(position);
        if (holder instanceof FeaturedVH) {
            ((FeaturedVH) holder).bind(s, listener);
        } else if (holder instanceof NormalVH) {
            ((NormalVH) holder).bind(s, listener);
        }
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    // ---------- ViewHolders ----------

    static class FeaturedVH extends RecyclerView.ViewHolder {
        final TextView hostname;
        final TextView address;
        final TextView description;
        final TextView playersText;
        final TextView pingText;
        final Chip gamemodeChip;
        final Chip languageChip;
        final ImageView lockIcon;
        final MaterialButton joinBtn;
        final MaterialCardView card;

        FeaturedVH(View v) {
            super(v);
            card = (MaterialCardView) v;
            hostname = v.findViewById(R.id.featured_hostname);
            address = v.findViewById(R.id.featured_address);
            description = v.findViewById(R.id.featured_description);
            playersText = v.findViewById(R.id.featured_players);
            pingText = v.findViewById(R.id.featured_ping);
            gamemodeChip = v.findViewById(R.id.featured_gamemode);
            languageChip = v.findViewById(R.id.featured_language);
            lockIcon = v.findViewById(R.id.featured_lock);
            joinBtn = v.findViewById(R.id.featured_join);
        }

        void bind(final ServerInfo s, final OnServerActionListener l) {
            Context ctx = itemView.getContext();
            hostname.setText(s.hostname == null || s.hostname.isEmpty()
                    ? "Djava RP Official"
                    : s.hostname);
            address.setText(s.getIpPort());
            description.setText(s.description == null || s.description.isEmpty()
                    ? ctx.getString(R.string.featured_description_default)
                    : s.description);
            playersText.setText(s.players + " / " + s.maxPlayers);

            if (s.ping > 0) {
                pingText.setText(s.ping + " ms");
                pingText.setTextColor(ContextCompat.getColor(ctx, pingColor(s.ping)));
            } else {
                pingText.setText("-- ms");
                pingText.setTextColor(ContextCompat.getColor(ctx, R.color.md_status_offline));
            }

            gamemodeChip.setText(s.gamemode == null || s.gamemode.isEmpty() ? "RolePlay" : s.gamemode);
            languageChip.setText(s.language == null || s.language.isEmpty() ? "Indonesia" : s.language);
            lockIcon.setVisibility(s.hasPassword ? View.VISIBLE : View.GONE);

            View.OnClickListener click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (l != null) l.onOpenDetails(s);
                }
            };
            joinBtn.setOnClickListener(click);
            card.setOnClickListener(click);
        }
    }

    static class NormalVH extends RecyclerView.ViewHolder {
        final TextView hostname;
        final TextView address;
        final TextView gamemode;
        final TextView playersText;
        final TextView pingText;
        final ImageView lockIcon;
        final ImageView deleteBtn;
        final ImageView playBtn;
        final MaterialCardView card;

        NormalVH(View v) {
            super(v);
            card = (MaterialCardView) v;
            hostname = v.findViewById(R.id.server_hostname);
            address = v.findViewById(R.id.server_address);
            gamemode = v.findViewById(R.id.server_gamemode);
            playersText = v.findViewById(R.id.server_players);
            pingText = v.findViewById(R.id.server_ping);
            lockIcon = v.findViewById(R.id.server_lock);
            deleteBtn = v.findViewById(R.id.server_delete);
            playBtn = v.findViewById(R.id.server_play);
        }

        void bind(final ServerInfo s, final OnServerActionListener l) {
            Context ctx = itemView.getContext();
            hostname.setText(s.hostname == null || s.hostname.isEmpty()
                    ? s.getIpPort()
                    : s.hostname);
            address.setText(s.getIpPort());
            gamemode.setText(s.gamemode == null ? "" : s.gamemode);
            playersText.setText(s.players + "/" + s.maxPlayers);

            if (s.ping > 0) {
                pingText.setText(s.ping + " ms");
                pingText.setTextColor(ContextCompat.getColor(ctx, pingColor(s.ping)));
            } else {
                pingText.setText("-- ms");
                pingText.setTextColor(ContextCompat.getColor(ctx, R.color.md_status_offline));
            }
            lockIcon.setVisibility(s.hasPassword ? View.VISIBLE : View.GONE);

            View.OnClickListener click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (l != null) l.onOpenDetails(s);
                }
            };
            playBtn.setOnClickListener(click);
            card.setOnClickListener(click);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (l != null) l.onDelete(s);
                }
            });
        }
    }

    private static int pingColor(int ping) {
        if (ping <= 80) return R.color.md_ping_good;
        if (ping <= 150) return R.color.md_ping_med;
        return R.color.md_ping_bad;
    }
}
