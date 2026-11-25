package net.aoba.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.FramerateLimitTracker;

import net.aoba.Aoba;
import net.aoba.module.modules.render.FocusFps;
import net.aoba.module.modules.render.FocusFpsPolicy;
import net.minecraft.client.Minecraft;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "getFramerateLimit()I", at = @At("RETURN"), cancellable = true)
	private void aoba$limitUnfocusedWindow(CallbackInfoReturnable<Integer> info) {
		if (Aoba.getInstance().moduleManager == null) {
			return;
		}

		FocusFps module = Aoba.getInstance().moduleManager.focusfps;
		boolean enabled = module != null && module.state.getValue();
		boolean focused = minecraft.getWindow().isFocused();
		info.setReturnValue(FocusFpsPolicy.resolveLimit(info.getReturnValue(),
				module == null ? 30 : module.getFps().intValue(), enabled, focused));
	}
}
