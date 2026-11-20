/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.aoba.gui.GuiManager;
import net.aoba.gui.colors.Color;
import net.aoba.gui.colors.Colors;
import net.aoba.settings.types.ShaderSetting;

/**
 * Built-in UI themes. Presets change colors and styling without changing fonts
 * or keybinds.
 */
public final class GuiThemes {
	public static final String FOREGROUND = "foreground";
	public static final String FOREGROUND_HEADER = "foreground_header";
	public static final String FOREGROUND_ACCENT = "foreground_accent";
	public static final String WINDOW_BORDER = "window_border";
	public static final String PANEL_BORDER = "panel_border";
	public static final String COMPONENT_BACKGROUND = "component_background";
	public static final String COMPONENT_HOVER_BACKGROUND = "component_hover_background";
	public static final String COMPONENT_BORDER = "component_border";
	public static final String WINDOW_BACKGROUND = "window_background";
	public static final String PANEL_BACKGROUND = "panel_background";
	public static final String BUTTON_BACKGROUND = "button_background";
	public static final String BUTTON_HOVER_BACKGROUND = "button_hover_background";
	public static final String BUTTON_PRESSED_BACKGROUND = "button_pressed_background";
	public static final String BUTTON_BORDER = "button_border";

	public static final String DEFAULT_THEME_ID = "aoba_purple";

	private static final Set<String> STYLE_KEYS = Set.of(FOREGROUND, FOREGROUND_HEADER, FOREGROUND_ACCENT,
			WINDOW_BORDER, PANEL_BORDER, COMPONENT_BACKGROUND, COMPONENT_HOVER_BACKGROUND, COMPONENT_BORDER,
			WINDOW_BACKGROUND, PANEL_BACKGROUND, BUTTON_BACKGROUND, BUTTON_HOVER_BACKGROUND,
			BUTTON_PRESSED_BACKGROUND, BUTTON_BORDER);

	private static final List<ThemePreset> THEMES = List.of(aobaPurple(), midnight(), light(), highContrast());

	private GuiThemes() {
	}

	public static List<ThemePreset> themes() {
		return THEMES;
	}

	public static Set<String> styleKeys() {
		return STYLE_KEYS;
	}

	public static ThemePreset defaultTheme() {
		return THEMES.get(0);
	}

	public static Optional<ThemePreset> findById(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return THEMES.stream().filter(theme -> theme.id().equalsIgnoreCase(id)).findFirst();
	}

	public static void apply(ThemePreset theme) {
		Objects.requireNonNull(theme, "theme");
		Map<String, ShaderSetting> settings = GuiManager.getThemeSettings();
		if (!theme.styles().keySet().equals(settings.keySet())) {
			throw new IllegalArgumentException("Theme must define every UI style");
		}

		theme.styles().forEach((key, shader) -> settings.get(key).setValue(shader.toShader()));
		GuiManager.roundingRadius.setValue(theme.roundingRadius());
		GuiManager.lineThickness.setValue(theme.lineThickness());
		GuiManager.themePreset.setValue(theme.id());
	}

	private static ThemePreset aobaPurple() {
		return builder(DEFAULT_THEME_ID, "Aoba Purple", 12f, 0.5f)
				.style(FOREGROUND, ThemeShader.solid(Colors.White))
				.style(FOREGROUND_HEADER, ThemeShader.solid(new Color(210, 80, 255)))
				.style(FOREGROUND_ACCENT, ThemeShader.solid(Colors.Gray))
				.style(WINDOW_BORDER, ThemeShader.solid(new Color(0, 0, 0)))
				.style(PANEL_BORDER, ThemeShader.solid(new Color(100, 100, 100, 100)))
				.style(COMPONENT_BACKGROUND, ThemeShader.solid(new Color(20, 20, 20, 95)))
				.style(COMPONENT_HOVER_BACKGROUND, ThemeShader.solid(new Color(50, 50, 50, 150)))
				.style(COMPONENT_BORDER, ThemeShader.solid(new Color(115, 115, 115, 200)))
				.style(WINDOW_BACKGROUND, ThemeShader.blur(new Color(55, 55, 55, 255), 60f, 5f))
				.style(PANEL_BACKGROUND, ThemeShader.solid(new Color(90, 90, 90, 60)))
				.style(BUTTON_BACKGROUND,
						ThemeShader.gradient(new Color(180, 50, 220), new Color(100, 20, 160), 90f))
				.style(BUTTON_HOVER_BACKGROUND,
						ThemeShader.gradient(new Color(210, 80, 255), new Color(130, 40, 190), 90f))
				.style(BUTTON_PRESSED_BACKGROUND,
						ThemeShader.gradient(new Color(150, 20, 195), new Color(70, 0, 130), 90f))
				.style(BUTTON_BORDER,
						ThemeShader.gradient(new Color(220, 100, 255), new Color(140, 50, 200), 90f))
				.build();
	}

