/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.rotation;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.events.SendMovementPacketEvent;
import net.aoba.event.events.SendPacketEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.event.listeners.SendMovementPacketListener;
import net.aoba.event.listeners.SendPacketListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.goals.EasingFunction;
import net.aoba.managers.rotation.goals.Goal;
import net.aoba.mixin.interfaces.ILocalPlayer;
import net.aoba.mixin.interfaces.IServerboundUseItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class RotationManager implements TickListener, Render3DListener, SendPacketListener, SendMovementPacketListener {
	private static final Minecraft MC = Minecraft.getInstance();
	private static final AobaClient AOBA = Aoba.getInstance();

	private Goal<?> currentGoal = null;
	private Rotation currentGoalStartingRotation;
	private double currentRotationGoalProgress = 0;
	private Rotation lastAppliedRotation;

	private Float serverYaw = null;
	private Float serverPitch = null;

	public RotationManager() {
		AOBA.eventManager.AddListener(TickListener.class, this);
		AOBA.eventManager.AddListener(Render3DListener.class, this);
		AOBA.eventManager.AddListener(SendPacketListener.class, this);
		AOBA.eventManager.AddListener(SendMovementPacketListener.class, this);
	}

	public static float getGCD() {
		double f = MC.options.sensitivity().get() * 0.6F + 0.2F;
		return (float) (f * f * f * 8.0) * 0.15F;
	}

	public Goal<?> getGoal() {
		return currentGoal;
	}

	public void setGoal(Goal<?> goal) {
		if (goal != null && goal.equals(currentGoal)) {
			currentGoal = goal;
			return;
		}

		boolean isCurrentGoalFake = currentGoal != null && currentGoal.isFakeRotation() && currentGoal.getRotationMode() != RotationMode.NONE;
		boolean isNextGoalFake = goal != null && goal.isFakeRotation() && goal.getRotationMode() != RotationMode.NONE;
		if (isCurrentGoalFake && !isNextGoalFake && MC.player != null && serverYaw != null && serverPitch != null) {
			Rotation aligned = new Rotation(serverYaw, serverPitch).stepByGCD(MC.player.getYRot() - serverYaw, MC.player.getXRot() - serverPitch);
			MC.player.setYRot((float) aligned.yaw());
			MC.player.setXRot((float) aligned.pitch());
		}

		currentGoal = goal;
		if (goal != null) {
			if (MC.player != null) {
				float startYaw = goal.isFakeRotation() && serverYaw != null ? serverYaw : MC.player.getYRot();
				float startPitch = goal.isFakeRotation() && serverPitch != null ? serverPitch : MC.player.getXRot();
				currentGoalStartingRotation = new Rotation(startYaw, startPitch);
			}else
				currentGoalStartingRotation = goal.getGoalRotation(1);
		} else {
			currentGoalStartingRotation = null;
			lastAppliedRotation = null;
		}
		currentRotationGoalProgress = 0;
	}
 
	public Float getServerYaw() {
		return serverYaw;
	}

	public Float getServerPitch() {
		return serverPitch;
	}

	@Override
	public void onTick(Pre event) {

	}

	@Override
	public void onTick(Post event) {

	}

	@Override
	public void onRender(Render3DEvent event) {
		if (MC.player == null)
			return;

		RotationMode mode = currentGoal == null ? RotationMode.NONE : currentGoal.getRotationMode();

		if (mode == RotationMode.NONE || !currentGoal.isFakeRotation() || serverYaw == null || serverPitch == null) {
			serverYaw = MC.player.getYRot();
			serverPitch = MC.player.getXRot();
		}

		if (mode == RotationMode.NONE)
			return;

		float partialTick = event.getRenderer().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float frameDelta = event.getRenderer().getDeltaTracker().getRealtimeDeltaTicks();

		float startYaw = currentGoal.isFakeRotation() ? serverYaw : MC.player.getYRot();
		float startPitch = currentGoal.isFakeRotation() ? serverPitch : MC.player.getXRot();

		Rotation finalRotation = getRotationFromGoal(startYaw, startPitch, partialTick, frameDelta);

		if (currentGoal.isFakeRotation()) {
			serverYaw = (float) finalRotation.yaw();
			serverPitch = (float) finalRotation.pitch();
			lastAppliedRotation = new Rotation(serverYaw, serverPitch);
		} else {
			// Stop goal if it is finished.
			if(currentRotationGoalProgress < 1.0) {
				MC.player.setYRot((float) finalRotation.yaw());
				MC.player.setXRot((float) finalRotation.pitch());
				lastAppliedRotation = new Rotation(MC.player.getYRot(), MC.player.getXRot());
			}else
				setGoal(null);
		}
	}

	private Rotation getRotationFromGoal(float startYaw, float startPitch, float partialTick, float frameDelta) {
		RotationMode mode = currentGoal.getRotationMode();

		Rotation playerRotation = new Rotation(startYaw, startPitch);
		Rotation goal = currentGoal.getGoalRotation(partialTick);
		
		// Add jitter
		double yawJitter = Math.random() * 2.0 - 1.0;
		double pitchJitter = Math.random() * 2.0 - 1.0;
		goal = new Rotation(goal.yaw() + yawJitter * currentGoal.getYawRandomness(),
				goal.pitch() + pitchJitter * currentGoal.getPitchRandomness());

		if (mode == RotationMode.NONE)
			return null;
		else if (mode == RotationMode.INSTANT)
			return playerRotation.stepByGCD(Mth.wrapDegrees(goal.yaw() - playerRotation.yaw()), goal.pitch() - playerRotation.pitch());
		else {
			// Set the start goal if the rotation has changed from the last applied rotation.
			if (!playerRotation.equals(lastAppliedRotation)) {
				currentGoalStartingRotation = playerRotation;
				currentRotationGoalProgress = 0.0;
			}

			// Calculate the progress from the start to the goal using the easing function slope and remaining distance.
			double distance = Rotation.difference(currentGoalStartingRotation, goal).magnitude();
			if (distance > 0) {
				double peakSlope = currentGoal.getEasingFunction().getPeakSlope();
				double rate = currentGoal.getMaxRotation() / (distance * peakSlope);
				currentRotationGoalProgress = Math.min(1.0, currentRotationGoalProgress + rate * frameDelta);
			} else {
				currentRotationGoalProgress = 1.0;
			}

			// Calculate the current easing function value and apply it to the rotation.
			double easingValue = EasingFunction.ease(currentGoal.getEasingFunction(), currentRotationGoalProgress);
			double lerpDeltaYaw = Mth.wrapDegrees(goal.yaw() - currentGoalStartingRotation.yaw());
			double lerpDeltaPitch = goal.pitch() - currentGoalStartingRotation.pitch();
			Rotation target = new Rotation(currentGoalStartingRotation.yaw() + lerpDeltaYaw * easingValue,
					currentGoalStartingRotation.pitch() + lerpDeltaPitch * easingValue);

			double maxStep = currentGoal.getMaxRotation() * frameDelta;
			double deltaYaw = Mth.wrapDegrees(target.yaw() - playerRotation.yaw());
			double deltaPitch = target.pitch() - playerRotation.pitch();

			double magnitude = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
			if (magnitude > maxStep && magnitude > 0) {
				double scale = maxStep / magnitude;
				deltaYaw *= scale;
				deltaPitch *= scale;
			}

			return playerRotation.stepByGCD(deltaYaw, deltaPitch);
		}
	}

	@Override
	public void onSendPacket(SendPacketEvent event) {
		if (currentGoal == null || !currentGoal.isFakeRotation())
			return;

		if (currentGoal.getRotationMode() == RotationMode.NONE)
			return;

		if (serverYaw == null || serverPitch == null)
			return;

		if (event.GetPacket() instanceof ServerboundUseItemPacket packet && 
				(packet.yRot() != serverYaw || packet.xRot() != serverPitch)) {
			event.SetPacket(new ServerboundUseItemPacket(packet.hand(), packet.sequence(), serverYaw, serverPitch));
		}
	}

	@Override
	public void onSendMovementPacket(SendMovementPacketEvent.Pre event) {
		if (currentGoal == null || !currentGoal.isFakeRotation())
			return;

		if (currentGoal.getRotationMode() == RotationMode.NONE)
			return;

		// Fabricate our own packet.
		event.cancel();

		ILocalPlayer iPlayer = (ILocalPlayer) MC.player;

		double d = MC.player.getX() - iPlayer.getXLast();
		double e = MC.player.getY() - iPlayer.getYLast();
		double f = MC.player.getZ() - iPlayer.getZLast();
		double g = serverYaw - iPlayer.getYRotLast();
		double h = serverPitch - iPlayer.getXRotLast();
		iPlayer.setTicksSinceLastPositionPacketSent(iPlayer.getTicksSinceLastPositionPacketSent() + 1);

		boolean bl = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4)
				|| iPlayer.getTicksSinceLastPositionPacketSent() >= 20;
		boolean bl2 = g != 0.0 || h != 0.0;

		if (bl && bl2) {
			MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(MC.player.getX(), MC.player.getY(),
					MC.player.getZ(), serverYaw, serverPitch, MC.player.onGround(), MC.player.horizontalCollision));
		} else if (bl) {
			MC.getConnection().send(new ServerboundMovePlayerPacket.Pos(MC.player.getX(), MC.player.getY(),
					MC.player.getZ(), MC.player.onGround(), MC.player.horizontalCollision));
		} else if (bl2) {
			MC.getConnection().send(new ServerboundMovePlayerPacket.Rot(serverYaw, serverPitch, MC.player.onGround(),
					MC.player.horizontalCollision));
		} else if (iPlayer.getLastOnGround() != MC.player.onGround()
				|| iPlayer.getLastHorizontalCollision() != MC.player.horizontalCollision) {
			MC.getConnection().send(
					new ServerboundMovePlayerPacket.StatusOnly(MC.player.onGround(), MC.player.horizontalCollision));
		}

		if (bl) {
			iPlayer.setXLast(MC.player.getX());
			iPlayer.setYLast(MC.player.getY());
			iPlayer.setZLast(MC.player.getZ());
			iPlayer.setTicksSinceLastPositionPacketSent(0);
		}

		if (bl2) {
			iPlayer.setYRotLast(serverYaw);
			iPlayer.setXRotLast(serverPitch);
		}

		iPlayer.setLastOnGround(MC.player.onGround());
		iPlayer.setLastHorizontalCollision(MC.player.horizontalCollision);
		iPlayer.setAutoJumpEnabled(MC.options.autoJump().get());
	}

	@Override
	public void onSendMovementPacket(SendMovementPacketEvent.Post event) {

	}
}
