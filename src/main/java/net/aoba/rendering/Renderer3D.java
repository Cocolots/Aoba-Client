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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.aoba.Aoba;
import net.aoba.rendering.shaders.Shader;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Renderer3D extends AbstractRenderer {

	private @Nullable GpuBuffer vertexGpuBuffer;
	private int vertexGpuBufferSize;
	private @Nullable GpuTexture offscreenColorTexture;
	private @Nullable GpuTextureView offscreenColorView;
	private @Nullable GpuTexture offscreenDepthTexture;
	private @Nullable GpuTextureView offscreenDepthView;
	private @Nullable GpuSampler offscreenSampler;
	private int offscreenWidth, offscreenHeight;

	private PoseStack matrixStack;
	private PoseStack currentPoseStack = new PoseStack();
	private final ModelCapturingCollector modelCollector = new ModelCapturingCollector();
	private final Set<Class<?>> excludedModels = new HashSet<>();
	private Camera camera;
	private CameraRenderState cameraState;
	private Frustum frustum;
	private DeltaTracker deltaTracker;

	private final ByteBufferBuilder sharedByteBuffer = new ByteBufferBuilder(65536);
	private @Nullable BufferBuilder currentBuilder;
	private @Nullable Shader currentShader;
	private final ArrayList<DrawBatch> pendingBatches = new ArrayList<>();

	/**
	 * Sets the per-frame context.
	 */
	public void beginFrame(PoseStack matrixStack, Frustum frustum, Camera camera, CameraRenderState cameraState,
			DeltaTracker deltaTracker) {
		this.matrixStack = matrixStack;
		this.frustum = frustum;
		this.camera = camera;
		this.cameraState = cameraState;
		this.deltaTracker = deltaTracker;
	}

	public PoseStack getMatrixStack() {
		return matrixStack;
	}

	public Camera getCamera() {
		return camera;
	}

	public Frustum getFrustum() {
		return frustum;
	}

	public DeltaTracker getDeltaTracker() {
		return deltaTracker;
	}

	private void ensureBatch(Shader shader) {
		if (!Objects.equals(currentShader, shader)) {
			finalizeBatch();
			currentShader = shader;
			currentBuilder = new BufferBuilder(sharedByteBuffer, PrimitiveTopology.TRIANGLES,
					DefaultVertexFormat.POSITION_TEX);
		}
	}

	private void finalizeBatch() {
		if (currentBuilder == null || currentShader == null) {
			return;
		}
		
		MeshData mesh = currentBuilder.build();
		if (mesh != null) {
			pendingBatches.add(new DrawBatch(mesh, currentShader));
		}
		currentBuilder = null;
		currentShader = null;
	}

	public void drawBox(AABB box, Shader shader, float lineThickness) {
		ensureBatch(shader);

		AABB b = box.move(camera.position().scale(-1));
		Matrix4f matrix4f = matrixStack.last().pose();

		float x0 = (float) b.minX;
		float y0 = (float) b.minY;
		float z0 = (float) b.minZ;
		float x1 = (float) b.maxX;
		float y1 = (float) b.maxY;
		float z1 = (float) b.maxZ;

		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x1, y0, z0, 1, 0, x1, y0, z1, 1, 1);
		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x1, y0, z1, 1, 1, x0, y0, z1, 0, 1);
		tri(currentBuilder, matrix4f, x0, y1, z0, 0, 0, x0, y1, z1, 0, 1, x1, y1, z1, 1, 1);
		tri(currentBuilder, matrix4f, x0, y1, z0, 0, 0, x1, y1, z1, 1, 1, x1, y1, z0, 1, 0);
		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x0, y1, z0, 0, 1, x1, y1, z0, 1, 1);
		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x1, y1, z0, 1, 1, x1, y0, z0, 1, 0);
		tri(currentBuilder, matrix4f, x0, y0, z1, 0, 0, x1, y0, z1, 1, 0, x1, y1, z1, 1, 1);
		tri(currentBuilder, matrix4f, x0, y0, z1, 0, 0, x1, y1, z1, 1, 1, x0, y1, z1, 0, 1);
		tri(currentBuilder, matrix4f, x1, y0, z0, 0, 0, x1, y1, z0, 0, 1, x1, y1, z1, 1, 1);
		tri(currentBuilder, matrix4f, x1, y0, z0, 0, 0, x1, y1, z1, 1, 1, x1, y0, z1, 1, 0);
		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x0, y0, z1, 1, 0, x0, y1, z1, 1, 1);
		tri(currentBuilder, matrix4f, x0, y0, z0, 0, 0, x0, y1, z1, 1, 1, x0, y1, z0, 0, 1);

		edge(currentBuilder, matrix4f, camera, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, lineThickness);
		edge(currentBuilder, matrix4f, camera, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, lineThickness);
	}

	public void drawLine(Vec3 pos1, Vec3 pos2, Shader shader) {
		drawLine(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, shader);
	}
	
	public void drawLine(Vec3 pos1, Vec3 pos2, Shader shader, float thickness) {
		drawLine(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, shader, thickness);
	}

	public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Shader shader) {
		drawLine(x1, y1, z1, x2, y2, z2, shader, 1f);
	}
	
	public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Shader shader, float thickness) {
		ensureBatch(shader);
		Matrix4f matrix4f = matrixStack.last().pose();
		edge(currentBuilder, matrix4f, camera, x1, y1, z1, x2, y2, z2, thickness);
	}

	private EntityModel<LivingEntityRenderState> getEntityModel(
			LivingEntityRenderer<LivingEntity, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> renderer,
			LivingEntityRenderState renderState) {

		Class<?> rendererClass = renderer.getClass();
		if (!excludedModels.contains(rendererClass)) {
			try {
				renderer.submit(renderState, currentPoseStack, modelCollector, cameraState);
			} catch (Throwable throwable) {
				excludedModels.add(rendererClass);
				currentPoseStack = new PoseStack();
			}
		}
		return renderer.getModel();
	}

	@SuppressWarnings("unchecked")
	public void drawEntityModel(Entity entity, Shader shader) {
		ensureBatch(shader);
		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
		EntityRenderer<?, ?> renderer = MC.getEntityRenderDispatcher().getRenderer(entity);

		if (entity instanceof LivingEntity livingEntity) {
			matrixStack.pushPose();

			LivingEntityRenderer<LivingEntity, LivingEntityRenderState, EntityModel<LivingEntityRenderState>> leRenderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, EntityModel<LivingEntityRenderState>>) renderer;
			LivingEntityRenderState renderState = leRenderer.createRenderState(livingEntity, partialTicks);
			EntityModel<LivingEntityRenderState> model = getEntityModel(leRenderer, renderState);
			model.setupAnim(renderState);
			Direction sleepDirection = livingEntity.getBedOrientation();

			Vec3 interpolatedEntityPosition = getEntityPositionInterpolated(entity, partialTicks)
					.add(camera.position().scale(-1));
			float interpolatedBodyYaw = Mth.rotLerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
			matrixStack.translate(interpolatedEntityPosition.x(), interpolatedEntityPosition.y(),
					interpolatedEntityPosition.z());

			if (livingEntity.hasPose(Pose.SLEEPING) && sleepDirection != null) {
				float sleepingEyeHeight = livingEntity.getEyeHeight(Pose.STANDING) - 0.1f;
				matrixStack.translate(-sleepDirection.getStepX() * sleepingEyeHeight, 0.0f,
						-sleepDirection.getStepZ() * sleepingEyeHeight);
			}

			float entityScale = livingEntity.getScale();
			matrixStack.scale(entityScale, entityScale, entityScale);

			if (entity.isFullyFrozen()) {
				interpolatedBodyYaw += (float) (Math.cos((livingEntity.tickCount * 3.25) * Math.PI * 0.4f));
			}

			if (!livingEntity.hasPose(Pose.SLEEPING)) {
				matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f - interpolatedBodyYaw));
			}

			if (livingEntity.deathTime > 0) {
				float dyingAngle = Mth.sqrt((livingEntity.deathTime + partialTicks - 1.0f) / 20.0f * 1.6f);
				if (dyingAngle > 1.0f) {
					dyingAngle = 1.0f;
				}
				matrixStack.mulPose(Axis.ZP.rotationDegrees(dyingAngle * 90f));
			} else if (livingEntity.isAutoSpinAttack()) {
				matrixStack.mulPose(Axis.XP.rotationDegrees(-90.0f - livingEntity.getXRot()));
				matrixStack.mulPose(Axis.YP.rotationDegrees((livingEntity.tickCount + partialTicks) * -75.0f));
			} else if (livingEntity.hasPose(Pose.SLEEPING)) {
				float sleepAngle = sleepDirection != null ? getYaw(sleepDirection) : interpolatedBodyYaw;
				matrixStack.mulPose(Axis.YP.rotationDegrees(sleepAngle));
				matrixStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
				matrixStack.mulPose(Axis.YP.rotationDegrees(270.0f));
			}

			matrixStack.scale(-1.0f, -1.0f, 1.0f);
			matrixStack.translate(0.0f, -1.501f, 0.0f);

			model.root().visit(matrixStack, (pose, name, cubeIndex, cube) -> {
				Matrix4f m = pose.pose();
				for (ModelPart.Polygon polygon : cube.polygons) {
					ModelPart.Vertex[] verts = polygon.vertices();
					if (verts.length != 4) {
						continue;
					}

					currentBuilder.addVertex(m, verts[0].worldX(), verts[0].worldY(), verts[0].worldZ())
							.setUv(verts[0].u(), verts[0].v());
					currentBuilder.addVertex(m, verts[1].worldX(), verts[1].worldY(), verts[1].worldZ())
							.setUv(verts[1].u(), verts[1].v());
					currentBuilder.addVertex(m, verts[2].worldX(), verts[2].worldY(), verts[2].worldZ())
							.setUv(verts[2].u(), verts[2].v());
					currentBuilder.addVertex(m, verts[0].worldX(), verts[0].worldY(), verts[0].worldZ())
							.setUv(verts[0].u(), verts[0].v());
					currentBuilder.addVertex(m, verts[2].worldX(), verts[2].worldY(), verts[2].worldZ())
							.setUv(verts[2].u(), verts[2].v());
					currentBuilder.addVertex(m, verts[3].worldX(), verts[3].worldY(), verts[3].worldZ())
							.setUv(verts[3].u(), verts[3].v());
				}
			});
			matrixStack.popPose();
		}
	}

	@Override
	public void render() {
		finalizeBatch();
		if (pendingBatches.isEmpty())
			return;
		
		try {
			ensureWhiteTexture();

			int screenW = MC.getWindow().getWidth();
			int screenH = MC.getWindow().getHeight();
			int batchCount = pendingBatches.size();

			int totalSize = 0;
			int maxIdx = 0;
			for (DrawBatch batch : pendingBatches) {
				totalSize += batch.mesh.vertexBuffer().remaining();
				int ic = batch.mesh.drawState().indexCount();
				if (ic > maxIdx) {
					maxIdx = ic;
				}
			}
			ensureVertexGpuBuffer(totalSize);

			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
			int[] offsets = new int[batchCount];
			int offset = 0;
			for (int i = 0; i < batchCount; i++) {
				ByteBuffer vertData = pendingBatches.get(i).mesh.vertexBuffer();
				int size = vertData.remaining();
				offsets[i] = offset;
				try (GpuBufferSlice.MappedView mapped = vertexGpuBuffer.map(offset, size, false, true)) {
					MemoryUtil.memCopy(vertData, mapped.data());
				}
				offset += size;
			}

			// Fill params.
			List<Shader> shadersList = new ArrayList<>(batchCount);
			for (DrawBatch batch : pendingBatches) {
				shadersList.add(batch.shader);
			}
			List<GpuBufferSlice> paramSlices = getShaderParamsBuffer().upload(shadersList, 1);
			RenderSystem.AutoStorageIndexBuffer seqBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES);
			GpuBuffer idxBuffer = seqBuf.getBuffer(maxIdx);
			IndexType idxType = seqBuf.type();
			GpuBufferSlice transforms = uploadIdentityTransform();
			
			ensureOffscreenResources(screenW, screenH);
			try (RenderPass pass = encoder.createRenderPass(() -> "Aoba 3D Shaders", offscreenColorView,
					Optional.of(new Vector4f(0.0F)), offscreenDepthView, OptionalDouble.of(1.0))) {

				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", transforms);
				pass.setVertexBuffer(0, vertexGpuBuffer.slice());
				pass.setIndexBuffer(idxBuffer, idxType);
				pass.disableScissor();

				int vertexSize = DefaultVertexFormat.POSITION_TEX.getVertexSize();

				for (int i = 0; i < batchCount; i++) {
					DrawBatch batch = pendingBatches.get(i);
					pass.setPipeline(batch.shader.pipeline3D());
					pass.setUniform("AobaShaderParams", paramSlices.get(i));
					pass.bindTexture("Sampler0", getWhiteTextureView(), getWhiteSampler());
					pass.drawIndexed(batch.mesh.drawState().indexCount(), 1, 0, offsets[i] / vertexSize, 0);
				}
			}

			Aoba.getInstance().compositor.compose(offscreenColorView, offscreenSampler);
		} finally {
			for (DrawBatch batch : pendingBatches)
				batch.mesh.close();
			pendingBatches.clear();
		}
	}

	@Override
	public void close() {
		destroyOffscreenResources();
		if (vertexGpuBuffer != null) {
			vertexGpuBuffer.close();
			vertexGpuBuffer = null;
			vertexGpuBufferSize = 0;
		}
		sharedByteBuffer.close();
		getShaderParamsBuffer().close();
	}

	private void ensureOffscreenResources(int width, int height) {
		if (offscreenColorTexture != null && offscreenWidth == width && offscreenHeight == height)
			return;

		destroyOffscreenResources();
		offscreenColorTexture = RenderSystem.getDevice().createTexture("aoba_3d_color",
				GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, width,
				height, 1, 1);
		offscreenColorView = RenderSystem.getDevice().createTextureView(offscreenColorTexture);
		offscreenDepthTexture = RenderSystem.getDevice().createTexture("aoba_3d_depth",
				GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.D32_FLOAT, width,
				height, 1, 1);
		offscreenDepthView = RenderSystem.getDevice().createTextureView(offscreenDepthTexture);
		offscreenSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		offscreenWidth = width;
		offscreenHeight = height;
	}

	private void destroyOffscreenResources() {
		if (offscreenColorView != null) {
			offscreenColorView.close();
			offscreenColorView = null;
		}
		if (offscreenColorTexture != null) {
			offscreenColorTexture.close();
			offscreenColorTexture = null;
		}
		if (offscreenDepthView != null) {
			offscreenDepthView.close();
			offscreenDepthView = null;
		}
		if (offscreenDepthTexture != null) {
			offscreenDepthTexture.close();
			offscreenDepthTexture = null;
		}
		offscreenWidth = offscreenHeight = 0;
	}

	private void ensureVertexGpuBuffer(int requiredSize) {
		if (vertexGpuBuffer == null || vertexGpuBufferSize < requiredSize) {
			if (vertexGpuBuffer != null)
				vertexGpuBuffer.close();
			vertexGpuBufferSize = Math.max(requiredSize, 8192);
			vertexGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "aoba_3d_shader", VERTEX_BUFFER_USAGE,
					vertexGpuBufferSize);
		}
	}

	private static void tri(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float u1, float v1, float x2,
			float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3) {
		vc.addVertex(m, x1, y1, z1).setUv(u1, v1);
		vc.addVertex(m, x2, y2, z2).setUv(u2, v2);
		vc.addVertex(m, x3, y3, z3).setUv(u3, v3);
	}

	/**
	 * Draws a camera-facing edge in 3D space from one point to another.
	 */
	private static void edge(VertexConsumer vc, Matrix4f m, Camera camera, double x1, double y1, double z1, double x2,
			double y2, double z2, float thickness) {
		Vec3 cameraPos = camera.position();

		Vec3 pos1 = new Vec3(x1, y1, z1);
		Vec3 pos2 = new Vec3(x2, y2, z2);
		Vec3 startPos = pos1.subtract(cameraPos);
		Vec3 endPos = pos2.subtract(cameraPos);
		Vec3 lineDirection = endPos.subtract(startPos);

		// Face the lines toward the camera.
		double fovRad = Math.toRadians(MC.options.fov().get());
		double pixelToWorld = Math.tan(fovRad * 0.5f) / MC.getWindow().getHeight();

		// Start offset
		double distStart = startPos.length();
		Vec3 offsetStart = new Vec3(
				lineDirection.y * startPos.z - lineDirection.z * startPos.y,
				lineDirection.z * startPos.x - lineDirection.x * startPos.z,
				lineDirection.x * startPos.y - lineDirection.y * startPos.x);
		double offsetStartLen = offsetStart.length();
		if (offsetStartLen < 0.000001)
			return;
		offsetStart = offsetStart.scale(thickness * distStart * pixelToWorld / offsetStartLen);

		// End offset
		double distEnd = endPos.length();
		Vec3 offsetEnd = new Vec3(
				lineDirection.y * endPos.z - lineDirection.z * endPos.y,
				lineDirection.z * endPos.x - lineDirection.x * endPos.z,
				lineDirection.x * endPos.y - lineDirection.y * endPos.x);
		double offsetEndLen = offsetEnd.length();
		if (offsetEndLen < 0.000001)
			return;
		offsetEnd = offsetEnd.scale(thickness * distEnd * pixelToWorld / offsetEndLen);

		vc.addVertex(m, (float)(startPos.x - offsetStart.x), (float)(startPos.y - offsetStart.y), (float)(startPos.z - offsetStart.z)).setUv(0, 0);
		vc.addVertex(m, (float)(startPos.x + offsetStart.x), (float)(startPos.y + offsetStart.y), (float)(startPos.z + offsetStart.z)).setUv(0, 1);
		vc.addVertex(m, (float)(endPos.x + offsetEnd.x), (float)(endPos.y + offsetEnd.y), (float)(endPos.z + offsetEnd.z)).setUv(1, 1);
		vc.addVertex(m, (float)(startPos.x - offsetStart.x), (float)(startPos.y - offsetStart.y), (float)(startPos.z - offsetStart.z)).setUv(0, 0);
		vc.addVertex(m, (float)(endPos.x + offsetEnd.x), (float)(endPos.y + offsetEnd.y), (float)(endPos.z + offsetEnd.z)).setUv(1, 1);
		vc.addVertex(m, (float)(endPos.x - offsetEnd.x), (float)(endPos.y - offsetEnd.y), (float)(endPos.z - offsetEnd.z)).setUv(1, 0);
	}

	private static float getYaw(Direction direction) {
		return switch (direction) {
			case SOUTH -> 90.0f;
			case WEST -> 0.0f;
			case NORTH -> 270.0f;
			case EAST -> 180.0f;
			default -> 0.0f;
		};
	}

	public static Vec3 getEntityPositionInterpolated(Entity entity, float delta) {
		return new Vec3(Mth.lerp(delta, entity.xo, entity.getX()), Mth.lerp(delta, entity.yo, entity.getY()),
				Mth.lerp(delta, entity.zo, entity.getZ()));
	}

	public static Vec3 getEntityPositionOffsetInterpolated(Entity entity, float delta) {
		Vec3 interpolated = getEntityPositionInterpolated(entity, delta);
		return entity.position().subtract(interpolated);
	}

	private record DrawBatch(MeshData mesh, Shader shader) {
	}
}
