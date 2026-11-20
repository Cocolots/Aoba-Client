package net.aoba.module.modules.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

class ScaffoldPlannerTest {
	@Test
	void predictionUsesOnlyHorizontalMotionAndClampsInput() {
		Vec3 position = new Vec3(2, 64, -3);
		Vec3 motion = new Vec3(0.2, -0.08, -0.1);

		Vec3 predicted = ScaffoldPlanner.predictPosition(position, motion, 2);
		Vec3 clamped = ScaffoldPlanner.predictPosition(position, motion, 100);

		assertThat(predicted.x).isCloseTo(2.4, within(1.0E-9));
		assertThat(predicted.y).isEqualTo(64);
		assertThat(predicted.z).isCloseTo(-3.2, within(1.0E-9));
		assertThat(clamped.x).isCloseTo(4.0, within(1.0E-9));
	}

	@Test
	void disabledPredictionKeepsOriginalPosition() {
		Vec3 position = new Vec3(2, 64, -3);

		assertThat(ScaffoldPlanner.predictPosition(position, Vec3.ZERO, 0)).isSameAs(position);
		assertThat(ScaffoldPlanner.predictPosition(position, null, 3)).isSameAs(position);
	}

	@Test
	void normalModeTargetsBlockBelowPlayer() {
		BlockPos player = new BlockPos(0, 65, 0);

		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.OFF, false, false, false, 0))
				.isEqualTo(64);
	}

	@Test
	void sameYRetainsFloorUntilTowerJump() {
		BlockPos player = new BlockPos(0, 67, 0);

		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.ON, true, false, false, 0))
				.isEqualTo(63);
		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.ON, true, true, false, 0))
				.isEqualTo(66);
	}

	@Test
	void jumpKeyModeCanRaiseFloorWithoutTowerSetting() {
		BlockPos player = new BlockPos(0, 67, 0);

		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.JUMP_KEY, false, true, false, 0))
				.isEqualTo(66);
	}

	@Test
	void fallingModeKeepsHeightOnlyNearApex() {
		BlockPos player = new BlockPos(0, 67, 0);

		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.FALLING, false, false, false, 0.1))
				.isEqualTo(63);
		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.FALLING, false, false, false, 0.3))
				.isEqualTo(66);
	}

	@Test
	void descendOverridesEveryHeightMode() {
		BlockPos player = new BlockPos(0, 67, 0);

		assertThat(ScaffoldPlanner.resolvePlacementY(player, 63, ScaffoldHeightMode.ON, true, true, true, 0))
				.isEqualTo(65);
	}

	@Test
	void candidateSearchIsSymmetricAndSortedByDistance() {
		Vec3 origin = new Vec3(0.5, 64, 0.5);

		List<BlockPos> plan = ScaffoldPlanner.planCandidates(origin, 63, 1.5);

		assertThat(plan).startsWith(new BlockPos(0, 63, 0));
		assertThat(plan).contains(new BlockPos(1, 63, 0), new BlockPos(-1, 63, 0),
				new BlockPos(0, 63, 1), new BlockPos(0, 63, -1));
		assertThat(plan).doesNotHaveDuplicates();
		assertThat(plan).allMatch(pos -> pos.getY() == 63);
	}

	@Test
	void invalidCandidateSearchHasNoPositions() {
		assertThat(ScaffoldPlanner.planCandidates(null, 63, 4)).isEmpty();
		assertThat(ScaffoldPlanner.planCandidates(Vec3.ZERO, 63, -1)).isEmpty();
		assertThat(ScaffoldPlanner.planCandidates(Vec3.ZERO, 63, Double.NaN)).isEmpty();
	}
}
