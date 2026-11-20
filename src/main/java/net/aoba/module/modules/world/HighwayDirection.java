package net.aoba.module.modules.world;

import net.minecraft.core.Direction;

public enum HighwayDirection {
	NORTH(Direction.NORTH, 180f),
	SOUTH(Direction.SOUTH, 0f),
	EAST(Direction.EAST, -90f),
	WEST(Direction.WEST, 90f);

	private final Direction direction;
	private final float yaw;

	HighwayDirection(Direction direction, float yaw) {
		this.direction = direction;
		this.yaw = yaw;
	}

	public Direction direction() {
		return direction;
	}

	public float yaw() {
		return yaw;
	}
}
