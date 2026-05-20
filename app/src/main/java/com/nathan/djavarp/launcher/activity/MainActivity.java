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

/**
 * Launcher entry point. Hosts four Fragments under a BottomNavigationView.
 * The actual game runs in {@link com.nathan.djavarp.game.SAMP} which the
 * launcher launches via {@link com.nathan.djavarp.launcher.util.ServerConnector}.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
}
