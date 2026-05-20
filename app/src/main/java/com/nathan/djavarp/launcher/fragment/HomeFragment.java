package com.nathan.djavarp.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.adapter.AnnouncementAdapter;
import com.nathan.djavarp.launcher.model.Announcement;
import com.nathan.djavarp.launcher.model.ServerConfig;
import com.nathan.djavarp.launcher.model.ServerInfo;
import com.nathan.djavarp.launcher.util.SampQuery;
import com.nathan.djavarp.launcher.util.SettingsIO;

import org.ini4j.Wini;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Home tab — shows user greeting, latest announcements, and featured server status.
 */
public class HomeFragment extends Fragment {

    private TextView featuredValue;
    private TextView nickValue;
    private TextView greetingText;
    private RecyclerView rvAnnouncements;
    private View announcementLoader;

    private static final String ANNOUNCEMENT_URL = "https://raw.githubusercontent.com/Nathan-Studios/DjavaLauncher/main/announcements.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        featuredValue = view.findViewById(R.id.home_featured_value);
        nickValue = view.findViewById(R.id.home_nick_value);
        greetingText = view.findViewById(R.id.home_greeting);
        rvAnnouncements = view.findViewById(R.id.home_rv_announcements);
        announcementLoader = view.findViewById(R.id.home_announcement_loader);

        fetchRemoteAnnouncements();
        updateGreeting();

        // Show current nickname from settings.ini.
        try {
            Wini ini = SettingsIO.load(requireContext());
            String nick = SettingsIO.getString(ini, "client", "name", getString(R.string.home_default_nick));
            nickValue.setText(nick);
        } catch (Exception ignored) {
            nickValue.setText(R.string.home_default_nick);
        }

        // Quick connect play button
        view.findViewById(R.id.home_btn_play).setOnClickListener(v -> {
            // Logic to launch game with featured IP
        });

        // Async: fetch live info for the hero stat card.
        SampQuery.queryInfo(ServerConfig.FEATURED_IP, ServerConfig.FEATURED_PORT, new SampQuery.Callback() {
            @Override
            public void onResult(ServerInfo info) {
                if (!isAdded()) return;
                featuredValue.setText("Pemain: " + info.players + " / " + info.maxPlayers);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                featuredValue.setText(R.string.status_offline);
            }
        });
    }

    private void fetchRemoteAnnouncements() {
        if (announcementLoader != null) announcementLoader.setVisibility(View.VISIBLE);
        
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(
                com.android.volley.Request.Method.GET,
                ANNOUNCEMENT_URL,
                null,
                response -> {
                    if (!isAdded()) return;
                    if (announcementLoader != null) announcementLoader.setVisibility(View.GONE);
                    
                    List<Announcement> list = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            org.json.JSONObject obj = response.getJSONObject(i);
                            list.add(new Announcement(
                                    obj.optString("title", ""),
                                    obj.optString("description", ""),
                                    obj.optString("date", ""),
                                    obj.optString("imageUrl", "")
                            ));
                        }
                    } catch (org.json.JSONException e) {
                        e.printStackTrace();
                    }
                    
                    if (list.isEmpty()) {
                        setupFallbackAnnouncements();
                    } else {
                        updateAnnouncementList(list);
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    if (announcementLoader != null) announcementLoader.setVisibility(View.GONE);
                    setupFallbackAnnouncements();
                }
        );

        com.android.volley.toolbox.Volley.newRequestQueue(requireContext()).add(request);
    }

    private void updateAnnouncementList(List<Announcement> list) {
        AnnouncementAdapter adapter = new AnnouncementAdapter(list);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvAnnouncements.setAdapter(adapter);
    }

    private void setupFallbackAnnouncements() {
        List<Announcement> list = new ArrayList<>();
        list.add(new Announcement("Djava Launcher", "Selamat datang di client SA:MP Mobile terbaru.", "Now", ""));
        updateAnnouncementList(list);
    }

    private void updateGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (timeOfDay >= 0 && timeOfDay < 12) {
            greeting = "Selamat Pagi,";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            greeting = "Selamat Siang,";
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            greeting = "Selamat Sore,";
        } else {
            greeting = "Selamat Malam,";
        }
        if (greetingText != null) greetingText.setText(greeting);
    }
}
