package net.aoba.module.modules.world;

import java.util.Set;

import net.aoba.utils.player.InteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Selects stable building blocks without silently changing inventory contents. */
final class ScaffoldBlockSelector {
	record Selection(int slot, InteractionHand hand, int score) {
	}

	private ScaffoldBlockSelector() {
	}

	static Selection select(Minecraft minecraft, Set<Block> disallowed, boolean autoSelect, boolean preferFullBlocks,
			boolean avoidFallingBlocks, boolean avoidEntityBlocks) {
		if (minecraft.player == null || minecraft.level == null) {
			return null;
		}

		Selection best = null;
		int selectedSlot = minecraft.player.getInventory().getSelectedSlot();
		for (int slot = 0; slot < 9; slot++) {
			if (!autoSelect && slot != selectedSlot) {
				continue;
			}
			ItemStack stack = minecraft.player.getInventory().getItem(slot);
			int score = score(stack, minecraft.level, disallowed, preferFullBlocks, avoidFallingBlocks,
					avoidEntityBlocks);
			if (slot == selectedSlot && score >= 0) {
				score += 8;
			}
			if (score >= 0 && (best == null || score > best.score())) {
				best = new Selection(slot, InteractionHand.MAIN_HAND, score);
			}
		}

		ItemStack offhand = minecraft.player.getOffhandItem();
		int offhandScore = score(offhand, minecraft.level, disallowed, preferFullBlocks, avoidFallingBlocks,
				avoidEntityBlocks);
		if (offhandScore >= 0 && (best == null || offhandScore > best.score())) {
			best = new Selection(InteractionUtils.OFFHAND, InteractionHand.OFF_HAND, offhandScore);
		}
		return best;
	}

	private static int score(ItemStack stack, ClientLevel level, Set<Block> disallowed, boolean preferFullBlocks,
			boolean avoidFallingBlocks, boolean avoidEntityBlocks) {
		if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
			return -1;
		}

		Block block = blockItem.getBlock();
		if (disallowed.contains(block) || avoidFallingBlocks && block instanceof FallingBlock
				|| avoidEntityBlocks && block instanceof EntityBlock) {
			return -1;
		}

		BlockState state = block.defaultBlockState();
		if (state.getCollisionShape(level, net.minecraft.core.BlockPos.ZERO).isEmpty()) {
			return -1;
		}

		int score = stack.getCount();
		if (!(block instanceof EntityBlock)) {
			score += 32;
		}
		if (preferFullBlocks && state.isCollisionShapeFullBlock(level, net.minecraft.core.BlockPos.ZERO)) {
			score += 128;
		}
		return score;
	}
}
