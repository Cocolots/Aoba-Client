package net.aoba.rendering;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Composites an offscreen texture onto the main render target
 */
public final class Compositor implements AutoCloseable {
	private static final int VERTEX_BUFFER_USAGE = GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE;

	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
					.withLocation("pipeline/aoba_composite")
					.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
					.withCull(false).build());

	private @Nullable ByteBufferBuilder byteBuilder;
	private @Nullable MappableRingBuffer vertexBuffer;
	private @Nullable ProjectionMatrixBuffer projectionBuffer;

	public void compose(GpuTextureView textureView, GpuSampler sampler) {
		Minecraft mc = Minecraft.getInstance();
		var window = mc.getWindow();

		if (projectionBuffer == null) {
			projectionBuffer = new ProjectionMatrixBuffer("aoba_composite");
		}
			
		if (byteBuilder == null) {
			byteBuilder = new ByteBufferBuilder(512);
		}

		float sw = (float) window.getWidth() / window.getGuiScale();
		float sh = (float) window.getHeight() / window.getGuiScale();

		Projection projection = new Projection();
		projection.setupOrtho(1000.0F, 11000.0F, sw, sh, true);
		RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);

		GpuBufferSlice transforms = AbstractRenderer.uploadGuiTransform();

		RenderSystem.AutoStorageIndexBuffer seqBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		GpuBuffer idxBuffer = seqBuf.getBuffer(6);
		IndexType idxType = seqBuf.type();

		BufferBuilder builder = new BufferBuilder(byteBuilder, PrimitiveTopology.QUADS,
				DefaultVertexFormat.POSITION_TEX_COLOR);
		builder.addVertex(0, 0, 0).setUv(0, 1).setColor(-1);
		builder.addVertex(sw, 0, 0).setUv(1, 1).setColor(-1);
		builder.addVertex(sw, sh, 0).setUv(1, 0).setColor(-1);
		builder.addVertex(0, sh, 0).setUv(0, 0).setColor(-1);

		MeshData mesh = builder.build();
		if (mesh == null) {
			byteBuilder.discard();
			return;
		}

		try {
			int requiredSize = mesh.vertexBuffer().remaining();
			if (vertexBuffer == null || vertexBuffer.size() < requiredSize) {
				if (vertexBuffer != null) {
					vertexBuffer.close();
				}
				vertexBuffer = new MappableRingBuffer(() -> "aoba_composite", VERTEX_BUFFER_USAGE, requiredSize);
			}

			GpuBuffer gpuBuf = vertexBuffer.currentBuffer();
			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
			try (GpuBufferSlice.MappedView mapped = gpuBuf.map(0, requiredSize, false, true)) {
				MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
			}

			try (RenderPass pass = encoder.createRenderPass(() -> "Aoba Composite",
					mc.gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty(),
					mc.gameRenderer.mainRenderTarget().hasDepth() ? mc.gameRenderer.mainRenderTarget().getDepthTextureView() : null,
					OptionalDouble.empty())) {

				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", transforms);
				pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
				pass.setVertexBuffer(0, gpuBuf.slice());
				pass.setUniform("Sampler0", textureView, sampler);
				pass.disableScissor();
				pass.setIndexBuffer(idxBuffer, idxType);
				pass.drawIndexed(6, 1, 0, 0, 0);
			}

			vertexBuffer.rotate();
		} finally {
			mesh.close();
		}
	}

	@Override
	public void close() {
		if (byteBuilder != null) {
			byteBuilder.close();
			byteBuilder = null;
		}
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
		if (projectionBuffer != null) {
			projectionBuffer.close();
			projectionBuffer = null;
		}
	}
}
