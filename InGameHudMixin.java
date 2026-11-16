package com.example.noxvisuals.mixin;

import com.example.noxvisuals.NoxConfig;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Скрывает ванильный прицел, когда включён свой. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void nox$crosshair(CallbackInfo ci) {
        if (NoxConfig.crossStyle != 0) ci.cancel();
    }
}
