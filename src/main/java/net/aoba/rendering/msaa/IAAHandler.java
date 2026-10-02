package net.aoba.rendering.msaa;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jspecify.annotations.Nullable;

/**
 * Abstracts MSAA so that it can be supported by all available rendering backends.
 */
public interface IAAHandler extends AutoCloseable {

	/** 
	 * Single-sample view + sampler for compositing.
	 */
	record ResolvedTarget(@Nullable GpuTextureView view, @Nullable GpuSampler sampler) {}

	/**
	 * Ensure MSAA resources are allocated at the correct screen dimensions and sample count.
	 **/
	void prepare(int width, int height, int samples);

	/**
	 * Gets the render scale of the AntiAliasing handler.
	 **/
	default int renderScale() {
		return 1;
	}

	/**
	 * Create the render pass for the MSAA
	 **/
	RenderPass beginPass();

	/**
	 * Resolve the multi-sampled content to a target and returns it.
	 **/
	ResolvedTarget resolve();

	@Override
	void close();
}
