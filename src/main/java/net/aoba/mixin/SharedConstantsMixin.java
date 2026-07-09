/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {
    @Inject(method = "getProtocolVersion", at = @At("HEAD"), cancellable = true)
    private static void onGetProtocolVersion(CallbackInfoReturnable<Integer> cir) {
        if (ProtocolManager.OVERRIDE_PROTOCOL != -1) {
            cir.setReturnValue(ProtocolManager.OVERRIDE_PROTOCOL);
        }
    }
} // mixin to get the protocol version
