package net.aoba.module.modules.movement;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.managers.EventManager;

class MovementModuleLifecycleTest {
	private AobaClient previousInstance;

	@BeforeEach
	void installAobaInstance() {
		previousInstance = Aoba.getInstance();
		new Aoba().onInitialize();
		Aoba.getInstance().eventManager = new EventManager();
	}

	@AfterEach
	void restoreAobaInstance() throws ReflectiveOperationException {
		Field instanceField = Aoba.class.getDeclaredField("INSTANCE");
		instanceField.setAccessible(true);
		instanceField.set(null, previousInstance);
	}

	@Test
	void stepCanBeToggledWithoutAPlayer() {
		Step step = new Step();

		assertThatCode(() -> {
			step.onEnable();
			step.onDisable();
		}).doesNotThrowAnyException();
	}

	@Test
	void speedCanBeToggledWithoutAPlayer() {
		Speed speed = new Speed();

		assertThatCode(() -> {
			speed.onEnable();
			speed.onDisable();
		}).doesNotThrowAnyException();
	}
}
