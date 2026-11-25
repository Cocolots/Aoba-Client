package net.aoba.module.modules.misc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.aoba.module.modules.misc.AutoFishPlanner.Action;

class AutoFishPlannerTest {
	@Test
	void splashReelsOnceAndRecastsAfterDelay() {
		AutoFishPlanner planner = new AutoFishPlanner();

		assertThat(planner.onSplash(1000, 300)).isEqualTo(Action.REEL);
		assertThat(planner.onTick(1100)).isEqualTo(Action.NONE);
		assertThat(planner.onTick(1299)).isEqualTo(Action.NONE);
		assertThat(planner.onTick(1300)).isEqualTo(Action.RECAST);
		assertThat(planner.onTick(1301)).isEqualTo(Action.NONE);
	}

	@Test
	void duplicateSplashesDoNotRestartPendingRecast() {
		AutoFishPlanner planner = new AutoFishPlanner();
		planner.onSplash(1000, 300);

		assertThat(planner.onSplash(1050, 300)).isEqualTo(Action.NONE);
		assertThat(planner.onTick(1300)).isEqualTo(Action.RECAST);
	}

	@Test
	void splashBurstIsSuppressed() {
		AutoFishPlanner planner = new AutoFishPlanner();
		planner.onSplash(1000, 0);
		planner.onTick(1000);

		assertThat(planner.onSplash(1100, 0)).isEqualTo(Action.NONE);
		assertThat(planner.onSplash(1300, 0)).isEqualTo(Action.REEL);
	}

	@Test
	void initialAutoCastDoesNotReplacePendingAction() {
		AutoFishPlanner planner = new AutoFishPlanner();
		planner.scheduleRecast(1000, 200);
		planner.scheduleRecast(1000, 500);

		assertThat(planner.onTick(1199)).isEqualTo(Action.NONE);
		assertThat(planner.onTick(1200)).isEqualTo(Action.RECAST);
	}

	@Test
	void resetCancelsPendingWork() {
		AutoFishPlanner planner = new AutoFishPlanner();
		planner.onSplash(1000, 200);

		planner.reset();

		assertThat(planner.isWaitingForRecast()).isFalse();
		assertThat(planner.onTick(5000)).isEqualTo(Action.NONE);
	}
}
