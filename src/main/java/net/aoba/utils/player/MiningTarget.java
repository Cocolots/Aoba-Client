package net.aoba.utils.player;

import java.util.Comparator;

import net.minecraft.core.BlockPos;

public record MiningTarget(BlockPos pos, double distanceSqr, float destroySpeed) {
	public static Comparator<MiningTarget> comparator(MiningPriority priority) {
		return switch (priority) {
			case CLOSEST -> Comparator.comparingDouble(MiningTarget::distanceSqr);
			case FARTHEST -> Comparator.comparingDouble(MiningTarget::distanceSqr).reversed();
			case SOFTEST -> Comparator.comparingDouble(MiningTarget::destroySpeed);
			case HARDEST -> Comparator.comparingDouble(MiningTarget::destroySpeed).reversed();
			case LOWEST -> Comparator.comparingInt((MiningTarget target) -> target.pos().getY());
			case HIGHEST -> Comparator.comparingInt((MiningTarget target) -> target.pos().getY()).reversed();
		};
	}
}
