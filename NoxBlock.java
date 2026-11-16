package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Подсветка блока, на котором стоишь: контур, сетка или пульсирующий квадрат. Только у тебя на экране. */
public class NoxBlock {
    public static final String[] MODES = {"Off", "Line", "Grid", "Pulse"};
    static int n;

    public static void tick(MinecraftClient mc) {
        if (NoxConfig.blockFx == 0 || mc.player == null || mc.world == null || !mc.player.isOnGround()) return;
        n++;
        if (n % 2 != 0) return;
        World w = mc.world;
        BlockPos bp = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        if (w.getBlockState(bp).isAir()) return;
        double cx = bp.getX() + 0.5, cz = bp.getZ() + 0.5, y = mc.player.getY() + 0.03;
        int col = NoxConfig.PALETTE[NoxConfig.blockColor];
        int mode = NoxConfig.blockFx;
        if (mode == 1) {
            square(w, col, cx, cz, y, 0.5);
        } else if (mode == 2) {
            square(w, col, cx, cz, y, 0.5);
            line(w, col, cx - 0.5, cz, cx + 0.5, cz, y);
            line(w, col, cx, cz - 0.5, cx, cz + 0.5, y);
            square(w, NoxScreen.mix(col, 0xFFFFFF, 0.4f), cx, cz, y, 0.25);
        } else {
            double ph = (n % 40) / 40.0;
            square(w, col, cx, cz, y, 0.12 + 0.38 * ph);
            if (ph < 0.5) square(w, NoxScreen.mix(col, 0xFFFFFF, 0.4f), cx, cz, y, 0.05 + 0.2 * ph);
        }
    }

    static void square(World w, int col, double cx, double cz, double y, double h) {
        line(w, col, cx - h, cz - h, cx + h, cz - h, y);
        line(w, col, cx + h, cz - h, cx + h, cz + h, y);
        line(w, col, cx + h, cz + h, cx - h, cz + h, y);
        line(w, col, cx - h, cz + h, cx - h, cz - h, y);
    }

    static void line(World w, int col, double ax, double az, double bx, double bz, double y) {
        double len = Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
        int steps = Math.max(2, (int) Math.ceil(len / 0.2));
        for (int s = 0; s <= steps; s++) {
            double k = (double) s / steps;
            NoxEsp.dust(w, col, 0.5f, ax + (bx - ax) * k, y, az + (bz - az) * k);
        }
    }
}
