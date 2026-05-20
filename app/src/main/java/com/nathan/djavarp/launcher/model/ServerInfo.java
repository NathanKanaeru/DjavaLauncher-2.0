package com.nathan.djavarp.launcher.model;

/**
 * Plain data holder for a SA:MP server entry shown in the launcher.
 */
public class ServerInfo {

    public String ip;
    public int port;
    public String hostname;
    public String gamemode;
    public String language;
    public int players;
    public int maxPlayers;
    public int ping;
    public boolean hasPassword;
    public boolean isFeatured;
    public String description;

    public ServerInfo() {
    }

    public ServerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.hostname = ip + ":" + port;
        this.gamemode = "Unknown";
        this.language = "Unknown";
        this.players = 0;
        this.maxPlayers = 0;
        this.ping = -1;
        this.hasPassword = false;
        this.isFeatured = false;
        this.description = "";
    }

    public String getIpPort() {
        return ip + ":" + port;
    }

    /** Returns true if a SAMP query response has populated this entry. */
    public boolean isOnline() {
        return hostname != null && !hostname.equals(ip + ":" + port) && maxPlayers > 0;
    }
}
