package com.nathan.djavarp.launcher.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.nathan.djavarp.R;

import org.json.JSONException;

public class SplashScreenActivity extends AppCompatActivity {

    private TextView tvStatus;
    private static final int CURRENT_VERSION_CODE = 2; // Matches build.gradle
    private static final String VERSION_URL = "https://raw.githubusercontent.com/Nathan-Studios/DjavaLauncher/main/version_control.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvStatus = findViewById(R.id.splash_status);

        new Handler().postDelayed(this::checkInternet, 1500);
    }

    private void checkInternet() {
        tvStatus.setText("Memeriksa koneksi internet...");
        if (isNetworkAvailable()) {
            checkVersion();
        } else {
            showErrorDialog("Koneksi Internet Diperlukan", 
                    "Maaf, Anda memerlukan koneksi internet untuk masuk ke Djava Launcher. Silakan aktifkan data seluler atau WiFi.", 
                    true);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkVersion() {
        tvStatus.setText("Memeriksa pembaruan...");
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, VERSION_URL, null,
                response -> {
                    try {
                        int latestCode = response.getInt("latestVersionCode");
                        String latestVersion = response.getString("latestVersion");
                        String downloadUrl = response.getString("downloadUrl");
                        String changelog = response.getString("changeLog");

                        if (latestCode > CURRENT_VERSION_CODE) {
                            Intent intent = new Intent(this, UpdateActivity.class);
                            intent.putExtra("version", latestVersion);
                            intent.putExtra("changelog", changelog);
                            intent.putExtra("url", downloadUrl);
                            startActivity(intent);
                            finish();
                        } else {
                            tvStatus.setText("Versi sudah terbaru!");
                            startMainActivity();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        startMainActivity();
                    }
                },
                error -> {
                    // Fail-safe: if version check fails, just proceed to main activity
                    startMainActivity();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void startMainActivity() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
            finish();
        }, 1000);
    }

    private void showErrorDialog(String title, String message, boolean isFatal) {
        new AlertDialog.Builder(this, R.style.ThemeOverlay_Djava_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Coba Lagi", (dialog, which) -> {
                    dialog.dismiss();
                    checkInternet();
                })
                .setNegativeButton("Keluar", (dialog, which) -> finishAffinity())
                .show();
    }
}