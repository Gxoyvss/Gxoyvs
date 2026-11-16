package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Свой прицел: точка, крест или кольцо. Расходится и вспыхивает при ударе. */
public class NoxCrosshair {
    public static final String[] STYLES = {"Stock", "Dot", "Cross", "Ring"};

    static void draw(DrawContext ctx, MinecraftClient mc) {
        if (NoxConfig.crossStyle == 0 || mc.currentScreen != null || !mc.options.getPerspective().isFirstPerson()) return;
        int cx = (ctx.getScaledWindowWidth() - 1) / 2, cy = (ctx.getScaledWindowHeight() - 1) / 2;
        int spread = NoxConfig.crossSpread ? NoxVisualsClient.pulse : 0;
        render(ctx, cx, cy, NoxConfig.crossStyle, NoxConfig.crossSize, spread);
    }

    /** Рисует прицел в точке (cx, cy); используется и в игре, и в превью меню. */
    static void render(DrawContext ctx, int cx, int cy, int style, int size, int spread) {
        int rgb = spread > 0 ? 0xFFFFFF : (NoxConfig.crossAcc ? NoxConfig.accLight() : 0xFFFFFF);
        int col = 0xFF000000 | rgb, shade = 0xA0000000;
        if (style == 1) {
            ctx.fill(cx - 2, cy - 2, cx + 3, cy + 3, shade);
            ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        } else if (style == 2) {
            int g = 3 + spread, l = size;
            int[][] arms = {
                {cx, cy - g - l, cx + 1, cy - g},
                {cx, cy + 1 + g, cx + 1, cy + 1 + g + l},
                {cx - g - l, cy, cx - g, cy + 1},
                {cx + 1 + g, cy, cx + 1 + g + l, cy + 1}
            };
            for (int[] a : arms) ctx.fill(a[0] - 1, a[1] - 1, a[2] + 1, a[3] + 1, shade);
            for (int[] a : arms) ctx.fill(a[0], a[1], a[2], a[3], col);
        } else if (style == 3) {
            double r = size / 2.0 + 2 + spread * 0.7;
            int pts = Math.max(20, (int) (r * 8));
            for (int pass = 0; pass < 2; pass++) {
                double rr = pass == 0 ? r + 1 : r;
                for (int i = 0; i < pts; i++) {
                    double a = i * Math.PI * 2 / pts;
                    int x = cx + (int) Math.round(Math.cos(a) * rr), y = cy + (int) Math.round(Math.sin(a) * rr);
                    ctx.fill(x, y, x + 1, y + 1, pass == 0 ? shade : col);
                }
            }
            ctx.fill(cx, cy, cx + 1, cy + 1, col);
        }
    }
}
