/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.world;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module implements TickListener {
	private final FloatSetting radius = FloatSetting.builder().id("scaffold_radius").displayName("Radius")
			.description("How far Scaffold will place a block below the player.").defaultValue(4f).minValue(1f)
			.maxValue(10f).step(0.5f).build();

	private final KeybindSetting descendScaffoldKey = KeybindSetting.builder().id("scaffold_descend_key")
			.displayName("Descend Key")
			.description("Hold this key to descend down.")
			.defaultValue(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LSHIFT)).build();

	private final FloatSetting placeDelay = FloatSetting.builder().id("scaffold_place_delay").displayName("Place Delay")
			.description("How long (in ticks) until Scaffold will place the next block below the player.")
			.defaultValue(0f).minValue(0f).maxValue(20f).step(1f).build();

	private final FloatSetting maxBlocksPerTick = FloatSetting.builder().id("scaffold_max_blocks_per_tick")
			.displayName("Max Blocks Per Tick")
			.description("The maximum number of blocks that can be placed in a single tick.")
			.defaultValue(4f).minValue(1f).maxValue(10f).step(1f).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("scaffold_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates.").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("scaffold_max_rotation")
			.displayName("Max Rotation").description("The max speed that Aimbot will rotate").defaultValue(10.0f)
			.minValue(1.0f).maxValue(360.0f).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("scaffold_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("The randomness of the player's yaw").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("scaffold_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("The randomness of the player's pitch").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("scaffold_fake_rotation")
			.displayName("Fake Rotation")
			.description("Spoofs the client's rotation so that the player appears rotated on the server")
			.defaultValue(false).build();

	private final BooleanSetting moveFix = BooleanSetting.builder().id("scaffold_move_fix")
			.displayName("Move Fix")
			.description("Corrects movement to match spoofed rotation by using the server yaw for velocity.")
			.defaultValue(false).build();

	private final BooleanSetting swingHand = BooleanSetting.builder().id("scaffold_swing_hand")
			.displayName("Swing Hand").description("Swing hand when placing blocks.").defaultValue(true).build();

	private final BooleanSetting disableSprint = BooleanSetting.builder().id("scaffold_disable_sprint")
			.displayName("Disable Sprint").description("Prevents the player from sprinting while scaffolding.")
			.defaultValue(false).build();

	private final BooleanSetting disableSneak = BooleanSetting.builder().id("scaffold_disable_sneak")
			.displayName("Disable Sneak").description("Prevents sneaking while down scaffolding.")
			.defaultValue(true).build();

	private int yPosition = 0;
	private int curDelay = 0;

	public Scaffold() {
		super("Scaffold");
		setCategory(Category.of("World"));
		setDescription("Automatically places blocks below the player.");

		addSetting(radius);
		addSetting(descendScaffoldKey);
		addSetting(placeDelay);
		addSetting(maxBlocksPerTick);
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
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().rotationManager.setGoal(null);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onTick(Pre event) {
		if (disableSprint.getValue())
			MC.options.keySprint.setDown(false);

		Inventory inventory = MC.player.getInventory();
		ItemStack currentHand = inventory.getSelectedItem();
		if (currentHand.getItem() instanceof BlockItem) {
			if (curDelay < placeDelay.getValue()) {
				Aoba.getInstance().rotationManager.setGoal(null);
				curDelay++;
				return;
			}

			int blocksPlaced = 0;
			int maxBlocks = maxBlocksPerTick.getValue().intValue();

			while (blocksPlaced < maxBlocks) {
				BlockPos placementPos = findBlockPosToPlace();
				if (placementPos == null)
					break;

				Vec3 placementPosVec = Vec3.atCenterOf(placementPos);
				Vec3dGoal rotation = Vec3dGoal.builder().goal(placementPosVec).mode(rotationMode.getValue())
						.maxRotation(maxRotation.getValue()).pitchRandomness(pitchRandomness.getValue())
						.yawRandomness(yawRandomness.getValue()).fakeRotation(fakeRotation.getValue())
						.moveFix(moveFix.getValue()).build();
				Aoba.getInstance().rotationManager.setGoal(rotation);

				if (!InteractionUtils.placeBlock(placementPos, InteractionHand.MAIN_HAND))
					break;

				blocksPlaced++;
			}

			if (blocksPlaced > 0) {
				curDelay = 0;
			} else {
				Aoba.getInstance().rotationManager.setGoal(null);
			}
		}
	}

	@Override
	public void onTick(Post event) {
		if (disableSneak.getValue())
			MC.options.keyShift.setDown(false);

		// Determine the height at which the player will build.
		Key descendKey = descendScaffoldKey.getValue();
		if (descendKey.getValue() > 0 && InputConstants.isKeyDown(descendKey.getValue())) {
			yPosition = MC.player.blockPosition().getY() - 2;
		} else if (MC.player.onGround()) {
			if (MC.options.keyJump.isDown()) {
				yPosition = MC.player.blockPosition().getY();
			} else
				yPosition = MC.player.blockPosition().getY() - 1;
		}
	}

	/**
	 * Finds the next BlockPos to place at to build the bridge to
	 * underneath the player.
	 * @return BlockPos to place at, if any.
	 */
	private BlockPos findBlockPosToPlace() {
		BlockPos playerPos = MC.player.blockPosition();
		int ux = playerPos.getX();
		int uz = playerPos.getZ();
		BlockPos underneathPos = new BlockPos(ux, yPosition, uz);

		// Block already filled.
		if (!MC.level.isEmptyBlock(underneathPos))
			return null;

		// Check underneath and return if it is empty and has an adjacent solid block.
		if (hasAdjacentSolid(underneathPos))
			return underneathPos;

		// Search outward for closest empty block with an adjacent solid block.
		BlockPos result = null;
		double lastDistanceSqr = Double.MAX_VALUE;
		int radiusInt = radius.getValue().intValue();
		double radiusSqr = radius.getValueSqr();

		for (int x = -radiusInt; x < radiusInt; x++) {
			for (int z = -radiusInt; z < radiusInt; z++) {
				
				// Ignore because was already checked earlier.
				if (x == 0 && z == 0)
					continue;

				// Get the sqr distance and compare it to the previous sqr distance.
				double distSqr = x * x + z * z;
				if (distSqr >= lastDistanceSqr || distSqr > radiusSqr)
					continue;

				// Check the block to ensure a block can be placed there.
				BlockPos checkPos = new BlockPos(ux + x, yPosition, uz + z);
				if (!MC.level.isEmptyBlock(checkPos))
					continue;

				if (hasAdjacentSolid(checkPos)) {
					result = checkPos;
					lastDistanceSqr = distSqr;
				}
			}
		}

		return result;
	}
	
	/**
	 * Returns whether a block has an adjacent solid block.
	 * @param pos Position to check
	 * @return True if there is, otherwise false.
	 */
	private boolean hasAdjacentSolid(BlockPos pos) {
		for (Direction dir : Direction.values()) {
			BlockState state = MC.level.getBlockState(pos.relative(dir));

			if (!state.isAir() && state.getFluidState().isEmpty())
				return true;
		}

		return false;
	}
}