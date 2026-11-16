package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;

/** HUD-модули: клавиши WASD, CPS, броня и эффекты. */
public class NoxHud {
    static final ArrayDeque<Long> lmb = new ArrayDeque<>(), rmb = new ArrayDeque<>();
    static boolean wasL, wasR;
    static final float[] k = new float[7];   // W A S D Space LMB RMB

    static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean l = mc.options.attackKey.isPressed(), r = mc.options.useKey.isPressed();
        if (l && !wasL) lmb.addLast(now);
        if (r && !wasR) rmb.addLast(now);
        wasL = l;
        wasR = r;
        prune(lmb, now);
        prune(rmb, now);
    }

    static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    static void draw(DrawContext ctx, MinecraftClient mc) {
        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        left(ctx, mc);
        inventory(ctx, mc, W);
        keys(ctx, mc, W, H);
    }

    // ---------- справа вверху: инвентарь (3 ряда по 9 слотов) ----------

    static final int INV_W = 9 * 18 + 8, INV_H = 3 * 18 + 8;

    static void inventory(DrawContext ctx, MinecraftClient mc, int W) {
        if (!NoxConfig.hudInventory || mc.currentScreen instanceof HandledScreen<?>) return;
        int x = W - INV_W - 4, y = 4;
        int acc = 0xFF000000 | NoxConfig.acc();
        ctx.fill(x, y, x + INV_W, y + INV_H, 0xDD0A0A10);   // фон
        ctx.fill(x, y, x + 2, y + INV_H, acc);                // полоса слева
        ctx.fill(x, y, x + INV_W, y + 1, acc);                // линия сверху
        var inv = mc.player.getInventory();
        for (int i = 0; i < 27; i++) {
            int sx = x + 4 + (i % 9) * 18, sy = y + 4 + (i / 9) * 18;
            ctx.fill(sx, sy, sx + 17, sy + 17, 0x882A1B3D);
            ItemStack st = inv.getStack(9 + i);   // слоты 9..35 = основной инвентарь (без хотбара)
            if (st.isEmpty()) continue;
            ctx.drawItem(st, sx + 1, sy + 1);
            ctx.drawStackOverlay(mc.textRenderer, st, sx + 1, sy + 1);
        }
    }

    // ---------- слева: броня и эффекты (под основной панелью) ----------

    static void left(DrawContext ctx, MinecraftClient mc) {
        var tr = mc.textRenderer;
        int acc = 0xFF000000 | NoxConfig.acc();
        int ly = NoxVisualsClient.hudH > 0 ? 4 + NoxVisualsClient.hudH + 6 : 6;

        if (NoxConfig.hudArmor) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (st.isEmpty()) continue;
                String d = st.isDamageable() ? String.valueOf(st.getMaxDamage() - st.getDamage()) : "";
                int w = d.isEmpty() ? 28 : 32 + tr.getWidth(d);
                ctx.fill(4, ly, 4 + w, ly + 18, 0xDD0A0A10);
                ctx.fill(4, ly, 6, ly + 18, acc);
                ctx.drawItem(st, 8, ly + 1);
                if (!d.isEmpty()) {
                    float f = 1f - (float) st.getDamage() / Math.max(1, st.getMaxDamage());
                    int col = f > 0.5f ? 0xFFB6F0C8 : (f > 0.25f ? 0xFFFFE08A : 0xFFFF8A9A);
                    ctx.drawTextWithShadow(tr, d, 28, ly + 5, col);
                }
                ly += 20;
            }
        }

        if (NoxConfig.hudEffects) {
            int shown = 0;
            for (StatusEffectInstance ef : mc.player.getStatusEffects()) {
                if (shown++ >= 8) break;
                var type = ef.getEffectType().value();
                String line = type.getName().getString()
                    + (ef.getAmplifier() > 0 ? " " + (ef.getAmplifier() + 1) : "")
                    + "  " + (ef.isInfinite() ? "inf" : time(ef.getDuration() / 20));
                boolean good = type.isBeneficial();
                int w = tr.getWidth(line) + 14;
                ctx.fill(4, ly, 4 + w, ly + 13, 0xDD0A0A10);
                ctx.fill(4, ly, 6, ly + 13, good ? acc : 0xFFE02040);
                ctx.drawTextWithShadow(tr, line, 10, ly + 3, good ? 0xFFE9D5FF : 0xFFFFA0B0);
                ly += 15;
            }
        }
    }

    static String time(int sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    // ---------- справа внизу: клавиши и CPS ----------

    static void keys(DrawContext ctx, MinecraftClient mc, int W, int H) {
        if (!NoxConfig.hudKeys && !NoxConfig.hudCps) return;
        var o = mc.options;
        boolean[] on = {o.forwardKey.isPressed(), o.leftKey.isPressed(), o.backKey.isPressed(), o.rightKey.isPressed(),
            o.jumpKey.isPressed(), o.attackKey.isPressed(), o.useKey.isPressed()};
        for (int i = 0; i < k.length; i++) k[i] += ((on[i] ? 1f : 0f) - k[i]) * 0.35f;

        int height = (NoxConfig.hudKeys ? 52 : 0) + (NoxConfig.hudCps ? 18 : 0);
        int minY = NoxConfig.hudInventory ? 4 + INV_H + 4 : 4;   // ниже инвентаря
        int ox = W - 64, oy = Math.max(minY, H / 2 - height - 6);   // выше Target HUD, чтобы не перекрывать
        int y = oy;
        if (NoxConfig.hudKeys) {
            box(ctx, mc, ox + 20, oy, 18, 18, "W", k[0]);
            box(ctx, mc, ox, oy + 20, 18, 18, "A", k[1]);
            box(ctx, mc, ox + 20, oy + 20, 18, 18, "S", k[2]);
            box(ctx, mc, ox + 40, oy + 20, 18, 18, "D", k[3]);
            box(ctx, mc, ox, oy + 40, 58, 10, "", k[4]);
            ctx.fill(ox + 19, oy + 44, ox + 39, oy + 46, 0xFF000000 | NoxScreen.mix(0xB0B0BA, 0xFFFFFF, k[4]));
            y = oy + 52;
        }
        if (NoxConfig.hudCps) {
            box(ctx, mc, ox, y, 28, 18, "L " + lmb.size(), k[5]);
            box(ctx, mc, ox + 30, y, 28, 18, "R " + rmb.size(), k[6]);
        }
    }

    static void box(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, float a) {
        int bg = NoxScreen.mix(0x0A0A10, NoxConfig.acc(), a * 0.85f);
        ctx.fill(x, y, x + w, y + h, 0xCC000000 | bg);
        if (!label.isEmpty()) {
            int col = 0xFF000000 | NoxScreen.mix(0xB0B0BA, 0xFFFFFF, a);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, label, x + w / 2, y + (h - 8) / 2, col);
        }
    }
}
