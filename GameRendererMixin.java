package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxSaturation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Насыщенность цвета: применяется к картинке мира после её отрисовки, до интерфейса. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void nox$saturation(CallbackInfo ci) {
        NoxSaturation.apply(MinecraftClient.getInstance());
    }
}
