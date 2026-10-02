/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.world;

import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BlocksSetting;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class Surround extends Module implements TickListener {
	private final FloatSetting placeHeight = FloatSetting.builder().id("surround_height").displayName("Height")
			.description("Height that surround walls will go.").defaultValue(1f).minValue(1f).maxValue(3f).step(1.0f)
			.build();

	private final BlocksSetting blocks = BlocksSetting.builder().id("surround_blocks").displayName("Blocks")
			.description("Blocks that will be used to place surrounding blocks.")
			.defaultValue(new HashSet<Block>(Lists.newArrayList(Blocks.OBSIDIAN, Blocks.ENDER_CHEST,
					Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
					Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.ANCIENT_DEBRIS, Blocks.RESPAWN_ANCHOR)))
			.build();

	private final BooleanSetting alignCharacter = BooleanSetting.builder().id("surround_align").displayName("Align")
			.description("Aligns the character to the nearest block.").defaultValue(false).build();

	private final BooleanSetting autoDisable = BooleanSetting.builder().id("surround_autodisable")
			.displayName("Auto-Disable").description("Disables the module when the blocks have finished placing.")
			.defaultValue(false).build();

	private static final List<Block> BREAKABLE_BLOCKS = Lists.newArrayList(Blocks.TALL_GRASS, Blocks.FERN,
			Blocks.LARGE_FERN, Blocks.DEAD_BUSH, Blocks.VINE, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES,
			Blocks.BEETROOTS);

	public Surround() {
		super("Surround");
		setCategory(Category.of("World"));
		setDescription("Surrounds the player with blocks.");

		addSetting(placeHeight);
		addSetting(blocks);
		addSetting(alignCharacter);
		addSetting(autoDisable);

		setDetectable(AntiCheat.Vulcan, AntiCheat.AdvancedAntiCheat, AntiCheat.Verus, AntiCheat.Grim, AntiCheat.Matrix,
				AntiCheat.Negativity, AntiCheat.Karhu);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		if (alignCharacter.getValue()) {
			BlockPos blockPos = MC.player.blockPosition();
			MC.player.absSnapTo(blockPos.getX() + 0.5f, MC.player.getY(), blockPos.getZ() + 0.5f);
			MC.player.connection.send(new ServerboundMovePlayerPacket.Pos(blockPos.getX() + 0.5f,
					MC.player.getY(), blockPos.getZ() + 0.5f, MC.player.onGround(), false));
		}
	}

	@Override
	public void onToggle() {

	}

	private int getBlockInventorySlot() {
		HashSet<Block> availableBlocks = blocks.getValue();
		for (int i = 0; i < 36; i++) {
			ItemStack stack = MC.player.getInventory().getItem(i);
			if (stack != null && availableBlocks.contains(Block.byItem(stack.getItem()))) {
				return i;
			}
		}
		return -1;
	}

	private void breakBlock(BlockPos pos) {
		MC.gameMode.startDestroyBlock(pos, Direction.UP);
		MC.player.swing(InteractionHand.MAIN_HAND, MC.player.getMainHandItem().getAttackAnimation(), false);
		MC.player.connection.send(ServerboundPunchPacket.INSTANCE);
	}

	@Override
	public void onTick(Pre event) {
		int foundBlockSlot = getBlockInventorySlot();
		int oldSlot = MC.player.getInventory().getSelectedSlot();

		// Disable the module if no block was found in the inventory.
		if (foundBlockSlot == -1) {
			state.setValue(false);
			return;
		}

		// Change the selected slot and determine which hand it is in.
		MC.player.getInventory().setSelectedSlot(foundBlockSlot);
		MC.player.connection.send(new ServerboundSetCarriedItemPacket(foundBlockSlot));
		InteractionHand hand = foundBlockSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

		// Get the block positions around the player and place the blocks.
		BlockPos playerPosition = MC.player.blockPosition();
		int height = placeHeight.getValue().intValue();

		for (int i = 0; i < height; i++) {
			BlockPos newPos = playerPosition.offset(0, i, 0);
			List<BlockPos> placePositions = Lists.newArrayList(newPos.north(), newPos.east(), newPos.south(),
					newPos.west());
			for (BlockPos pos : placePositions) {
				if (MC.level.getBlockState(pos).canBeReplaced()) {
					InteractionUtils.placeBlock(pos, hand);
				} else if (BREAKABLE_BLOCKS.contains(MC.level.getBlockState(pos).getBlock())) {
					breakBlock(pos);
					InteractionUtils.placeBlock(pos, hand);
				}
			}
		}

		// Return Selected Slot back to original slot.
		MC.player.getInventory().setSelectedSlot(oldSlot);
		MC.player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));

		// Disable state if auto-disable is enabled.
		if (autoDisable.getValue()) {
			state.setValue(false);
		}
	}

	@Override
	public void onTick(Post event) {

	}
}
