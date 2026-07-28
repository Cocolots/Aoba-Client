/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.Render3DEvent;
import net.aoba.rendering.Renderer3D;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@Inject(method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At("TAIL"))
	private void onAfterRenderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
			boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
			Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		Minecraft mc = Minecraft.getInstance();
		if (aoba == null || aoba.moduleManager == null || mc.level == null || mc.player == null)
			return;

		Camera camera = mc.gameRenderer.mainCamera();
		Matrix4f viewRot = new Matrix4f();
		camera.getViewRotationMatrix(viewRot);

		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(viewRot);

		Frustum cullFrustum = cameraState.cullFrustum;

		Renderer3D renderer = aoba.render3D;
		renderer.beginFrame(matrixStack, cullFrustum, camera, cameraState, deltaTracker);
		aoba.eventManager.Fire(new Render3DEvent(renderer));
		renderer.render();

		if (aoba.render2D != null) {
			aoba.render2D.captureGameSnapshot();
		}
	}

	@Inject(at = @At("HEAD"), method = "submitBlockOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V", cancellable = true)
	private void onSubmitBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
		if (Aoba.getInstance().moduleManager.freecam.state.getValue())
			ci.cancel();
	}
}
