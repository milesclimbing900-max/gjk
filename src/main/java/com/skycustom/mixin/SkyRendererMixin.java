package com.skycustom.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.skycustom.SkyConfig;
import com.skycustom.SkyDomeRenderer;
import com.skycustom.SkyImageManager;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    /** Runs right before vanilla draws the sun, moon and stars, so those still render on top of the image. */
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void skycustom$drawImageSky(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
                                        MoonPhase moonPhase, float rainBrightness, float starBrightness,
                                        CallbackInfo ci) {
        SkyDomeRenderer.render(poseStack);
        SkyConfig cfg = SkyConfig.get();
        if (cfg.enabled && cfg.hideCelestials && SkyImageManager.isReady()) {
            ci.cancel();
        }
    }
}
