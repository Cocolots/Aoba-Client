package net.aoba.module.modules.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class SurroundTest {
	@Test
	void planStartsAtFeetAndUsesDeterministicCardinalOrder() {
		BlockPos center = new BlockPos(10, 64, -4);

		List<BlockPos> plan = SurroundPlanner.planPositions(center, 1);

		assertThat(plan).containsExactly(center.north(), center.east(), center.south(), center.west());
	}

	@Test
	void planBuildsEveryLayerWithoutDuplicates() {
		BlockPos center = BlockPos.ZERO;

		List<BlockPos> plan = SurroundPlanner.planPositions(center, 3);

		assertThat(plan).hasSize(12);
		assertThat(plan).doesNotHaveDuplicates();
		assertThat(plan.subList(4, 8)).allMatch(pos -> pos.getY() == 1);
		assertThat(plan.subList(8, 12)).allMatch(pos -> pos.getY() == 2);
	}

	@Test
	void invalidPlanHasNoWork() {
		assertThat(SurroundPlanner.planPositions(BlockPos.ZERO, 0)).isEmpty();
		assertThat(SurroundPlanner.planPositions(null, 1)).isEmpty();
	}
}
