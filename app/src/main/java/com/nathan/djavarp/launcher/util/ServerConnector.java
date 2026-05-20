package com.nathan.djavarp.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import com.nathan.djavarp.game.SAMP;
import com.nathan.djavarp.launcher.model.ServerConfig;
import com.nathan.djavarp.launcher.model.ServerInfo;

import org.ini4j.Wini;

/**
 * Bridges the launcher UI and the existing native connect flow.
 *
 * The native CSettings constructor reads SAMP/settings.ini at startup and
 * the main loop selects the server by [client]/serverid. We extend that
 * convention with serverid==99 meaning "use [client]/host and [client]/port"
 * (handled by a small native patch in main.cpp). The launcher therefore:
 *   1. Writes ip, port, serverid=99, nickname, password into settings.ini
 *   2. Starts SAMP activity
 *   3. Finishes itself
 */
public final class ServerConnector {

    private static final String TAG = "ServerConnector";

    private ServerConnector() {
    }

    public static void connect(Activity activity, ServerInfo server, String nickname) {
        connect(activity, server, nickname, null);
    }

    public static void connect(Activity activity, ServerInfo server, String nickname, String password) {
        if (activity == null || server == null) return;

        // 1. Validate Config Files
        ConfigValidator.validateConfigFiles(activity);

        // 2. Update settings.ini (handled via SettingsIO below)
        
        // 3. Pengecekan Aset Penting (Text/american.gxt, Textures/fonts/RussianFont.png)
        File extDir = activity.getExternalFilesDir(null);
        if (extDir != null) {
            File gxt = new File(extDir, "Text/american.gxt");
            File font = new File(extDir, "Textures/fonts/RussianFont.png");
            if (!gxt.exists() || !font.exists()) {
                Toast.makeText(activity, "Peringatan: Beberapa aset penting (Text/Textures) tidak ditemukan!", Toast.LENGTH_LONG).show();
            }
        }

        if (TextUtils.isEmpty(nickname)) {
            nickname = "Player_" + (int) (Math.random() * 9000 + 1000);
        }
        if (password == null) {
            password = "";
        }

        try {
            Wini ini = SettingsIO.load(activity);
            SettingsIO.put(ini, "client", "host", server.ip);
            SettingsIO.put(ini, "client", "port", server.port);
            SettingsIO.put(ini, "client", "name", nickname);
            SettingsIO.put(ini, "client", "password", password);
            SettingsIO.put(ini, "client", "serverid", ServerConfig.CUSTOM_SERVER_ID);
            SettingsIO.save(activity, ini);
        } catch (Exception e) {
            Log.e(TAG, "failed writing settings.ini: " + e.getMessage());
            Toast.makeText(activity,
                    "Gagal menyimpan settings: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(activity, SAMP.class);
        intent.putExtra("server_ip", server.ip);
        intent.putExtra("server_port", server.port);
        intent.putExtra("nickname", nickname);
        intent.putExtra("server_password", password);
        activity.startActivity(intent);
        activity.finish();
    }
}
