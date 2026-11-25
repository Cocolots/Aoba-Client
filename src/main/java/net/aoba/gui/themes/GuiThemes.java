/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.aoba.gui.GuiManager;
import net.aoba.settings.types.ShaderSetting;

/**
 * File-backed UI themes. Applying or saving a theme never changes fonts or
 * keybinds.
 */
public final class GuiThemes {
	public static final String FOREGROUND = "ui_foreground_color";
	public static final String FOREGROUND_HEADER = "ui_header_foreground_color";
	public static final String FOREGROUND_ACCENT = "ui_accent_foreground_color";
	public static final String WINDOW_BORDER = "ui_window_border_color";
	public static final String PANEL_BORDER = "ui_panel_border_color";
	public static final String COMPONENT_BACKGROUND = "ui_component_background_color";
	public static final String COMPONENT_HOVER_BACKGROUND = "ui_component_hover_background_color";
	public static final String COMPONENT_BORDER = "ui_component_border_color";
	public static final String WINDOW_BACKGROUND = "ui_window_background_color";
	public static final String PANEL_BACKGROUND = "ui_panel_background_color";
	public static final String BUTTON_BACKGROUND = "ui_button_background_color";
	public static final String BUTTON_HOVER_BACKGROUND = "ui_button_hover_background_color";
	public static final String BUTTON_PRESSED_BACKGROUND = "ui_button_pressed_background_color";
	public static final String BUTTON_BORDER = "ui_button_border_color";
	public static final String ROUNDING_RADIUS = "hud_rounding_radius";
	public static final String LINE_THICKNESS = "ui_line_thickness";

	public static final String DEFAULT_THEME_ID = "aoba_purple";

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Set<String> STYLE_KEYS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(FOREGROUND,
			FOREGROUND_HEADER, FOREGROUND_ACCENT, WINDOW_BORDER, PANEL_BORDER, COMPONENT_BACKGROUND,
			COMPONENT_HOVER_BACKGROUND, COMPONENT_BORDER, WINDOW_BACKGROUND, PANEL_BACKGROUND, BUTTON_BACKGROUND,
			BUTTON_HOVER_BACKGROUND, BUTTON_PRESSED_BACKGROUND, BUTTON_BORDER)));

	private static GuiThemeStore store;

	private GuiThemes() {
	}

	public static synchronized void initialize(Path gameDirectory) {
		Objects.requireNonNull(gameDirectory, "gameDirectory");
		store = new GuiThemeStore(gameDirectory.resolve("aoba").resolve("themes"));
		try {
			store.copyBuiltinThemes();
		} catch (IOException e) {
			LOGGER.error("Failed to copy bundled UI themes", e);
		}
	}

	public static List<ThemePreset> themes() {
		try {
			return requireStore().loadAll();
		} catch (IOException e) {
			LOGGER.error("Failed to scan UI themes", e);
			return List.of();
		}
	}

	public static Set<String> styleKeys() {
		return STYLE_KEYS;
	}

	public static ThemePreset defaultTheme() {
		List<ThemePreset> themes = themes();
		return themes.stream().filter(theme -> theme.id().equals(DEFAULT_THEME_ID)).findFirst()
				.orElseGet(() -> themes.stream().findFirst()
						.orElseThrow(() -> new IllegalStateException("No UI themes are available")));
	}

	public static Optional<ThemePreset> findById(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return themes().stream().filter(theme -> theme.id().equalsIgnoreCase(id)).findFirst();
	}

	public static ThemePreset saveCurrent(String displayName) throws IOException {
		String id = themeIdForDisplayName(displayName);
		Map<String, ThemeShader> styles = new LinkedHashMap<>();
		GuiManager.getThemeSettings().forEach((key, setting) -> styles.put(key, ThemeShader.fromSetting(setting)));
		ThemePreset theme = new ThemePreset(id, displayName, styles, GuiManager.roundingRadius.getValue(),
				GuiManager.lineThickness.getValue());
		requireStore().save(theme);
		GuiManager.themePreset.setValue(theme.id());
		return theme;
	}

	public static String themeIdForDisplayName(String displayName) {
		Objects.requireNonNull(displayName, "displayName");
		String id = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFD).toLowerCase(Locale.ROOT)
				.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (id.length() > 64) {
			id = id.substring(0, 64).replaceAll("[_-]+$", "");
		}
		if (!ThemePreset.isValidId(id)) {
			throw new IllegalArgumentException("Theme name must contain a letter or number");
		}
		return id;
	}

	public static void apply(ThemePreset theme) {
		Objects.requireNonNull(theme, "theme");
		Map<String, ShaderSetting> settings = GuiManager.getThemeSettings();
		if (!theme.styles().keySet().equals(settings.keySet())) {
			throw new IllegalArgumentException("Theme must define every UI style");
		}

		theme.styles().forEach((key, shader) -> shader.applyTo(settings.get(key)));
		GuiManager.roundingRadius.setValue(theme.roundingRadius());
		GuiManager.lineThickness.setValue(theme.lineThickness());
		GuiManager.themePreset.setValue(theme.id());
	}

	private static GuiThemeStore requireStore() {
		if (store == null) {
			throw new IllegalStateException("UI theme store has not been initialized");
		}
		return store;
	}
}
