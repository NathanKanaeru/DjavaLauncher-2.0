package com.nathan.djavarp.launcher.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.fragment.DownloadFragment;
import com.nathan.djavarp.launcher.fragment.HomeFragment;
import com.nathan.djavarp.launcher.fragment.ServerFragment;
import com.nathan.djavarp.launcher.fragment.SettingsFragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Launcher entry point. Hosts four Fragments under a BottomNavigationView.
 * The actual game runs in {@link com.nathan.djavarp.game.SAMP} which the
 * launcher launches via {@link com.nathan.djavarp.launcher.util.ServerConnector}.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();

        // Edge-to-edge so the green/black palette flows under the system bars.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return showFragment(new HomeFragment());
                } else if (id == R.id.nav_server) {
                    return showFragment(new ServerFragment());
                } else if (id == R.id.nav_download) {
                    return showFragment(new DownloadFragment());
                } else if (id == R.id.nav_settings) {
                    return showFragment(new SettingsFragment());
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    private boolean showFragment(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                )
                .replace(R.id.fragment_container, f)
                .commit();
        return true;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                Toast.makeText(this, "Please allow 'All Files Access' to use Custom Storage Path", Toast.LENGTH_LONG).show();
            } else {
                // Ensure custom directory exists if we have permission
                File customDir = new File("/storage/emulated/0/Android/DjavaLauncher/files/");
                if (!customDir.exists()) {
                    if (customDir.mkdirs()) {
                        Log.i("MainActivity", "Created custom storage directory: " + customDir.getAbsolutePath());
                    }
                }
            }
        }
    }
}
