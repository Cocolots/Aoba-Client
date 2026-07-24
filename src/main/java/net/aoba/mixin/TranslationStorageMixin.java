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

@Mixin(targets = "net.minecraft.client.resources.language.ClientLanguage")
public class TranslationStorageMixin {
    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void onGetOrDefault(String key, String defaultValue, CallbackInfoReturnable<String> cir) {
        if (ProtocolManager.PROBES_PROTECTION && key != null) {
            if (key.startsWith("key.") || key.contains("aoba") || key.startsWith("mod.")) {
                cir.setReturnValue(defaultValue != null ? defaultValue : key);
            }
        }
    }
}
