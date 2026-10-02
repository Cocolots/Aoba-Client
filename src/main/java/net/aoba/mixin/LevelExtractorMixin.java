/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.PlayerRenderState;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

	@Inject(method = "extractPlayerState(Lnet/minecraft/client/Camera;Lnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/renderer/state/level/PlayerRenderState;)V", at = @At("TAIL"))
	private void onExtractPlayerState(Camera camera, DeltaTracker deltaTracker, float worldPartialTicks,
			PlayerRenderState state, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba == null || aoba.moduleManager == null)
			return;

		if (aoba.moduleManager.norender.state.getValue()) {
			state.portalEffectIntensity = 0.0F;
			state.nauseaEffectIntensity = 0.0F;
		}
	}
}
