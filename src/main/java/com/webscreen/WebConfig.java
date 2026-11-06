package com.webscreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class WebConfig {
    public static final int TITLE_H = 14;
    public static final int TOOLBAR_H = 18;
    public static final int CHROME_H = TITLE_H + TOOLBAR_H;
    public static final int MIN_W = 160;
    public static final int MIN_H = 110;

    public int x = 20, y = 20, w = 480, h = 300;
    public String url = "https://www.google.com";
    public boolean pinned = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("webscreen.json");
    }

    public int contentX() { return x; }
    public int contentY() { return y + CHROME_H; }
    public int contentW() { return w; }
    public int contentH() { return h - CHROME_H; }

    public static WebConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                WebConfig c = GSON.fromJson(Files.readString(p), WebConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) {}
        return new WebConfig();
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (Exception ignored) {}
    }
}
