package net.aoba.managers.rotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

class RotationMathTest {
	@Test
	void zeroRotationLooksAlongPositiveZ() {
		Vec3 view = RotationMath.calculateViewVector(0, 0);

		assertThat(view.x).isCloseTo(0, within(1.0E-6));
		assertThat(view.y).isCloseTo(0, within(1.0E-6));
		assertThat(view.z).isCloseTo(1, within(1.0E-6));
	}

	@Test
	void yawAndPitchAreNotSwapped() {
		Vec3 yawOnly = RotationMath.calculateViewVector(0, 90);
		Vec3 pitchOnly = RotationMath.calculateViewVector(90, 0);

		assertThat(yawOnly.x).isCloseTo(-1, within(1.0E-6));
		assertThat(yawOnly.y).isCloseTo(0, within(1.0E-6));
		assertThat(yawOnly.z).isCloseTo(0, within(1.0E-6));
		assertThat(pitchOnly.x).isCloseTo(0, within(1.0E-6));
		assertThat(pitchOnly.y).isCloseTo(-1, within(1.0E-6));
		assertThat(pitchOnly.z).isCloseTo(0, within(1.0E-6));
	}

	@Test
	void viewVectorIsNormalized() {
		Vec3 view = RotationMath.calculateViewVector(37.5f, -123f);

		assertThat(view.length()).isCloseTo(1, within(1.0E-6));
	}
}
