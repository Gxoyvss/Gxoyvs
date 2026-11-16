package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

/** Фрилок: пока зажат левый Alt, камера крутится отдельно от игрока. */
public class NoxFreelook {
    public static boolean active;
    public static float yaw, pitch;
    static Perspective prev;
    static boolean changedView;

    static void tick(MinecraftClient mc, KeyBinding key) {
        boolean want = NoxConfig.freelook && mc.player != null && mc.currentScreen == null && key.isPressed();
        if (want && !active) start(mc);
        else if (!want && active) stop(mc);
    }

    static void start(MinecraftClient mc) {
        active = true;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        prev = mc.options.getPerspective();
        changedView = false;
        if (NoxConfig.freelookThird && prev.isFirstPerson()) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            changedView = true;
        }
    }

    static void stop(MinecraftClient mc) {
        active = false;
        if (changedView && prev != null) mc.options.setPerspective(prev);
        changedView = false;
    }

    /** Вызывается из миксина вместо поворота игрока (те же коэффициенты, что в ванили). */
    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }
}
