package com.skycustom;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Decodes arbitrary image bytes into ARGB pixels. STB first (PNG/JPG/GIF/BMP/TGA/PSD/HDR/PIC/PNM), ImageIO as fallback. */
final class ImageDecoder {
    static final int MAX_SIDE = 4096;
    private static final long MAX_PIXELS = 150_000_000L;

    record Decoded(int width, int height, int[] argb) {}

    private static final class TooLarge extends IOException {
        TooLarge() { super("Image has too many pixels"); }
    }

    static Decoded decode(byte[] data) throws IOException {
        Decoded d = null;
        try {
            d = decodeWithStb(data);
        } catch (TooLarge e) {
            throw e;
        } catch (Throwable ignored) {
        }
        if (d == null) {
            try {
                d = decodeWithImageIo(data);
            } catch (TooLarge e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        if (d == null) throw new IOException("Unsupported or corrupt image format");
        return downscale(d);
    }

    private static Decoded decodeWithStb(byte[] data) throws IOException {
        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        try {
            buf.put(data).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                if (!STBImage.stbi_info_from_memory(buf, w, h, c)) return null;
                if ((long) w.get(0) * h.get(0) > MAX_PIXELS) throw new TooLarge();
                ByteBuffer px = STBImage.stbi_load_from_memory(buf, w, h, c, 4);
                if (px == null) return null;
                try {
                    int width = w.get(0), height = h.get(0);
                    int[] out = new int[width * height];
                    for (int i = 0; i < out.length; i++) {
                        int r = px.get(i * 4) & 0xFF;
                        int g = px.get(i * 4 + 1) & 0xFF;
                        int b = px.get(i * 4 + 2) & 0xFF;
                        int a = px.get(i * 4 + 3) & 0xFF;
                        out[i] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    return new Decoded(width, height, out);
                } finally {
                    STBImage.stbi_image_free(px);
                }
            }
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    private static Decoded decodeWithImageIo(byte[] data) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) return null;
        int w = img.getWidth(), h = img.getHeight();
        if ((long) w * h > MAX_PIXELS) throw new TooLarge();
        return new Decoded(w, h, img.getRGB(0, 0, w, h, null, 0, w));
    }

    /** Box-filter downscale so the longest side fits in MAX_SIDE. */
    private static Decoded downscale(Decoded d) {
        int longest = Math.max(d.width(), d.height());
        if (longest <= MAX_SIDE) return d;
        int f = (longest + MAX_SIDE - 1) / MAX_SIDE;
        int nw = Math.max(1, d.width() / f);
        int nh = Math.max(1, d.height() / f);
        int[] src = d.argb();
        int[] out = new int[nw * nh];
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                long a = 0, r = 0, g = 0, b = 0;
                int n = 0;
                for (int dy = 0; dy < f; dy++) {
                    int sy = y * f + dy;
                    if (sy >= d.height()) break;
                    for (int dx = 0; dx < f; dx++) {
                        int sx = x * f + dx;
                        if (sx >= d.width()) break;
                        int p = src[sy * d.width() + sx];
                        a += p >>> 24;
                        r += (p >> 16) & 0xFF;
                        g += (p >> 8) & 0xFF;
                        b += p & 0xFF;
                        n++;
                    }
                }
                out[y * nw + x] = ((int) (a / n) << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
            }
        }
        return new Decoded(nw, nh, out);
    }

    private ImageDecoder() {}
}
