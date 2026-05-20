package com.nathan.djavarp.launcher.util;

import android.content.Context;
import android.util.Log;

import org.ini4j.Wini;
import org.ini4j.Profile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Wrapper around the SAMP/settings.ini file that the native side reads
 * at startup. The launcher writes [client]/host, [client]/port,
 * [client]/serverid, [client]/name (and others) and the native
 * CSettings constructor picks them up.
 */
public final class SettingsIO {

    private static final String TAG = "SettingsIO";

    /** Folder name (under getExternalFilesDir) the native code uses. */
    private static final String DIR = "SAMP";
    private static final String FILE = "settings.ini";
    private static final String ASSET_FALLBACK = "settings.ini";

    private SettingsIO() {
    }

    /** Resolve the on-disk settings.ini path used by the native side. */
    public static File getSettingsFile(Context ctx) {
        File base = ctx.getExternalFilesDir(null);
        File dir = new File(base, DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, FILE);
    }

    /**
     * Load SAMP/settings.ini, creating it from the bundled asset if missing.
     * Never throws — returns an empty Wini if everything fails.
     */
    public static Wini load(Context ctx) {
        File f = getSettingsFile(ctx);
        if (!f.exists()) {
            try {
                copyAssetIfPresent(ctx, ASSET_FALLBACK, f);
            } catch (IOException e) {
                Log.w(TAG, "could not copy default settings.ini: " + e.getMessage());
            }
        }
        Wini ini;
        try {
            ini = new Wini();
            if (f.exists()) {
                ini.load(f);
            }
        } catch (IOException e) {
            Log.w(TAG, "load failed: " + e.getMessage());
            ini = new Wini();
        }
        ensureSection(ini, "client");
        ensureSection(ini, "debug");
        ensureSection(ini, "gui");
        return ini;
    }

    /** Persist the Wini back to disk. */
    public static void save(Context ctx, Wini ini) {
        File f = getSettingsFile(ctx);
        try {
            ini.store(f);
        } catch (IOException e) {
            Log.e(TAG, "save failed: " + e.getMessage());
        }
    }

    public static String getString(Wini ini, String section, String key, String fallback) {
        Profile.Section s = ini.get(section);
        if (s == null) return fallback;
        String v = s.get(key);
        return v == null ? fallback : v;
    }

    public static int getInt(Wini ini, String section, String key, int fallback) {
        try {
            return Integer.parseInt(getString(ini, section, key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static float getFloat(Wini ini, String section, String key, float fallback) {
        try {
            return Float.parseFloat(getString(ini, section, key, Float.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean getBool(Wini ini, String section, String key, boolean fallback) {
        String v = getString(ini, section, key, fallback ? "true" : "false").trim();
        if (v.equalsIgnoreCase("true") || v.equals("1")) return true;
        if (v.equalsIgnoreCase("false") || v.equals("0")) return false;
        return fallback;
    }

    public static void put(Wini ini, String section, String key, Object value) {
        ensureSection(ini, section);
        ini.get(section).put(key, value == null ? "" : String.valueOf(value));
    }

    private static void ensureSection(Wini ini, String name) {
        if (ini.get(name) == null) ini.add(name);
    }

    private static void copyAssetIfPresent(Context ctx, String assetName, File dest) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ctx.getAssets().open(assetName);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }
}
