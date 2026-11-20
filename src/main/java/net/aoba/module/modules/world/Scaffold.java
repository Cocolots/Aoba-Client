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

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BlocksSetting;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.aoba.settings.types.KeybindSetting;
import net.aoba.utils.input.Keybind;
import net.aoba.utils.player.InteractionUtils;
import net.aoba.utils.player.RotationGoalOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module implements TickListener {
	private static final long PLACEMENT_TIMEOUT_MS = 2000;

	private final FloatSetting radius = FloatSetting.builder().id("scaffold_radius").displayName("Radius")
			.description("Horizontal search radius for supported placements").defaultValue(4f).minValue(1f)
			.maxValue(10f).step(0.5f).build();

	private final KeybindSetting descendScaffoldKey = KeybindSetting.builder().id("scaffold_descend_key")
			.displayName("Descend Key").description("Hold this key to place one block lower")
			.defaultValue(Keybind.from(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LSHIFT))).build();

	private final FloatSetting placeDelay = FloatSetting.builder().id("scaffold_place_delay").displayName("Place Delay")
			.description("Delay between placement batches in ticks").defaultValue(0f).minValue(0f).maxValue(20f)
			.step(1f).build();

	private final FloatSetting maxBlocksPerTick = FloatSetting.builder().id("scaffold_max_blocks_per_tick")
			.displayName("Max Blocks Per Tick").description("Maximum successful placements in one tick")
			.defaultValue(4f).minValue(1f).maxValue(10f).step(1f).build();

	private final EnumSetting<ScaffoldHeightMode> heightMode = EnumSetting.<ScaffoldHeightMode>builder()
			.id("scaffold_same_y").displayName("Same Y").description("Controls whether jumping changes the floor height")
			.defaultValue(ScaffoldHeightMode.OFF).build();

	private final BooleanSetting tower = BooleanSetting.builder().id("scaffold_tower").displayName("Tower")
			.description("Allows the jump key to raise Same-Y placement").defaultValue(true).build();

	private final IntegerSetting predictionTicks = IntegerSetting.builder().id("scaffold_prediction_ticks")
			.displayName("Prediction Ticks").description("Horizontal motion used to plan upcoming placements")
			.defaultValue(1).minValue(0).maxValue(5).step(1).build();

	private final BooleanSetting autoSelect = BooleanSetting.builder().id("scaffold_auto_block")
			.displayName("Auto Block").description("Selects the best suitable hotbar or offhand block")
			.defaultValue(true).build();

	private final BooleanSetting preferFullBlocks = BooleanSetting.builder().id("scaffold_prefer_full_blocks")
			.displayName("Prefer Full Blocks").description("Ranks full collision blocks above slabs and other shapes")
			.defaultValue(true).build();

	private final BooleanSetting avoidFallingBlocks = BooleanSetting.builder().id("scaffold_avoid_falling")
			.displayName("Avoid Falling Blocks").description("Rejects sand, gravel, and other falling blocks")
			.defaultValue(true).build();

	private final BooleanSetting avoidEntityBlocks = BooleanSetting.builder().id("scaffold_avoid_entity_blocks")
			.displayName("Avoid Block Entities").description("Rejects chests, furnaces, and other block-entity blocks")
			.defaultValue(true).build();

	private final BlocksSetting disallowedBlocks = BlocksSetting.builder().id("scaffold_disallowed_blocks")
			.displayName("Disallowed Blocks").description("Blocks never selected for scaffolding")
			.defaultValue(new HashSet<Block>(Lists.newArrayList(Blocks.TNT, Blocks.COBWEB, Blocks.NETHER_PORTAL,
					Blocks.MAGMA_BLOCK)))
			.build();

	private final BooleanSetting requireSight = BooleanSetting.builder().id("scaffold_require_sight")
			.displayName("Require Sight").description("Only places when the support face is visible")
			.defaultValue(false).build();

	private final BooleanSetting safeWalk = BooleanSetting.builder().id("scaffold_safe_walk").displayName("Safe Walk")
			.description("Sneaks while grounded to reduce accidental edge falls").defaultValue(true).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("scaffold_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("scaffold_max_rotation")
			.displayName("Max Rotation").description("Maximum rotation speed in degrees per tick").defaultValue(10f)
			.minValue(1f).maxValue(360f).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("scaffold_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("Random yaw offset applied by the rotation manager")
			.defaultValue(0f).minValue(0f).maxValue(10f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("scaffold_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("Random pitch offset applied by the rotation manager")
			.defaultValue(0f).minValue(0f).maxValue(10f).step(0.1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("scaffold_fake_rotation")
			.displayName("Fake Rotation").description("Uses interaction rotations without moving the camera")
			.defaultValue(false).build();

	private final BooleanSetting moveFix = BooleanSetting.builder().id("scaffold_move_fix").displayName("Move Fix")
			.description("Corrects movement input when using spoofed rotations").defaultValue(false).build();

	private final BooleanSetting swingHand = BooleanSetting.builder().id("scaffold_swing_hand")
			.displayName("Swing Hand").description("Swing the selected hand after successful placement")
			.defaultValue(true).build();

	private final BooleanSetting disableSprint = BooleanSetting.builder().id("scaffold_disable_sprint")
			.displayName("Disable Sprint").description("Prevents sprinting while scaffolding").defaultValue(false)
			.build();

	private final BooleanSetting disableSneak = BooleanSetting.builder().id("scaffold_disable_sneak")
			.displayName("Disable Descend Sneak").description("Prevents Safe Walk sneaking while the descend key is held")
			.defaultValue(true).build();

	private final RotationGoalOwner rotationOwner = new RotationGoalOwner();
	private final Map<BlockPos, Long> pendingPlacements = new HashMap<>();
	private int placementY;
	private int placementCooldown;
	private boolean safeWalkApplied;
	private boolean previousSprint;
	private boolean previousSneak;

	public Scaffold() {
		super("Scaffold");
		setCategory(Category.of("World"));
		setDescription("Builds beneath the player using predictive, supported block placement.");

		addSetting(radius);
		addSetting(descendScaffoldKey);
		addSetting(placeDelay);
		addSetting(maxBlocksPerTick);
		addSetting(heightMode);
		addSetting(tower);
		addSetting(predictionTicks);
		addSetting(autoSelect);
		addSetting(preferFullBlocks);
		addSetting(avoidFallingBlocks);
		addSetting(avoidEntityBlocks);
		addSetting(disallowedBlocks);
		addSetting(requireSight);
		addSetting(safeWalk);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(yawRandomness);
		addSetting(pitchRandomness);
		addSetting(fakeRotation);
		addSetting(moveFix);
		addSetting(swingHand);
		addSetting(disableSprint);
		addSetting(disableSneak);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		placementY = MC.player == null ? 0 : MC.player.blockPosition().getY() - 1;
		placementCooldown = 0;
		pendingPlacements.clear();
		previousSprint = MC.options.keySprint.isDown();
		previousSneak = MC.options.keyShift.isDown();
		safeWalkApplied = false;
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		resetWork();
		MC.options.keySprint.setDown(previousSprint);
		MC.options.keyShift.setDown(previousSneak);
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

		if (MC.player.onGround()) {
			placementY = MC.player.blockPosition().getY() - 1;
		}
		if (disableSprint.getValue()) {
			MC.options.keySprint.setDown(false);
		}

		boolean descending = isDescendDown();
		updateSafeWalk(descending);
		prunePendingPlacements();

		if (placementCooldown > 0) {
			placementCooldown--;
			rotationOwner.clear();
			return;
		}

		int targetY = ScaffoldPlanner.resolvePlacementY(MC.player.blockPosition(), placementY, heightMode.getValue(),
				tower.getValue(), MC.options.keyJump.isDown(), descending, MC.player.getDeltaMovement().y);
		Vec3 predicted = ScaffoldPlanner.predictPosition(MC.player.position(), MC.player.getDeltaMovement(),
				predictionTicks.getValue());
		List<BlockPos> candidates = ScaffoldPlanner.planCandidates(predicted, targetY, radius.getValue());
		BlockPos firstTarget = findFirstTarget(candidates);
		if (firstTarget == null) {
			rotationOwner.clear();
			return;
		}

		ScaffoldBlockSelector.Selection selection = ScaffoldBlockSelector.select(MC, disallowedBlocks.getValue(),
				autoSelect.getValue(), preferFullBlocks.getValue(), avoidFallingBlocks.getValue(),
				avoidEntityBlocks.getValue());
		if (selection == null) {
			rotationOwner.clear();
			return;
		}

		BlockHitResult firstHit = InteractionUtils.getPlacementHit(firstTarget);
		if (firstHit == null || !updateRotation(firstHit)) {
			return;
		}

		placeBatch(candidates, selection);
	}

	@Override
	public void onTick(Post event) {
	}

	private BlockPos findFirstTarget(List<BlockPos> candidates) {
		for (BlockPos pos : candidates) {
			if (canAttempt(pos)) {
				return pos;
			}
		}
		return null;
	}

	private boolean canAttempt(BlockPos pos) {
		BlockState state = MC.level.getBlockState(pos);
		return state.canBeReplaced() && state.getFluidState().isEmpty() && !pendingPlacements.containsKey(pos)
				&& InteractionUtils.getPlacementHit(pos) != null;
	}

	private void placeBatch(List<BlockPos> candidates, ScaffoldBlockSelector.Selection selection) {
		boolean swapped = selection.hand() == InteractionHand.MAIN_HAND
				&& selection.slot() != MC.player.getInventory().getSelectedSlot() && swap(selection.slot(), true);
		if (selection.hand() == InteractionHand.MAIN_HAND
				&& selection.slot() != MC.player.getInventory().getSelectedSlot() && !swapped) {
			rotationOwner.clear();
			return;
		}

		int placed = 0;
		try {
			for (BlockPos pos : candidates) {
				if (placed >= maxBlocksPerTick.getValue().intValue() || !canAttempt(pos)) {
					continue;
				}

				BlockHitResult hit = InteractionUtils.getPlacementHit(pos);
				if (hit == null || requireSight.getValue() && !InteractionUtils.isPlacementVisible(hit)) {
					continue;
				}
				if (!InteractionUtils.interactBlock(hit, selection.hand(), swingHand.getValue())) {
					continue;
				}

				pendingPlacements.put(pos, System.currentTimeMillis());
				placed++;
				if (placed >= maxBlocksPerTick.getValue().intValue()) {
					break;
				}
			}
		} finally {
			if (swapped) {
				swapBack();
			}
		}

		if (placed > 0) {
			placementCooldown = placeDelay.getValue().intValue();
		} else {
			rotationOwner.clear();
		}
	}

	private boolean updateRotation(BlockHitResult hit) {
		if (rotationMode.getValue() == RotationMode.NONE) {
			rotationOwner.clear();
			return true;
		}

		Vec3 target = hit.getLocation();
		if (rotationOwner.isActive() && rotationOwner.getOwnedGoal() instanceof Vec3dGoal goal
				&& target.equals(goal.getGoal())) {
			return true;
		}

		rotationOwner.set(Vec3dGoal.builder().goal(target).mode(rotationMode.getValue())
				.maxRotation(maxRotation.getValue()).pitchRandomness(pitchRandomness.getValue())
				.yawRandomness(yawRandomness.getValue()).fakeRotation(fakeRotation.getValue()).moveFix(moveFix.getValue())
				.build());
		return false;
	}

	private void updateSafeWalk(boolean descending) {
		if (descending && disableSneak.getValue()) {
			if (safeWalkApplied) {
				MC.options.keyShift.setDown(false);
				safeWalkApplied = false;
			}
			return;
		}

		if (safeWalk.getValue() && MC.player.onGround()) {
			MC.options.keyShift.setDown(true);
			safeWalkApplied = true;
		} else if (safeWalkApplied) {
			MC.options.keyShift.setDown(false);
			safeWalkApplied = false;
		}
	}

	private boolean isDescendDown() {
		Keybind keybind = descendScaffoldKey.getValue();
		return keybind.getKey() != InputConstants.UNKNOWN && isKeyPressed(keybind.getKeyCode());
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

	private void resetWork() {
		rotationOwner.clear();
		pendingPlacements.clear();
		placementCooldown = 0;
		swapBack();
		if (safeWalkApplied) {
			MC.options.keyShift.setDown(false);
			safeWalkApplied = false;
		}
	}
}
