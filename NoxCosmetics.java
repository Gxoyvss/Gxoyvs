package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Косметика из частиц: крылья, рюкзак, нимб/корона. Видна только у тебя (мод клиентский). */
public class NoxCosmetics {
    public static final String[] WINGS = {"Off", "Angel", "Demon", "Pixel"};
    public static final String[] PACKS = {"Off", "Pack", "Jet"};
    public static final String[] HEADS = {"Off", "Halo", "Crown"};

    static int ticks;
    static World world;

    /** Точка в системе игрока: side — вправо, up — вверх от ног, back — назад. */
    static Vec3d at(Entity e, double yaw, double side, double up, double back) {
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r), fz = Math.cos(r);
        double rx = -Math.cos(r), rz = -Math.sin(r);
        return new Vec3d(e.getX() + rx * side - fx * back, e.getY() + up, e.getZ() + rz * side - fz * back);
    }

    static void dust(int rgb, float size, Vec3d p) {
        world.addParticle(new DustParticleEffect(rgb, size), p.x, p.y, p.z, 0, 0, 0);
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity pl = mc.player;
        if (pl == null || mc.world == null) return;
        world = mc.world;
        ticks++;
        if (ticks % 2 != 0) return;
        double yaw = pl.getBodyYaw();
        if (NoxConfig.wings != 0) wings(pl, yaw);
        if (NoxConfig.pack != 0) pack(pl, yaw);
        if (NoxConfig.head != 0) head(pl);
    }

    static void wings(ClientPlayerEntity pl, double yaw) {
        int kind = NoxConfig.wings;
        double flap = pl.isOnGround() ? 0.08 : 0.3;
        double open = 0.8 + flap * Math.sin(ticks * 0.3);
        int col = kind == 1 ? 0xF4F4FF : kind == 2 ? 0xC01838 : NoxConfig.acc();
        for (int sd = -1; sd <= 1; sd += 2) {
            if (kind == 1) {            // ангельские: 5 перьев веером
                for (int f = 0; f < 5; f++) {
                    double a = Math.toRadians(75 - f * 27.5), len = 1.3 - f * 0.15;
                    for (double d = 0.2; d <= len; d += 0.18) spoke(pl, yaw, sd, a, d, open, col);
                }
            } else if (kind == 2) {     // демонические: 4 длинные кости + перепонка
                double[] ang = {85, 50, 15, -20}, len = {1.5, 1.35, 1.1, 0.8};
                double[][] tip = new double[4][];
                for (int f = 0; f < 4; f++) {
                    double a = Math.toRadians(ang[f]);
                    for (double d = 0.2; d <= len[f]; d += 0.2) spoke(pl, yaw, sd, a, d, open, col);
                    tip[f] = new double[]{0.12 + Math.cos(a) * len[f] * open, 1.25 + Math.sin(a) * len[f], 0.2 + len[f] * 0.1};
                }
                for (int f = 0; f < 3; f++) {
                    for (int k = 1; k <= 2; k++) {
                        double t = k / 3.0;
                        dust(col, 0.7f, at(pl, yaw, sd * (tip[f][0] + (tip[f + 1][0] - tip[f][0]) * t),
                            tip[f][1] + (tip[f + 1][1] - tip[f][1]) * t, tip[f][2] + (tip[f + 1][2] - tip[f][2]) * t));
                    }
                }
            } else {                    // пиксельные: ступенчатый треугольник
                for (int r = 0; r < 5; r++) {
                    for (int c = 0; c < 6 - r; c++) {
                        dust(col, 0.7f, at(pl, yaw, sd * (0.18 + c * 0.17 * open), 1.6 - r * 0.17, 0.2));
                    }
                }
            }
        }
    }

    static void spoke(Entity pl, double yaw, int sd, double a, double d, double open, int col) {
        dust(col, 0.7f, at(pl, yaw, sd * (0.12 + Math.cos(a) * d * open), 1.25 + Math.sin(a) * d, 0.2 + d * 0.1));
    }

    static void pack(ClientPlayerEntity pl, double yaw) {
        if (NoxConfig.pack == 1) {
            box(pl, yaw, -0.2, 0.2, 0.7, 1.3, 0.18, 0.4, 0x8B5A2B);
            box(pl, yaw, -0.12, 0.12, 0.75, 0.95, 0.4, 0.46, 0xC08040);
        } else {
            box(pl, yaw, -0.17, -0.03, 0.55, 1.3, 0.18, 0.32, 0x9999AA);
            box(pl, yaw, 0.03, 0.17, 0.55, 1.3, 0.18, 0.32, 0x9999AA);
            if (!pl.isOnGround()) {
                for (int sd = -1; sd <= 1; sd += 2) {
                    Vec3d p = at(pl, yaw, sd * 0.1, 0.5, 0.25);
                    world.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, -0.05, 0);
                }
            }
        }
    }

    static void box(Entity e, double yaw, double x0, double x1, double y0, double y1, double b0, double b1, int col) {
        double[] xs = {x0, x1}, ys = {y0, y1}, bs = {b0, b1};
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++) {
            if (i == 0) line(e, yaw, xs[0], ys[j], bs[k], xs[1], ys[j], bs[k], col);
            if (j == 0) line(e, yaw, xs[i], ys[0], bs[k], xs[i], ys[1], bs[k], col);
            if (k == 0) line(e, yaw, xs[i], ys[j], bs[0], xs[i], ys[j], bs[1], col);
        }
    }

    static void line(Entity e, double yaw, double ax, double ay, double ab, double bx, double by, double bb, int col) {
        for (int s = 0; s <= 3; s++) {
            double t = s / 3.0;
            dust(col, 0.6f, at(e, yaw, ax + (bx - ax) * t, ay + (by - ay) * t, ab + (bb - ab) * t));
        }
    }

    static void head(ClientPlayerEntity pl) {
        double h = pl.getHeight();
        boolean crown = NoxConfig.head == 2;
        double y = crown ? h + 0.05 : h + 0.3 + Math.sin(ticks * 0.1) * 0.03;
        double rad = crown ? 0.26 : 0.28;
        int n = crown ? 10 : 14;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            double x = pl.getX() + Math.cos(a) * rad, z = pl.getZ() + Math.sin(a) * rad;
            dust(0xFFE066, 0.7f, new Vec3d(x, pl.getY() + y, z));
            if (crown && i % 2 == 0) {
                dust(0xFFE066, 0.7f, new Vec3d(x, pl.getY() + y + 0.08, z));
                dust(0xFFE066, 0.7f, new Vec3d(x, pl.getY() + y + 0.16, z));
            }
        }
    }
}
