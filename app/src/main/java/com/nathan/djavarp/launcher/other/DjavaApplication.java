package com.nathan.djavarp.launcher.other;

import android.app.Activity;
import android.app.Application;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;

public class DjavaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .build();
        PRDownloader.initialize(getApplicationContext(), config);
    }
}
