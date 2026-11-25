package net.aoba.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;

/**
 * Tracks one active mining target and keeps packet ordering consistent between
 * creative, instant-break, and survival mining.
 */
public final class MiningController {
	public interface Actions {
		void start(BlockPos pos, Direction face);

		boolean continueMining(BlockPos pos, Direction face);

		void stop();

		void swing();
	}

	private final Actions actions;
	private BlockPos target;
	private Direction face;
	private boolean mining;
	private long lastActionTime = Long.MIN_VALUE;

	public MiningController(Actions actions) {
		this.actions = actions;
	}

	public static MiningController create(Minecraft minecraft) {
		return new MiningController(new Actions() {
			@Override
			public void start(BlockPos pos, Direction face) {
				if (minecraft.gameMode != null) {
					minecraft.gameMode.startDestroyBlock(pos, face);
				}
			}

			@Override
			public boolean continueMining(BlockPos pos, Direction face) {
				return minecraft.gameMode != null && minecraft.gameMode.continueDestroyBlock(pos, face);
			}

			@Override
			public void stop() {
				if (minecraft.gameMode != null) {
					minecraft.gameMode.stopDestroyBlock();
				}
			}

			@Override
			public void swing() {
				if (minecraft.player != null) {
					minecraft.player.swing(InteractionHand.MAIN_HAND,
							minecraft.player.getMainHandItem().getAttackAnimation(), false);
					minecraft.player.connection.send(ServerboundPunchPacket.INSTANCE);
				}
			}
		});
	}

	public boolean mine(BlockPos nextTarget, Direction nextFace, boolean immediate, long delayMs, long nowMs) {
		if (nextTarget == null) {
			reset();
			return false;
		}

		if (!nextTarget.equals(target)) {
			reset();
			target = nextTarget;
			face = nextFace;
		} else if (nextFace != null) {
			face = nextFace;
		}

		if (lastActionTime != Long.MIN_VALUE && nowMs - lastActionTime < delayMs) {
			return false;
		}

		if (!mining) {
			actions.start(target, face);
			mining = true;
			actions.swing();
		} else if (!immediate && actions.continueMining(target, face)) {
			actions.swing();
		}

		if (immediate) {
			actions.stop();
			clear();
		}

		lastActionTime = nowMs;
		return true;
	}

	public void reset() {
		if (mining) {
			actions.stop();
		}
		clear();
	}

	public void clear() {
		target = null;
		face = null;
		mining = false;
	}

	public void resetTiming() {
		lastActionTime = Long.MIN_VALUE;
	}

	public BlockPos getTarget() {
		return target;
	}
}
