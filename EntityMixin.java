package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxFreelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Во время фрилока мышь крутит камеру, а не игрока. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void nox$freelookMouse(double dx, double dy, CallbackInfo ci) {
        if (NoxFreelook.active && (Object) this == MinecraftClient.getInstance().player) {
            NoxFreelook.look(dx, dy);
            ci.cancel();
        }
    }
}
