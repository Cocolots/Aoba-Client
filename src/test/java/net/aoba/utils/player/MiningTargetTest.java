package net.aoba.utils.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class MiningTargetTest {
	private final MiningTarget nearSoftLow = new MiningTarget(new BlockPos(0, 1, 0), 4, 0.2f);
	private final MiningTarget farHardHigh = new MiningTarget(new BlockPos(0, 10, 0), 25, 5f);

	@Test
	void sortsEveryMiningPriority() {
		assertThat(sorted(MiningPriority.CLOSEST)).containsExactly(nearSoftLow, farHardHigh);
		assertThat(sorted(MiningPriority.FARTHEST)).containsExactly(farHardHigh, nearSoftLow);
		assertThat(sorted(MiningPriority.SOFTEST)).containsExactly(nearSoftLow, farHardHigh);
		assertThat(sorted(MiningPriority.HARDEST)).containsExactly(farHardHigh, nearSoftLow);
		assertThat(sorted(MiningPriority.LOWEST)).containsExactly(nearSoftLow, farHardHigh);
		assertThat(sorted(MiningPriority.HIGHEST)).containsExactly(farHardHigh, nearSoftLow);
	}

	private List<MiningTarget> sorted(MiningPriority priority) {
		return List.of(farHardHigh, nearSoftLow).stream().sorted(MiningTarget.comparator(priority)).toList();
	}
}
