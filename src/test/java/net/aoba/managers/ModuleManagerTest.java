package net.aoba.managers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.aoba.module.Module;
import net.aoba.settings.types.KeybindSetting;
import net.aoba.utils.input.Keybind;

class ModuleManagerTest {
	@Test
	void releasesMomentaryBindWithoutDependingOnCurrentScreen() {
		Module module = moduleWithBind(65, true);

		ModuleManager.releaseMomentaryModules(List.of(module), 65);

		verify(module).disable();
	}

	@Test
	void ignoresToggleBindsAndOtherKeys() {
		Module toggleModule = moduleWithBind(65, false);
		Module otherKeyModule = moduleWithBind(66, true);

		ModuleManager.releaseMomentaryModules(List.of(toggleModule, otherKeyModule), 65);

		verify(toggleModule, never()).disable();
		verify(otherKeyModule, never()).disable();
	}

	private static Module moduleWithBind(int keyCode, boolean momentary) {
		Module module = mock(Module.class);
		KeybindSetting setting = mock(KeybindSetting.class);
		Keybind binding = mock(Keybind.class);
		when(module.getBind()).thenReturn(setting);
		when(setting.getValue()).thenReturn(binding);
		when(binding.getKeyCode()).thenReturn(keyCode);
		when(binding.getIsMomentary()).thenReturn(momentary);
		return module;
	}
}
