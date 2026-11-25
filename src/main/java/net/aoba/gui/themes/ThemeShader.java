/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.aoba.gui.colors.Color;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.ShaderSetting;

/**
 * Immutable shader value using the same serialization as {@link ShaderSetting}.
 */
public record ThemeShader(String id, List<Float> uniformValues) {
	public ThemeShader {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Shader id must not be blank");
		}
		Objects.requireNonNull(uniformValues, "uniformValues");
		uniformValues = List.copyOf(uniformValues);
		for (Float value : uniformValues) {
			if (value == null || !Float.isFinite(value)) {
				throw new IllegalArgumentException("Shader parameters must be finite");
			}
		}
	}

	public static ThemeShader solid(Color color) {
		return new ThemeShader("solid", colorComponents(color));
	}

	public static ThemeShader gradient(Color start, Color end, float angle) {
		List<Float> values = new ArrayList<>(colorComponents(start));
		values.addAll(colorComponents(end));
		values.add(angle);
		return new ThemeShader("gradient", values);
	}

	public static ThemeShader blur(Color tint, float radius, float quality) {
		List<Float> values = new ArrayList<>(List.of(0f, 0f, 0f));
		values.addAll(colorComponents(tint));
		values.add(radius);
		values.add(quality);
		return new ThemeShader("blur", values);
	}

	public static ThemeShader fromShader(Shader shader) {
		Objects.requireNonNull(shader, "shader");
		List<Float> values = new ArrayList<>();
		for (float value : shader.uniformValues()) {
			values.add(value);
		}
		return new ThemeShader(shader.id(), values);
	}

	public static ThemeShader fromSetting(ShaderSetting setting) {
		return parse(setting.getSerializedValue());
	}

	public static ThemeShader parse(String value) {
		Objects.requireNonNull(value, "value");
		String[] parts = value.split(",", -1);
		List<Float> uniforms = new ArrayList<>();
		for (int i = 1; i < parts.length; i++) {
			try {
				uniforms.add(Float.parseFloat(parts[i]));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid shader parameter: " + parts[i], e);
			}
		}
		return new ThemeShader(parts[0], uniforms);
	}

	public String serialize() {
		StringBuilder value = new StringBuilder(id);
		for (float uniform : uniformValues) {
			value.append(",").append(uniform);
		}
		return value.toString();
	}

	public void applyTo(ShaderSetting setting) {
		setting.setSerializedValue(serialize());
	}

	private static List<Float> colorComponents(Color color) {
		Objects.requireNonNull(color, "color");
		return List.of(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}
}
