package com.nathan.djavarp.launcher.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.adapter.ServerAdapter;
import com.nathan.djavarp.launcher.model.ServerConfig;
import com.nathan.djavarp.launcher.model.ServerInfo;
import com.nathan.djavarp.launcher.util.SampQuery;
import com.nathan.djavarp.launcher.util.ServerConnector;
import com.nathan.djavarp.launcher.util.ServerStore;
import com.nathan.djavarp.launcher.util.SettingsIO;

import org.ini4j.Wini;

import java.util.ArrayList;
import java.util.List;

public class ServerFragment extends Fragment {

    private RecyclerView recycler;
    private SwipeRefreshLayout swipeRefresh;
    private ServerAdapter adapter;
    private final List<ServerInfo> servers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_servers);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_server);
        swipeRefresh.setColorSchemeResources(R.color.md_primary, R.color.md_accent_green);

        adapter = new ServerAdapter(servers, new ServerAdapter.OnServerActionListener() {
            @Override
            public void onOpenDetails(ServerInfo server) {
                showServerDetails(server);
            }

            @Override
            public void onDelete(ServerInfo server) {
                confirmDelete(server);
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        seedList();
        refreshAll();

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAll();
            }
        });
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddServerDialog();
            }
        });
    }

    private String readNickname() {
        try {
            Wini ini = SettingsIO.load(requireContext());
            String n = SettingsIO.getString(ini, "client", "name", "");
            if (n != null && !n.trim().isEmpty()) return n.trim();
        } catch (Exception ignored) {}
        return getString(R.string.home_default_nick);
    }

    private static final String HOSTED_SERVERS_URL = "https://samp-mobile.shop/hosted.json";

    private void seedList() {
        servers.clear();

        ServerInfo featured = new ServerInfo(ServerConfig.FEATURED_IP, ServerConfig.FEATURED_PORT);
        featured.isFeatured = true;
        featured.hostname = "Djava RP Official";
        featured.gamemode = "RolePlay";
        featured.language = "Indonesia";
        featured.description = ServerConfig.FEATURED_DESCRIPTION;
        servers.add(featured);

        servers.addAll(ServerStore.load(requireContext()));
        fetchHostedServers();
        adapter.notifyDataSetChanged();
    }

    private void fetchHostedServers() {
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(
                com.android.volley.Request.Method.GET,
                HOSTED_SERVERS_URL,
                null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            org.json.JSONObject obj = response.getJSONObject(i);
                            String ip = obj.getString("ip");
                            int port = obj.getInt("port");
                            if (!ServerStore.contains(servers, ip, port)) {
                                ServerInfo info = new ServerInfo(ip, port);
                                servers.add(info);
                                queryServer(info, null);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } catch (org.json.JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (isAdded()) {
                        // Log or ignore
                    }
                }
        );
        com.android.volley.toolbox.Volley.newRequestQueue(requireContext()).add(request);
    }

    private void refreshAll() {
        if (servers.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        swipeRefresh.setRefreshing(true);
        final int total = servers.size();
        final int[] done = {0};

        for (ServerInfo server : new ArrayList<>(servers)) {
            queryServer(server, new Runnable() {
                @Override
                public void run() {
                    done[0]++;
                    if (done[0] >= total && swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                }
            });
        }
    }

    private void queryServer(final ServerInfo source, final Runnable finished) {
        SampQuery.queryInfo(source.ip, source.port, new SampQuery.Callback() {
            @Override
            public void onResult(ServerInfo info) {
                if (!isAdded()) return;
                int index = findServerIndex(info.ip, info.port);
                if (index >= 0) {
                    ServerInfo current = servers.get(index);
                    info.isFeatured = current.isFeatured;
                    info.description = current.description;
                    servers.set(index, info);
                    adapter.notifyItemChanged(index);
                }
                if (finished != null) finished.run();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                if (finished != null) finished.run();
            }
        });
    }

    private void showAddServerDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_server, null);
        final TextInputLayout inputLayout = view.findViewById(R.id.input_layout_server_ip);
        final TextInputEditText input = view.findViewById(R.id.input_server_ip);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_add_title)
                .setIcon(R.drawable.ic_add_24)
                .setView(view)
                .setNegativeButton(R.string.server_dialog_cancel, null)
                .setPositiveButton(R.string.server_add_action, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputLayout.setError(null);
                String value = input.getText() == null ? "" : input.getText().toString().trim();
                
                String ip;
                int port = 7777;
                
                if (value.matches("^([0-9a-zA-Z\\-\\.\\_]+)\\:([0-9]+)$")) {
                    String[] split = value.split(":");
                    ip = split[0];
                    try {
                        port = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        inputLayout.setError(getString(R.string.server_add_error_format));
                        return;
                    }
                } else if (value.matches("^([0-9a-zA-Z\\-\\.\\_]+)$")) {
                    ip = value;
                } else {
                    inputLayout.setError(getString(R.string.server_add_error_format));
                    return;
                }

                if (ServerStore.contains(servers, ip, port)) {
                    inputLayout.setError(getString(R.string.server_add_error_duplicate));
                    return;
                }
                
                int index = servers.size();
                ServerInfo server = new ServerInfo(ip, port);
                servers.add(server);
                adapter.notifyItemInserted(index);
                ServerStore.save(requireContext(), servers);
                queryServer(server, null);
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private void confirmDelete(final ServerInfo server) {
        if (server == null || server.isFeatured) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_delete)
                .setIcon(R.drawable.ic_delete_24)
                .setMessage("Apakah Anda yakin ingin menghapus server " + server.getIpPort() + " dari daftar?")
                .setNegativeButton(R.string.server_dialog_cancel, null)
                .setPositiveButton(R.string.server_delete, (dialog, which) -> {
                    int index = findServerIndex(server.ip, server.port);
                    if (index >= 0 && !servers.get(index).isFeatured) {
                        servers.remove(index);
                        adapter.notifyItemRemoved(index);
                        ServerStore.save(requireContext(), servers);
                    }
                })
                .show();
    }

    private void showServerDetails(final ServerInfo server) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_server_detail, null);

        TextView nameText = view.findViewById(R.id.detail_name);
        TextView addressText = view.findViewById(R.id.detail_address);
        TextView playersText = view.findViewById(R.id.detail_players);
        TextView pingText = view.findViewById(R.id.detail_ping);
        TextView modeText = view.findViewById(R.id.detail_mode);
        TextView langText = view.findViewById(R.id.detail_language);

        nameText.setText(displayName(server));
        addressText.setText(server.getIpPort());
        playersText.setText(server.players + " / " + server.maxPlayers);
        pingText.setText(server.ping > 0 ? server.ping + " ms" : "-- ms");
        modeText.setText(safeText(server.gamemode, "Unknown"));
        langText.setText(safeText(server.language, "Unknown"));

        final TextInputLayout nickLayout = view.findViewById(R.id.input_layout_nickname);
        final TextInputEditText nickInput = view.findViewById(R.id.input_nickname);
        nickInput.setText(readNickname());

        final TextInputLayout passwordLayout = view.findViewById(R.id.input_layout_password);
        final TextInputEditText passwordInput = view.findViewById(R.id.input_password);

        if (server.hasPassword) {
            passwordLayout.setVisibility(View.VISIBLE);
        }

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_detail_title)
                .setIcon(R.drawable.ic_server)
                .setView(view)
                .setNegativeButton(R.string.server_dialog_cancel, null)
                .setPositiveButton(R.string.server_detail_connect, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = nickInput.getText() == null ? "" : nickInput.getText().toString().trim();
                String password = "";
                if (server.hasPassword) {
                    password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
                    if (password.trim().isEmpty()) {
                        passwordLayout.setError(getString(R.string.server_detail_password_required));
                        return;
                    }
                }
                ServerConnector.connect(requireActivity(), server, nickname, password);
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private int findServerIndex(String ip, int port) {
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo server = servers.get(i);
            if (server != null && ip != null && ip.equalsIgnoreCase(server.ip) && server.port == port) {
                return i;
            }
        }
        return -1;
    }

    private static TextView detailLine(Context context, String label, String value) {
        TextView view = new TextView(context);
        view.setText(label + ": " + value);
        view.setPadding(0, 0, 0, dp(context, 8));
        return view;
    }

    private static String displayName(ServerInfo server) {
        if (server == null) return "";
        if (server.hostname != null && !server.hostname.trim().isEmpty()) {
            return server.hostname;
        }
        return server.getIpPort();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
