package net.aoba.managers.rotation;

import net.minecraft.world.phys.Vec3;

/** Rotation math kept independent from client state for deterministic tests. */
public final class RotationMath {
	private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

	private RotationMath() {
	}

	public static Vec3 calculateViewVector(float pitch, float yaw) {
		double yawRadians = -yaw * DEGREES_TO_RADIANS;
		double pitchRadians = pitch * DEGREES_TO_RADIANS;
		double yawCos = Math.cos(yawRadians);
		double yawSin = Math.sin(yawRadians);
		double pitchCos = Math.cos(pitchRadians);
		double pitchSin = Math.sin(pitchRadians);
		return new Vec3(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
	}
}
