package com.skycustom;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Draws the user's image on an equirectangular sphere around the camera. */
public final class SkyDomeRenderer {
    private static final int LON = 64;
    private static final int LAT = 32;
    private static final float RADIUS = 100.0f;
    /** x, y, z, u, v per vertex, (LAT+1) x (LON+1) grid. */
    private static final float[] GRID = buildGrid();

    private static RenderType renderType;

    private static float[] buildGrid() {
        float[] g = new float[(LAT + 1) * (LON + 1) * 5];
        int k = 0;
        for (int i = 0; i <= LAT; i++) {
            double lat = Math.PI / 2.0 - Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double lon = 2.0 * Math.PI * j / LON;
                g[k++] = (float) (Math.cos(lat) * Math.sin(lon) * RADIUS);
                g[k++] = (float) (Math.sin(lat) * RADIUS);
                g[k++] = (float) (-Math.cos(lat) * Math.cos(lon) * RADIUS);
                g[k++] = (float) j / LON;
                g[k++] = (float) i / LAT;
            }
        }
        return g;
    }

    private static RenderType renderType() {
        if (renderType == null) {
            RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(SkyCustomClient.MOD_ID, "pipeline/user_sky"))
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .build();
            renderType = RenderType.create("skycustom_user_sky",
                    RenderSetup.builder(pipeline)
                            .withTexture("Sampler0", SkyImageManager.TEXTURE_ID)
                            .createRenderSetup());
        }
        return renderType;
    }

    public static void render(PoseStack poseStack) {
        SkyConfig cfg = SkyConfig.get();
        if (!cfg.enabled || !SkyImageManager.isReady()) return;

        RenderType type = renderType();
        Matrix4f m = new Matrix4f(poseStack.last().pose()).rotateY((float) Math.toRadians(cfg.yaw));
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(type);
        float c = cfg.brightness;
        float a = cfg.opacity;

        for (int i = 0; i < LAT; i++) {
            for (int j = 0; j < LON; j++) {
                vertex(vc, m, i, j, c, a);
                vertex(vc, m, i + 1, j, c, a);
                vertex(vc, m, i + 1, j + 1, c, a);
                vertex(vc, m, i, j + 1, c, a);
            }
        }
        buffers.endBatch(type);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, int i, int j, float c, float a) {
        int k = (i * (LON + 1) + j) * 5;
        vc.addVertex(m, GRID[k], GRID[k + 1], GRID[k + 2]).setUv(GRID[k + 3], GRID[k + 4]).setColor(c, c, c, a);
    }

    private SkyDomeRenderer() {}
}
