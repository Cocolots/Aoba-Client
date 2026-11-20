/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A named collection of UI shaders and styling values.
 */
public record ThemePreset(String id, String displayName, Map<String, ThemeShader> styles, float roundingRadius,
		float lineThickness) {
	public ThemePreset {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Theme id must not be blank");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Theme display name must not be blank");
		}
		Objects.requireNonNull(styles, "styles");
		LinkedHashMap<String, ThemeShader> styleCopy = new LinkedHashMap<>();
		styles.forEach((key, shader) -> {
			if (key == null || key.isBlank()) {
				throw new IllegalArgumentException("Theme style keys must not be blank");
			}
			styleCopy.put(key, Objects.requireNonNull(shader, "Theme shader must not be null"));
		});
		styles = Collections.unmodifiableMap(styleCopy);
		if (!Float.isFinite(roundingRadius) || roundingRadius < 0f || roundingRadius > 20f) {
			throw new IllegalArgumentException("Rounding radius must be between 0 and 20");
		}
		if (!Float.isFinite(lineThickness) || lineThickness < 0.5f || lineThickness > 4f) {
			throw new IllegalArgumentException("Line thickness must be between 0.5 and 4");
		}
	}

	@Override
	public String toString() {
		return displayName;
	}
}
