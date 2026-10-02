package net.aoba.rendering;

import java.nio.ByteBuffer;

import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.aoba.rendering.shaders.ShaderUniformRingBuffer;

public abstract class AbstractRenderer implements AutoCloseable {
	protected static final int VERTEX_BUFFER_USAGE = GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE;
	protected static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

	private static @Nullable GpuTexture whiteTexture;
	private static @Nullable GpuTextureView whiteTextureView;
	private static @Nullable GpuSampler whiteSampler;

	private final ShaderUniformRingBuffer shaderParamsBuffer = new ShaderUniformRingBuffer();

	public static GpuBufferSlice uploadGuiTransform() {
		return RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0f, 0f, -11000f),
				new Vector4f(1f, 1f, 1f, 1f), new Vector3f(0f, 0f, 0f), new Matrix4f());
	}

	public static GpuBufferSlice uploadIdentityTransform() {
		return RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(), new Vector4f(1f, 1f, 1f, 1f),
				new Vector3f(0f, 0f, 0f), new Matrix4f());
	}

	public abstract void render();

	@Override
	public abstract void close();

	/**
	 * Lazily creates the 1x1 white pixel texture.
	 */
	public static void ensureWhiteTexture() {
		if (whiteTextureView != null)
			return;

		whiteTexture = RenderSystem.getDevice().createTexture("aoba_white_pixel",
				GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, 1, 1, 1, 1);

		whiteTextureView = RenderSystem.getDevice().createTextureView(whiteTexture);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer pixel = stack.malloc(4).put((byte) -1).put((byte) -1).put((byte) -1).put((byte) -1).flip();
			RenderSystem.getDevice().createCommandEncoder().writeToTexture(whiteTexture, pixel, 0, 0, 0,
					0, 1, 1);
		}

		whiteSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
	}

	public static GpuTextureView getWhiteTextureView() {
		return whiteTextureView;
	}

	public static GpuSampler getWhiteSampler() {
		return whiteSampler;
	}

	public ShaderUniformRingBuffer getShaderParamsBuffer() {
		return shaderParamsBuffer;
	}

	public static void closeSharedResources() {
		if (whiteTextureView != null) {
			whiteTextureView.close();
			whiteTextureView = null;
		}
		if (whiteTexture != null) {
			whiteTexture.close();
			whiteTexture = null;
		}
		whiteSampler = null;
	}
}
