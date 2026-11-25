package net.aoba.module.modules.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FocusFpsPolicyTest {
	@Test
	void focusedWindowKeepsCurrentLimit() {
		assertThat(FocusFpsPolicy.resolveLimit(240, 30, true, true)).isEqualTo(240);
	}

	@Test
	void disabledModuleKeepsCurrentLimit() {
		assertThat(FocusFpsPolicy.resolveLimit(240, 30, false, false)).isEqualTo(240);
	}

	@Test
	void unfocusedWindowUsesConfiguredLimit() {
		assertThat(FocusFpsPolicy.resolveLimit(240, 30, true, false)).isEqualTo(30);
	}

	@Test
	void vanillaLowerLimitIsNotIncreased() {
		assertThat(FocusFpsPolicy.resolveLimit(10, 30, true, false)).isEqualTo(10);
	}

	@Test
	void configuredLimitIsAlwaysPositive() {
		assertThat(FocusFpsPolicy.resolveLimit(240, 0, true, false)).isEqualTo(1);
	}
}
