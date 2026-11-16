package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxConfig;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Свой туман: цвет и дальность. Не трогает воду, лаву, слепоту и подобные эффекты. */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void nox$fog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color,
                                float viewDistance, boolean thickenFog, float tickDelta,
                                CallbackInfoReturnable<Fog> cir) {
        if (NoxConfig.fogMode == 0 || fogType != BackgroundRenderer.FogType.FOG_TERRAIN) return;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;
        Fog orig = cir.getReturnValue();
        if (orig == null || orig.end() < viewDistance * 0.9f) return;
        int c = NoxConfig.ATMO[NoxConfig.fogColor];
        float end = Math.max(8f, viewDistance * NoxConfig.fogDist);
        cir.setReturnValue(new Fog(end * 0.1f, end, FogShape.SPHERE,
            ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1f));
    }
}
