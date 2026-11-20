package net.aoba.module.modules.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Pure placement-position calculations shared by Scaffold and its tests. */
final class ScaffoldPlanner {
	private ScaffoldPlanner() {
	}

	static Vec3 predictPosition(Vec3 position, Vec3 motion, int ticks) {
		if (position == null) {
			throw new IllegalArgumentException("position is required");
		}
		if (motion == null || ticks <= 0) {
			return position;
		}
		int clampedTicks = Math.min(ticks, 10);
		return position.add(motion.x * clampedTicks, 0, motion.z * clampedTicks);
	}

	static int resolvePlacementY(BlockPos playerPosition, int floorY, ScaffoldHeightMode mode, boolean tower,
			boolean jumpDown, boolean descendDown, double verticalMotion) {
		if (playerPosition == null) {
			throw new IllegalArgumentException("playerPosition is required");
		}
		int belowPlayer = playerPosition.getY() - 1;
		if (descendDown) {
			return playerPosition.getY() - 2;
		}
		if (jumpDown && (tower || mode == ScaffoldHeightMode.JUMP_KEY)) {
			return belowPlayer;
		}
		return switch (mode) {
		case OFF -> belowPlayer;
		case ON, JUMP_KEY -> floorY;
		case FALLING -> verticalMotion < 0.2 ? floorY : belowPlayer;
		};
	}

	static List<BlockPos> planCandidates(Vec3 origin, int y, double radius) {
		List<BlockPos> result = new ArrayList<>();
		if (origin == null || !Double.isFinite(radius) || radius < 0) {
			return result;
		}

		BlockPos center = BlockPos.containing(origin);
		int extent = (int) Math.ceil(radius);
		double radiusSqr = radius * radius;
		List<ScoredPosition> candidates = new ArrayList<>();
		for (int x = -extent; x <= extent; x++) {
			for (int z = -extent; z <= extent; z++) {
				BlockPos pos = center.offset(x, y - center.getY(), z);
				double dx = pos.getX() + 0.5 - origin.x;
				double dz = pos.getZ() + 0.5 - origin.z;
				double distanceSqr = dx * dx + dz * dz;
				if (distanceSqr <= radiusSqr) {
					candidates.add(new ScoredPosition(pos, distanceSqr));
				}
			}
		}

		candidates.sort(Comparator.comparingDouble(ScoredPosition::distanceSqr).thenComparingInt(
				candidate -> candidate.pos().getX()).thenComparingInt(candidate -> candidate.pos().getZ()));
		for (ScoredPosition candidate : candidates) {
			result.add(candidate.pos());
		}
		return result;
	}

	private record ScoredPosition(BlockPos pos, double distanceSqr) {
	}
}
