package com.nathan.djavarp.launcher.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.util.SettingsIO;

import org.ini4j.Wini;

/**
 * Settings tab — surfaces every option that the native side reads from
 * SAMP/settings.ini, grouped into Account / Display / Chat / Voice / Advanced.
 * Changes are persisted with a small debounce so the UI stays responsive
 * while users drag sliders or type into the nickname field.
 */
public class SettingsFragment extends Fragment {

    private Wini ini;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private final Runnable saveRunnable = new Runnable() {
        @Override public void run() {
            if (isAdded() && ini != null) {
                SettingsIO.save(requireContext(), ini);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ini = SettingsIO.load(requireContext());

        // Account
        TextInputEditText etNick = view.findViewById(R.id.et_nickname);
        TextInputEditText etPass = view.findViewById(R.id.et_password);
        etNick.setText(SettingsIO.getString(ini, "client", "name", ""));
        etPass.setText(SettingsIO.getString(ini, "client", "password", ""));
        etNick.addTextChangedListener(stringWriter("client", "name"));
        etPass.addTextChangedListener(stringWriter("client", "password"));

        // Display
        bindSwitch(view, R.id.row_first_person, "First-person view", R.drawable.ic_launcher_play, "gui", "firstperson", true);
        bindSwitch(view, R.id.row_show_fps, "Show FPS counter", R.drawable.ic_signal_24, "gui", "fps", true);
        bindSlider(view, R.id.slider_fps, R.id.lbl_fps_limit, "gui", "FPSLimit", 60);
        bindSwitch(view, R.id.row_cutout, "Use cutout area", R.drawable.ic_story_stroke, "gui", "cutout", false);
        bindSwitch(view, R.id.row_skybox, "Custom SkyBox", R.drawable.ic_globe_24, "gui", "skybox", false);
        bindSwitch(view, R.id.row_snow, "Snow effect", R.drawable.story_fg, "gui", "snow", false);
        bindSwitch(view, R.id.row_custom_hud, "Custom HUD", R.drawable.ic_mainmenu, "gui", "hud", false);
        bindSwitch(view, R.id.row_radar_rect, "Round radar", R.drawable.ic_star, "gui", "radarrect", false);

        // Chat & UI
        bindSwitch(view, R.id.row_hp_armour, "HP / Armour as text", R.drawable.ic_hud_hp, "gui", "hparmourtext", false);
        bindSwitch(view, R.id.row_pc_money, "PC-style money", R.drawable.hud_ruble, "gui", "pcmoney", false);
        bindSlider(view, R.id.slider_font_size, R.id.lbl_font_size, "gui", "FontSize", 30);
        bindSwitch(view, R.id.row_android_keyboard, "Native Android keyboard", R.drawable.ic_buttoncolor, "gui", "androidkeyboard", false);
        bindSwitch(view, R.id.row_outfit_guns, "Show outfit guns", R.drawable.hud_weapon_colt, "gui", "outfitguns", false);

        // Voice
        bindSwitch(view, R.id.row_voice, "Enable Voice Chat", R.drawable.ic_baseline_notifications_24, "gui", "VoiceChatEnable", true);

        // Advanced
        bindSwitch(view, R.id.row_debug, "Debug mode", R.drawable.ic_settings_m3, "debug", "debug", false);
        bindSwitch(view, R.id.row_online, "Online mode", R.drawable.ic_url, "debug", "online", true);
    }

    // ---------- helpers ----------

    private TextWatcher stringWriter(final String section, final String key) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (ini == null) return;
                SettingsIO.put(ini, section, key, s.toString());
                schedulePersist();
            }
        };
    }

    private void bindSwitch(View root, int includeId, String label, int iconRes,
                            final String section, final String key, boolean defaultVal) {
        View row = root.findViewById(includeId);
        if (row == null) return;
        TextView lbl = row.findViewById(R.id.setting_label);
        if (lbl != null) lbl.setText(label);
        
        android.widget.ImageView icon = row.findViewById(R.id.setting_icon);
        if (icon != null) icon.setImageResource(iconRes);

        MaterialSwitch sw = row.findViewById(R.id.setting_switch);
        if (sw == null) return;
        boolean current = SettingsIO.getBool(ini, section, key, defaultVal);
        sw.setChecked(current);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ini == null) return;
                SettingsIO.put(ini, section, key, isChecked ? "true" : "false");
                schedulePersist();
            }
        });
    }

    private void bindSlider(View root, int sliderId, int labelId,
                            final String section, final String key, int defaultVal) {
        Slider slider = root.findViewById(sliderId);
        final TextView lbl = root.findViewById(labelId);
        int initial = SettingsIO.getInt(ini, section, key, defaultVal);
        if (slider == null) return;
        try {
            slider.setValue(clamp(initial, slider.getValueFrom(), slider.getValueTo()));
        } catch (IllegalArgumentException ignored) {
            slider.setValue(slider.getValueFrom());
        }
        if (lbl != null) lbl.setText(String.valueOf((int) slider.getValue()));
        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider s, float value, boolean fromUser) {
                int v = (int) value;
                if (lbl != null) lbl.setText(String.valueOf(v));
                if (ini == null) return;
                SettingsIO.put(ini, section, key, v);
                schedulePersist();
            }
        });
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void schedulePersist() {
        debounce.removeCallbacks(saveRunnable);
        debounce.postDelayed(saveRunnable, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        debounce.removeCallbacks(saveRunnable);
        if (ini != null) {
            SettingsIO.save(requireContext(), ini);
        }
    }
}
