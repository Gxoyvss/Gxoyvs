package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Цифры урона над целью, эффект убийства и счётчик серии убийств. */
public class NoxFx {
    static class Num { double x, y, z, dx, dz; long t0; String s; int tier; }

    static final List<Num> nums = new ArrayList<>();
    static final double LIFE = 1.2;   // секунд жизни цифры
    static final String[] KILL_NAMES = {"", "Kill", "Double Kill", "Triple Kill", "Quad Kill", "Rampage"};

    static LivingEntity last;
    static float lastHp;
    static boolean killDone;
    static int streak, streakTicks, streakShow;

    static float hp(LivingEntity e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }

    static void tick(MinecraftClient mc) {
        LivingEntity t = NoxVisualsClient.target;
        if (t != last) {
            last = t;
            killDone = false;
            lastHp = t != null ? hp(t) : 0f;
        }
        if (t != null) {
            float now = hp(t);
            if (NoxConfig.dmgNumbers && now < lastHp - 0.05f) spawn(t, lastHp - now);
            lastHp = now;
            if (!killDone && (t.isDead() || t.getHealth() <= 0f)) {
                killDone = true;
                onKill(mc, t);
            }
        }
        if (streakTicks > 0 && --streakTicks == 0) streak = 0;
        if (streakShow > 0) streakShow--;
        if (mc.player != null && mc.player.getHealth() <= 0f) streak = streakTicks = streakShow = 0;
    }

    static void spawn(LivingEntity t, float dmg) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Num n = new Num();
        n.x = t.getX() + (r.nextDouble() - 0.5) * 0.6;
        n.y = t.getY() + t.getHeight() + 0.1;
        n.z = t.getZ() + (r.nextDouble() - 0.5) * 0.6;
        n.dx = (r.nextDouble() - 0.5) * 0.5;
        n.dz = (r.nextDouble() - 0.5) * 0.5;
        n.t0 = System.currentTimeMillis();
        n.tier = dmg >= 8f ? 2 : (dmg >= 4f ? 1 : 0);
        String s = String.format(Locale.ROOT, "%.1f", dmg);
        n.s = s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
        if (nums.size() >= 24) nums.remove(0);
        nums.add(n);
    }

    static void onKill(MinecraftClient mc, LivingEntity t) {
        if (NoxConfig.killFx && mc.world != null) {
            killBurst(mc.world, t);
            NoxVisualsClient.flash = 14;
        }
        if (NoxConfig.killStreak) {
            streak++;
            streakTicks = 200;   // 10 секунд на следующее убийство
            streakShow = 60;
        }
    }

    /** Взрыв душ: частицы вверх, два кольца на земле и спираль вокруг тела. */
    static void killBurst(World w, LivingEntity e) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double cx = e.getX(), cz = e.getZ(), h = e.getHeight(), y = e.getY() + h / 2;
        for (int i = 0; i < 36; i++) {
            var type = i % 3 == 0 ? ParticleTypes.SOUL : (i % 3 == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.WITCH);
            w.addParticle(type, cx, y, cz,
                (r.nextDouble() - 0.5) * 0.5, 0.1 + r.nextDouble() * 0.25, (r.nextDouble() - 0.5) * 0.5);
        }
        for (int i = 0; i < 10; i++) {
            w.addParticle(ParticleTypes.LARGE_SMOKE, cx, y, cz,
                (r.nextDouble() - 0.5) * 0.3, 0.05, (r.nextDouble() - 0.5) * 0.3);
        }
        for (int ring = 0; ring < 2; ring++) {
            double rad = 0.5 + ring * 0.6;
            for (int i = 0; i < 28; i++) {
                double a = i * Math.PI * 2 / 28;
                NoxEsp.dust(w, ring == 0 ? NoxConfig.acc() : NoxConfig.accLight(), 0.8f,
                    cx + Math.cos(a) * rad, e.getY() + 0.1, cz + Math.sin(a) * rad);
            }
        }
        for (int i = 0; i < 40; i++) {
            double a = i * 0.55, rad = 0.7 * (1 - i / 60.0);
            NoxEsp.dust(w, NoxConfig.accLight(), 0.6f,
                cx + Math.cos(a) * rad, e.getY() + h * i / 40.0, cz + Math.sin(a) * rad);
        }
    }

    // ---------- HUD ----------

    static void hud(DrawContext ctx, MinecraftClient mc) {
        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        drawNumbers(ctx, mc, W, H);
        drawStreak(ctx, mc, W, H);
    }

    /** Переводим мировую точку в экран по направлению и FOV камеры (без доступа к матрицам). */
    static void drawNumbers(DrawContext ctx, MinecraftClient mc, int W, int H) {
        if (nums.isEmpty()) return;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw()), pit = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pit), cpi = Math.cos(pit);
        double fx = -sy * cpi, fy = -sp, fz = cy * cpi;      // вперёд
        double rx = -cy, rz = -sy;                            // вправо
        double ux = -sy * sp, uy = cpi, uz = cy * sp;         // вверх
        double fov = mc.options.getFov().getValue();
        double th = Math.tan(Math.toRadians(fov) / 2);
        double aspect = (double) W / H;
        long now = System.currentTimeMillis();

        Iterator<Num> it = nums.iterator();
        while (it.hasNext()) {
            Num n = it.next();
            double age = (now - n.t0) / 1000.0;
            if (age > LIFE) { it.remove(); continue; }
            double k = age / LIFE, ease = 1 - (1 - k) * (1 - k);
            double vx = n.x + n.dx * age - cp.x, vy = n.y + 0.9 * ease - cp.y, vz = n.z + n.dz * age - cp.z;
            double z = vx * fx + vy * fy + vz * fz;
            if (z < 0.3) continue;
            double px = (vx * rx + vz * rz) / (z * th * aspect);
            double py = (vx * ux + vy * uy + vz * uz) / (z * th);
            float sx = (float) (W / 2.0 + px * W / 2.0), sY = (float) (H / 2.0 - py * H / 2.0);

            double fade = k > 0.6 ? 1 - (k - 0.6) / 0.4 : 1;
            int a = (int) (fade * 255);
            if (a < 12) continue;
            float pop = 1f + 0.6f * (float) Math.max(0, 1 - age / 0.18);
            float s = (float) Math.max(0.55, Math.min(1.5, 8 / z)) * pop * 1.1f;
            int rgb = n.tier == 2 ? 0xFFD54A : (n.tier == 1 ? 0xFFFFFF : NoxConfig.accLight());

            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(sx, sY, 0f);
            ms.scale(s, s, 1f);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, n.s, 0, -4, (a << 24) | rgb);
            ms.pop();
        }
    }

    static void drawStreak(DrawContext ctx, MinecraftClient mc, int W, int H) {
        if (!NoxConfig.killStreak || streakShow <= 0 || streak <= 0) return;
        int a = Math.min(255, streakShow * 255 / 15);
        if (a < 12) return;
        float pop = 1f + 0.5f * Math.max(0f, 1f - (60 - streakShow) / 6f);
        String name = KILL_NAMES[Math.min(streak, KILL_NAMES.length - 1)];
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(W / 2f, H / 2f - 58, 0f);
        ms.scale(1.8f * pop, 1.8f * pop, 1f);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, name, 0, 0, (a << 24) | NoxConfig.accLight());
        ms.pop();
        ctx.drawCenteredTextWithShadow(mc.textRenderer, "Streak x" + streak, W / 2, H / 2 - 40, (a << 24) | 0xE9D5FF);
    }
}
