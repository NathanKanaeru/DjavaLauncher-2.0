package com.nathan.djavarp.launcher.model;

public class Announcement {
    public String title;
    public String description;
    public String date;
    public String imageUrl;

    public Announcement(String title, String description, String date, String imageUrl) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
    }
}