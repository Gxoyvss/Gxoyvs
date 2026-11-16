package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxConfig;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Облака: цвет, высота или полное отключение. */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void nox$hideClouds(CallbackInfo ci) {
        if (NoxConfig.cloudMode == 2) ci.cancel();
    }

    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int nox$cloudColor(int color) {
        return NoxConfig.cloudMode == 1 ? 0xFF000000 | NoxConfig.ATMO[NoxConfig.cloudColor] : color;
    }

    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float nox$cloudHeight(float height) {
        return NoxConfig.cloudMode == 1 ? NoxConfig.cloudHeight : height;
    }
}
