package net.aoba.rendering.shaders;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.gui.colors.Color;
import net.aoba.rendering.RenderApi;

public class Shader {
	private final String id;
	private final String name;
	private final String description;
	private final RenderApi backend;
	private final int uboSize;
	private RenderPipeline pipeline;
	private RenderPipeline pipeline3D;
	private final List<ShaderUniform> uniformDefs;
	private final int[] uniformOffsets;
	private final float[] values;

	public Shader(String id, String name, String description, RenderApi backend,
			RenderPipeline pipeline, RenderPipeline pipeline3D, List<ShaderUniform> uniformDefs) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.backend = backend;
		this.pipeline = pipeline;
		this.pipeline3D = pipeline3D;
		this.uniformDefs = uniformDefs;

		this.uniformOffsets = new int[uniformDefs.size()];
		int offset = 0;
		for (int i = 0; i < uniformDefs.size(); i++) {
			uniformOffsets[i] = offset;
			offset += uniformDefs.get(i).type().size;
		}

		this.values = new float[offset];
		for (int i = 0; i < uniformDefs.size(); i++) {
			ShaderUniform u = uniformDefs.get(i);
			if (u.type().size > 0)
				System.arraycopy(u.defaultValue(), 0, values, uniformOffsets[i], u.type().size);
		}

		this.uboSize = computeUBOSize();
	}

	private Shader(Shader template) {
		this.id = template.id;
		this.name = template.name;
		this.description = template.description;
		this.backend = template.backend;
		this.pipeline = template.pipeline;
		this.pipeline3D = template.pipeline3D;
		this.uniformDefs = template.uniformDefs;
		this.uniformOffsets = template.uniformOffsets;
		this.values = template.values.clone();
		this.uboSize = template.uboSize;
	}

	public Shader copy() {
		return new Shader(this);
	}

	private static Shader getTemplate(String id) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba == null || aoba.shaderManager == null)
			throw new IllegalStateException("Shader '" + id + "' created before the Shader Manager was initialized.");

		Shader shader = aoba.shaderManager.getShader(id);
		if (shader == null)
			throw new IllegalStateException("Shader '" + id + "' is not registered in the Shader Manager");
			
		return shader.copy();
	}

	public static Shader solid(Color color) {
		Shader shader = getTemplate("solid");
		shader.setColor(0, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
		return shader;
	}

	public static Shader gradient(Color start, Color end, float angle) {
		Shader shader = getTemplate("gradient");
		shader.setColor(0, start.getRed(), start.getGreen(), start.getBlue(), start.getAlpha());
		shader.setColor(1, end.getRed(), end.getGreen(), end.getBlue(), end.getAlpha());
		shader.setFloat(2, angle);
		return shader;
	}

	public static Shader radialGradient(Color inner, Color outer, float centerX, float centerY, float radius) {
		Shader shader = getTemplate("radial_gradient");
		shader.setColor(0, inner.getRed(), inner.getGreen(), inner.getBlue(), inner.getAlpha());
		shader.setColor(1, outer.getRed(), outer.getGreen(), outer.getBlue(), outer.getAlpha());
		shader.setFloat(2, centerX);
		shader.setFloat(3, centerY);
		shader.setFloat(4, radius);
		return shader;
	}

	public static Shader conicalGradient(Color start, Color end, float startAngle, float centerX, float centerY) {
		Shader shader = getTemplate("conical_gradient");
		shader.setColor(0, start.getRed(), start.getGreen(), start.getBlue(), start.getAlpha());
		shader.setColor(1, end.getRed(), end.getGreen(), end.getBlue(), end.getAlpha());
		shader.setFloat(2, startAngle);
		shader.setFloat(3, centerX);
		shader.setFloat(4, centerY);
		return shader;
	}

	public static Shader diamondGradient(Color inner, Color outer, float centerX, float centerY, float size) {
		Shader shader = getTemplate("diamond_gradient");
		shader.setColor(0, inner.getRed(), inner.getGreen(), inner.getBlue(), inner.getAlpha());
		shader.setColor(1, outer.getRed(), outer.getGreen(), outer.getBlue(), outer.getAlpha());
		shader.setFloat(2, centerX);
		shader.setFloat(3, centerY);
		shader.setFloat(4, size);
		return shader;
	}

	public static Shader blur(Color tint, float radius, float quality) {
		Shader shader = getTemplate("blur");
		shader.setColor(3, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha());
		shader.setFloat(4, radius);
		shader.setFloat(5, quality);
		return shader;
	}

	public static Shader blurGradient(Color start, Color end,
			float radius, float quality, float angle) {
		Shader shader = getTemplate("blur_gradient");
		shader.setColor(3, start.getRed(), start.getGreen(), start.getBlue(), start.getAlpha());
		shader.setColor(4, end.getRed(), end.getGreen(), end.getBlue(), end.getAlpha());
		shader.setFloat(5, radius);
		shader.setFloat(6, quality);
		shader.setFloat(7, angle);
		return shader;
	}

	public static Shader image(Color tint) {
		Shader shader = getTemplate("image");
		shader.setColor(0, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha());
		return shader;
	}

	public static Shader rainbow(float speed, float scale, float saturation, float brightness, float transparency) {
		Shader shader = getTemplate("rainbow");
		shader.setFloat(0, speed);
		shader.setFloat(1, scale);
		shader.setFloat(2, saturation);
		shader.setFloat(3, brightness);
		shader.setFloat(4, transparency);
		return shader;
	}

	public RenderPipeline pipeline() { return pipeline; }
	public RenderPipeline pipeline3D() { return pipeline3D; }

	public String id() { return id; }
	public String name() { return name; }
	public String description() { return description; }
	public RenderApi backend() { return backend; }
	public int uboSize() { return uboSize; }
	public List<ShaderUniform> uniforms() { return uniformDefs; }
	public float[] uniformValues() { return values; }

	public boolean needsGameFramebuffer() {
		for (ShaderUniform u : uniformDefs)
			if (u.type() == ShaderUniformType.FRAMEBUFFER) return true;
		return false;
	}

	public int uniformOffset(int index) {
		return uniformOffsets[index];
	}

	public float getFloat(int index) {
		return values[uniformOffsets[index]];
	}

	public void setFloat(int index, float value) {
		values[uniformOffsets[index]] = value;
	}

	public void setColor(int index, float r, float g, float b, float a) {
		int off = uniformOffsets[index];
		values[off] = r;
		values[off + 1] = g;
		values[off + 2] = b;
		values[off + 3] = a;
	}

	private int computeUBOSize() {
		Std140SizeCalculator calc = new Std140SizeCalculator();
		for (ShaderUniform u : uniformDefs) {
			switch (u.type()) {
				case TIME, FLOAT -> calc.putFloat();
				case RESOLUTION -> calc.putVec2();
				case COLOR -> calc.putVec4();
				case FRAMEBUFFER -> {}
			}
		}
		return calc.get();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Shader other)) return false;
		return id.equals(other.id) && Arrays.equals(values, other.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, Arrays.hashCode(values));
	}

	public void writeUBO(Std140Builder builder, float time, float resW, float resH) {
		for (int i = 0; i < uniformDefs.size(); i++) {
			int off = uniformOffsets[i];
			switch (uniformDefs.get(i).type()) {
				case TIME -> builder.putFloat(time);
				case RESOLUTION -> builder.putVec2(resW, resH);
				case FLOAT -> builder.putFloat(values[off]);
				case COLOR -> builder.putVec4(values[off], values[off + 1], values[off + 2], values[off + 3]);
				case FRAMEBUFFER -> {}
			}
		}
	}
}
