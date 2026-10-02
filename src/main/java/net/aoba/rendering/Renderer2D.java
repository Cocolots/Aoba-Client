/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.rendering;

import static net.aoba.AobaClient.MC;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.aoba.Aoba;
import net.aoba.gui.types.Rectangle;
import net.aoba.rendering.msaa.IAAHandler;
import net.aoba.rendering.msaa.SSAAHandler;
import net.aoba.rendering.shaders.Shader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class Renderer2D extends AbstractRenderer {
	private GuiGraphicsExtractor drawContext;
	private DeltaTracker deltaTracker;
	private final ArrayList<Element> elements = new ArrayList<>();
	private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(131072);
	private @Nullable MappableRingBuffer vertexBuffer;
	private @Nullable ProjectionMatrixBuffer projectionBuffer;
	private static final int MSAA_SAMPLES = 4;
	private final IAAHandler msaaHandler = new SSAAHandler();
	private @Nullable GpuTexture gameSnapshotTexture;
	private @Nullable GpuTextureView gameSnapshotTextureView;
	private int gameSnapshotWidth, gameSnapshotHeight;
	private final Projection guiProjection = new Projection();

	public void beginFrame(GuiGraphicsExtractor drawContext, DeltaTracker deltaTracker) {
		this.drawContext = drawContext;
		this.deltaTracker = deltaTracker;
	}

	public GuiGraphicsExtractor getDrawContext() {
		return drawContext;
	}

	public DeltaTracker getDeltaTracker() {
		return deltaTracker;
	}

	public void captureGameSnapshot() {
		GpuTexture mainColor = MC.gameRenderer.mainRenderTarget().getColorTexture();
		if (mainColor == null)
			return;

		int fbW = MC.gameRenderer.mainRenderTarget().width;
		int fbH = MC.gameRenderer.mainRenderTarget().height;
		ensureGameSnapshot(fbW, fbH);
		RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(mainColor, gameSnapshotTexture, 0, 0, 0, 0,
				0, fbW, fbH);
	}

	private void submitShader(Shader shader, float[] vertices, float[] uvs) {
		submitShader(shader, vertices, uvs, vertices.length / 2);
	}

	private void submitShader(Shader shader, VertexCollector vc) {
		submitShader(shader, vc.positions, vc.uvs, vc.count);
	}

	private void submitShader(Shader shader, float[] vertices, float[] uvs, int vertexCount) {
		Matrix3x2fc pose = new Matrix3x2f(drawContext.pose());
		ScreenRectangle scissor = drawContext.scissorStack.peek();
		submitElement(pose, vertices, uvs, vertexCount, scissor, shader);
	}

	public void drawTexturedQuad(Identifier texture, float x1, float y1, float width, float height, Shader shader) {
		if (shader == null)
			return;
		float x2 = x1 + width, y2 = y1 + height;
		float[] verts = { x1, y1, x2, y1, x2, y2, x1, y1, x2, y2, x1, y2 };
		float[] uvs = { 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1 };
		Matrix3x2fc pose = new Matrix3x2f(drawContext.pose());
		ScreenRectangle scissor = drawContext.scissorStack.peek();
		submitElement(pose, verts, uvs, 6, scissor, shader, texture);
	}

	/**
	 * Draws a sub-region of an atlas texture as a quad.
	 */
	public void drawSprite(Identifier atlas, float u0, float v0, float u1, float v1, float x, float y, float width,
			float height, Shader shader) {
		if (shader == null)
			return;
		float x2 = x + width, y2 = y + height;
		float[] verts = { x, y, x2, y, x2, y2, x, y, x2, y2, x, y2 };
		float[] uvs = { u0, v0, u1, v0, u1, v1, u0, v0, u1, v1, u0, v1 };
		Matrix3x2fc pose = new Matrix3x2f(drawContext.pose());
		ScreenRectangle scissor = drawContext.scissorStack.peek();
		submitElement(pose, verts, uvs, 6, scissor, shader, atlas);
	}

	public void drawBox(float x, float y, float width, float height, Shader shader) {
		if (shader == null)
			return;
		float x2 = x + width, y2 = y + height;
		float[] verts = { x, y, x2, y, x2, y2, x, y, x2, y2, x, y2 };
		float[] uvs = { 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1 };
		submitShader(shader, verts, uvs);
	}

	/**
	 * Draws a filled rounded box using a shader.
	 */
	public void drawRoundedBox(float x, float y, float width, float height, float radius, Shader shader) {
		if (shader == null)
			return;

		float r = Math.min(radius, Math.min(width / 2, height / 2));
		VertexCollector vc = new VertexCollector(128, x, y, width, height);
		buildFilledArc(vc, x + r, y + r, r, 180.0f, 90.0f);
		buildFilledArc(vc, x + width - r, y + r, r, 270.0f, 90.0f);
		buildFilledArc(vc, x + width - r, y + height - r, r, 0.0f, 90.0f);
		buildFilledArc(vc, x + r, y + height - r, r, 90.0f, 90.0f);

		vc.tri(x, y + r, x + width, y + r, x + width, y + height - r);
		vc.tri(x, y + r, x + width, y + height - r, x, y + height - r);

		vc.tri(x + r, y, x + width - r, y, x + width - r, y + r);
		vc.tri(x + r, y, x + width - r, y + r, x + r, y + r);
		vc.tri(x + r, y + height - r, x + width - r, y + height - r, x + width - r, y + height);
		vc.tri(x + r, y + height - r, x + width - r, y + height, x + r, y + height);

		submitShader(shader, vc);
	}

	public void drawEllipse(float x, float y, float radius, Shader shader) {
		drawEllipse(x, y, radius, radius, shader);
	}

	public void drawEllipse(float x, float y, float radiusX, float radiusY, Shader shader) {
		if (shader == null)
			return;
		float bx = x - radiusX, by = y - radiusY, bw = radiusX * 2, bh = radiusY * 2;
		VertexCollector vc = new VertexCollector(93, bx, by, bw, bh);
		double roundedInterval = (360.0f / 30.0f);
		for (int i = 0; i < 30; i++) {
			double angle = Math.toRadians(i * roundedInterval);
			double angle2 = Math.toRadians((i + 1) * roundedInterval);
			float rx1 = (float) (Math.cos(angle) * radiusX);
			float ry1 = (float) (Math.sin(angle) * radiusY);
			float rx2 = (float) (Math.cos(angle2) * radiusX);
			float ry2 = (float) (Math.sin(angle2) * radiusY);
			vc.tri(x, y, x + rx1, y + ry1, x + rx2, y + ry2);
		}
		submitShader(shader, vc);
	}

	/**
	 * Draws a polygon within a given rectangle, scaled to size.
	 */
	public void drawPolygon(float x, float y, float width, float height, float[] tris, Shader shader) {
		if (shader == null || tris == null || tris.length < 6)
			return;

		int triCount = tris.length / 6;
		VertexCollector vc = new VertexCollector(triCount * 3, x, y, width, height);
		for (int i = 0; i < tris.length; i += 6) {
			vc.tri(x + tris[i] * width, y + tris[i + 1] * height, x + tris[i + 2] * width, y + tris[i + 3] * height,
					x + tris[i + 4] * width, y + tris[i + 5] * height);
		}
		submitShader(shader, vc);
	}

	/**
	 * Fills then outlines a plain (non-rounded) box.
	 */
	public void drawOutlinedBox(float x, float y, float width, float height, Shader outlineShader, Shader bgShader) {
		if (bgShader != null) {
			drawBox(x, y, width, height, bgShader);
		}

		if (outlineShader != null) {
			drawBoxOutline(x, y, width, height, outlineShader);
		}
	}

	/**
	 * Fills then outlines a rounded box.
	 */
	public void drawOutlinedRoundedBox(float x, float y, float width, float height, float radius, Shader outlineShader,
			Shader bgShader) {
		if (bgShader != null) {
			drawRoundedBox(x, y, width, height, radius, bgShader);
		}

		if (outlineShader != null) {
			drawRoundedBoxOutline(x, y, width, height, radius, 1f, outlineShader);
		}
	}

	/**
	 ** OUTLINES
	 **/

	public void drawBoxOutline(float x, float y, float width, float height, Shader shader) {
		drawBoxOutline(x, y, width, height, 1.0f, shader);
	}

	public void drawBoxOutline(float x, float y, float width, float height, float thickness, Shader shader) {
		if (shader == null || thickness <= 0f)
			return;

		float halfT = thickness * 0.5f;
		float ix = x + halfT;
		float iy = y + halfT;
		float iw = width - thickness;
		float ih = height - thickness;
		float t = thickness;

		VertexCollector vc = new VertexCollector(24, x, y, width, height);
		buildLineQuad(vc, ix, iy, ix + iw, iy, t);
		buildLineQuad(vc, ix + iw, iy, ix + iw, iy + ih, t);
		buildLineQuad(vc, ix + iw, iy + ih, ix, iy + ih, t);
		buildLineQuad(vc, ix, iy + ih, ix, iy, t);
		submitShader(shader, vc);
	}

	/**
	 * Draws the outline of a rounded box
	 */
	public void drawRoundedBoxOutline(float x, float y, float width, float height, float radius, float thickness,
			Shader shader) {
		if (shader == null || thickness <= 0f)
			return;

		float halfT = thickness * 0.5f;
		float ix = x + halfT;
		float iy = y + halfT;
		float iw = width - thickness;
		float ih = height - thickness;

		float r = Math.min(radius, Math.min(iw / 2, ih / 2));
		float t = thickness;

		VertexCollector vc = new VertexCollector(256, x, y, width, height);
		buildOutlineArc(vc, ix + r, iy + r, r, 180.0f, 90.0f, t);
		buildLineQuad(vc, ix + r, iy, ix + iw - r, iy, t);
		buildOutlineArc(vc, ix + iw - r, iy + r, r, 270.0f, 90.0f, t);
		buildLineQuad(vc, ix + iw, iy + r, ix + iw, iy + ih - r, t);
		buildOutlineArc(vc, ix + iw - r, iy + ih - r, r, 0.0f, 90.0f, t);
		buildLineQuad(vc, ix + iw - r, iy + ih, ix + r, iy + ih, t);
		buildOutlineArc(vc, ix + r, iy + ih - r, r, 90.0f, 90.0f, t);
		buildLineQuad(vc, ix, iy + ih - r, ix, iy + r, t);

		submitShader(shader, vc);
	}

	/**
	 * Draws a line
	 */
	public void drawLine(float x1, float y1, float x2, float y2, Shader shader) {
		if (shader == null)
			return;
		drawLine(x1, y1, x2, y2, 1.0f, shader);
	}

	public void drawLine(float x1, float y1, float x2, float y2, float thickness, Shader shader) {
		if (shader == null)
			return;
		float minX = Math.min(x1, x2), minY = Math.min(y1, y2);
		float w = Math.max(Math.abs(x2 - x1), 1f), h = Math.max(Math.abs(y2 - y1), 1f);
		VertexCollector vc = new VertexCollector(6, minX, minY, w, h);
		buildLineQuad(vc, x1, y1, x2, y2, thickness);
		submitShader(shader, vc);
	}

	/**
	 * Draws an item at the given position.
	 */
	public void drawItem(ItemStack stack, float x, float y) {
		drawContext.item(stack, (int) x, (int) y);
	}

	/**
	 * Draws a string at a certain position. The shader dictates the on-screen
	 * color.
	 */
	public void drawString(String text, float x, float y, Shader shader, Font font) {
		if (shader == null)
			return;
		drawStringWithScale(text, x, y, shader, 2.0f, font);
	}

	/**
	 * Draws a string at the given position using a font size in points.
	 *
	 * @param fontSize Font size in points
	 */
	public void drawString(String text, float x, float y, Shader shader, Font font, float fontSize) {
		if (shader == null)
			return;
		drawStringInternal(text, x, y, shader, 1.0f, font);
	}

	/**
	 * Draws a string at a certain position with a scale.
	 */
	public void drawStringWithScale(String text, float x, float y, Shader shader, float scale, Font font) {
		if (shader == null)
			return;
		drawStringInternal(text, x, y, shader, scale, font);
	}

	private void drawStringInternal(String text, float x, float y, Shader shader, float scale, Font font) {
		drawContext.pose().pushMatrix();
		try {
			drawContext.pose().translate(x, y);
			if (scale != 1.0f) {
				drawContext.pose().scale(scale, scale);
			}

			Matrix3x2fc pose = new Matrix3x2f(drawContext.pose());
			ScreenRectangle scissor = drawContext.scissorStack.peek();
			GpuSampler textSampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
			Font.PreparedText prepared = font.prepareText(text, 0, 0, 0xFFFFFFFF, false, 0);
			prepared.visit(new Font.GlyphVisitor() {
				@Override
				public void acceptGlyph(TextRenderable.Styled styled) {
					QuadCapture capture = new QuadCapture();
					styled.render(IDENTITY_MATRIX, capture, 15728880, false);

					if (capture.count == 0)
						return;

					GpuTextureView textureView = styled.textureView();
					submitElement(pose, capture.positions, capture.uvs, capture.count, scissor, shader, textureView,
							textSampler);
				}

				@Override
				public void acceptEffect(TextRenderable renderable) {
					QuadCapture capture = new QuadCapture();
					renderable.render(IDENTITY_MATRIX, capture, 15728880, false);

					if (capture.count == 0)
						return;

					GpuTextureView textureView = renderable.textureView();
					submitElement(pose, capture.positions, capture.uvs, capture.count, scissor, shader, textureView,
							textSampler);
				}
			});
		} finally {
			drawContext.pose().popMatrix();
		}
	}

	private static int arcSegments(float radius) {
		return Math.max(8, (int) Math.ceil(radius));
	}

	private static void buildFilledArc(VertexCollector vc, float x, float y, float radius, float startAngle,
			float sweepAngle) {
		int segments = arcSegments(radius);
		float interval = sweepAngle / segments;

		for (int i = 0; i < segments; i++) {
			double angle = Math.toRadians(startAngle + (i * interval));
			double angle2 = Math.toRadians(startAngle + ((i + 1) * interval));
			float rx1 = (float) (Math.cos(angle) * radius);
			float ry1 = (float) (Math.sin(angle) * radius);
			float rx2 = (float) (Math.cos(angle2) * radius);
			float ry2 = (float) (Math.sin(angle2) * radius);

			vc.tri(x, y, x + rx1, y + ry1, x + rx2, y + ry2);
		}
	}

	private static void buildOutlineArc(VertexCollector vc, float cx, float cy, float radius, float startAngle,
			float sweepAngle, float thickness) {
		int segments = arcSegments(radius);
		float interval = sweepAngle / segments;
		float halfT = thickness * 0.5f;
		float innerR = radius - halfT;
		float outerR = radius + halfT;

		for (int i = 0; i < segments; i++) {
			double a1 = Math.toRadians(startAngle + (i * interval));
			double a2 = Math.toRadians(startAngle + ((i + 1) * interval));
			float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
			float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

			float ix1 = cx + cos1 * innerR, iy1 = cy + sin1 * innerR;
			float ox1 = cx + cos1 * outerR, oy1 = cy + sin1 * outerR;
			float ix2 = cx + cos2 * innerR, iy2 = cy + sin2 * innerR;
			float ox2 = cx + cos2 * outerR, oy2 = cy + sin2 * outerR;

			vc.tri(ix1, iy1, ox1, oy1, ox2, oy2);
			vc.tri(ix1, iy1, ox2, oy2, ix2, iy2);
		}
	}

	private static void buildLineQuad(VertexCollector vc, float x1, float y1, float x2, float y2, float thickness) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.001f)
			return;
		float nx = -dy / len * thickness * 0.5f;
		float ny = dx / len * thickness * 0.5f;

		vc.tri(x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny);
		vc.tri(x1 + nx, y1 + ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny);
	}

	/**
	 * Enables scissor clipping to the given rectangle
	 */
	public void beginClip(Rectangle clip) {
		int x1 = (int) clip.x();
		int y1 = (int) clip.y();
		int x2 = (int) (clip.x() + clip.width());
		int y2 = (int) (clip.y() + clip.height());
		drawContext.enableScissor(x1, y1, x2, y2);
	}

	public void endClip() {
		drawContext.disableScissor();
	}

	public static int getStringWidth(String text, Font font) {
		return font.width(text);
	}

	public static float getStringWidth(String text, Font font, float fontSize) {
		return font.width(text);
	}

	private void submitElement(Matrix3x2fc pose, float[] vertices, float[] uvs, int vertexCount,
			@Nullable ScreenRectangle scissor, Shader shader) {
		elements.add(new Element(new Matrix3x2f(pose), vertices, uvs, vertexCount,
				new RenderState(shader, scissor, TextureSource.NONE)));
	}

	private void submitElement(Matrix3x2fc pose, float[] vertices, float[] uvs, int vertexCount,
			@Nullable ScreenRectangle scissor, Shader shader, Identifier texture) {
		elements.add(new Element(new Matrix3x2f(pose), vertices, uvs, vertexCount,
				new RenderState(shader, scissor, TextureSource.of(texture))));
	}

	private void submitElement(Matrix3x2fc pose, float[] vertices, float[] uvs, int vertexCount,
			@Nullable ScreenRectangle scissor, Shader shader, GpuTextureView textureView, GpuSampler sampler) {
		elements.add(new Element(new Matrix3x2f(pose), vertices, uvs, vertexCount,
				new RenderState(shader, scissor, TextureSource.direct(textureView, sampler))));
	}

	@Override
	public void render() {
		if (elements.isEmpty())
			return;

		List<Batch> batches = buildBatches();
		if (batches.isEmpty()) {
			elements.clear();
			return;
		}

		ensureWhiteTexture();

		RenderSystem.backupProjectionMatrix();
		try {
			List<DrawCmd> draws = uploadVertexData(batches);
			if (!draws.isEmpty()) {
				executeDraw(draws);
			}
		} finally {
			RenderSystem.restoreProjectionMatrix();
			if (vertexBuffer != null)
				vertexBuffer.rotate();
			getShaderParamsBuffer().rotate();
			for (Batch batch : batches) {
				batch.close();
			}

			elements.clear();
		}
	}

	@Override
	public void close() {
		byteBufferBuilder.close();
		if (vertexBuffer != null)
			vertexBuffer.close();
		if (projectionBuffer != null)
			projectionBuffer.close();
		msaaHandler.close();
		destroyGameSnapshot();
		getShaderParamsBuffer().close();
	}

	private List<Batch> buildBatches() {
		List<Batch> batches = new ArrayList<>();
		RenderState currentState = elements.get(0).state;

		BufferBuilder builder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.TRIANGLES,
				DefaultVertexFormat.POSITION_TEX);

		for (Element elem : elements) {
			if (!elem.state.batchesWith(currentState)) {
				finalizeBatch(builder, batches, currentState);
				currentState = elem.state;
				builder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.TRIANGLES,
						DefaultVertexFormat.POSITION_TEX);
			}

			int vertCount = elem.vertexCount;
			for (int i = 0; i < vertCount; i++) {
				builder.addVertexWith2DPose(elem.pose, elem.vertices[i * 2], elem.vertices[i * 2 + 1])
						.setUv(elem.uvs[i * 2], elem.uvs[i * 2 + 1]);
			}
		}

		finalizeBatch(builder, batches, currentState);
		return batches;
	}

	private void finalizeBatch(BufferBuilder builder, List<Batch> batches, RenderState state) {
		MeshData mesh = builder.build();
		if (mesh != null)
			batches.add(new Batch(mesh, state));
	}

	private List<DrawCmd> uploadVertexData(List<Batch> batches) {
		int totalBytes = 0;
		for (Batch b : batches)
			totalBytes += b.mesh.vertexBuffer().remaining();
		ensureVertexBuffer(totalBytes);

		List<Shader> shadersList = new ArrayList<>(batches.size());
		for (Batch b : batches)
			shadersList.add(b.state.shader);
		List<GpuBufferSlice> shaderSlices = getShaderParamsBuffer().upload(shadersList, msaaHandler.renderScale());

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		int offset = 0;
		int vertexSize = DefaultVertexFormat.POSITION_TEX.getVertexSize();
		List<DrawCmd> draws = new ArrayList<>();

		for (int i = 0; i < batches.size(); i++) {
			Batch batch = batches.get(i);
			ByteBuffer vertData = batch.mesh.vertexBuffer();
			int size = vertData.remaining();

			GpuBuffer gpuBuf = vertexBuffer.currentBuffer();
			try (GpuBufferSlice.MappedView mapped = gpuBuf.map(offset, size, false, true)) {
				MemoryUtil.memCopy(vertData, mapped.data());
			}

			GpuTextureView texView = null;
			GpuSampler sampler = null;
			TextureSource ts = batch.state.textureSource;
			if (ts.identifier != null) {
				AbstractTexture tex = MC.getTextureManager().getTexture(ts.identifier);
				texView = tex.getTextureView();
				sampler = tex.getSampler();
			} else if (ts.directView != null) {
				texView = ts.directView;
				sampler = ts.directSampler;
			}

			MeshData.DrawState ds = batch.mesh.drawState();
			draws.add(new DrawCmd(gpuBuf, offset / vertexSize, ds.indexCount(), batch.state.shader, batch.state.scissor,
					texView, sampler, shaderSlices.get(i)));
			offset += size;
		}
		return draws;
	}

	private void executeDraw(List<DrawCmd> draws) {
		Window window = MC.getWindow();
		int screenW = window.getWidth();
		int screenH = window.getHeight();

		if (projectionBuffer == null)
			projectionBuffer = new ProjectionMatrixBuffer("aoba_gui");

		float sw = (float) screenW / window.getGuiScale();
		float sh = (float) screenH / window.getGuiScale();
		this.guiProjection.setupOrtho(1000.0F, 11000.0F, sw, sh, true);
		RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(guiProjection), ProjectionType.ORTHOGRAPHIC);

		GpuBufferSlice transforms = uploadGuiTransform();

		int maxIdx = 0;
		for (DrawCmd cmd : draws) {
			if (cmd.indexCount > maxIdx) {
				maxIdx = cmd.indexCount;
			}
		}

		RenderSystem.AutoStorageIndexBuffer seqBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES);
		GpuBuffer idxBuffer = seqBuf.getBuffer(maxIdx);
		IndexType idxType = seqBuf.type();

		msaaHandler.prepare(screenW, screenH, MSAA_SAMPLES);

		try (RenderPass pass = msaaHandler.beginPass()) {
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);

			for (DrawCmd draw : draws) {
				pass.setPipeline(RenderSystem.getCompiledPipeline(draw.shader.pipeline()));
				pass.setVertexBuffer(0, draw.vertexBuffer.slice());

				if (draw.scissor != null) {
					enableScissor(draw.scissor, pass, window);
				} else {
					pass.disableScissor();
				}

				if (draw.shader.needsGameFramebuffer() && gameSnapshotTextureView != null) {
					pass.setUniform("Sampler0", gameSnapshotTextureView,
							RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
				} else if (draw.textureView != null) {
					pass.setUniform("Sampler0", draw.textureView, draw.sampler);
				} else {
					pass.setUniform("Sampler0", getWhiteTextureView(), getWhiteSampler());
				}

				pass.setUniform("AobaShaderParams", draw.shaderParamsSlice);
				pass.setIndexBuffer(idxBuffer, idxType);
				pass.drawIndexed(draw.indexCount, 1, 0, draw.baseVertex, 0);
			}
		}

		IAAHandler.ResolvedTarget resolved = msaaHandler.resolve();
		Aoba.getInstance().compositor.compose(resolved.view(), resolved.sampler());
	}

	private void ensureVertexBuffer(int requiredSize) {
		if (vertexBuffer == null || vertexBuffer.size() < requiredSize) {
			if (vertexBuffer != null)
				vertexBuffer.close();
			vertexBuffer = new MappableRingBuffer(() -> "aoba_gui", VERTEX_BUFFER_USAGE, requiredSize);
		}
	}

	private void enableScissor(ScreenRectangle rect, RenderPass pass, Window window) {
		int ss = msaaHandler.renderScale();
		int scale = window.getGuiScale() * ss;
		int height = window.getHeight() * ss;
		pass.enableScissor((int) (rect.left() * scale), (int) (height - rect.bottom() * scale),
				Math.max(0, (int) (rect.width() * scale)), Math.max(0, (int) (rect.height() * scale)));
	}

	private void ensureGameSnapshot(int width, int height) {
		if (gameSnapshotTexture != null && gameSnapshotWidth == width && gameSnapshotHeight == height)
			return;
		destroyGameSnapshot();
		gameSnapshotTexture = RenderSystem.getDevice().createTexture("aoba_game_snapshot",
				GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
		gameSnapshotTextureView = RenderSystem.getDevice().createTextureView(gameSnapshotTexture);
		gameSnapshotWidth = width;
		gameSnapshotHeight = height;
	}

	private void destroyGameSnapshot() {
		if (gameSnapshotTextureView != null) {
			gameSnapshotTextureView.close();
			gameSnapshotTextureView = null;
		}

		if (gameSnapshotTexture != null) {
			gameSnapshotTexture.close();
			gameSnapshotTexture = null;
		}
		gameSnapshotWidth = gameSnapshotHeight = 0;
	}

	private record TextureSource(@Nullable Identifier identifier, @Nullable GpuTextureView directView,
			@Nullable GpuSampler directSampler) {
		static final TextureSource NONE = new TextureSource(null, null, null);

		static TextureSource of(Identifier id) {
			return new TextureSource(id, null, null);
		}

		static TextureSource direct(GpuTextureView view, GpuSampler sampler) {
			return new TextureSource(null, view, sampler);
		}

		@Nullable
		Object batchKey() {
			return identifier != null ? identifier : directView;
		}
	}

	private record RenderState(Shader shader, @Nullable ScreenRectangle scissor, TextureSource textureSource) {
		boolean batchesWith(RenderState other) {
			Shader otherShader = other.shader;
			boolean shadersEqual = shader == otherShader || (shader.pipeline() == otherShader.pipeline()
					&& Arrays.equals(shader.uniformValues(), otherShader.uniformValues()));
			return shadersEqual && Objects.equals(scissor, other.scissor)
					&& Objects.equals(textureSource.batchKey(), other.textureSource.batchKey());
		}
	}

	private record Element(Matrix3x2fc pose, float[] vertices, float[] uvs, int vertexCount, RenderState state) {
	}

	private record Batch(MeshData mesh, RenderState state) implements AutoCloseable {
		@Override
		public void close() {
			mesh.close();
		}
	}

	private record DrawCmd(GpuBuffer vertexBuffer, int baseVertex, int indexCount, Shader shader,
			@Nullable ScreenRectangle scissor, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler,
			GpuBufferSlice shaderParamsSlice) {
	}
}
