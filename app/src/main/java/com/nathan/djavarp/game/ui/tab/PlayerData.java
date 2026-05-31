package com.nathan.djavarp.game.ui.tab;

public class PlayerData {
    public int id;
    public int color;
    public String name;
    public int score;
    public int ping;

    public PlayerData(int id, int color, String name, int score, int ping) {
        this.id = id;
        this.color = color;
        this.name = name;
        this.score = score;
        this.ping = ping;
    }
}
