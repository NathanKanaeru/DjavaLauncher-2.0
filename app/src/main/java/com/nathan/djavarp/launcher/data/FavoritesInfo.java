package com.nathan.djavarp.launcher.data;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FavoritesInfo {
    public int id;
    public int serverid;
    public String ip;
    public int port;

    public FavoritesInfo() {}
    public FavoritesInfo(int id, int serverid, String ip, int port) {
        this.id = id;
        this.serverid = serverid;
        this.ip = ip;
        this.port = port;
    }

    public static List<FavoritesInfo> getServerList(Context context) {
        List<FavoritesInfo> list = new ArrayList<>();
        File file = new File(context.getExternalFilesDir(null), "SAMP/favorites.json");
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new FavoritesInfo(
                    obj.optInt("id"),
                    obj.optInt("serverid"),
                    obj.optString("ip"),
                    obj.optInt("port")
                ));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveServerList(Context context, List<FavoritesInfo> list) {
        File file = new File(context.getExternalFilesDir(null), "SAMP/favorites.json");
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            JSONArray arr = new JSONArray();
            for (FavoritesInfo info : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", info.id);
                obj.put("serverid", info.serverid);
                obj.put("ip", info.ip);
                obj.put("port", info.port);
                arr.put(obj);
            }
            writer.write(arr.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean IsServerExists(Context context, String ip, int port) {
        List<FavoritesInfo> list = getServerList(context);
        for (FavoritesInfo info : list) {
            if (info.ip != null && info.ip.equalsIgnoreCase(ip) && info.port == port) return true;
        }
        return false;
    }
}
