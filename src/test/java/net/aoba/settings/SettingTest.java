package net.aoba.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.aoba.settings.types.StringSetting;

class SettingTest {
	@Test
	void stringSettingPreservesDisplayName() {
		StringSetting setting = StringSetting.builder().id("selected_config").displayName("Selected profile")
				.description("Active profile").defaultValue("default").build();

		assertThat(setting.displayName).isEqualTo("Selected profile");
		assertThat(setting.description).isEqualTo("Active profile");
	}

	@Test
	void invalidValueDoesNotNotifyListeners() {
		AtomicInteger updates = new AtomicInteger();
		IntegerSetting setting = IntegerSetting.builder().id("test_integer").defaultValue(5).minValue(1)
				.maxValue(10).onUpdate(value -> updates.incrementAndGet()).build();

		setting.setValue(11);

		assertThat(setting.getValue()).isEqualTo(5);
		assertThat(updates).hasValue(0);
	}

	@Test
	void resetToDefaultNotifiesOnlyOnce() {
		AtomicInteger updates = new AtomicInteger();
		IntegerSetting setting = IntegerSetting.builder().id("test_integer").defaultValue(5).minValue(1)
				.maxValue(10).onUpdate(value -> updates.incrementAndGet()).build();
		setting.setValue(7);
		updates.set(0);

		setting.resetToDefault();

		assertThat(setting.getValue()).isEqualTo(5);
		assertThat(updates).hasValue(1);
	}

	@Test
	void floatStepsAreRelativeToMinimumAndStayWithinRange() {
		FloatSetting setting = FloatSetting.builder().id("test_float").defaultValue(0.3f).minValue(0.1f)
				.maxValue(0.5f).step(0.2f).build();

		setting.setValue(0.31f);
		assertThat(setting.getValue()).isEqualTo(0.3f);
		assertThat(setting.getValueSqr()).isEqualTo(0.3f * 0.3f);

		setting.setValue(0.49f);
		assertThat(setting.getValue()).isEqualTo(0.5f);

		setting.setValue(-10f);
		assertThat(setting.getValue()).isEqualTo(0.1f);

		setting.setValue(10f);
		assertThat(setting.getValue()).isEqualTo(0.5f);
	}
}
