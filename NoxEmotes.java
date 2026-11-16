package com.example.noxvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;

/** Эмоции: Griddy, 6-7 и Twerk. Видны только тебе (мод клиентский), камера уходит на третье лицо. */
public class NoxEmotes {
    public static final String[] NAMES = {"Griddy", "6 7", "Twerk"};
    public static int active = 0;   // 0 нет, 1..3 номер эмоции
    static long start;
    static Perspective prev;

    public static void play(MinecraftClient mc, int emote) {
        if (mc.player == null) return;
        if (active == 0) prev = mc.options.getPerspective();
        active = emote;
        start = System.currentTimeMillis();
        mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
    }

    public static void stop(MinecraftClient mc) {
        if (active == 0) return;
        active = 0;
        if (prev != null) mc.options.setPerspective(prev);
    }

    /** Эмоция прерывается, если игрок пошёл или прыгнул. */
    public static void tick(MinecraftClient mc) {
        if (active == 0 || mc.player == null) return;
        if (mc.player.getVelocity().horizontalLengthSquared() > 0.004 || !mc.player.isOnGround()) stop(mc);
    }

    /** Вызывается из миксина после расчёта стандартной позы модели игрока. */
    public static void pose(BipedEntityModel<?> m) {
        double t = (System.currentTimeMillis() - start) / 1000.0;
        ModelPart ra = m.rightArm, la = m.leftArm, rl = m.rightLeg, ll = m.leftLeg, body = m.body, head = m.head;
        if (active == 1) {          // Griddy: руки в стороны, шаг и покачивание корпуса
            float s = (float) Math.sin(t * 9), c = (float) Math.cos(t * 9);
            ra.pitch = -0.2f + 0.5f * s; ra.roll = 0.9f + 0.25f * c;
            la.pitch = -0.2f - 0.5f * s; la.roll = -0.9f - 0.25f * c;
            body.yaw = 0.35f * s; head.yaw -= 0.2f * s;
            rl.pitch = -0.6f * Math.max(0f, s); rl.roll = 0.15f;
            ll.pitch = -0.6f * Math.max(0f, -s); ll.roll = -0.15f;
        } else if (active == 2) {   // 6 7: ладони впереди, руки поочерёдно вверх-вниз
            float s = (float) Math.sin(t * 8);
            ra.pitch = -1.3f + 0.6f * s; ra.yaw = -0.2f;
            la.pitch = -1.3f - 0.6f * s; la.yaw = 0.2f;
            rl.pitch = -0.15f + 0.1f * s; ll.pitch = -0.15f - 0.1f * s;
        } else if (active == 3) {   // Twerk: наклон вперёд и быстрое покачивание бёдрами
            float s = (float) Math.sin(t * 18);
            body.pitch = 0.9f; body.yaw = 0.3f * s;
            head.pitch -= 0.7f;
            ra.pitch = 0.9f; la.pitch = 0.9f;
            rl.pitch = -0.5f; ll.pitch = -0.5f;
            rl.roll = 0.25f; ll.roll = -0.25f;
            rl.yaw = -0.3f * s; ll.yaw = -0.3f * s;
        }
    }
}
