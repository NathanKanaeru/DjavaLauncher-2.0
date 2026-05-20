package com.nathan.djavarp.launcher.other;

public class DownloadModel {
    public String title;
    public String url;
    public String fileName;
    public int percent = 0;
    public String size = "";
    public String speed = "";
    public String eta = "";
    public String status = "Ready";
    public boolean isDownloading = false;
    public boolean isExtracting = false;

    public DownloadModel(String title, String url, String fileName) {
        this.title = title;
        this.url = url;
        this.fileName = fileName;
    }
}
