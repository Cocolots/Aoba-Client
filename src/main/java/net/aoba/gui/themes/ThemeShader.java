/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.util.Objects;

import net.aoba.gui.colors.Color;
import net.aoba.rendering.shaders.Shader;

/**
 * Immutable, renderer-independent shader description used by theme presets.
 */
public record ThemeShader(Kind kind, Color primary, Color secondary, float angle, float radius, float quality) {
	public enum Kind {
		SOLID, GRADIENT, BLUR
	}

	public ThemeShader {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(primary, "primary");
		Objects.requireNonNull(secondary, "secondary");
		if (!Float.isFinite(angle) || !Float.isFinite(radius) || !Float.isFinite(quality)) {
			throw new IllegalArgumentException("Shader parameters must be finite");
		}
	}

	public static ThemeShader solid(Color color) {
		return new ThemeShader(Kind.SOLID, color, color, 0f, 0f, 0f);
	}

	public static ThemeShader gradient(Color start, Color end, float angle) {
		return new ThemeShader(Kind.GRADIENT, start, end, angle, 0f, 0f);
	}

	public static ThemeShader blur(Color tint, float radius, float quality) {
		return new ThemeShader(Kind.BLUR, tint, tint, 0f, radius, quality);
	}

	public Shader toShader() {
		return switch (kind) {
		case SOLID -> Shader.solid(primary);
		case GRADIENT -> Shader.gradient(primary, secondary, angle);
		case BLUR -> Shader.blur(primary, radius, quality);
		};
	}
}