	private static ThemePreset midnight() {
		return builder("midnight", "Midnight Blue", 8f, 1f)
				.style(FOREGROUND, ThemeShader.solid(new Color(230, 238, 248)))
				.style(FOREGROUND_HEADER, ThemeShader.solid(new Color(94, 210, 255)))
				.style(FOREGROUND_ACCENT, ThemeShader.solid(new Color(148, 163, 184)))
				.style(WINDOW_BORDER, ThemeShader.solid(new Color(24, 40, 64)))
				.style(PANEL_BORDER, ThemeShader.solid(new Color(50, 75, 105, 180)))
				.style(COMPONENT_BACKGROUND, ThemeShader.solid(new Color(17, 28, 45, 220)))
				.style(COMPONENT_HOVER_BACKGROUND, ThemeShader.solid(new Color(35, 55, 80, 230)))
				.style(COMPONENT_BORDER, ThemeShader.solid(new Color(75, 110, 145, 220)))
				.style(WINDOW_BACKGROUND, ThemeShader.blur(new Color(8, 15, 28, 245), 50f, 5f))
				.style(PANEL_BACKGROUND, ThemeShader.solid(new Color(22, 35, 55, 180)))
				.style(BUTTON_BACKGROUND,
						ThemeShader.gradient(new Color(25, 118, 210), new Color(20, 75, 150), 90f))
				.style(BUTTON_HOVER_BACKGROUND,
						ThemeShader.gradient(new Color(45, 155, 240), new Color(25, 100, 185), 90f))
				.style(BUTTON_PRESSED_BACKGROUND,
						ThemeShader.gradient(new Color(15, 85, 160), new Color(10, 55, 110), 90f))
				.style(BUTTON_BORDER,
						ThemeShader.gradient(new Color(85, 190, 255), new Color(35, 115, 205), 90f))
				.build();
	}

	private static ThemePreset light() {
		return builder("light", "Daylight", 10f, 1f)
				.style(FOREGROUND, ThemeShader.solid(new Color(30, 41, 59)))
				.style(FOREGROUND_HEADER, ThemeShader.solid(new Color(37, 99, 235)))
				.style(FOREGROUND_ACCENT, ThemeShader.solid(new Color(71, 85, 105)))
				.style(WINDOW_BORDER, ThemeShader.solid(new Color(148, 163, 184)))
				.style(PANEL_BORDER, ThemeShader.solid(new Color(203, 213, 225)))
				.style(COMPONENT_BACKGROUND, ThemeShader.solid(new Color(255, 255, 255, 230)))
				.style(COMPONENT_HOVER_BACKGROUND, ThemeShader.solid(new Color(226, 232, 240)))
				.style(COMPONENT_BORDER, ThemeShader.solid(new Color(148, 163, 184)))
				.style(WINDOW_BACKGROUND, ThemeShader.blur(new Color(241, 245, 249, 250), 50f, 5f))
				.style(PANEL_BACKGROUND, ThemeShader.solid(new Color(226, 232, 240, 180)))
				.style(BUTTON_BACKGROUND,
						ThemeShader.gradient(new Color(96, 165, 250), new Color(37, 99, 235), 90f))
				.style(BUTTON_HOVER_BACKGROUND,
						ThemeShader.gradient(new Color(125, 180, 250), new Color(59, 130, 246), 90f))
				.style(BUTTON_PRESSED_BACKGROUND,
						ThemeShader.gradient(new Color(37, 99, 235), new Color(29, 78, 216), 90f))
				.style(BUTTON_BORDER,
						ThemeShader.gradient(new Color(147, 197, 253), new Color(59, 130, 246), 90f))
				.build();
	}

	private static ThemePreset highContrast() {
		return builder("high_contrast", "High Contrast", 4f, 2f)
				.style(FOREGROUND, ThemeShader.solid(Colors.White))
				.style(FOREGROUND_HEADER, ThemeShader.solid(new Color(255, 220, 0)))
				.style(FOREGROUND_ACCENT, ThemeShader.solid(Colors.White))
				.style(WINDOW_BORDER, ThemeShader.solid(Colors.White))
				.style(PANEL_BORDER, ThemeShader.solid(Colors.White))
				.style(COMPONENT_BACKGROUND, ThemeShader.solid(Colors.Black))
				.style(COMPONENT_HOVER_BACKGROUND, ThemeShader.solid(new Color(40, 40, 40)))
				.style(COMPONENT_BORDER, ThemeShader.solid(Colors.White))
				.style(WINDOW_BACKGROUND, ThemeShader.solid(Colors.Black))
				.style(PANEL_BACKGROUND, ThemeShader.solid(new Color(20, 20, 20)))
				.style(BUTTON_BACKGROUND, ThemeShader.solid(new Color(0, 70, 180)))
				.style(BUTTON_HOVER_BACKGROUND, ThemeShader.solid(new Color(0, 110, 255)))
				.style(BUTTON_PRESSED_BACKGROUND, ThemeShader.solid(new Color(0, 40, 110)))
				.style(BUTTON_BORDER, ThemeShader.solid(new Color(255, 220, 0)))
				.build();
	}

	private static Builder builder(String id, String displayName, float roundingRadius, float lineThickness) {
		return new Builder(id, displayName, roundingRadius, lineThickness);
	}

	private static final class Builder {
		private final String id;
		private final String displayName;
		private final float roundingRadius;
		private final float lineThickness;
		private final Map<String, ThemeShader> styles = new LinkedHashMap<>();

		private Builder(String id, String displayName, float roundingRadius, float lineThickness) {
			this.id = id;
			this.displayName = displayName;
			this.roundingRadius = roundingRadius;
			this.lineThickness = lineThickness;
		}

		private Builder style(String key, ThemeShader shader) {
			styles.put(key, shader);
			return this;
		}

		private ThemePreset build() {
			if (!styles.keySet().equals(STYLE_KEYS)) {
				throw new IllegalStateException("Theme '" + id + "' must define every UI style");
			}
			return new ThemePreset(id, displayName, styles, roundingRadius, lineThickness);
		}
	}
}
