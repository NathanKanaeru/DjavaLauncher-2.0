package com.nathan.djavarp.launcher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.hzy.lib7z.Z7Extractor;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.activity.MainActivity;
import com.nathan.djavarp.launcher.other.UnZipCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadService extends Service {

    public static final String ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD";
    public static final String ACTION_PROGRESS_UPDATE = "ACTION_PROGRESS_UPDATE";
    public static final String ACTION_DOWNLOAD_COMPLETE = "ACTION_DOWNLOAD_COMPLETE";
    public static final String ACTION_DOWNLOAD_ERROR = "ACTION_DOWNLOAD_ERROR";
    public static final String ACTION_EXTRACTING = "ACTION_EXTRACTING";
    public static final String ACTION_EXTRACT_PROGRESS = "ACTION_EXTRACT_PROGRESS";

    private static final String CHANNEL_ID = "DownloadChannel";
    private static final int NOTIFICATION_ID = 1001;

    private int downloadId = -1;
    private List<DownloadItem> queue = new ArrayList<>();
    private int currentIndex = 0;

    private long lastBytes = 0;
    private long lastTime = 0;
    private long lastBroadcastTime = 0;
    private int lastPercent = -1;
    private String currentSpeed = "0 KB/s";
    private String currentEta = "--";

    private com.nathan.djavarp.launcher.util.DownloadStore downloadStore;

    public static class DownloadItem {
        public String url, fileName, label;
        public DownloadItem(String url, String fileName, String label) {
            this.url = url; this.fileName = fileName; this.label = label;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadStore = new com.nathan.djavarp.launcher.util.DownloadStore(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START_DOWNLOAD.equals(intent.getAction())) {
            String url = intent.getStringExtra("url");
            String fileName = intent.getStringExtra("file_name");
            String label = intent.getStringExtra("label");

            queue.clear();
            queue.add(new DownloadItem(url, fileName, label));
            currentIndex = 0;
            lastPercent = -1;

            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification("Memulai unduhan...", 0));

            startNext();
        }
        return START_NOT_STICKY;
    }

    private void startNext() {
        if (currentIndex >= queue.size()) {
            extractFiles();
            return;
        }

        DownloadItem item = queue.get(currentIndex);
        String path = getExternalFilesDir(null).getAbsolutePath();

        lastTime = System.currentTimeMillis();
        lastBytes = 0;
        lastBroadcastTime = 0;

        downloadId = PRDownloader.download(item.url, path, item.fileName)
                .build()
                .setOnProgressListener(progress -> {
                    if (progress.totalBytes > 0) {
                        int percent = (int) (progress.currentBytes * 100 / progress.totalBytes);
                        calculateSpeedAndEta(progress.currentBytes, progress.totalBytes);
                        
                        long now = System.currentTimeMillis();
                        if (now - lastBroadcastTime >= 500 || percent == 100) {
                            updateNotification(item.label + " (" + percent + "%)", percent);
                            broadcastProgress(item.label, percent, progress.currentBytes, progress.totalBytes);
                            lastBroadcastTime = now;
                        }
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        downloadStore.setStatus(item.fileName, com.nathan.djavarp.launcher.util.DownloadStore.Status.DOWNLOADED);
                        currentIndex++;
                        startNext();
                    }

                    @Override
                    public void onError(Error error) {
                        broadcastError(item.fileName, error.getServerErrorMessage());
                        stopForeground(true);
                        stopSelf();
                    }
                });
    }

    private void calculateSpeedAndEta(long currentBytes, long totalBytes) {
        long now = System.currentTimeMillis();
        long timeDiff = now - lastTime;
        if (timeDiff >= 1000) {
            long byteDiff = currentBytes - lastBytes;
            double speed = (byteDiff / 1024.0) / (timeDiff / 1000.0); // KB/s
            
            if (speed > 1024) {
                currentSpeed = String.format(Locale.getDefault(), "%.2f MB/s", speed / 1024.0);
            } else {
                currentSpeed = String.format(Locale.getDefault(), "%.1f KB/s", speed);
            }

            long remainingBytes = totalBytes - currentBytes;
            if (speed > 0) {
                long remainingSeconds = (long) (remainingBytes / 1024.0 / speed);
                currentEta = formatTime(remainingSeconds);
            }

            lastTime = now;
            lastBytes = currentBytes;
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    private void extractFiles() {
        new Thread(() -> {
            File targetDir = getExternalFilesDir(null);
            for (DownloadItem item : queue) {
                broadcastStatus(ACTION_EXTRACTING, item.fileName);
                updateNotification("Mengekstrak " + item.label + "...", -1);

                File zipFile = new File(targetDir, item.fileName);
                if (zipFile.exists()) {
                    final Object lock = new Object();
                    final boolean[] extracted = {false};
                    final int[] totalFiles = {0};
                    final int[] currentFile = {0};
                    lastPercent = -1;

                    Z7Extractor.extractFile(zipFile.getAbsolutePath(), targetDir.getAbsolutePath(), new UnZipCallback() {
                        @Override
                        public void onGetFileNum(int fileNum) {
                            totalFiles[0] = fileNum;
                        }

                        @Override
                        public void onProgress(String name, long size) {
                            currentFile[0]++;
                            if (totalFiles[0] > 0) {
                                int percent = (currentFile[0] * 100) / totalFiles[0];
                                broadcastExtractProgress(item.fileName, percent, name);
                                if (percent != lastPercent) {
                                    lastPercent = percent;
                                    updateNotification("Mengekstrak: " + percent + "% (" + name + ")", percent);
                                }
                            }
                        }

                        @Override public void onSucceed() {
                            zipFile.delete();
                            downloadStore.setStatus(item.fileName, com.nathan.djavarp.launcher.util.DownloadStore.Status.EXTRACTED);
                            synchronized (lock) { extracted[0] = true; lock.notifyAll(); }
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            broadcastError(item.fileName, "Extraction error: " + message);
                            synchronized (lock) { extracted[0] = true; lock.notifyAll(); }
                        }
                    });
                    synchronized (lock) {
                        while(!extracted[0]) {
                            try { lock.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                        }
                    }
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                broadcastStatus(ACTION_DOWNLOAD_COMPLETE, queue.get(0).fileName);
                stopForeground(true);
                stopSelf();
            });
        }).start();
    }

    private void broadcastProgress(String label, int percent, long current, long total) {
        Intent intent = new Intent(ACTION_PROGRESS_UPDATE);
        intent.putExtra("fileName", queue.get(currentIndex).fileName);
        intent.putExtra("label", label);
        intent.putExtra("percent", percent);
        intent.putExtra("current", current);
        intent.putExtra("total", total);
        intent.putExtra("speed", currentSpeed);
        intent.putExtra("eta", currentEta);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastExtractProgress(String fileName, int percent, String currentFile) {
        Intent intent = new Intent(ACTION_EXTRACT_PROGRESS);
        intent.putExtra("fileName", fileName);
        intent.putExtra("percent", percent);
        intent.putExtra("currentFile", currentFile);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastStatus(String action, String fileName) {
        Intent intent = new Intent(action);
        intent.putExtra("fileName", fileName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastError(String fileName, String msg) {
        Intent intent = new Intent(ACTION_DOWNLOAD_ERROR);
        intent.putExtra("fileName", fileName);
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_m3)
                .setContentTitle("Djava Launcher Downloader")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    private void updateNotification(String text, int progress) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(text, progress));
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}