/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.Goal;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BlocksSetting;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.aoba.utils.FindItemResult;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Surround extends Module implements TickListener {
	private static final long PLACEMENT_TIMEOUT_MS = 2000;

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
			.displayName("Auto-Disable").description("Disables only after every required placement is complete.")
			.defaultValue(false).build();

	private final IntegerSetting blocksPerTick = IntegerSetting.builder().id("surround_blocks_per_tick")
			.displayName("Blocks Per Tick").description("Maximum placement attempts in one tick").defaultValue(4)
			.minValue(1).maxValue(12).step(1).build();

	private final IntegerSetting placeDelay = IntegerSetting.builder().id("surround_place_delay")
			.displayName("Place Delay").description("Delay between placement batches in ticks").defaultValue(0)
			.minValue(0).maxValue(20).step(1).build();

	private final FloatSetting miningDelay = FloatSetting.builder().id("surround_mining_delay")
			.displayName("Mining Delay").description("Minimum delay between survival mining actions in milliseconds")
			.defaultValue(50f).minValue(0f).maxValue(1000f).step(10f).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("surround_rotation_mode").displayName("Rotation Mode")
			.description("Rotates toward blocks before mining or placement").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("surround_max_rotation")
			.displayName("Max Rotation").description("Maximum rotation speed in degrees per tick").defaultValue(180f)
			.minValue(1f).maxValue(360f).step(1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("surround_fake_rotation")
			.displayName("Fake Rotation").description("Uses interaction rotations without moving the camera")
			.defaultValue(true).build();

	private static final Set<Block> BREAKABLE_BLOCKS = Set.of(Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
			Blocks.DEAD_BUSH, Blocks.VINE, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);

	private final RotationOwner rotationOwner = new RotationOwner();
	private final Map<BlockPos, Long> pendingPlacements = new HashMap<>();
	private BlockPos miningTarget;
	private Direction miningFace;
	private boolean mining;
	private long lastMiningActionTime = Long.MIN_VALUE;
	private int placementCooldownTicks;

	public Surround() {
		super("Surround");
		setCategory(Category.of("World"));
		setDescription("Surrounds the player with controlled block mining and placement.");

		addSetting(placeHeight);
		addSetting(blocks);
		addSetting(alignCharacter);
		addSetting(autoDisable);
		addSetting(blocksPerTick);
		addSetting(placeDelay);
		addSetting(miningDelay);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(fakeRotation);

		setDetectable(AntiCheat.Vulcan, AntiCheat.AdvancedAntiCheat, AntiCheat.Verus, AntiCheat.Grim, AntiCheat.Matrix,
				AntiCheat.Negativity, AntiCheat.Karhu);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		pendingPlacements.clear();
		placementCooldownTicks = 0;
		lastMiningActionTime = Long.MIN_VALUE;

		if (alignCharacter.getValue() && MC.player != null && MC.player.connection != null) {
			BlockPos blockPos = MC.player.blockPosition();
			double x = blockPos.getX() + 0.5;
			double y = MC.player.getY();
			double z = blockPos.getZ() + 0.5;
			MC.player.absSnapTo(x, y, z);
			MC.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, MC.player.onGround(), false));
		}
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		resetMining();
		rotationOwner.clear();
		pendingPlacements.clear();
		swapBackSafely();
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null || MC.level == null || MC.gameMode == null) {
			resetWork();
			return;
		}

		if (placementCooldownTicks > 0) {
			placementCooldownTicks--;
		}

		prunePendingPlacements();
		List<BlockPos> plan = SurroundPlanner.planPositions(MC.player.blockPosition(), placeHeight.getValue().intValue());
		BlockPos miningTarget = findMiningTarget(plan);
		if (miningTarget != null) {
			mineTarget(miningTarget);
			return;
		}

		finishMining();
		BlockPos placementTarget = findPlacementTarget(plan);
		if (placementTarget == null) {
			rotationOwner.clear();
			if (autoDisable.getValue() && pendingPlacements.isEmpty()) {
				state.setValue(false);
			}
			return;
		}

		if (placementCooldownTicks > 0) {
			rotationOwner.clear();
			return;
		}

		placeBlocks(plan, placementTarget);
	}

	@Override
	public void onTick(Post event) {
	}

	private BlockPos findMiningTarget(List<BlockPos> plan) {
		BlockPos active = miningTarget;
		if (active != null && isBreakable(active)) {
			return active;
		}

		for (BlockPos pos : plan) {
			if (isBreakable(pos)) {
				return pos;
			}
		}
		return null;
	}

	private boolean isBreakable(BlockPos pos) {
		BlockState state = MC.level.getBlockState(pos);
		return !state.isAir() && !state.canBeReplaced() && BREAKABLE_BLOCKS.contains(state.getBlock())
				&& state.getFluidState().isEmpty() && state.getDestroySpeed(MC.level, pos) >= 0;
	}

	private BlockPos findPlacementTarget(List<BlockPos> plan) {
		for (BlockPos pos : plan) {
			BlockState state = MC.level.getBlockState(pos);
			if (state.getFluidState().isEmpty() && state.canBeReplaced() && !pendingPlacements.containsKey(pos)) {
				return pos;
			}
		}
		return null;
	}

	private void mineTarget(BlockPos pos) {
		if (!updateRotation(pos)) {
			return;
		}
		float destroySpeed = MC.level.getBlockState(pos).getDestroySpeed(MC.level, pos);
		boolean immediate = MC.player.isCreative() || destroySpeed == 0f;
		mine(pos, getFace(pos), immediate, miningDelay.getValue().longValue(), System.currentTimeMillis());
	}

	private void placeBlocks(List<BlockPos> plan, BlockPos firstTarget) {
		FindItemResult item = findInHotbar(stack -> blocks.getValue().contains(Block.byItem(stack.getItem())));
		if (!item.found()) {
			sendChatMessage("Surround stopped: no configured blocks in the hotbar or offhand.");
			state.setValue(false);
			return;
		}

		if (!updateRotation(firstTarget)) {
			return;
		}

		InteractionHand hand = item.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		boolean swapped = hand == InteractionHand.MAIN_HAND && !item.isMainHand() && swap(item.slot(), true);
		if (hand == InteractionHand.MAIN_HAND && !item.isMainHand() && !swapped) {
			sendChatMessage("Surround stopped: unable to select a configured block.");
			state.setValue(false);
			return;
		}

		int placed = 0;
		try {
			for (BlockPos pos : plan) {
				if (placed >= blocksPerTick.getValue()) {
					break;
				}
				BlockState state = MC.level.getBlockState(pos);
				if (!state.getFluidState().isEmpty() || !state.canBeReplaced() || pendingPlacements.containsKey(pos)) {
					continue;
				}
				if (!placeBlock(pos, hand)) {
					break;
				}
				pendingPlacements.put(pos, System.currentTimeMillis());
				placed++;
			}
		} finally {
			if (swapped) {
				swapBack();
			}
		}

		if (placed > 0) {
			placementCooldownTicks = placeDelay.getValue();
		} else {
			rotationOwner.clear();
		}
	}

	private void prunePendingPlacements() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<BlockPos, Long>> iterator = pendingPlacements.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<BlockPos, Long> entry = iterator.next();
			if (!MC.level.getBlockState(entry.getKey()).canBeReplaced()
					|| now - entry.getValue() >= PLACEMENT_TIMEOUT_MS) {
				iterator.remove();
			}
		}
	}

	private void finishMining() {
		if (miningTarget == null) {
			return;
		}
		if (MC.level.getBlockState(miningTarget).isAir()) {
			clearMining();
		} else {
			resetMining();
		}
	}

	private boolean placeBlock(BlockPos blockPos, InteractionHand hand) {
		Inventory inventory = MC.player.getInventory();
		ItemStack itemInHand = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedItem()
				: inventory.getItem(InteractionUtils.OFFHAND);
		if (!(itemInHand.getItem() instanceof BlockItem)) {
			return false;
		}

		BlockHitResult placementHit = getPlacementHit(blockPos);
		return placementHit != null && InteractionUtils.interactBlock(placementHit, hand);
	}

	private BlockHitResult getPlacementHit(BlockPos blockPos) {
		Direction side = getPlaceSide(blockPos);
		if (side == null) {
			return null;
		}

		BlockPos neighbour = blockPos.relative(side);
		Vec3 placePos = Vec3.atCenterOf(blockPos).add(side.getStepX() * 0.5, side.getStepY() * 0.5,
				side.getStepZ() * 0.5);
		return new BlockHitResult(placePos, side.getOpposite(), neighbour, false);
	}

	private Direction getPlaceSide(BlockPos blockPos) {
		Vec3 lookVec = Vec3.atCenterOf(blockPos).subtract(MC.player.getEyePosition());
		double bestRelevancy = -Double.MAX_VALUE;
		Direction bestSide = null;

		for (Direction side : Direction.values()) {
			BlockPos neighborPos = blockPos.relative(side);
			BlockState blockState = MC.level.getBlockState(neighborPos);
			if (blockState.isAir() || !blockState.getFluidState().isEmpty() || blockState.canBeReplaced()
					|| blockState.getCollisionShape(MC.level, neighborPos).isEmpty()) {
				continue;
			}

			Direction.AxisDirection direction = side.getAxisDirection();
			double relevancy = side.getAxis().choose(lookVec.x(), lookVec.y(), lookVec.z()) * direction.getStep();
			if (relevancy > bestRelevancy) {
				bestRelevancy = relevancy;
				bestSide = side;
			}
		}
		return bestSide;
	}

	private void mine(BlockPos nextTarget, Direction nextFace, boolean immediate, long delayMs, long nowMs) {
		if (nextTarget == null) {
			resetMining();
			return;
		}

		if (!nextTarget.equals(miningTarget)) {
			resetMining();
			miningTarget = nextTarget;
			miningFace = nextFace;
		} else if (nextFace != null) {
			miningFace = nextFace;
		}

		if (lastMiningActionTime != Long.MIN_VALUE && nowMs - lastMiningActionTime < delayMs) {
			return;
		}

		if (!mining) {
			if (MC.gameMode != null) {
				MC.gameMode.startDestroyBlock(miningTarget, miningFace);
			}
			mining = true;
			swingMiningHand();
		} else if (!immediate && MC.gameMode != null && MC.gameMode.continueDestroyBlock(miningTarget, miningFace)) {
			swingMiningHand();
		}

		if (immediate) {
			if (MC.gameMode != null) {
				MC.gameMode.stopDestroyBlock();
			}
			clearMining();
		}

		lastMiningActionTime = nowMs;
	}

	private void swingMiningHand() {
		if (MC.player != null) {
			MC.player.swing(InteractionHand.MAIN_HAND, MC.player.getMainHandItem().getAttackAnimation(), false);
			MC.player.connection.send(ServerboundPunchPacket.INSTANCE);
		}
	}

	private void resetMining() {
		if (mining && MC.gameMode != null) {
			MC.gameMode.stopDestroyBlock();
		}
		clearMining();
	}

	private void clearMining() {
		miningTarget = null;
		miningFace = null;
		mining = false;
	}

	private Direction getFace(BlockPos pos) {
		Vec3 offset = MC.player.getEyePosition().subtract(Vec3.atCenterOf(pos));
		return Direction.getNearest(BlockPos.containing(offset), Direction.UP);
	}

	private boolean updateRotation(BlockPos pos) {
		if (rotationMode.getValue() == RotationMode.NONE) {
			rotationOwner.clear();
			return true;
		}

		Vec3 center = Vec3.atCenterOf(pos);
		if (rotationOwner.isActive() && rotationOwner.getOwnedGoal() instanceof Vec3dGoal goal
				&& center.equals(goal.getGoal())) {
			return true;
		}

		rotationOwner.set(Vec3dGoal.builder().goal(center).mode(rotationMode.getValue())
				.maxRotation(maxRotation.getValue()).fakeRotation(fakeRotation.getValue()).build());
		return false;
	}

	private void resetWork() {
		resetMining();
		rotationOwner.clear();
		pendingPlacements.clear();
		swapBackSafely();
	}

	private void swapBackSafely() {
		if (MC.player != null && MC.gameMode != null) {
			swapBack();
		}
	}

	private static final class RotationOwner {
		private Goal<?> ownedGoal;

		boolean isActive() {
			return ownedGoal != null && Aoba.getInstance().rotationManager.getGoal() == ownedGoal;
		}

		Goal<?> getOwnedGoal() {
			return ownedGoal;
		}

		void set(Goal<?> goal) {
			ownedGoal = goal;
			Aoba.getInstance().rotationManager.setGoal(goal);
		}

		void clear() {
			if (ownedGoal != null && Aoba.getInstance().rotationManager.getGoal() == ownedGoal) {
				Aoba.getInstance().rotationManager.setGoal(null);
			}
			ownedGoal = null;
		}
	}
}
