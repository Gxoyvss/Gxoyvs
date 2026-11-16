package com.example.noxvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** Меню с боковой панелью вкладок. Открывается правым Shift. */
public class NoxScreen extends Screen {
    static final String[] THEMES = {"Off", "Winter", "Spring"};
    static final String[] SHORT = {"Off", "Snow", "Rain"};
    static final String[] TABS = {"Visual", "HUD", "Weather", "Cosmetics", "Graffiti", "Target", "Clouds", "Fog", "Block", "Emotes", "Crosshair", "Info", "Settings"};
    static final int W = 380, H = 304, SW = 104;

    static class Slider { int x, w; double min, max; DoubleConsumer set; }
    static class Hit { int x, y, w, h; Runnable click; Slider slider; }

    final List<Hit> hits = new ArrayList<>();
    final Map<String, Float> anims = new HashMap<>();
    Slider drag;
    long opened;
    int tab = 0, mx, my, px, pw;
    float ta = 1f, indY = -1f, fade = 1f, ca = 255f;

    public NoxScreen() {
        super(Text.literal("NoxVisuals"));
    }

    @Override
    protected void init() {
        opened = System.currentTimeMillis();
    }

    // ---------- помощники рисования ----------

    float anim(String key, float target) {
        float v = anims.getOrDefault(key, target);
        v += (target - v) * 0.25f;
        anims.put(key, v);
        return v;
    }

