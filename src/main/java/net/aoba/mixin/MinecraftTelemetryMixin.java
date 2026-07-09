/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */


package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.Minecraft")
public class MinecraftTelemetryMixin {

    @Inject(method = "allowsTelemetry()Z", at = @At("HEAD"), cancellable = true)
    public void onAllowsTelemetry(CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolManager.BLOCK_TELEMETRY) {
            cir.setReturnValue(false);
        }
    }
}
