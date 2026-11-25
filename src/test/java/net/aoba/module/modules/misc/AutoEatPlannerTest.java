package net.aoba.module.modules.misc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AutoEatPlannerTest {
	@Test
	void startsForEitherHungerOrHealthThreshold() {
		assertThat(AutoEatPlanner.shouldStart(20, 20, 10, 10)).isFalse();
		assertThat(AutoEatPlanner.shouldStart(10, 20, 10, 10)).isTrue();
		assertThat(AutoEatPlanner.shouldStart(20, 8, 10, 10)).isTrue();
	}

	@Test
	void fillToFullDoesNotStopAtInitialThreshold() {
		assertThat(AutoEatPlanner.shouldContinue(19, 10, true)).isTrue();
		assertThat(AutoEatPlanner.shouldContinue(20, 10, true)).isFalse();
		assertThat(AutoEatPlanner.shouldContinue(11, 10, false)).isFalse();
	}

	@Test
	void applePriorityIsDeterministic() {
		assertThat(AutoEatPlanner.foodScore(4, false, false, true)).isLessThan(
				AutoEatPlanner.foodScore(4, true, false, true));
		assertThat(AutoEatPlanner.foodScore(4, true, false, true)).isLessThan(
				AutoEatPlanner.foodScore(4, false, true, true));
		assertThat(AutoEatPlanner.foodScore(8, false, false, false)).isGreaterThan(
				AutoEatPlanner.foodScore(4, true, false, false));
	}
}
