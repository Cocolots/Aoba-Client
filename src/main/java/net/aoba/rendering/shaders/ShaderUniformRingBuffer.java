package net.aoba.rendering.shaders;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;

/**
 * Manages per-shader UBO uploads.
 * Each shader defines its own UBO structure.
 */
public class ShaderUniformRingBuffer implements AutoCloseable {
	private static final int USAGE = GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE;
	private final long startTimeNanos = System.nanoTime();
	private @Nullable MappableRingBuffer ringBuffer;

	/**
	 * Upload UBOs for a list of shaders.
	 * Returns one GpuBufferSlice per shader.
	 */
	public List<GpuBufferSlice> upload(List<Shader> shaders, int renderScale) {
		if (shaders.isEmpty())
			return Collections.emptyList();

		int numShaders = shaders.size();
		int alignment = RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
		float time = (System.nanoTime() - startTimeNanos) / 1_000_000_000f;
		float resW = Minecraft.getInstance().getWindow().getWidth() * renderScale;
		float resH = Minecraft.getInstance().getWindow().getHeight() * renderScale;

		// Determine the total size of the buffer from all shaders.
		int totalSize = 0;
		for (Shader shader : shaders)
			totalSize += Math.ceilDiv(shader.uboSize(), alignment) * alignment;

		// Ensure that the buffer is the correct size.
		if (ringBuffer == null || ringBuffer.size() < totalSize) {
			if (ringBuffer != null)
				ringBuffer.close();
			ringBuffer = new MappableRingBuffer(() -> "aoba_shader_params", USAGE, Math.max(totalSize, 4096));
		}
		GpuBuffer gpuBuf = ringBuffer.currentBuffer();

		// Create a UBO slice for each shader.
		List<GpuBufferSlice> slices = new ArrayList<>(numShaders);
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBufferSlice.MappedView mapped = gpuBuf.map(0, totalSize, false, true)) {
			ByteBuffer data = mapped.data();
			int offset = 0;
			for (Shader shader : shaders) {
				data.position(offset);
				shader.writeUBO(Std140Builder.intoBuffer(data), time, resW, resH);
				slices.add(gpuBuf.slice(offset, shader.uboSize()));
				offset += Math.ceilDiv(shader.uboSize(), alignment) * alignment;
			}
		}
		return slices;
	}


	/**
	 * Rotates the ring buffer.
	 */
	public void rotate() {
		if (ringBuffer != null)
			ringBuffer.rotate();
	}

	/**
	 * Closes the ring buffer.
	 */
	@Override
	public void close() {
		if (ringBuffer != null)
			ringBuffer.close();
	}
}
