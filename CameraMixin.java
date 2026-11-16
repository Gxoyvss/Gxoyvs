package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxFreelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Подменяет поворот камеры на свой, пока активен фрилок. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0, shift = At.Shift.AFTER))
    private void nox$freelookCamera(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView,
                                    float tickDelta, CallbackInfo ci) {
        if (NoxFreelook.active && focused == MinecraftClient.getInstance().player) {
            setRotation(NoxFreelook.yaw, NoxFreelook.pitch);
        }
    }
}
