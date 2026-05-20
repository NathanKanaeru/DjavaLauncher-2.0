package com.nathan.djavarp.launcher.util;

import android.app.Activity;
import java.io.File;

public class ConfigValidator {
    public static void validateConfigFiles(Activity activity) {
        File externalFilesDir = activity.getExternalFilesDir(null);
        if (externalFilesDir == null) return;

        File sampDir = new File(externalFilesDir, "SAMP");
        if (!sampDir.exists()) {
            sampDir.mkdirs();
        }

        File settingsFile = new File(sampDir, "settings.ini");
        if (!settingsFile.exists()) {
            try {
                settingsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Ensure other directories mentioned in analysis exist if needed
        new File(externalFilesDir, "Text").mkdirs();
        new File(externalFilesDir, "Textures/fonts").mkdirs();
    }
}
