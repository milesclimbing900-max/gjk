package com.skycustom;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads user images off-thread and uploads them as a dynamic texture on the render thread. */
public final class SkyImageManager {
    public static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(SkyCustomClient.MOD_ID, "user_sky");
    private static final long MAX_FILE_BYTES = 256L * 1024 * 1024;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SkyCustom-Decoder");
        t.setDaemon(true);
        return t;
    });

    private static boolean textureReady;
    private static boolean restoreStarted;
    private static volatile boolean loading;
    private static volatile Component status = Component.empty();

    /** Called every frame from the sky renderer; the first call restores the previously saved image. */
    public static boolean isReady() {
        if (!restoreStarted) {
            restoreStarted = true;
            Path saved = SkyConfig.imageFile();
            if (Files.isRegularFile(saved)) submit(saved, SkyConfig.get().imageName, false);
        }
        return textureReady;
    }

    public static Component getStatus() {
        Component s = status;
        if (!s.getString().isEmpty()) return s;
        String name = SkyConfig.get().imageName;
        if (textureReady && !name.isEmpty()) return Component.translatable("skycustom.status.current", name);
        return Component.translatable("skycustom.status.none");
    }

    public static void setStatus(Component c) {
        status = c;
    }

    public static void loadFromPath(Path path) {
        Path fileName = path.getFileName();
        submit(path, fileName == null ? path.toString() : fileName.toString(), true);
    }

    private static void submit(Path path, String displayName, boolean persist) {
        if (loading) {
            status = Component.translatable("skycustom.status.busy");
            return;
        }
        loading = true;
        status = Component.translatable("skycustom.status.loading", displayName);
        WORKER.execute(() -> {
            try {
                if (!Files.isRegularFile(path)) throw new IOException("File not found");
                if (Files.size(path) > MAX_FILE_BYTES) throw new IOException("File is larger than 256 MB");
                byte[] bytes = Files.readAllBytes(path);
                ImageDecoder.Decoded decoded = ImageDecoder.decode(bytes);
                if (persist) {
                    Files.createDirectories(SkyConfig.dir());
                    Files.write(SkyConfig.imageFile(), bytes);
                }
                Minecraft.getInstance().execute(() -> apply(decoded, displayName, persist));
            } catch (Throwable t) {
                SkyCustomClient.LOGGER.warn("Failed to load sky image {}", path, t);
                status = Component.translatable("skycustom.status.failed", String.valueOf(t.getMessage()));
                loading = false;
            }
        });
    }

    private static void apply(ImageDecoder.Decoded d, String name, boolean persist) {
        try {
            int w = d.width(), h = d.height();
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            int[] px = d.argb();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    // setPixel takes ARGB. If colours look red/blue swapped, use setPixelABGR with swapped channels.
                    image.setPixel(x, y, px[y * w + x]);
                }
            }
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.release(TEXTURE_ID);
            tm.register(TEXTURE_ID, new DynamicTexture(() -> "SkyCustom user sky", image));
            textureReady = true;
            if (persist) {
                SkyConfig.get().imageName = name;
                SkyConfig.save();
            }
            status = Component.translatable("skycustom.status.loaded", name, String.valueOf(w), String.valueOf(h));
        } catch (Throwable t) {
            SkyCustomClient.LOGGER.warn("Failed to upload sky texture", t);
            status = Component.translatable("skycustom.status.failed", String.valueOf(t.getMessage()));
        } finally {
            loading = false;
        }
    }

    public static void clear() {
        Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
        textureReady = false;
        try {
            Files.deleteIfExists(SkyConfig.imageFile());
        } catch (IOException ignored) {
        }
        SkyConfig.get().imageName = "";
        SkyConfig.save();
        status = Component.translatable("skycustom.status.cleared");
    }

    private SkyImageManager() {}
}
