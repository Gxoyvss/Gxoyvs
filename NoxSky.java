package com.example.noxvisuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.Random;

/** Ночное небо: мерцающие звёзды и северное сияние. Рисуется за всем миром (с проверкой глубины). */
public class NoxSky {
    static final int N = 1100;
    static final float[] sx = new float[N], sy = new float[N], sz = new float[N], sb = new float[N], sp = new float[N];

    static {
        Random r = new Random(7);
        for (int i = 0; i < N; i++) {
            double z = r.nextDouble() * 2 - 1, a = r.nextDouble() * Math.PI * 2, q = Math.sqrt(1 - z * z);
            sx[i] = (float) (q * Math.cos(a));
            sy[i] = (float) z;
            sz[i] = (float) (q * Math.sin(a));
            sb[i] = r.nextFloat();
            sp[i] = r.nextFloat() * 6.28f;
        }
    }

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(NoxSky::render);
    }

    static void render(WorldRenderContext ctx) {
        if (NoxConfig.sky == 0 || ctx.matrixStack() == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null || w.getRegistryKey() != World.OVERWORLD) return;

        float ang = w.getSkyAngle(0f);
        float g = MathHelper.clamp(1f - (MathHelper.cos(ang * (float) (Math.PI * 2)) * 2f + 0.25f), 0f, 1f);
        float night = g * g * (1f - w.getRainGradient(0f) * 0.9f);
        if (night < 0.02f) return;

        float R = Math.min(600f, Math.max(96f, mc.options.getClampedViewDistance() * 16f * 1.25f));
        Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
        float time = (System.currentTimeMillis() % 1000000L) / 1000f;
        int accL = NoxConfig.accLight();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // звёзды: небо вращается вокруг оси восток-запад, как ванильное
        float th = ang * (float) (Math.PI * 2), c = MathHelper.cos(th), s = MathHelper.sin(th);
        for (int i = 0; i < N; i++) {
            float x = sx[i] * c - sy[i] * s, y = sx[i] * s + sy[i] * c, z = sz[i];
            if (y < -0.03f) continue;
            float hl = (float) Math.sqrt(x * x + z * z);
            if (hl < 0.001f) continue;
            float rx = -z / hl, rz = x / hl;                       // «вправо» на небесной сфере
            float ux = y * rz, uy = z * rx - x * rz, uz = -y * rx; // «вверх»
            float tw = 0.55f + 0.45f * (float) Math.sin(time * 3 + sp[i]);
            float al = MathHelper.clamp(night * tw * (0.35f + 0.65f * sb[i]), 0f, 1f);
            float hs = R * (0.004f + 0.006f * sb[i]);
            int col = i % 4 == 0 ? accL : 0xFFFFFF;
            quad(buf, m, x * R, y * R, z * R, rx * hs, 0, rz * hs, ux * hs, uy * hs, uz * hs, col, al);
        }

        // северное сияние: три волнистые занавеси на севере
        if (NoxConfig.sky >= 2) {
            int segs = 36;
            for (int cu = 0; cu < 3; cu++) {
                float[] bx = new float[segs + 1], by = new float[segs + 1], bz = new float[segs + 1];
                float[] tx = new float[segs + 1], ty = new float[segs + 1], tz = new float[segs + 1];
                float span = 2.4f, phi0 = -1.2f + cu * 0.15f;
                float thB = 0.12f + cu * 0.06f, hgt = 0.35f + 0.1f * (float) Math.sin(time * 0.3f + cu);
                for (int i = 0; i <= segs; i++) {
                    float u = (float) i / segs;
                    float phi = phi0 + u * span + 0.12f * (float) Math.sin(time * 0.6f + u * 5f + cu * 2f);
                    float tb = thB + 0.05f * (float) Math.sin(time * 0.4f + u * 9f + cu);
                    float tt = tb + hgt * (0.6f + 0.4f * (float) Math.sin(time * 0.5f + u * 7f + cu * 1.7f));
                    float sinP = (float) Math.sin(phi), cosP = (float) Math.cos(phi);
                    bx[i] = sinP * (float) Math.cos(tb) * R; by[i] = (float) Math.sin(tb) * R; bz[i] = -cosP * (float) Math.cos(tb) * R;
                    tx[i] = sinP * (float) Math.cos(tt) * R; ty[i] = (float) Math.sin(tt) * R; tz[i] = -cosP * (float) Math.cos(tt) * R;
                }
                int low = cu == 1 ? 0x5CF0C0 : accL, high = NoxConfig.acc();
                for (int i = 0; i < segs; i++) {
                    float u0 = (float) i / segs, u1 = (float) (i + 1) / segs;
                    float a0 = night * 0.55f * (float) Math.sin(u0 * Math.PI) * (0.65f + 0.35f * (float) Math.sin(time * 0.8f + u0 * 11f + cu));
                    float a1 = night * 0.55f * (float) Math.sin(u1 * Math.PI) * (0.65f + 0.35f * (float) Math.sin(time * 0.8f + u1 * 11f + cu));
                    vert(buf, m, bx[i], by[i], bz[i], low, a0);
                    vert(buf, m, bx[i + 1], by[i + 1], bz[i + 1], low, a1);
                    vert(buf, m, tx[i + 1], ty[i + 1], tz[i + 1], high, 0f);
                    vert(buf, m, tx[i], ty[i], tz[i], high, 0f);
                }
            }
        }

        BuiltBuffer built = buf.endNullable();
        if (built == null) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static void vert(BufferBuilder b, Matrix4f m, float x, float y, float z, int rgb, float a) {
        b.vertex(m, x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (MathHelper.clamp(a, 0f, 1f) * 255));
    }

    static void quad(BufferBuilder b, Matrix4f m, float x, float y, float z,
                     float rx, float ry, float rz, float ux, float uy, float uz, int rgb, float a) {
        vert(b, m, x - rx - ux, y - ry - uy, z - rz - uz, rgb, a);
        vert(b, m, x + rx - ux, y + ry - uy, z + rz - uz, rgb, a);
        vert(b, m, x + rx + ux, y + ry + uy, z + rz + uz, rgb, a);
        vert(b, m, x - rx + ux, y - ry + uy, z - rz + uz, rgb, a);
    }
}
