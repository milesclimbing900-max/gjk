package com.skycustom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SkyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SkyConfig instance;

    public boolean enabled = true;
    public boolean hideCelestials = false;
    public float brightness = 1.0f;   // 0..1
    public float opacity = 1.0f;      // 0..1
    public float yaw = 0.0f;          // degrees, 0..360
    public String imageName = "";

    public static SkyConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("skycustom");
    }

    public static Path imageFile() {
        return dir().resolve("sky_image.bin");
    }

    private static Path file() {
        return dir().resolve("config.json");
    }

    private static void load() {
        try {
            if (Files.isRegularFile(file())) {
                instance = GSON.fromJson(Files.readString(file()), SkyConfig.class);
            }
        } catch (Exception e) {
            SkyCustomClient.LOGGER.warn("Could not read config, using defaults", e);
        }
        if (instance == null) instance = new SkyConfig();
    }

    public static void save() {
        try {
            Files.createDirectories(dir());
            Files.writeString(file(), GSON.toJson(get()));
        } catch (IOException e) {
            SkyCustomClient.LOGGER.warn("Could not save config", e);
        }
    }
}
