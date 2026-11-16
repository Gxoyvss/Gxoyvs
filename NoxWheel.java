package com.example.noxvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Колесо эмоций. Зажми клавишу (по умолчанию B), наведи мышь на сектор и отпусти. Или нажми и кликни. */
public class NoxWheel extends Screen {
    static final int R0 = 24, R1 = 70;
    static final double SEC = Math.PI * 2 / 3;

    long opened;
    int lastHover = -1;

    public NoxWheel() {
        super(Text.literal("Emotes"));
    }

    @Override
    protected void init() {
        opened = System.currentTimeMillis();
    }

    static double angle(double dx, double dy) {
        double a = Math.atan2(dy, dx) + Math.PI / 2;
        return ((a % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
    }

    /** 0..2 сектор эмоции, 3 центр (стоп), -1 мимо. */
    int hover(int mx, int my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0, d = Math.sqrt(dx * dx + dy * dy);
        if (d < R0 - 3) return 3;
        if (d < R0 || d > R1 + 40) return -1;
        return (int) (angle(dx, dy) / SEC);
    }

    int color(int sec, int hov) {
        int acc = NoxConfig.acc();
        if (sec == hov) return 0xEE000000 | acc;
        if (NoxEmotes.active == sec + 1) return 0xEE000000 | NoxScreen.mix(acc, 0xFFFFFF, 0.25f);
        return 0xDD14141C;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        int cx = width / 2, cy = height / 2;
        int hov = hover(mouseX, mouseY);
        lastHover = hov;

        for (int dy = -R1; dy <= R1; dy++) {
            int runStart = 0, runSec = -2;
            for (int dx = -R1; dx <= R1 + 1; dx++) {
                int sec = -2;
                if (dx <= R1) {
                    double d2 = dx * dx + dy * dy;
                    if (d2 >= R0 * R0 && d2 <= R1 * R1) {
                        double a = angle(dx, dy), pos = a % SEC;
                        if (pos > 0.035 && pos < SEC - 0.035) sec = (int) (a / SEC);
                    }
                }
                if (sec != runSec) {
                    if (runSec >= 0) c.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, color(runSec, hov));
                    runStart = dx;
                    runSec = sec;
                }
            }
        }

        int rc = R0 - 4;
        int core = hov == 3 ? (0xEE000000 | NoxConfig.acc()) : 0xDD0A0A10;
        for (int dy = -rc; dy <= rc; dy++) {
            int hw = (int) Math.sqrt(rc * rc - dy * dy);
            c.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, core);
        }

        String[] names = NoxEmotes.NAMES;
        int rm = (R0 + R1) / 2;
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(i * 120 + 60);
            int tx = cx + (int) Math.round(Math.sin(a) * rm), ty = cy - (int) Math.round(Math.cos(a) * rm);
            c.drawTextWithShadow(textRenderer, names[i], tx - textRenderer.getWidth(names[i]) / 2, ty - 4, 0xFFFFFFFF);
        }
        String stop = "Stop";
        c.drawTextWithShadow(textRenderer, stop, cx - textRenderer.getWidth(stop) / 2, cy - 4, 0xFFE9D5FF);

        String title = "Emotes";
        c.drawTextWithShadow(textRenderer, title, cx - textRenderer.getWidth(title) / 2, cy - R1 - 22, 0xFF000000 | NoxConfig.accLight());
        String key = NoxVisualsClient.wheelKey.getBoundKeyLocalizedText().getString();
        String hint = "Hold " + key + ", aim, release";
        c.drawTextWithShadow(textRenderer, hint, cx - textRenderer.getWidth(hint) / 2, cy + R1 + 14, 0xFF8A8A96);
    }

    void choose(int h) {
        if (h >= 0 && h <= 2) NoxEmotes.play(client, h + 1);
        else if (h == 3) NoxEmotes.stop(client);
        else return;
        close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hover((int) mouseX, (int) mouseY) >= 0) {
            choose(hover((int) mouseX, (int) mouseY));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (NoxVisualsClient.wheelKey.matchesKey(keyCode, scanCode) && System.currentTimeMillis() - opened > 250) {
            if (lastHover >= 0) choose(lastHover);
            else close();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
