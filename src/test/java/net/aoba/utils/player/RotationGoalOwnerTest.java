package net.aoba.utils.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.aoba.managers.rotation.goals.Goal;

class RotationGoalOwnerTest {
	private Goal<?> activeGoal;
	private RotationGoalOwner owner;

	@BeforeEach
	void setUp() {
		RotationGoalOwner.GoalAccess access = new RotationGoalOwner.GoalAccess() {
			@Override
			public Goal<?> getGoal() {
				return activeGoal;
			}

			@Override
			public void setGoal(Goal<?> goal) {
				activeGoal = goal;
			}
		};
		owner = new RotationGoalOwner(access);
	}

	@Test
	void ownedGoalIsActiveAndCanBeCleared() {
		Goal<?> goal = mock(Goal.class);

		owner.set(goal);

		assertThat(owner.isActive()).isTrue();
		assertThat(activeGoal).isSameAs(goal);

		owner.clear();

		assertThat(activeGoal).isNull();
		assertThat(owner.getOwnedGoal()).isNull();
		assertThat(owner.isActive()).isFalse();
	}

	@Test
	void clearDoesNotRemoveGoalInstalledByAnotherModule() {
		Goal<?> owned = mock(Goal.class);
		Goal<?> external = mock(Goal.class);
		owner.set(owned);
		activeGoal = external;

		owner.clear();

		assertThat(activeGoal).isSameAs(external);
		assertThat(owner.getOwnedGoal()).isNull();
	}

	@Test
	void externalGoalMakesPreviousOwnershipInactive() {
		Goal<?> owned = mock(Goal.class);
		owner.set(owned);
		activeGoal = mock(Goal.class);

		assertThat(owner.isActive()).isFalse();
		assertThat(owner.getOwnedGoal()).isSameAs(owned);
	}
}
