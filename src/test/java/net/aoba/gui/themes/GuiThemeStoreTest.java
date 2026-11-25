package net.aoba.gui.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiThemeStoreTest {
	@TempDir
	Path tempDirectory;

	private GuiThemeStore store;

	@BeforeEach
	void setUp() throws IOException {
		store = new GuiThemeStore(tempDirectory.resolve("themes"));
		store.copyBuiltinThemes();
	}

	@Test
	void copiesPresetsWithoutOverwritingUserChanges() throws IOException {
		Path preset = tempDirectory.resolve("themes/aoba_purple.xml");
		String customized = Files.readString(preset).replace(">Aoba Purple<", ">Custom Purple<");
		Files.writeString(preset, customized);

		store.copyBuiltinThemes();

		assertThat(store.load(GuiThemes.DEFAULT_THEME_ID).displayName()).isEqualTo("Custom Purple");
		assertThat(tempDirectory.resolve("themes/midnight.xml")).exists();
		assertThat(tempDirectory.resolve("themes/light.xml")).exists();
		assertThat(tempDirectory.resolve("themes/high_contrast.xml")).exists();
	}

	@Test
	void roundTripsUserThemeUsingExistingSettingFormat() throws IOException {
		ThemePreset preset = store.load(GuiThemes.DEFAULT_THEME_ID);
		ThemePreset custom = new ThemePreset("my_theme", "My Theme", preset.styles(), 6.5f, 1.25f);

		store.save(custom);

		assertThat(store.load("my_theme")).isEqualTo(custom);
		assertThat(store.loadAll()).contains(custom);
	}

	@Test
	void skipsMalformedThemeFilesWithoutHidingValidThemes() throws IOException {
		Files.writeString(tempDirectory.resolve("themes/broken.xml"), "not valid XML");

		assertThat(store.loadAll()).hasSize(4);
		assertThat(store.load(GuiThemes.DEFAULT_THEME_ID)).isNotNull();
	}

	@Test
	void rejectsUnsafeThemeIdentifiers() {
		assertThatThrownBy(() -> store.load("../outside")).isInstanceOf(IllegalArgumentException.class);
	}
}
