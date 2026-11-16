package com.example.noxvisuals;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Locale;

/**
 * Радиус действия предметов: пока предмет в руке, вокруг тебя рисуется зона из частиц.
 * Предмет определяется по названию (в нижнем регистре ищется ключевое слово).
 */
public class NoxRadius {
    static final int TRAP = 0, CIRCLE = 1;

    static final class Def {
        final String key;       // ключевое слово в названии предмета
        final int shape;        // TRAP — клетка 3x3, CIRCLE — круг
        final double radius;    // для круга, в блоках
        final int color;
        final String[] lines;   // подпись на экране

        Def(String key, int shape, double radius, int color, String... lines) {
            this.key = key;
            this.shape = shape;
            this.radius = radius;
            this.color = color;
            this.lines = lines;
        }
    }

    /** Радиус Божьей ауры: в запросе он не был указан, поставил 5. Поменяй число здесь, если на сервере другое. */
    static final double AURA_RADIUS = 5.0;

    static final Def[] ITEMS = {
        new Def("трапк", TRAP, 0, 0x8A2BE2, "Трапка", "Клетка 3x3 (9 блоков)"),
        new Def("дезориентац", CIRCLE, 10, 0xE0407A, "Дезориентация", "Радиус: 10 блоков"),
        new Def("явная пыль", CIRCLE, 10, 0xE0782C, "Явная пыль", "Радиус: 10 блоков"),
        new Def("божья аура", CIRCLE, AURA_RADIUS, 0xFFD54A, "Божья аура", "Радиус: " + (int) AURA_RADIUS + " блоков",
            "Снимает негативные эффекты", "Даёт положительные")
    };

    static Def current;   // предмет в руке (для подписи), null = ничего
    static int n;

    static Def find(ItemStack st) {
        if (st.isEmpty()) return null;
        String name = st.getName().getString().toLowerCase(Locale.ROOT);
        for (Def d : ITEMS) if (name.contains(d.key)) return d;
        return null;
    }

    static void tick(MinecraftClient mc) {
        current = null;
        if (!NoxConfig.itemRadius || mc.player == null || mc.world == null) return;
        Def d = find(mc.player.getMainHandStack());
        if (d == null) d = find(mc.player.getOffHandStack());
        current = d;
        if (d == null || ++n % 3 != 0) return;

        World w = mc.world;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        if (d.shape == TRAP) {
            trap(w, d.color, px, py, pz);
        } else {
            circle(w, d.color, px, py, pz, d.radius);
            if (d.key.equals("божья аура")) {
                // внутреннее пульсирующее кольцо, чтобы аура выглядела «живой»
                double ph = (n % 60) / 60.0;
                circle(w, 0xFFF3B0, px, py, pz, Math.max(0.5, d.radius * (0.25 + 0.75 * ph)));
            }
        }
    }

    /** Высота земли в точке (x, z) рядом с игроком, чтобы круг лежал по рельефу. */
    static double groundY(World w, double x, double z, double py) {
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        int top = MathHelper.floor(py), bot = top - 4;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int y = top; y >= bot; y--) {
            m.set(bx, y, bz);
            BlockState st = w.getBlockState(m);
            if (!st.getCollisionShape(w, m).isEmpty()) return y + 1.05;
        }
        return py + 0.05;
    }

    static void circle(World w, int col, double cx, double cy, double cz, double r) {
        int pts = Math.max(24, (int) (Math.PI * 2 * r / 0.5));
        for (int i = 0; i < pts; i++) {
            double a = i * Math.PI * 2 / pts;
            double x = cx + Math.cos(a) * r, z = cz + Math.sin(a) * r;
            NoxEsp.dust(w, col, 0.5f, x, groundY(w, x, z, cy), z);
        }
    }

    /** Клетка 3x3: сетка на земле, верхний контур и четыре столба по углам. */
    static void trap(World w, int col, double px, double py, double pz) {
        double cx = MathHelper.floor(px) + 0.5, cz = MathHelper.floor(pz) + 0.5;
        double y = groundY(w, cx, cz, py);
        double h = 1.5;   // половина стороны: 3 блока
        for (int i = 0; i <= 3; i++) {
            double o = -h + i;
            NoxBlock.line(w, col, cx - h, cz + o, cx + h, cz + o, y);
            NoxBlock.line(w, col, cx + o, cz - h, cx + o, cz + h, y);
        }
        int top = 3;
        NoxBlock.line(w, col, cx - h, cz - h, cx + h, cz - h, y + top);
        NoxBlock.line(w, col, cx + h, cz - h, cx + h, cz + h, y + top);
        NoxBlock.line(w, col, cx + h, cz + h, cx - h, cz + h, y + top);
        NoxBlock.line(w, col, cx - h, cz + h, cx - h, cz - h, y + top);
        double[] sx = {-h, h}, sz = {-h, h};
        for (double ox : sx) for (double oz : sz) {
            for (double yy = 0; yy <= top; yy += 0.3) NoxEsp.dust(w, col, 0.5f, cx + ox, y + yy, cz + oz);
        }
    }

    /** Подпись слева от прицела: название, радиус, описание. */
    static void hud(DrawContext ctx, MinecraftClient mc) {
        Def d = current;
        if (d == null || !NoxConfig.itemRadius) return;
        var tr = mc.textRenderer;
        int w = 0;
        for (String l : d.lines) w = Math.max(w, tr.getWidth(l));
        w += 14;
        int h = 8 + d.lines.length * 10;
        int x = ctx.getScaledWindowWidth() / 2 - 24 - w, y = ctx.getScaledWindowHeight() / 2 + 16;
        int acc = 0xFF000000 | d.color;
        ctx.fill(x, y, x + w, y + h, 0xDD0A0A10);
        ctx.fill(x, y, x + 2, y + h, acc);
        for (int i = 0; i < d.lines.length; i++) {
            int col = i == 0 ? (0xFF000000 | NoxConfig.accLight()) : 0xFFE9D5FF;
            ctx.drawTextWithShadow(tr, d.lines[i], x + 8, y + 5 + i * 10, col);
        }
    }
}
