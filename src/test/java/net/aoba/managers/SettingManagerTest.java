package net.aoba.managers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.aoba.settings.Setting;
import net.aoba.settings.types.BlocksSetting;
import net.aoba.settings.types.EntitiesSetting;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

class SettingManagerTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void emptyBlockSetRoundTripClearsPreviousProfileValues() throws ReflectiveOperationException {
		BlocksSetting source = blocksSetting(new HashSet<>());
		Properties properties = serialize(source);
		BlocksSetting target = blocksSetting(new HashSet<>(Set.of(Blocks.STONE)));

		deserialize(properties, target);

		assertThat(properties.getProperty("test_blocks")).isEmpty();
		assertThat(target.getValue()).isEmpty();
	}

	@Test
	void nonEmptyBlockSetRoundTripPreservesValues() throws ReflectiveOperationException {
		BlocksSetting source = blocksSetting(new HashSet<>(Set.of(Blocks.STONE, Blocks.DIRT)));
		Properties properties = serialize(source);
		BlocksSetting target = blocksSetting(new HashSet<>());

		deserialize(properties, target);

		assertThat(target.getValue()).containsExactlyInAnyOrder(Blocks.STONE, Blocks.DIRT);
	}

	@Test
	void configFilesCannotEscapeSettingsDirectory(@TempDir Path tempDir) throws IOException {
		File settingsFolder = tempDir.resolve("settings").toFile();
		File validConfig = SettingManager.getConfigFile(settingsFolder, "valid profile");

		assertThat(validConfig.getCanonicalPath())
				.isEqualTo(settingsFolder.getCanonicalFile().toPath().resolve("valid profile.xml").toString());
		assertThatThrownBy(() -> SettingManager.getConfigFile(settingsFolder, "../outside"))
				.isInstanceOf(IOException.class);
		assertThatThrownBy(() -> SettingManager.getConfigFile(settingsFolder,
				tempDir.resolve("outside").toString())).isInstanceOf(IOException.class);
	}

	@Test
	void unknownBlockIdDoesNotReplaceConfiguredValueWithAir() throws ReflectiveOperationException {
		Properties properties = new Properties();
		properties.setProperty("test_blocks", "aoba:missing");
		BlocksSetting target = blocksSetting(new HashSet<>(Set.of(Blocks.STONE)));

		deserialize(properties, target);

		assertThat(target.getValue()).containsExactly(Blocks.STONE);
	}

	@Test
	void unknownEntityIdDoesNotReplaceConfiguredValue() throws ReflectiveOperationException {
		Properties properties = new Properties();
		properties.setProperty("test_entities", "aoba:missing");
		EntitiesSetting target = EntitiesSetting.builder().id("test_entities")
				.defaultValue(Set.of(EntityTypes.PLAYER)).build();

		deserialize(properties, target);

		assertThat(target.getValue()).containsExactly(EntityTypes.PLAYER);
	}

	private static BlocksSetting blocksSetting(HashSet<Block> value) {
		return BlocksSetting.builder().id("test_blocks").defaultValue(value).build();
	}

	private static Properties serialize(Setting<?> setting) throws ReflectiveOperationException {
		HashSet<Setting<?>> settings = new HashSet<>();
		settings.add(setting);
		Properties properties = new Properties();
		Method method = SettingManager.class.getDeclaredMethod("fillProperties", Properties.class, HashSet.class);
		method.setAccessible(true);
		method.invoke(null, properties, settings);
		return properties;
	}

	private static void deserialize(Properties properties, Setting<?> setting) throws ReflectiveOperationException {
		HashSet<Setting<?>> settings = new HashSet<>();
		settings.add(setting);
		Method method = SettingManager.class.getDeclaredMethod("deserializeSettings", Properties.class, HashSet.class);
		method.setAccessible(true);
		method.invoke(null, properties, settings);
	}
}
