package com.nathan.djavarp.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;

public class DownloadStore {
    private static final String PREF_NAME = "download_status";
    private static final String KEY_PREFIX = "status_";

    public enum Status {
        NOT_DOWNLOADED(0),
        DOWNLOADED(1),
        EXTRACTED(2);

        public final int value;
        Status(int value) { this.value = value; }
        public static Status fromInt(int i) {
            for (Status s : Status.values()) if (s.value == i) return s;
            return NOT_DOWNLOADED;
        }
    }

    private final SharedPreferences prefs;

    public DownloadStore(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setStatus(String fileName, Status status) {
        prefs.edit().putInt(KEY_PREFIX + fileName, status.value).apply();
    }

    public Status getStatus(String fileName) {
        return Status.fromInt(prefs.getInt(KEY_PREFIX + fileName, Status.NOT_DOWNLOADED.value));
    }

    public boolean isAllFinished() {
        // This is a bit specific to the files we know
        return getStatus("base.zip") == Status.EXTRACTED && 
               (getStatus("adreno.zip") == Status.EXTRACTED || 
                getStatus("mali.zip") == Status.EXTRACTED || 
                getStatus("powervr.zip") == Status.EXTRACTED);
    }
}
