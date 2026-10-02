/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.logging.LogUtils;

import net.aoba.gui.colors.Color;
import net.aoba.gui.types.Rectangle;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.Setting;
import net.aoba.settings.types.*;
import net.aoba.utils.types.Range;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class SettingManager {
	private static final Minecraft MC = Minecraft.getInstance();

	private static final StringSetting currentConfig = StringSetting.builder().id("selected_config").defaultValue("default")
			.onUpdate(s -> {
				loadSettings();
			}).build();

	private final static HashSet<Setting<?>> globalSettings = new HashSet<>();
	private final static HashSet<Setting<?>> settings = new HashSet<>();
	public final static List<String> configNames = new ArrayList<String>();

	public SettingManager() {
		refreshSettingFiles();
	}

	/**
	 * Registers a setting that is a part of a settings profile.
	 * 
	 * @param setting Setting to add to the settings list.
	 */
	public static void registerSetting(Setting<?> setting) {
		settings.add(setting);
	}

	/**
	 * Registers a global setting that will not change regardless of settings
	 * profile.
	 * 
	 * @param setting Setting to add to global settings list.
	 */
	public static void registerGlobalSetting(Setting<?> setting) {
		globalSettings.add(setting);
	}

	public static void setCurrentConfig(String name) {
		if (configNames.contains(name)) {
			currentConfig.setValue(name);
		}
	}

	/**
	 * Rescans the %appdata%\.minecraft\aoba\settings directory for settings XML
	 * files.
	 */
	public static void refreshSettingFiles() {
		File settingsDirecotry = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "settings");

		if (settingsDirecotry.exists() && settingsDirecotry.isDirectory()) {
			LogUtils.getLogger().info("Found Settings Directory: " + settingsDirecotry.getAbsolutePath());
			File[] files = settingsDirecotry.listFiles((dir, name) -> name.endsWith(".xml"));

			if (files != null) {
				for (File file : files) {
					String name = file.getName().replace(".xml", "");
					if (!name.equals("globals"))
						configNames.add(name);
				}
			}
		}
	}

	/**
	 * Saves the current settings profile to the disk (usually on shutdown)
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void saveSettings() throws FileNotFoundException, IOException {
		File settingsFolder = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "settings");
		if (!settingsFolder.exists() && !settingsFolder.mkdirs()) {
			throw new IOException("Failed to create settings folder: " + settingsFolder.getAbsolutePath());
		} else {
			// Save Global config.
			LogUtils.getLogger().info("Saving global config.");
			Properties globalConfig = new Properties();
			fillProperties(globalConfig, globalSettings);
			try (FileOutputStream out = new FileOutputStream(settingsFolder.getPath() + File.separator + "globals.xml")) {
				globalConfig.storeToXML(out, null);
			}

			// Save selectable config.
			String configName = currentConfig.getValue();
			LogUtils.getLogger().info("Saving config " + configName + ".");
			Properties config = new Properties();
			fillProperties(config, settings);
			try (FileOutputStream out = new FileOutputStream(settingsFolder.getPath() + File.separator + configName + ".xml")) {
				config.storeToXML(out, null);
			}
		}
	}

	/**
	 * Saves a copy of the current settings profile to the disk.
	 * 
	 * @param fileName File name to save to.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void saveCopy(String fileName) throws FileNotFoundException, IOException {
		// Don't allow this
		if (fileName.equals("default") || fileName.equals("globals"))
			return;

		File settingsFolder = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "settings");
		if (!settingsFolder.exists() && !settingsFolder.mkdirs()) {
			throw new IOException("Failed to create settings folder: " + settingsFolder.getAbsolutePath());
		} else {
			LogUtils.getLogger().info("Saving config " + fileName + ".");
			Properties config = new Properties();
			fillProperties(config, settings);
			try (FileOutputStream out = new FileOutputStream(settingsFolder.getPath() + File.separator + fileName + ".xml")) {
				config.storeToXML(out, null);
			}
		}
	}

	private static void fillProperties(Properties properties, HashSet<Setting<?>> settings) {
		for (Setting<?> setting : settings) {
			try {
				switch (setting.type) {
				case FLOAT, INTEGER, BOOLEAN, STRING -> {
					properties.setProperty(setting.ID, String.valueOf(setting.getValue()));
				}
				case FONT -> {
					FontSetting fontSetting = (FontSetting) setting;
					properties.setProperty(setting.ID, fontSetting.getFontName());
				}
				case KEYBIND -> {
					Key key = ((Key) setting.getValue());
					properties.setProperty(setting.ID, String.valueOf(key.getValue()));
				}
				case RECTANGLE -> {
					Rectangle rect = (Rectangle) setting.getValue();
					properties.setProperty(setting.ID,
							rect.x() + "," + rect.y() + "," + rect.width() + "," + rect.height());
				}
				case COLOR -> {
					Color c = (Color) setting.getValue();
					properties.setProperty(setting.ID, c.getColorAsHex());
				}
				case SHADER -> {
					ShaderSetting sSetting = (ShaderSetting) setting;
					StringBuilder s = new StringBuilder();
					s.append(sSetting.getShaderId());
					Shader shader = sSetting.getValue();
					if (shader != null && shader.uniformValues().length > 0) {
						for (float v : shader.uniformValues())
							s.append(",").append(v);
					}
					properties.setProperty(setting.ID, s.toString());
				}
				case BLOCKS -> {
					@SuppressWarnings("unchecked")
					HashSet<Block> s = (HashSet<Block>) setting.getValue();
					StringBuilder result = new StringBuilder();

					int iteration = 0;
					for (Block block : s) {
						Identifier id = BuiltInRegistries.BLOCK.getKey(block);
						result.append(id.getNamespace()).append(":").append(id.getPath());
						if (iteration != s.size() - 1) {
							result.append(",");
						}
						iteration++;
					}

					properties.setProperty(setting.ID, result.toString());
				}
				case ENTITIES -> {
					@SuppressWarnings("unchecked")
					Set<EntityType<?>> s = (Set<EntityType<?>>) setting.getValue();
					StringBuilder result = new StringBuilder();

					int iteration = 0;
					for (EntityType<?> entityType : s) {
						Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
						result.append(id.getNamespace()).append(":").append(id.getPath());
						if (iteration != s.size() - 1) {
							result.append(",");
						}
						iteration++;
					}

					properties.setProperty(setting.ID, result.toString());
				}
				case ENUM -> {
					properties.setProperty(setting.ID, ((Enum<?>) setting.getValue()).name());
				}
				case VEC3D -> {
					Vec3 vec = (Vec3) setting.getValue();
					properties.setProperty(setting.ID, vec.x + "," + vec.y + "," + vec.z);
				}
				case RANGE -> {
					Range range = (Range) setting.getValue();
					properties.setProperty(setting.ID, range.min() + "," + range.max());
				}
				case HOTBAR -> {
					@SuppressWarnings("unchecked")
					List<Boolean> s = (List<Boolean>) setting.getValue();
					StringBuilder result = new StringBuilder();

					int iteration = 0;
					for (Boolean value : s) {
						result.append(value.toString());
						if (iteration != s.size() - 1) {
							result.append(",");
						}
						iteration++;
					}

					properties.setProperty(setting.ID, result.toString());
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + setting.type);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads the global settings from the globals.xml
	 */
	public static void loadGlobalSettings() {
		try {
			LogUtils.getLogger().info("Loading global config.");

			// Load config from file.
			Properties config = new Properties();
			try (FileInputStream fis = new FileInputStream(MC.gameDirectory + File.separator + "aoba" + File.separator
					+ "settings" + File.separator + "globals.xml")) {
				config.loadFromXML(fis);
			} catch (InvalidPropertiesFormatException e) {
				LogUtils.getLogger().error("Invalid XML format in properties file: " + e.getMessage());
			} catch (IOException e) {
				LogUtils.getLogger().error("IOException while loading properties file: " + e.getMessage());
			}
			deserializeSettings(config, globalSettings);
		} catch (Exception e) {
			LogUtils.getLogger().error(e.getMessage());
		}
	}

	/**
	 * Loads the settings based off of the current configuration value.
	 */
	public static void loadSettings() {
		try {
			String configName = currentConfig.getValue();
			LogUtils.getLogger().info("Loading config " + configName + ".");

			// Load config from file.
			Properties config = new Properties();
			try (FileInputStream fis = new FileInputStream(MC.gameDirectory + File.separator + "aoba" + File.separator
					+ "settings" + File.separator + configName + ".xml")) {
				config.loadFromXML(fis);
			} catch (InvalidPropertiesFormatException e) {
				LogUtils.getLogger().error("Invalid XML format in properties file: " + e.getMessage());
			} catch (IOException e) {
				LogUtils.getLogger().error("IOException while loading properties file: " + e.getMessage());
			}
			deserializeSettings(config, settings);
		} catch (Exception e) {
			LogUtils.getLogger().error(e.getMessage());
		}
	}

	private	static void deserializeSettings(Properties config, HashSet<Setting<?>> settings) {
		for (Setting setting : settings) {
			try {
				String value = config.getProperty(setting.ID, null);
				if (value == null)
					continue;

				switch (setting.type) {
					case FLOAT -> {
						FloatSetting floatSetting = (FloatSetting) setting;
						floatSetting.setValue(Float.parseFloat(value));
					}
					case INTEGER -> {
						IntegerSetting intSetting = (IntegerSetting) setting;
						intSetting.setValue(Integer.parseInt(value));
					}
					case BOOLEAN -> {
						setting.setValue(Boolean.parseBoolean(value));
					}
					case STRING -> {
						setting.setValue(value);
					}
					case FONT -> {
						FontSetting fontSetting = (FontSetting) setting;
						fontSetting.setFontName(value);
					}
					case KEYBIND -> {
						int keyCode = Integer.parseInt(value);
						setting.setValue(InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
					}
					case RECTANGLE -> {
						String[] dimensions = value.split(",");
						if (dimensions.length == 4) {
							float x = dimensions[0].equals("null") ? 0f : Float.parseFloat(dimensions[0]);
							float y = dimensions[1].equals("null") ? 0f : Float.parseFloat(dimensions[1]);
							float width = dimensions[2].equals("null") ? 0f : Float.parseFloat(dimensions[2]);
							float height = dimensions[3].equals("null") ? 0f : Float.parseFloat(dimensions[3]);

							setting.setValue(new Rectangle(x, y, width, height));
						}
					}
					case COLOR -> {
						String hex = value.replace("#", "");
						Color c = Color.convertHextoRGB(hex);
						setting.setValue(c);
					}
					case SHADER -> {
						String[] splits = value.split(",");
						ShaderSetting sSetting = (ShaderSetting) setting;
						if (splits.length >= 1) {
							sSetting.setShaderId(splits[0]);
							Shader shader = sSetting.getValue();
							if (shader != null && splits.length > 1) {
								float[] vals = shader.uniformValues();
								for (int idx = 0; idx < Math.min(splits.length - 1, vals.length); idx++) {
									try {
										vals[idx] = Float.parseFloat(splits[idx + 1]);
									} catch (NumberFormatException ignored) {}
								}
							}
						}
					}
					case BLOCKS -> {
						String[] ids = value.split(",");
						HashSet<Block> result = new HashSet<Block>();
						for (String str : ids) {
							Identifier i = Identifier.parse(str);
							result.add(BuiltInRegistries.BLOCK.getValue(i));
						}
						setting.setValue(result);
					}
					case ENTITIES -> {
						String[] ids = value.split(",");
						HashSet<EntityType<?>> result = new HashSet<EntityType<?>>();
						for (String str : ids) {
							if (str.isEmpty())
								continue;
							Identifier i = Identifier.parse(str);
							result.add(BuiltInRegistries.ENTITY_TYPE.getValue(i));
						}
						setting.setValue(result);
					}
					case INDEXEDSTRINGLIST, STRINGLIST ->
							throw new UnsupportedOperationException("Unimplemented case: " + setting.type);
					case ENUM -> {
						Enum<?> enumValue = Enum.valueOf((((Enum<?>) setting.getValue()).getDeclaringClass()),
								value);
						setting.setValue(enumValue);
					}
					case VEC3D -> {
						String[] components = value.split(",");
						if (components.length == 3) {
							float x = Float.parseFloat(components[0]);
							float y = Float.parseFloat(components[1]);
							float z = Float.parseFloat(components[2]);
							setting.setValue(new Vec3(x, y, z));
						}
					}
					case RANGE -> {
						String[] components = value.split(",");
						if (components.length == 2) {
							float lo = Float.parseFloat(components[0]);
							float hi = Float.parseFloat(components[1]);
							setting.setValue(new Range(lo, hi));
						}
					}
					case HOTBAR -> {
						List<Boolean> result = Arrays.stream(value.split(","))
								.map(String::trim)
								.map(Boolean::parseBoolean)
								.collect(Collectors.toList());
						setting.setValue(result);
					}
					default -> throw new IllegalArgumentException("Unexpected value: " + setting.type);
				}
			} catch (Exception e) {
				LogUtils.getLogger().error(e.getMessage());
			}
		}
	}
}
