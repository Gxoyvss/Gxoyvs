package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/** Target ESP: рамка-квадрат вокруг цели, уголки, кольцо, лунная метка над головой, пульс при ударе. */
public class NoxEsp {
    public static final String[] STYLES = {"Ring", "Box", "Corner"};
    static int n;

    static void dust(World w, int rgb, float size, double x, double y, double z) {
        w.addParticle(new DustParticleEffect(rgb, size), x, y, z, 0, 0, 0);
    }

    /** Цвет от красного (мало здоровья) к зелёному (много). */
    static int hpColor(LivingEntity t) {
        float k = Math.max(0f, Math.min(1f, t.getHealth() / Math.max(1f, t.getMaxHealth())));
        return NoxScreen.mix(0xE02040, 0x4CD070, k);
    }

    public static void tick(MinecraftClient mc, LivingEntity t, int pulse) {
        World w = mc.world;
        n++;
        if (NoxConfig.espStyle == 0) NoxVisualsClient.ring(w, t);
        if (n % 2 != 0) return;

        if (NoxConfig.espStyle != 0) {
            int col = NoxConfig.espHp ? hpColor(t) : NoxConfig.acc();
            boolean hot = NoxConfig.espPulse && pulse > 0;
            if (hot) col = 0xFFFFFF;
            Box b = t.getBoundingBox().expand(0.05 + (hot ? pulse * 0.04 : 0));
            double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
            for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++) {
                if (i == 0) edge(w, col, xs[0], ys[j], zs[k], xs[1], ys[j], zs[k]);
                if (j == 0) edge(w, col, xs[i], ys[0], zs[k], xs[i], ys[1], zs[k]);
                if (k == 0) edge(w, col, xs[i], ys[j], zs[0], xs[i], ys[j], zs[1]);
            }
        }
        if (NoxConfig.espMoon) moon(mc, w, t);
    }

    /** Линия из частиц; в режиме Corner рисуем только по краям ребра. */
    static void edge(World w, int col, double ax, double ay, double az, double bx, double by, double bz) {
        double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int steps = Math.max(2, (int) Math.ceil(len / 0.25));
        for (int s = 0; s <= steps; s++) {
            double k = (double) s / steps;
            if (NoxConfig.espStyle == 2 && k > 0.25 && k < 0.75) continue;
            dust(w, col, 0.6f, ax + (bx - ax) * k, ay + (by - ay) * k, az + (bz - az) * k);
        }
    }

    /** Маленький пиксельный полумесяц над головой, всегда повёрнут лицом к игроку. */
    static void moon(MinecraftClient mc, World w, LivingEntity t) {
        double dx = mc.player.getX() - t.getX(), dz = mc.player.getZ() - t.getZ();
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double rx = dz / len, rz = -dx / len;   // «вправо» с точки зрения игрока
        double cy = t.getY() + t.getHeight() + 0.6 + Math.sin(n * 0.1) * 0.05, cell = 0.07;
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            double d1 = i * i + j * j;
            if (d1 > 3.2 * 3.2) continue;
            double ex = i - 1.6, ey = j + 1.0;
            if (ex * ex + ey * ey <= 2.5 * 2.5) continue;
            int col = d1 > 2.2 * 2.2 ? NoxConfig.accLight() : 0xE9D5FF;
            dust(w, col, 0.5f, t.getX() + rx * i * cell, cy - j * cell, t.getZ() + rz * i * cell);
        }
    }
}
