/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.themes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * Loads and saves themes using the existing UI setting ids and shader format.
 */
public final class GuiThemeStore {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String RESOURCE_DIRECTORY = "/aoba/themes/";
	private static final String PRESET_INDEX = "index.txt";
	private static final String ID_PROPERTY = "theme.id";
	private static final String NAME_PROPERTY = "theme.name";

	private final Path directory;

	public GuiThemeStore(Path directory) {
		this.directory = directory;
	}

	/**
	 * Copies missing bundled presets without replacing user-modified files.
	 */
	public void copyBuiltinThemes() throws IOException {
		List<String> presetFiles = readBuiltinPresetIndex();
		Files.createDirectories(directory);
		for (String fileName : presetFiles) {
			Path destination = directory.resolve(fileName);
			try (InputStream input = GuiThemeStore.class.getResourceAsStream(RESOURCE_DIRECTORY + fileName)) {
				if (input == null) {
					throw new IOException("Missing bundled theme resource: " + fileName);
				}
				try {
					Files.copy(input, destination);
				} catch (FileAlreadyExistsException ignored) {
					// A user theme always takes precedence over its bundled counterpart.
				}
			}
		}
	}

	public List<ThemePreset> loadAll() throws IOException {
		if (!Files.isDirectory(directory)) {
			return List.of();
		}

		Map<String, ThemePreset> themes = new LinkedHashMap<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (Path file : files) {
				if (!Files.isRegularFile(file)
						|| !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml")) {
					continue;
				}
				try {
					ThemePreset theme = load(file);
					ThemePreset previous = themes.putIfAbsent(theme.id(), theme);
					if (previous != null) {
						LOGGER.warn("Skipping duplicate theme id '{}' in {}", theme.id(), file);
					}
				} catch (Exception e) {
					LOGGER.warn("Skipping invalid theme file {}: {}", file, e.getMessage());
				}
			}
		}

		List<ThemePreset> result = new ArrayList<>(themes.values());
		result.sort(Comparator.comparing(ThemePreset::displayName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ThemePreset::id));
		return List.copyOf(result);
	}

	public ThemePreset load(String id) throws IOException {
		if (!ThemePreset.isValidId(id)) {
			throw new IllegalArgumentException("Invalid theme id: " + id);
		}
		return load(directory.resolve(id + ".xml"));
	}

	public ThemePreset load(Path file) throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(file)) {
			properties.loadFromXML(input);
		}
		return fromProperties(properties);
	}

	public void save(ThemePreset theme) throws IOException {
		if (!theme.styles().keySet().equals(GuiThemes.styleKeys())) {
			throw new IllegalArgumentException("Theme must define every UI style");
		}

		Files.createDirectories(directory);
		Path destination = directory.resolve(theme.id() + ".xml");
		Path temporary = Files.createTempFile(directory, theme.id(), ".tmp");
		try {
			Properties properties = toProperties(theme);
			try (OutputStream output = Files.newOutputStream(temporary)) {
				properties.storeToXML(output, "Aoba UI theme", StandardCharsets.UTF_8);
			}
			try {
				Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public static ThemePreset fromProperties(Properties properties) {
		String id = requiredProperty(properties, ID_PROPERTY);
		String displayName = requiredProperty(properties, NAME_PROPERTY);
		Map<String, ThemeShader> styles = new LinkedHashMap<>();
		for (String key : GuiThemes.styleKeys()) {
			styles.put(key, ThemeShader.parse(requiredProperty(properties, key)));
		}
		try {
			float roundingRadius = Float.parseFloat(requiredProperty(properties, GuiThemes.ROUNDING_RADIUS));
			float lineThickness = Float.parseFloat(requiredProperty(properties, GuiThemes.LINE_THICKNESS));
			return new ThemePreset(id, displayName, styles, roundingRadius, lineThickness);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid theme styling value", e);
		}
	}

	public static Properties toProperties(ThemePreset theme) {
		Properties properties = new Properties();
		properties.setProperty(ID_PROPERTY, theme.id());
		properties.setProperty(NAME_PROPERTY, theme.displayName());
		properties.setProperty(GuiThemes.ROUNDING_RADIUS, Float.toString(theme.roundingRadius()));
		properties.setProperty(GuiThemes.LINE_THICKNESS, Float.toString(theme.lineThickness()));
		theme.styles().forEach((key, shader) -> properties.setProperty(key, shader.serialize()));
		return properties;
	}

	private static String requiredProperty(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing theme property: " + key);
		}
		return value.trim();
	}

	private static List<String> readBuiltinPresetIndex() throws IOException {
		try (InputStream input = GuiThemeStore.class.getResourceAsStream(RESOURCE_DIRECTORY + PRESET_INDEX)) {
			if (input == null) {
				throw new IOException("Missing bundled theme index");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
			Set<String> fileNames = new HashSet<>();
			String line;
			while ((line = reader.readLine()) != null) {
				String fileName = line.trim();
				if (fileName.isEmpty() || fileName.startsWith("#")) {
					continue;
				}
				if (!isSafePresetFileName(fileName) || !fileNames.add(fileName)) {
					throw new IOException("Invalid bundled theme index entry: " + fileName);
				}
			}
			return List.copyOf(fileNames);
		}
	}

	private static boolean isSafePresetFileName(String fileName) {
		if (!fileName.endsWith(".xml") || fileName.contains("/") || fileName.contains("\\")) {
			return false;
		}
		String id = fileName.substring(0, fileName.length() - 4);
		return ThemePreset.isValidId(id);
	}
}
