package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

import java.util.concurrent.ThreadLocalRandom;

/** Стили трейла за игроком: стандарт, спираль, кольца, звёздная пыль, огонь. */
public class NoxTrail {
    public static final String[] STYLES = {"Std", "Helix", "Rings", "Dust", "Fire"};
    static int n;

    static void tick(MinecraftClient mc) {
        n++;
        if (!NoxConfig.trail || mc.player == null || mc.world == null) return;
        if (mc.player.getVelocity().horizontalLengthSquared() <= 0.002) return;
        World w = mc.world;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        switch (NoxConfig.trailStyle) {
            case 1 -> {   // двойная спираль, которая «ходит» вверх-вниз
                double yy = y + 0.9 + Math.sin(n * 0.22) * 0.85;
                for (int k = 0; k < 2; k++) {
                    double a = n * 0.55 + k * Math.PI;
                    NoxEsp.dust(w, k == 0 ? NoxConfig.acc() : NoxConfig.accLight(), 0.7f,
                        x + Math.cos(a) * 0.5, yy, z + Math.sin(a) * 0.5);
                }
            }
            case 2 -> {   // кольца, остающиеся на земле
                if (n % 3 != 0) return;
                int col = (n / 3) % 2 == 0 ? NoxConfig.acc() : NoxConfig.accLight();
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI * 2 / 16;
                    NoxEsp.dust(w, col, 0.6f, x + Math.cos(a) * 0.55, y + 0.08, z + Math.sin(a) * 0.55);
                }
            }
            case 3 -> {   // звёздная пыль
                for (int i = 0; i < 2; i++) {
                    w.addParticle(ParticleTypes.END_ROD,
                        x + (r.nextDouble() - 0.5) * 0.8, y + r.nextDouble() * 1.8, z + (r.nextDouble() - 0.5) * 0.8,
                        0, 0.01, 0);
                }
                NoxEsp.dust(w, NoxConfig.accLight(), 0.6f,
                    x + (r.nextDouble() - 0.5) * 0.8, y + r.nextDouble() * 1.8, z + (r.nextDouble() - 0.5) * 0.8);
            }
            case 4 -> {   // синий огонь душ
                w.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    x + (r.nextDouble() - 0.5) * 0.4, y + 0.05, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
                if (n % 2 == 0) {
                    w.addParticle(ParticleTypes.WITCH,
                        x + (r.nextDouble() - 0.5) * 0.4, y + 0.2, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
                }
            }
            default -> w.addParticle(ParticleTypes.END_ROD, x, y + 0.1, z, 0, 0.01, 0);
        }
    }
}
