package net.aoba.rendering.msaa;

import java.util.Optional;
import java.util.OptionalDouble;

import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

public class SSAAHandler implements IAAHandler {

	private int scale = 1;
	private int currentWidth;
	private int currentHeight;

	private @Nullable GpuTexture colorTexture;
	private @Nullable GpuTextureView colorTextureView;
	private @Nullable GpuSampler sampler;

	private static int sampleCountToScale(int samples) {
		return Math.max(1, (int) Math.round(Math.sqrt(Math.max(1, samples))));
	}

	@Override
	public int renderScale() {
		return scale;
	}

	@Override
	public void prepare(int width, int height, int samples) {
		int newScale = sampleCountToScale(samples);
		int sw = width * newScale;
		int sh = height * newScale;
		if (colorTexture != null && currentWidth == sw && currentHeight == sh && scale == newScale)
			return;

		releaseBuffers();
		scale = newScale;

		colorTexture = RenderSystem.getDevice().createTexture("aoba_ssaa_color",
				GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, sw, sh, 1, 1);
		colorTextureView = RenderSystem.getDevice().createTextureView(colorTexture);
		sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

		currentWidth = sw;
		currentHeight = sh;
	}

	@Override
	public RenderPass beginPass() {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Aoba SSAA",
				colorTextureView, Optional.of(new Vector4f(0.0f)), null, OptionalDouble.empty());
	}

	@Override
	public ResolvedTarget resolve() {
		return new ResolvedTarget(colorTextureView, sampler);
	}

	private void releaseBuffers() {
		if (colorTextureView != null) {
			colorTextureView.close();
			colorTextureView = null;
		}
		if (colorTexture != null) {
			colorTexture.close();
			colorTexture = null;
		}
		sampler = null;
		currentWidth = 0;
		currentHeight = 0;
	}

	@Override
	public void close() {
		releaseBuffers();
	}
}
