package net.aoba.module.modules.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class HighwayDirectionTest {
	@Test
	void mapsEveryCardinalDirectionToItsVectorAndMinecraftYaw() {
		assertDirection(HighwayDirection.NORTH, Direction.NORTH, 180f);
		assertDirection(HighwayDirection.SOUTH, Direction.SOUTH, 0f);
		assertDirection(HighwayDirection.EAST, Direction.EAST, -90f);
		assertDirection(HighwayDirection.WEST, Direction.WEST, 90f);
	}

	@Test
	void widthRangesContainTheConfiguredNumberOfLanes() {
		for (int width = 1; width <= 3; width++) {
			int min = Highway.minLateral(width);
			int max = Highway.maxLateral(width);
			assertThat(max - min + 1).isEqualTo(width);
		}
		assertThat(Highway.minLateral(2)).isEqualTo(-1);
		assertThat(Highway.maxLateral(2)).isZero();
	}

	private static void assertDirection(HighwayDirection value, Direction direction, float yaw) {
		assertThat(value.direction()).isEqualTo(direction);
		assertThat(value.yaw()).isEqualTo(yaw);
	}
}
