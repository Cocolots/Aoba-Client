package net.aoba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.aoba.Aoba;
import net.aoba.module.modules.render.NoRender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

	@Inject(method = "submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("HEAD"), cancellable = true)
	private static void onRenderFireOverlay(PoseStack matrices, SubmitNodeCollector vertexConsumers,
			TextureAtlasSprite sprite, CallbackInfo info) {
		NoRender norender = Aoba.getInstance().moduleManager.norender;

		if (norender.state.getValue() && norender.getNoFireOverlay())
			info.cancel();
	}

	@Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
	private static void onRenderUnderwaterOverlay(PlayerRenderState.WaterOverlay waterOverlay, PoseStack matrices,
			SubmitNodeCollector vertexConsumers, CallbackInfo info) {
		NoRender norender = Aoba.getInstance().moduleManager.norender;

		if (norender.state.getValue() && norender.getNoLiquidOverlay())
			info.cancel();
	}
}
