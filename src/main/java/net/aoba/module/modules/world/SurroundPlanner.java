package net.aoba.module.modules.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

/** Deterministic surround layout independent from Minecraft registries. */
final class SurroundPlanner {
	private SurroundPlanner() {
	}

	static List<BlockPos> planPositions(BlockPos center, int height) {
		List<BlockPos> positions = new ArrayList<>();
		if (center == null || height <= 0) {
			return positions;
		}

		for (int y = 0; y < height; y++) {
			BlockPos column = center.above(y);
			positions.add(column.north());
			positions.add(column.east());
			positions.add(column.south());
			positions.add(column.west());
		}
		return positions;
	}
}
