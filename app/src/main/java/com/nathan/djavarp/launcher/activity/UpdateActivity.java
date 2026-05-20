package com.nathan.djavarp.launcher.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nathan.djavarp.R;

public class UpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        String version = getIntent().getStringExtra("version");
        String changelog = getIntent().getStringExtra("changelog");
        String url = getIntent().getStringExtra("url");

        TextView tvVersion = findViewById(R.id.update_version_label);
        TextView tvChangelog = findViewById(R.id.tv_changelog);

        if (version != null) tvVersion.setText("Versi Terbaru: " + version);
        if (changelog != null) tvChangelog.setText(changelog);

        findViewById(R.id.btn_update_download).setOnClickListener(v -> {
            if (url != null && !url.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent going back, forced update
        super.onBackPressed();
        finishAffinity();
    }
}