package net.aoba.gui.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class GuiThemesTest {
	@Test
	void catalogHasUniqueIdsAndDisplayNames() {
		assertThat(GuiThemes.themes().stream().map(ThemePreset::id)).doesNotHaveDuplicates();
		assertThat(GuiThemes.themes().stream().map(ThemePreset::displayName)).doesNotHaveDuplicates();
		assertThat(GuiThemes.findById(GuiThemes.DEFAULT_THEME_ID)).contains(GuiThemes.defaultTheme());
	}

	@Test
	void everyThemeDefinesEveryStyleExactlyOnce() {
		for (ThemePreset theme : GuiThemes.themes()) {
			assertThat(theme.styles().keySet()).isEqualTo(GuiThemes.styleKeys());
			assertThat(theme.styles().values()).doesNotContainNull();
			assertThat(theme.roundingRadius()).isBetween(0f, 20f);
			assertThat(theme.lineThickness()).isBetween(0.5f, 4f);
		}
	}

	@Test
	void lookupIsCaseInsensitiveAndRejectsUnknownIds() {
		assertThat(GuiThemes.findById("MIDNIGHT")).isPresent();
		assertThat(GuiThemes.findById("missing-theme")).isEmpty();
		assertThat(GuiThemes.findById(null)).isEmpty();
	}

	@Test
	void catalogAndThemeStylesAreImmutable() {
		ThemePreset theme = GuiThemes.defaultTheme();
		assertThatThrownBy(() -> GuiThemes.themes().add(theme)).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> theme.styles().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void presetDisplayTextIsUsedByComboBoxes() {
		List<String> labels = GuiThemes.themes().stream().map(Object::toString).toList();
		assertThat(labels).containsExactly("Aoba Purple", "Midnight Blue", "Daylight", "High Contrast");
	}
}
