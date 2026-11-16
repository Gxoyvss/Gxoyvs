package com.example.noxvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class NoxVisualsClient implements ClientModInitializer {
    static KeyBinding wheelKey;   // клавиша колеса эмоций
    static LivingEntity target;   // последний противник, которого вы ударили
    static int targetTicks;       // сколько ещё тиков показывать цель
    static float anim;            // плавное появление Target HUD, 0..1
    static float shownHp;         // сглаженное здоровье для полосы
    static float trailHp;         // «след» потерянного здоровья (белая полоса, догоняет медленно)
    static int pulse;             // тики «пульса» рамки после удара
    static int flash;             // короткая фиолетовая вспышка при ударе
    static int wave;              // тик расходящегося круга под ногами (0 = нет)
    static double waveX, waveY, waveZ;
    static final int WAVE_TICKS = 14;
    static final double WAVE_RADIUS = 4.5;   // радиус круга в блоках

    @Override
    public void onInitializeClient() {
        NoxConfig.load();
        NoxSky.init();

        // Единственная клавиша: правый Shift открывает меню (можно сменить в Управлении)
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.noxvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.noxvisuals"));

        // Клавиша G: нанести выбранное граффити на блок, на который смотришь
        KeyBinding spray = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.noxvisuals.spray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.noxvisuals"));

        // Колесо эмоций: зажми B, наведи мышь на сектор, отпусти (клавишу можно сменить в Управлении)
        wheelKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.noxvisuals.wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.noxvisuals"));

        // Фрилок: зажми левый Alt, чтобы осматриваться, не поворачивая игрока
        KeyBinding freelookKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.noxvisuals.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.noxvisuals"));

        // Удар по живому существу: запоминаем цель и запускаем эффекты
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) {
                if (target != le) shownHp = trailHp = le.getHealth();
                target = le;
                targetTicks = 100;
                pulse = 6;
                if (NoxConfig.hitFx) {
                    burst(world, le);
                    flash = 8;
                    if (NoxConfig.hitRing) wave = 1;
                    waveX = player.getX(); waveY = player.getY(); waveZ = player.getZ();
                }
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new NoxScreen());
            }

            while (wheelKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new NoxWheel());
            }
            NoxEmotes.tick(mc);
            NoxFreelook.tick(mc, freelookKey);

            while (spray.wasPressed()) {
                if (mc.currentScreen == null) NoxGraffiti.spray(mc);
            }

            // состояние цели и анимации
            if (targetTicks > 0) targetTicks--;
            boolean active = target != null && targetTicks > 0 && !target.isRemoved();
            anim = Math.max(0f, Math.min(1f, anim + (active ? 0.15f : -0.1f)));
            if (!active && anim <= 0f) target = null;
            if (target != null) {
                shownHp += (target.getHealth() - shownHp) * 0.2f;
                if (trailHp < shownHp) trailHp = shownHp;
                else trailHp += (shownHp - trailHp) * 0.04f;
            }
            if (flash > 0) flash--;
            if (pulse > 0) pulse--;

            if (mc.player == null || mc.world == null) return;

            // Target ESP: вращающиеся частицы вокруг цели (видны только там, где видна сама цель)
            if (NoxConfig.targetEsp && active) NoxEsp.tick(mc, target, pulse);

            // Круг под ногами: расходится до 4.5 блоков после удара
            if (wave > 0) {
                groundWave(mc.world);
                if (++wave > WAVE_TICKS) wave = 0;
            }

            // Трейл за игроком, когда он движется
            NoxTrail.tick(mc);

            // Косметика (крылья, рюкзак, нимб) и граффити
            NoxCosmetics.tick(mc);
            NoxGraffiti.tick(mc);
            NoxBlock.tick(mc);
            NoxFx.tick(mc);
            NoxHud.tick(mc);
            NoxRadius.tick(mc);

            // Погодная тема: снег или дождь вокруг игрока
            if (NoxConfig.theme != 0) weather(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;

            // вспышка при ударе
            if (NoxConfig.hitFx && flash > 0) {
                ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), ((flash * 5) << 24) | NoxConfig.acc());
            }

            drawHud(ctx, mc);
            drawTarget(ctx, mc);
            NoxCrosshair.draw(ctx, mc);
            NoxHud.draw(ctx, mc);
            NoxFx.hud(ctx, mc);
            NoxRadius.hud(ctx, mc);
        });
    }

    static int hudH;   // высота верхней левой панели (0 = скрыта), по ней встают броня и эффекты

    static void drawHud(DrawContext ctx, MinecraftClient mc) {
        hudH = 0;
        if (!NoxConfig.hud) return;
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<Boolean> accent = new java.util.ArrayList<>();

        if (NoxConfig.hudFps || NoxConfig.hudPing) {
            String s = "";
            if (NoxConfig.hudFps) s += mc.getCurrentFps() + " FPS";
            if (NoxConfig.hudFps && NoxConfig.hudPing) s += "  |  ";
            if (NoxConfig.hudPing) {
                var net = mc.getNetworkHandler();
                PlayerListEntry me = net != null ? net.getPlayerListEntry(mc.player.getUuid()) : null;
                s += (me != null ? me.getLatency() : 0) + " ms";
            }
            lines.add(s);
            accent.add(true);
        }
        if (NoxConfig.hudCoords) {
            lines.add(mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
            accent.add(false);
        }
        if (NoxConfig.hudSpeed) {
            lines.add(String.format("%.1f b/s", mc.player.getVelocity().horizontalLength() * 20));
            accent.add(false);
        }
        if (NoxConfig.hudTheme) {
            lines.add("Theme: " + NoxScreen.THEMES[NoxConfig.theme]);
            accent.add(true);
        }
        if (lines.isEmpty()) return;

        int w = 0;
        for (String l : lines) w = Math.max(w, mc.textRenderer.getWidth(l));
        w += 14;
        int h = 8 + lines.size() * 10;
        int acc = 0xFF000000 | NoxConfig.acc();
        ctx.fill(4, 4, 4 + w, 4 + h, 0xDD0A0A10);   // фон
        ctx.fill(4, 4, 6, 4 + h, acc);               // полоса слева
        ctx.fill(4, 4, 4 + w, 5, acc);               // линия сверху
        for (int i = 0; i < lines.size(); i++) {
            int col = accent.get(i) ? (0xFF000000 | NoxConfig.accLight()) : 0xFFE9D5FF;
            ctx.drawTextWithShadow(mc.textRenderer, lines.get(i), 11, 9 + i * 10, col);
        }
        hudH = h;
    }

    static int c(int rgb, int a) {
        return (Math.max(a, 8) << 24) | rgb;
    }

    /** Target HUD: голова, имя, здоровье (с «следом» урона и золотым поглощением), броня и расстояние. */
    static void drawTarget(DrawContext ctx, MinecraftClient mc) {
        if (!NoxConfig.targetHud || target == null || anim < 0.05f) return;
        float e = 1f - (1f - anim) * (1f - anim);
        int a = (int) (e * 255);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        // голова показывается только у игроков (скин берём из списка игроков)
        PlayerListEntry pe = null;
        if (target instanceof PlayerEntity pl && mc.getNetworkHandler() != null) {
            pe = mc.getNetworkHandler().getPlayerListEntry(pl.getUuid());
        }
        int head = pe != null ? 32 : 0;
        int w = 160, h = 52;
        int x = sw / 2 + 24 + (int) ((1f - e) * 30);
        int y = sh / 2 + 16;
        int tx = x + 8 + (head > 0 ? head + 6 : 0);   // начало текста/полосы
        int tw = x + w - 8 - tx;                       // ширина полосы

        float max = Math.max(1f, target.getMaxHealth());
        float ratio = Math.max(0f, Math.min(1f, shownHp / max));
        float trail = Math.max(0f, Math.min(1f, trailHp / max));
        float abs = target.getAbsorptionAmount();
        String name = target.getName().getString();
        String hp = String.format("%.1f", Math.max(0f, target.getHealth())) + (abs > 0 ? " +" + String.format("%.0f", abs) : "");
        String dist = String.format("%.1f m", mc.player.distanceTo(target));
        int armor = target.getArmor();

        ctx.fill(x, y, x + w, y + h, c(0x0A0A10, a * 220 / 255));
        ctx.fill(x, y, x + 2, y + h, c(NoxConfig.acc(), a));
        if (head > 0) PlayerSkinDrawer.draw(ctx, pe.getSkinTextures(), x + 8, y + 10, head);

        // имя обрезаем, чтобы не вылезало за рамку
        String shown = mc.textRenderer.trimToWidth(name, tw);
        ctx.drawTextWithShadow(mc.textRenderer, shown, tx, y + 7, c(0xE9D5FF, a));

        int by = y + 20;
        ctx.fill(tx, by, tx + tw, by + 6, c(0x2A1B3D, a));
        ctx.fill(tx, by, tx + (int) (tw * trail), by + 6, c(0xFFFFFF, a * 160 / 255));   // след урона
        ctx.fill(tx, by, tx + (int) (tw * ratio), by + 6, c(NoxConfig.acc(), a));
        ctx.fill(tx, by, tx + (int) (tw * ratio), by + 2, c(NoxConfig.accLight(), a));
        if (abs > 0) ctx.fill(tx, by + 5, tx + (int) (tw * Math.min(1f, abs / max)), by + 6, c(0xFFD54A, a));

        ctx.drawTextWithShadow(mc.textRenderer, hp, tx, y + 31, c(NoxConfig.accLight(), a));
        if (armor > 0) {
            String ar = "\u2748 " + armor;   // значок + значение брони
            ctx.drawTextWithShadow(mc.textRenderer, ar, tx + tw / 2 - mc.textRenderer.getWidth(ar) / 2, y + 31, c(0x9FB4D9, a));
        }
        ctx.drawTextWithShadow(mc.textRenderer, dist, tx + tw - mc.textRenderer.getWidth(dist), y + 31, c(0xE9D5FF, a));
    }

    /** Target ESP: три светящихся точки кружат вокруг цели и плавно ходят вверх-вниз. */
    static void ring(World world, LivingEntity t) {
        double time = world.getTime();
        double r = t.getWidth() * 0.5 + 0.45;
        double hgt = t.getHeight();
        for (int k = 0; k < 3; k++) {
            double ang = time * 0.35 + k * (Math.PI * 2 / 3);
            double y = t.getY() + hgt * (0.5 + 0.5 * Math.sin(time * 0.12 + k));
            world.addParticle(ParticleTypes.WITCH,
                t.getX() + Math.cos(ang) * r, y, t.getZ() + Math.sin(ang) * r, 0, 0, 0);
        }
    }

    static void weather(MinecraftClient mc) {
        World world = mc.world;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int count = (int) Math.round((NoxConfig.theme == 1 ? 10 : 14) * NoxConfig.density);
        for (int i = 0; i < count; i++) {
            double x = px + (world.random.nextDouble() - 0.5) * 28;
            double z = pz + (world.random.nextDouble() - 0.5) * 28;
            double y = py + 6 + world.random.nextDouble() * 4;
            // не создаём осадки там, где над головой крыша или пещера
            if (!world.isSkyVisible(BlockPos.ofFloored(x, y, z))) continue;
            if (NoxConfig.theme == 1) {
                double drift = (world.random.nextDouble() - 0.5) * 0.04;
                world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, drift, -0.06, drift);
            } else {
                world.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, -0.4, 0);
            }
        }
    }

    /** Двойной круг из частиц на земле: быстро расширяется (ease-out) и затухает. */
    static void groundWave(World w) {
        double t = (double) wave / WAVE_TICKS;
        double ease = 1 - (1 - t) * (1 - t);
        float size = (float) Math.max(0.35, 0.9 - t * 0.55);
        for (int ring = 0; ring < 2; ring++) {
            double r = Math.max(0.3, WAVE_RADIUS * ease * (ring == 0 ? 1.0 : 0.72));
            int pts = Math.max(24, (int) (r * 14));
            int col = ring == 0 ? NoxConfig.acc() : NoxConfig.accLight();
            for (int i = 0; i < pts; i++) {
                double a = i * Math.PI * 2 / pts + wave * 0.15 * (ring == 0 ? 1 : -1);
                double x = waveX + Math.cos(a) * r, z = waveZ + Math.sin(a) * r;
                NoxEsp.dust(w, col, size, x, waveY + 0.05, z);
                if (ring == 0 && i % 6 == 0) w.addParticle(ParticleTypes.END_ROD, x, waveY + 0.1, z, 0, 0.04, 0);
            }
        }
    }

    /** Эффект удара: всплеск частиц и расходящееся кольцо-ударная волна. */
    static void burst(World world, Entity e) {
        double y = e.getY() + e.getHeight() / 2;
        for (int i = 0; i < 18; i++) {
            double dx = (world.random.nextDouble() - 0.5) * 0.6;
            double dy = world.random.nextDouble() * 0.4;
            double dz = (world.random.nextDouble() - 0.5) * 0.6;
            var type = i % 3 == 0 ? ParticleTypes.WITCH : (i % 3 == 1 ? ParticleTypes.END_ROD : ParticleTypes.CRIT);
            world.addParticle(type, e.getX(), y, e.getZ(), dx, dy, dz);
        }
        for (int i = 0; i < 24; i++) {
            double ang = i * Math.PI * 2 / 24;
            world.addParticle(ParticleTypes.END_ROD, e.getX(), y, e.getZ(),
                Math.cos(ang) * 0.25, 0.02, Math.sin(ang) * 0.25);
        }
    }
}
