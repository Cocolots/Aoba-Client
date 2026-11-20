package net.aoba.utils.input;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

import net.aoba.settings.types.KeybindSetting;

class KeybindTest {
	@Test
	void equalityIncludesKeyAndMomentaryMode() {
		Keybind toggle = Keybind.from(InputConstants.KEY_A, false);
		Keybind sameToggle = Keybind.from(InputConstants.KEY_A, false);
		Keybind hold = Keybind.from(InputConstants.KEY_A, true);

		assertThat(toggle).isEqualTo(sameToggle);
		assertThat(toggle).hasSameHashCodeAs(sameToggle);
		assertThat(toggle).isNotEqualTo(hold);
		assertThat(toggle).isNotEqualTo(Keybind.from(InputConstants.KEY_B, false));
	}

	@Test
	void integerFactoryPreservesKeyCode() {
		Keybind bind = Keybind.from(InputConstants.KEY_RSHIFT, true);

		assertThat(bind.getKeyCode()).isEqualTo(InputConstants.KEY_RSHIFT);
		assertThat(bind.getIsMomentary()).isTrue();
	}

	@Test
	void settingRejectsNullKeybinds() {
		Keybind initial = Keybind.from(InputConstants.KEY_A);
		KeybindSetting setting = KeybindSetting.builder().id("test_bind").defaultValue(initial).build();

		setting.setValue(null);

		assertThat(setting.getValue()).isSameAs(initial);
	}

	@Test
	void builderMomentaryFlagIsNotIgnored() {
		KeybindSetting setting = KeybindSetting.builder().id("test_bind")
				.defaultValue(Keybind.from(InputConstants.KEY_A)).setMomentary(true).build();

		assertThat(setting.getValue().getIsMomentary()).isTrue();
		assertThat(setting.getValue().getKeyCode()).isEqualTo(InputConstants.KEY_A);
	}
}
