package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxEmotes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Накладывает позу эмоции на модель твоего игрока (до копирования слоёв одежды). */
@Mixin(BipedEntityModel.class)
public class BipedModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
    private void nox$emote(BipedEntityRenderState state, CallbackInfo ci) {
        if (NoxEmotes.active == 0 || !(state instanceof PlayerEntityRenderState)) return;
        var p = MinecraftClient.getInstance().player;
        if (p == null || state.id != p.getId()) return;
        NoxEmotes.pose((BipedEntityModel<?>) (Object) this);
    }
}
