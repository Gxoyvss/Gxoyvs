package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Коллекция граффити: рисунок 10x10, «рисуется» цветными частицами на блоке, на который смотришь. */
public class NoxGraffiti {
    public static final String[] NAMES = {"Skull", "Heart", "Smile", "Crown", "Bolt", "Nox"};

    // W белый, K тёмный, R красный, Y жёлтый, P акцентный цвет, . пусто
    public static final String[][] ART = {
        {"..WWWWWW..", ".WWWWWWWW.", "WWWWWWWWWW", "WKKWWWWKKW", "WKKWWWWKKW",
         "WWWWKKWWWW", ".WWWKKWWW.", "..WWWWWW..", "..W.W.W.W.", "..WWWWWW.."},
        {"..RR..RR..", ".RRRRRRRR.", "RRRRRRRRRR", "RRRRRRRRRR", "RRRRRRRRRR",
         ".RRRRRRRR.", "..RRRRRR..", "...RRRR...", "....RR....", ".........."},
        {"...YYYY...", ".YYYYYYYY.", ".YYYYYYYY.", "YYKYYYYKYY", "YYKYYYYKYY",
         "YYYYYYYYYY", "YKYYYYYYKY", ".YKKKKKKY.", ".YYYYYYYY.", "...YYYY..."},
        {"Y...YY...Y", "YY..YY..YY", "YYY.YY.YYY", "YYYYYYYYYY", "YYYYYYYYYY",
         "YYRYYYYRYY", "YYYYYYYYYY", "YYYYYYYYYY", "..........", ".........."},
        {"......PP..", ".....PP...", "....PP....", "...PPPPPP.", "....PPPP..",
         ".....PP...", "....PP....", "...PP.....", "..PP......", "..P......."},
        {"PP......PP", "PPP.....PP", "PPPP....PP", "PP.PP...PP", "PP..PP..PP",
         "PP...PP.PP", "PP....PPPP", "PP.....PPP", "PP......PP", "PP......PP"}
    };

    public static int color(char ch) {
        return switch (ch) {
            case 'W' -> 0xF2F2F2;
            case 'K' -> 0x2B2B3A;
            case 'R' -> 0xE02040;
            case 'Y' -> 0xFFD54A;
            default -> NoxConfig.acc();
        };
    }

    static class Tag {
        Vec3d o, u, v;   // угол-центр, вправо, вверх
        int design;
    }

    static final List<Tag> tags = new ArrayList<>();
    static int tick;

    public static void clear() {
        tags.clear();
    }

    /** Нанести выбранное граффити на блок, на который смотрит игрок. */
    public static void spray(MinecraftClient mc) {
        if (mc.player == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            mc.player.sendMessage(Text.literal("NoxVisuals: наведись на блок"), true);
            return;
        }
        Direction side = hit.getSide();
        Vec3d n = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Tag t = new Tag();
        t.design = NoxConfig.gDesign;
        t.o = hit.getPos().add(n.multiply(0.05));
        if (side.getAxis() == Direction.Axis.Y) {
            double r = Math.toRadians(mc.player.getYaw());
            t.u = new Vec3d(-Math.cos(r), 0, -Math.sin(r));
            t.v = new Vec3d(-Math.sin(r), 0, Math.cos(r));
        } else {
            t.v = new Vec3d(0, 1, 0);
            t.u = n.multiply(-1).crossProduct(t.v);
        }
        tags.add(t);
        if (tags.size() > 8) tags.remove(0);
        NoxConfig.gShow = true;
        mc.player.sendMessage(Text.literal("NoxVisuals: граффити «" + NAMES[t.design] + "»"), true);
    }

    /** Частицы живут недолго, поэтому каждые 5 тиков рисуем граффити заново. */
    public static void tick(MinecraftClient mc) {
        if (!NoxConfig.gShow || tags.isEmpty() || mc.world == null || mc.player == null) return;
        if (++tick % 5 != 0) return;
        double s = 0.1;
        for (Tag t : tags) {
            if (mc.player.getPos().squaredDistanceTo(t.o) > 32 * 32) continue;
            String[] rows = ART[t.design];
            for (int r = 0; r < 10; r++) {
                for (int q = 0; q < 10; q++) {
                    char ch = rows[r].charAt(q);
                    if (ch == '.') continue;
                    Vec3d p = t.o.add(t.u.multiply((q - 4.5) * s)).add(t.v.multiply((4.5 - r) * s));
                    mc.world.addParticle(new DustParticleEffect(color(ch), 0.9f), p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }
}
