package net.aoba.rendering;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class QuadCapture implements VertexConsumer {
	private final float[] qx = new float[4];
	private final float[] qy = new float[4];
	private final float[] qu = new float[4];
	private final float[] qv = new float[4];
	private int vi = 0;

	float[] positions = new float[96];
	float[] uvs = new float[96];
	int count = 0;

	private void grow() {
		int newSize = positions.length * 2;
		positions = Arrays.copyOf(positions, newSize);
		uvs = Arrays.copyOf(uvs, newSize);
	}

	private void emitVertex(int i) {
		if (count * 2 >= positions.length) grow();
		positions[count * 2] = qx[i];
		positions[count * 2 + 1] = qy[i];
		uvs[count * 2] = qu[i];
		uvs[count * 2 + 1] = qv[i];
		count++;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		qx[vi] = x;
		qy[vi] = y;
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) { return this; }

	@Override
	public VertexConsumer setColor(int color) { return this; }

	@Override
	public VertexConsumer setUv(float u, float v) {
		qu[vi] = u;
		qv[vi] = v;
		return this;
	}
	
	@Override
	public VertexConsumer setUv1(int u, int v) { return this; }

	@Override
	public VertexConsumer setUv2(int u, int v) {
		vi++;
		if (vi == 4) {
			emitVertex(0); emitVertex(1); emitVertex(2);
			emitVertex(0); emitVertex(2); emitVertex(3);
			vi = 0;
		}
		return this;
	}

	@Override
	public VertexConsumer setUv3(float u, float v) { return this; }

	@Override
	public VertexConsumer setNormal(float x, float y, float z) { return this; }

	@Override
	public VertexConsumer setLineWidth(float w) { return this; }
}