    static int mix(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    void rect(DrawContext c, int x, int y, int w, int h, int rgb, float alpha) {
        int al = (int) Math.max(0, Math.min(255, alpha * fade));
        if (al > 0 && w > 0 && h > 0) c.fill(x, y, x + w, y + h, (al << 24) | rgb);
    }

    /** Прямоугольник со срезанными углами (без наложения слоёв). */
    void rr(DrawContext c, int x, int y, int w, int h, int rgb, float alpha) {
        rect(c, x + 2, y, w - 4, 1, rgb, alpha);
        rect(c, x + 1, y + 1, w - 2, 1, rgb, alpha);
        rect(c, x, y + 2, w, h - 4, rgb, alpha);
        rect(c, x + 1, y + h - 2, w - 2, 1, rgb, alpha);
        rect(c, x + 2, y + h - 1, w - 4, 1, rgb, alpha);
    }

    void text(DrawContext c, String s, int x, int y, int rgb, float alpha) {
        int al = (int) Math.max(0, Math.min(255, alpha * fade));
        if (al >= 8) c.drawTextWithShadow(textRenderer, s, x, y, (al << 24) | rgb);
    }

    void hit(int x, int y, int w, int h, Runnable click) {
        Hit t = new Hit();
        t.x = x; t.y = y; t.w = w; t.h = h; t.click = click;
        hits.add(t);
    }

    // ---------- строки настроек ----------

    void label(DrawContext c, String l, String d, int y) {
        text(c, l, px, y, 0xFFFFFF, ca);
        text(c, d, px, y + 11, 0x8A8A96, ca);
        rect(c, px, y + 25, pw, 1, 0xFFFFFF, ca * 0.08f);
    }

    void toggle(DrawContext c, String l, String d, int y, boolean on, Runnable flip) {
        label(c, l, d, y);
        int bx = px + pw - 26, by = y + 2;
        float k = anim("t:" + l, on ? 1f : 0f);
        rr(c, bx, by, 24, 12, mix(0x2A2A33, NoxConfig.acc(), k), ca);
        rr(c, bx + 2 + (int) (k * 12), by + 2, 8, 8, 0xFFFFFF, ca);
        hit(px, y - 3, pw, 28, flip);
    }

    void seg(DrawContext c, String l, String d, int y, String[] names, int cur, IntConsumer set) {
        label(c, l, d, y);
        int bw = 34, bx = px + pw - bw * names.length;
        rr(c, bx - 2, y - 2, bw * names.length + 4, 18, 0x14141C, ca);
        for (int i = 0; i < names.length; i++) {
            final int j = i;
            int sx = bx + i * bw;
            float k = anim("s:" + l + i, i == cur ? 1f : 0f);
            rr(c, sx, y - 1, bw, 16, NoxConfig.acc(), ca * k);
            int tw = textRenderer.getWidth(names[i]);
            text(c, names[i], sx + (bw - tw) / 2, y + 3, i == cur ? 0xFFFFFF : 0xB0B0BA, ca);
            hit(sx, y - 1, bw, 16, () -> set.accept(j));
        }
    }

    void slider(DrawContext c, String l, String d, int y, double min, double max, double val, DoubleConsumer set) {
        slider(c, l, d, y, min, max, val, "%.1fx", set);
    }

    void palette(DrawContext c, String l, String d, int y, int[] pal, int cur, IntConsumer set) {
        label(c, l, d, y);
        for (int i = 0; i < pal.length; i++) {
            final int j = i;
            int sx = px + i * 22, sy = y + 30;
            float k = anim("p:" + l + i, cur == i ? 1f : 0f);
            rr(c, sx - 2, sy - 2, 18, 18, 0xFFFFFF, ca * k);
            rr(c, sx, sy, 14, 14, pal[i], ca);
            hit(sx - 2, sy - 2, 18, 18, () -> set.accept(j));
        }
    }

    void slider(DrawContext c, String l, String d, int y, double min, double max, double val, String fmt, DoubleConsumer set) {
        label(c, l, d, y);
        int w = 70, sx = px + pw - w - 34;
        double f = (val - min) / (max - min);
        rect(c, sx, y + 5, w, 4, 0x2A2A33, ca);
        rect(c, sx, y + 5, (int) (w * f), 4, NoxConfig.acc(), ca);
        rr(c, sx + (int) (w * f) - 3, y + 2, 6, 10, 0xFFFFFF, ca);
        text(c, String.format(fmt, val), px + pw - 28, y + 3, 0xFFFFFF, ca);
        Slider s = new Slider();
        s.x = sx; s.w = w; s.min = min; s.max = max; s.set = set;
        Hit h = new Hit();
        h.x = sx - 4; h.y = y - 2; h.w = w + 8; h.h = 16; h.slider = s;
        hits.add(h);
    }

    void colors(DrawContext c, String l, String d, int y) {
        label(c, l, d, y);
        for (int i = 0; i < NoxConfig.PALETTE.length; i++) {
            final int j = i;
            int sx = px + i * 22, sy = y + 30;
            float k = anim("c:" + i, NoxConfig.accent == i ? 1f : 0f);
            rr(c, sx - 2, sy - 2, 18, 18, 0xFFFFFF, ca * k);
            rr(c, sx, sy, 14, 14, NoxConfig.PALETTE[i], ca);
            hit(sx - 2, sy - 2, 18, 18, () -> NoxConfig.accent = j);
        }
    }

    void info(DrawContext c, String l, String d, int y, String value) {
        label(c, l, d, y);
        text(c, value, px + pw - textRenderer.getWidth(value), y + 3, NoxConfig.accLight(), ca);
    }

    void button(DrawContext c, String l, String d, int y, String name, Runnable click) {
        label(c, l, d, y);
        int bw = 52, bx = px + pw - bw;
        rr(c, bx, y, bw, 14, NoxConfig.acc(), ca * 0.4f);
        text(c, name, bx + (bw - textRenderer.getWidth(name)) / 2, y + 3, 0xFFFFFF, ca);
        hit(bx, y, bw, 14, click);
    }

    /** Маленький пиксельный полумесяц (логотип мода). */
    void moon(DrawContext c, int cx, int cy) {
        for (int i = -5; i <= 4; i++) {
            for (int j = -5; j <= 4; j++) {
                double d1 = i * i + j * j;
                if (d1 > 4.7 * 4.7) continue;
                double dx = i - 2.0, dy = j + 1.2;
                if (dx * dx + dy * dy <= 3.7 * 3.7) continue;
                rect(c, cx + i * 2, cy + j * 2, 2, 2, d1 > 3.2 * 3.2 ? NoxConfig.accLight() : 0xE9D5FF, 255);
            }
        }
    }

    /** Баннер вкладки Settings: луна, звёзды, название и бегущий блик. */
    void banner(DrawContext c, int y, float t) {
        int acc = NoxConfig.acc(), lite = NoxConfig.accLight();
        rr(c, px, y, pw, 52, 0x120B20, ca);
        for (int i = 0; i < 18; i++) {
            int sx = px + 6 + (i * 53) % (pw - 12), sy = y + 4 + (i * 29) % 40;
            if (sx < px + 44) continue;
            rect(c, sx, sy, 1, 1, i % 3 == 0 ? lite : acc, ca * (0.5f + 0.5f * (float) Math.sin(t * 2 + i)));
        }
        moon(c, px + 22, y + 26);
        text(c, "NoxVisuals", px + 44, y + 14, 0xE9D5FF, ca);
        text(c, "Dark purple visuals", px + 44, y + 27, lite, ca);
        text(c, "v1.7.1", px + 44, y + 38, 0x8A8A96, ca);
        rect(c, px, y + 49, pw, 3, acc, ca);
        int seg = 50, pos = (int) ((t * 90) % (pw + seg)) - seg;
        int a0 = Math.max(0, pos), a1 = Math.min(pw, pos + seg);
        if (a1 > a0) rect(c, px + a0, y + 49, a1 - a0, 3, lite, ca);
    }

    /** Миниатюра граффити 10x10, по 3 пикселя на клетку. */
    void thumb(DrawContext c, int design, int x, int y) {
        String[] rows = NoxGraffiti.ART[design];
        for (int r = 0; r < 10; r++) {
            for (int q = 0; q < 10; q++) {
                char ch = rows[r].charAt(q);
                if (ch != '.') rect(c, x + q * 3, y + r * 3, 3, 3, NoxGraffiti.color(ch), ca);
            }
        }
    }

    // ---------- отрисовка ----------

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        mx = mouseX;
        my = mouseY;
        hits.clear();

        float t = (System.currentTimeMillis() - opened) / 1000f;
        float e = Math.min(1f, t / 0.35f);
        e = 1f - (1f - e) * (1f - e) * (1f - e);
        fade = e;
        ca = 255f;
        int acc = NoxConfig.acc(), lite = NoxConfig.accLight();
        int x0 = width / 2 - W / 2, y0 = height / 2 - H / 2 + (int) ((1f - e) * 18);

        // плывущие вверх огоньки
        for (int i = 0; i < 28; i++) {
            float speed = 14 + (i % 4) * 8;
            int dx = (i * 83) % Math.max(1, width);
            int dy = height - (int) ((t * speed + i * 37) % Math.max(1, height));
            rect(c, dx, dy, 2, 2, lite, 40 + 40 * (float) Math.sin(t * 2 + i));
        }

        // окно
        rr(c, x0, y0, W, H, 0x0A0A10, 235);
        rect(c, x0 + 2, y0, W - 4, 1, acc, 200);
        rect(c, x0 + SW, y0 + 8, 1, H - 16, 0xFFFFFF, 25);
        int seg = 70, pos = (int) ((t * 120) % (W + seg)) - seg;
        int s0 = Math.max(2, pos), s1 = Math.min(W - 2, pos + seg);
        if (s1 > s0) rect(c, x0 + s0, y0, s1 - s0, 2, lite, 255);

        // боковая панель
        text(c, "NoxVisuals", x0 + 14, y0 + 12, 0xFFFFFF, 255);
        text(c, "Modules", x0 + 14, y0 + 28, 0x6F6F7C, 255);
        text(c, "Config", x0 + 14, y0 + 228, 0x6F6F7C, 255);
        int[] ty = new int[TABS.length];
        for (int i = 0; i < ty.length - 1; i++) ty[i] = y0 + 40 + i * 16;
        ty[ty.length - 1] = y0 + 240;
        float target = ty[tab];
        indY = indY < 0 ? target : indY + (target - indY) * 0.25f;
        rr(c, x0 + 8, (int) indY - 4, SW - 16, 16, acc, 70);
        rect(c, x0 + 8, (int) indY - 2, 2, 12, lite, 255);
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            boolean on = i == tab;
            boolean hov = mx >= x0 + 8 && mx < x0 + SW - 8 && my >= ty[i] - 4 && my < ty[i] + 12;
            if (hov && !on) rr(c, x0 + 8, ty[i] - 4, SW - 16, 16, 0xFFFFFF, 18);
            text(c, TABS[i], x0 + 18, ty[i], on ? 0xFFFFFF : 0xB0B0BA, 255);
            hit(x0 + 8, ty[i] - 4, SW - 16, 16, () -> {
                if (tab != idx) { tab = idx; ta = 0f; }
            });
        }
        rr(c, x0 + 8, y0 + H - 40, SW - 16, 32, 0x14141C, 255);
        rr(c, x0 + 13, y0 + H - 34, 20, 20, 0x1D1D27, 255);
        moon(c, x0 + 23, y0 + H - 24);
        text(c, "NoxVisuals", x0 + 37, y0 + H - 35, 0xFFFFFF, 255);
        text(c, "v1.7.1", x0 + 37, y0 + H - 25, 0x8A8A96, 255);

