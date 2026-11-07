package com.skycustom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.List;
import java.util.function.DoubleConsumer;

public class SkyConfigScreen extends Screen {
    private final Screen parent;
    private EditBox pathBox;

    public SkyConfigScreen(Screen parent) {
        super(Component.translatable("skycustom.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SkyConfig cfg = SkyConfig.get();
        int left = this.width / 2 - 155;
        int y = Math.max(40, this.height / 2 - 105);

        pathBox = new EditBox(this.font, left, y, 235, 20, Component.translatable("skycustom.path"));
        pathBox.setMaxLength(2048);
        pathBox.setHint(Component.translatable("skycustom.path.hint"));
        addRenderableWidget(pathBox);
        addRenderableWidget(Button.builder(Component.translatable("skycustom.browse"), b -> browse())
                .bounds(left + 240, y, 70, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("skycustom.load"), b -> loadTyped())
                .bounds(left, y, 152, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("skycustom.clear"), b -> SkyImageManager.clear())
                .bounds(left + 158, y, 152, 20).build());

        y += 30;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled).create(left, y, 310, 20,
                Component.translatable("skycustom.enabled"), (btn, value) -> cfg.enabled = value));

        y += 24;
        addRenderableWidget(new ValueSlider(left, y, 310, 20, "skycustom.brightness", 0, 100, cfg.brightness * 100, "%",
                v -> cfg.brightness = (float) (v / 100.0)));

        y += 24;
        addRenderableWidget(new ValueSlider(left, y, 310, 20, "skycustom.opacity", 0, 100, cfg.opacity * 100, "%",
                v -> cfg.opacity = (float) (v / 100.0)));

        y += 24;
        addRenderableWidget(new ValueSlider(left, y, 310, 20, "skycustom.rotation", 0, 360, cfg.yaw, "\u00b0",
                v -> cfg.yaw = (float) v));

        y += 24;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideCelestials).create(left, y, 310, 20,
                Component.translatable("skycustom.hide_celestials"), (btn, value) -> cfg.hideCelestials = value));

        y += 30;
        addRenderableWidget(Button.builder(Component.translatable("skycustom.done"), b -> onClose())
                .bounds(left + 55, y, 200, 20).build());
    }

    private void loadTyped() {
        String text = pathBox.getValue().trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1); // Windows "Copy as path" adds quotes
        }
        if (text.isEmpty()) return;
        try {
            SkyImageManager.loadFromPath(Path.of(text));
        } catch (Exception e) {
            SkyImageManager.setStatus(Component.translatable("skycustom.status.failed", e.getMessage()));
        }
    }

    private void browse() {
        Thread t = new Thread(() -> {
            String chosen = null;
            try {
                // No filter on purpose, so any image type can be picked.
                chosen = TinyFileDialogs.tinyfd_openFileDialog("Choose a sky image", null, null, null, false);
            } catch (Throwable e) {
                SkyImageManager.setStatus(Component.translatable("skycustom.status.dialog_failed"));
            }
            if (chosen != null && !chosen.isBlank()) {
                String path = chosen;
                Minecraft.getInstance().execute(() -> {
                    pathBox.setValue(path);
                    SkyImageManager.loadFromPath(Path.of(path));
                });
            }
        }, "SkyCustom-FileDialog");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!paths.isEmpty()) {
            pathBox.setValue(paths.get(0).toString());
            SkyImageManager.loadFromPath(paths.get(0));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, 15, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, SkyImageManager.getStatus(), cx, 28, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, Component.translatable("skycustom.tip"), cx, this.height - 20, 0xFF808080);
    }

    @Override
    public void removed() {
        SkyConfig.save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final String unit;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int w, int h, String key, double min, double max, double current,
                    String unit, DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), (current - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.onChange = onChange;
            updateMessage();
        }

        private double actual() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            if (key == null) return; // called by super constructor before fields are set
            setMessage(Component.translatable(key).append(": " + Math.round(actual()) + unit));
        }

        @Override
        protected void applyValue() {
            onChange.accept(actual());
        }
    }
}
