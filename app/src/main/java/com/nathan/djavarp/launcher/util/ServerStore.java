package com.nathan.djavarp.launcher.util;

import android.content.Context;
import com.nathan.djavarp.launcher.data.FavoritesInfo;
import com.nathan.djavarp.launcher.model.ServerInfo;
import java.util.ArrayList;
import java.util.List;

public final class ServerStore {

    private ServerStore() {
    }

    public static List<ServerInfo> load(Context context) {
        List<ServerInfo> out = new ArrayList<>();
        List<FavoritesInfo> favs = FavoritesInfo.getServerList(context);
        for (FavoritesInfo fav : favs) {
            if (fav != null && fav.ip != null) {
                out.add(new ServerInfo(fav.ip, fav.port));
            }
        }
        return out;
    }

    public static void save(Context context, List<ServerInfo> servers) {
        List<FavoritesInfo> favs = new ArrayList<>();
        int id = 1;
        for (ServerInfo server : servers) {
            if (server != null && !server.isFeatured && server.ip != null && server.port > 0) {
                favs.add(new FavoritesInfo(id++, 99, server.ip, server.port));
            }
        }
        FavoritesInfo.saveServerList(context, favs);
    }

    public static ServerInfo parse(String ipPort) {
        if (ipPort == null) return null;
        String value = ipPort.trim();
        int idx = value.lastIndexOf(':');
        if (idx <= 0 || idx >= value.length() - 1) {
            // Support IP only if needed, but ServerFragment will handle regex
            return null;
        }
        String ip = value.substring(0, idx).trim();
        String portText = value.substring(idx + 1).trim();
        if (ip.isEmpty() || portText.isEmpty()) return null;
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) return null;
            return new ServerInfo(ip, port);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean contains(List<ServerInfo> servers, String ip, int port) {
        if (servers == null || ip == null) return false;
        for (ServerInfo server : servers) {
            if (server != null && ip.equalsIgnoreCase(server.ip) && server.port == port) {
                return true;
            }
        }
        return false;
    }
}