        // правая часть: настройки выбранной вкладки (плавно выезжает при смене)
        ta += (1f - ta) * 0.2f;
        if (ta > 0.99f) ta = 1f;
        ca = 255f * ta;
        px = x0 + SW + 14 + (int) ((1f - ta) * 14);
        pw = W - SW - 28;
        int al = (int) (ca * fade);
        if (al >= 8) {
            c.drawTextWithShadow(textRenderer, Text.literal(TABS[tab]).formatted(Formatting.BOLD),
                px, y0 + 14, (al << 24) | 0xFFFFFF);
        }
        int ry = y0 + 36;
        switch (tab) {
            case 0 -> {
                toggle(c, "Trail", "Light trail behind you", ry, NoxConfig.trail, () -> NoxConfig.trail = !NoxConfig.trail);
                seg(c, "Trail style", "Pattern", ry + 34, NoxTrail.STYLES, NoxConfig.trailStyle, v -> NoxConfig.trailStyle = v);
                toggle(c, "Hit effects", "Burst, shockwave and flash", ry + 68, NoxConfig.hitFx, () -> NoxConfig.hitFx = !NoxConfig.hitFx);
                toggle(c, "Target ESP", "Ring around your target", ry + 102, NoxConfig.targetEsp, () -> NoxConfig.targetEsp = !NoxConfig.targetEsp);
                toggle(c, "Ground ring", "Circle under your feet on hit", ry + 136, NoxConfig.hitRing, () -> NoxConfig.hitRing = !NoxConfig.hitRing);
                toggle(c, "Freelook", "Hold L-Alt to look around", ry + 170, NoxConfig.freelook, () -> NoxConfig.freelook = !NoxConfig.freelook);
                toggle(c, "Freelook 3rd person", "Switch view while held", ry + 204, NoxConfig.freelookThird, () -> NoxConfig.freelookThird = !NoxConfig.freelookThird);
            }
            case 1 -> {
                toggle(c, "Target HUD", "Name, health, distance", ry, NoxConfig.targetHud, () -> NoxConfig.targetHud = !NoxConfig.targetHud);
                toggle(c, "Keystrokes", "WASD and space keys", ry + 34, NoxConfig.hudKeys, () -> NoxConfig.hudKeys = !NoxConfig.hudKeys);
                toggle(c, "CPS", "Clicks per second", ry + 68, NoxConfig.hudCps, () -> NoxConfig.hudCps = !NoxConfig.hudCps);
                toggle(c, "Armor", "Worn armor durability", ry + 102, NoxConfig.hudArmor, () -> NoxConfig.hudArmor = !NoxConfig.hudArmor);
                toggle(c, "Effects", "Active potion effects", ry + 136, NoxConfig.hudEffects, () -> NoxConfig.hudEffects = !NoxConfig.hudEffects);
                toggle(c, "Inventory", "Backpack items on screen", ry + 170, NoxConfig.hudInventory, () -> NoxConfig.hudInventory = !NoxConfig.hudInventory);
                toggle(c, "Item radius", "Zone of trap, dust, aura", ry + 204, NoxConfig.itemRadius, () -> NoxConfig.itemRadius = !NoxConfig.itemRadius);
            }
            case 2 -> {
                seg(c, "Theme", "Weather around you", ry, SHORT, NoxConfig.theme, v -> NoxConfig.theme = v);
                slider(c, "Density", "Flakes or drops count", ry + 34, 0.25, 3.0, NoxConfig.density,
                    v -> NoxConfig.density = (float) (Math.round(v * 20) / 20.0));
                seg(c, "Night sky", "Stars and aurora at night", ry + 68, new String[]{"Off", "Stars", "Aura"}, NoxConfig.sky, v -> NoxConfig.sky = v);
                toggle(c, "Color saturation", "Screen color intensity", ry + 102, NoxConfig.colorSat, () -> NoxConfig.colorSat = !NoxConfig.colorSat);
                slider(c, "Saturation", "0 gray, 1 normal, 2 vivid", ry + 136, 0.0, 2.0, NoxConfig.satValue,
                    v -> NoxConfig.satValue = (float) (Math.round(v * 20) / 20.0));
            }
            case 3 -> {
                seg(c, "Wings", "Worn on your back", ry, NoxCosmetics.WINGS, NoxConfig.wings, v -> NoxConfig.wings = v);
                seg(c, "Backpack", "Pack or jetpack", ry + 34, NoxCosmetics.PACKS, NoxConfig.pack, v -> NoxConfig.pack = v);
                seg(c, "Head", "Halo or crown", ry + 68, NoxCosmetics.HEADS, NoxConfig.head, v -> NoxConfig.head = v);
            }
            case 4 -> {
                label(c, "Collection", "Aim at a block, press G", ry);
                for (int i = 0; i < NoxGraffiti.ART.length; i++) {
                    final int j = i;
                    int sx = px + i * 40, sy = ry + 30;
                    float k = anim("g:" + i, NoxConfig.gDesign == i ? 1f : 0f);
                    rr(c, sx - 2, sy - 2, 34, 34, 0xFFFFFF, ca * k);
                    rr(c, sx, sy, 30, 30, 0x14141C, ca);
                    thumb(c, j, sx, sy);
                    hit(sx - 2, sy - 2, 34, 34, () -> NoxConfig.gDesign = j);
                }
                info(c, "Selected", "Spray key: G", ry + 76, NoxGraffiti.NAMES[NoxConfig.gDesign]);
                toggle(c, "Show graffiti", "Hide or show all tags", ry + 110, NoxConfig.gShow, () -> NoxConfig.gShow = !NoxConfig.gShow);
                button(c, "Clear all", "Remove sprayed tags", ry + 144, "Clear", NoxGraffiti::clear);
            }
            case 5 -> {
                seg(c, "ESP style", "Look of the target mark", ry, NoxEsp.STYLES, NoxConfig.espStyle, v -> NoxConfig.espStyle = v);
                toggle(c, "Moon mark", "Crescent above the target", ry + 34, NoxConfig.espMoon, () -> NoxConfig.espMoon = !NoxConfig.espMoon);
                toggle(c, "HP color", "Green to red by health", ry + 68, NoxConfig.espHp, () -> NoxConfig.espHp = !NoxConfig.espHp);
                toggle(c, "Hit pulse", "White flash when you hit", ry + 102, NoxConfig.espPulse, () -> NoxConfig.espPulse = !NoxConfig.espPulse);
                toggle(c, "Damage numbers", "Floating hit numbers", ry + 136, NoxConfig.dmgNumbers, () -> NoxConfig.dmgNumbers = !NoxConfig.dmgNumbers);
                toggle(c, "Kill effect", "Soul burst on a kill", ry + 170, NoxConfig.killFx, () -> NoxConfig.killFx = !NoxConfig.killFx);
                toggle(c, "Kill streak", "Combo counter on screen", ry + 204, NoxConfig.killStreak, () -> NoxConfig.killStreak = !NoxConfig.killStreak);
            }
            case 6 -> {
                seg(c, "Preset", "Clouds and fog at once", ry, new String[]{"Off", "Nox", "Dusk", "Mist"}, NoxConfig.atmoPreset, NoxConfig::preset);
                seg(c, "Clouds", "Stock, custom or hidden", ry + 34, new String[]{"Stock", "Nox", "Off"}, NoxConfig.cloudMode, v -> NoxConfig.cloudMode = v);
                slider(c, "Height", "How high clouds float", ry + 68, 120, 260, NoxConfig.cloudHeight, "%.0f",
                    v -> NoxConfig.cloudHeight = (float) Math.round(v / 5) * 5);
                palette(c, "Cloud color", "Used in Nox mode", ry + 102, NoxConfig.ATMO, NoxConfig.cloudColor, v -> NoxConfig.cloudColor = v);
            }
            case 7 -> {
                seg(c, "Fog", "Stock or custom", ry, new String[]{"Stock", "Nox"}, NoxConfig.fogMode, v -> NoxConfig.fogMode = v);
                slider(c, "Distance", "Lower = thicker fog", ry + 34, 0.2, 2.0, NoxConfig.fogDist,
                    v -> NoxConfig.fogDist = (float) (Math.round(v * 20) / 20.0));
                palette(c, "Fog color", "Used in Nox mode", ry + 68, NoxConfig.ATMO, NoxConfig.fogColor, v -> NoxConfig.fogColor = v);
            }
            case 8 -> {
                seg(c, "Block glow", "Block you stand on", ry, NoxBlock.MODES, NoxConfig.blockFx, v -> NoxConfig.blockFx = v);
                palette(c, "Block color", "Color of the glow", ry + 34, NoxConfig.PALETTE, NoxConfig.blockColor, v -> NoxConfig.blockColor = v);
            }
            case 9 -> {
                button(c, "Griddy", "Fortnite-style step", ry, "Play", () -> NoxEmotes.play(client, 1));
                button(c, "6 7", "Hands up and down", ry + 34, "Play", () -> NoxEmotes.play(client, 2));
                button(c, "Twerk", "Hip shake", ry + 68, "Play", () -> NoxEmotes.play(client, 3));
                button(c, "Stop", "Back to normal pose", ry + 102, "Stop", () -> NoxEmotes.stop(client));
                info(c, "Wheel key", "Hold, aim, release", ry + 136, "B");
            }
            case 10 -> {
                seg(c, "Style", "Replaces stock", ry, NoxCrosshair.STYLES, NoxConfig.crossStyle, v -> NoxConfig.crossStyle = v);
                slider(c, "Size", "Arm or ring size", ry + 34, 2, 12, NoxConfig.crossSize, "%.0f", v -> NoxConfig.crossSize = (int) Math.round(v));
                toggle(c, "Hit spread", "Widens and flashes on hit", ry + 68, NoxConfig.crossSpread, () -> NoxConfig.crossSpread = !NoxConfig.crossSpread);
                toggle(c, "Use accent", "Otherwise plain white", ry + 102, NoxConfig.crossAcc, () -> NoxConfig.crossAcc = !NoxConfig.crossAcc);
                label(c, "Preview", "Hit spread shown on the right", ry + 136);
                rr(c, px, ry + 168, 80, 44, 0x14141C, ca);
                rr(c, px + 100, ry + 168, 80, 44, 0x14141C, ca);
                NoxCrosshair.render(c, px + 40, ry + 190, NoxConfig.crossStyle, NoxConfig.crossSize, 0);
                NoxCrosshair.render(c, px + 140, ry + 190, NoxConfig.crossStyle, NoxConfig.crossSize, NoxConfig.crossSpread ? 6 : 0);
            }
            case 11 -> {
                toggle(c, "Info panel", "Top-left panel", ry, NoxConfig.hud, () -> NoxConfig.hud = !NoxConfig.hud);
                toggle(c, "FPS", "Frames per second", ry + 34, NoxConfig.hudFps, () -> NoxConfig.hudFps = !NoxConfig.hudFps);
                toggle(c, "Ping", "Latency in ms", ry + 68, NoxConfig.hudPing, () -> NoxConfig.hudPing = !NoxConfig.hudPing);
                toggle(c, "Coordinates", "X Y Z position", ry + 102, NoxConfig.hudCoords, () -> NoxConfig.hudCoords = !NoxConfig.hudCoords);
                toggle(c, "Speed", "Blocks per second", ry + 136, NoxConfig.hudSpeed, () -> NoxConfig.hudSpeed = !NoxConfig.hudSpeed);
                toggle(c, "Theme line", "Weather theme name", ry + 170, NoxConfig.hudTheme, () -> NoxConfig.hudTheme = !NoxConfig.hudTheme);
            }
            default -> {
                banner(c, ry - 4, t);
                info(c, "Menu key", "Change in Controls", ry + 62, "R-Shift");
                button(c, "Reset settings", "Back to defaults", ry + 96, "Reset", NoxConfig::reset);
                colors(c, "Accent color", "Color of HUD and menu", ry + 130);
            }
        }
    }

    // ---------- мышь и клавиши ----------

    void slide(double mouseX) {
        if (drag == null) return;
        double f = Math.max(0, Math.min(1, (mouseX - drag.x) / drag.w));
        drag.set.accept(drag.min + f * (drag.max - drag.min));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (mouseX >= h.x && mouseX < h.x + h.w && mouseY >= h.y && mouseY < h.y + h.h) {
                    if (h.slider != null) {
                        drag = h.slider;
                        slide(mouseX);
                    } else if (h.click != null) {
                        h.click.run();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (drag != null) {
            slide(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        drag = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // игра идёт, эффекты видно сразу
    }

    @Override
    public void removed() {
        NoxConfig.save();
    }
}
