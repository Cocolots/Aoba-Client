/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {

    @Inject(at = @At("HEAD"), method = "getClientModName", cancellable = true, remap = false)
    private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ProtocolManager.getClientBrand());
    }
}
