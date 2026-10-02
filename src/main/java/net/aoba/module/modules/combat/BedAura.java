/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.combat;

import net.aoba.Aoba;
import net.aoba.event.events.BlockStateEvent;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.BlockStateListener;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.gui.colors.Color;
import net.aoba.managers.rotation.Rotation;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.EasingFunction;
import net.aoba.managers.rotation.goals.RotationGoal;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.ShaderSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BedAura extends Module implements Render3DListener, TickListener, BlockStateListener {
	private final ShaderSetting color = ShaderSetting.builder().id("bedaura_color").displayName("Color")
			.description("Color").defaultValue(Shader.solid(new Color(0f, 1f, 1f, 1f))).build();

	private final FloatSetting radius = FloatSetting.builder().id("bedaura_radius").displayName("Radius")
			.description("Radius").defaultValue(5f).minValue(0f).maxValue(15f).step(1f).build();

	private final BooleanSetting useRaycast = BooleanSetting.builder().id("bedaura_use_raycast").displayName("Use Raycast")
			.description(
					"Whether a raycast will be used to ensure that the player is aiming at the bed before breaking it.")
			.defaultValue(false).build();

	private final BooleanSetting triggerOnClick = BooleanSetting.builder().id("bedaura_trigger_on_click")
			.displayName("Trigger On Click").description("Only break beds while the attack key is held down.")
			.defaultValue(false).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("bedaura_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates.").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("bedaura_max_rotation")
			.displayName("Max Rotation").description("The max speed that BedAura will rotate").defaultValue(10.0f)
			.minValue(1.0f).maxValue(360.0f).build();
  
	private final EnumSetting<EasingFunction> easingFunction = EnumSetting.<EasingFunction>builder()
			.id("bedaura_easing").displayName("Easing")
			.description("Easing curve applied to the rotation speed as it approaches the target.")
			.defaultValue(EasingFunction.SineEaseInOut).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("bedaura_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("The randomness of the player's yaw").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("bedaura_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("The randomness of the player's pitch").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private BlockPos currentBlockToBreak = null;

	public BedAura() {
		super("BedAura");
		setCategory(Category.of("Combat"));
		setDescription("Destroys the nearest Bed to the player.");

		addSetting(radius);
		addSetting(useRaycast);
		addSetting(triggerOnClick);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(easingFunction);
		addSetting(yawRandomness);
		addSetting(pitchRandomness);
		addSetting(color);
	}

	public void setRadius(int radius) {
		this.radius.setValue((float) radius);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(BlockStateListener.class, this);
		Aoba.getInstance().rotationManager.setGoal(null);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(Render3DListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(BlockStateListener.class, this);
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onRender(Render3DEvent event) {
		if (currentBlockToBreak != null) {
			event.getRenderer().drawBox(new AABB(currentBlockToBreak), color.getValue(),
					1.0f);
		}
	}

	@Override
	public void onBlockStateChanged(BlockStateEvent event) {
		if (currentBlockToBreak != null) {
			BlockPos blockPos = event.getBlockPos();
			BlockState oldBlockState = event.getPreviousBlockState();
			if (blockPos.equals(currentBlockToBreak) && (oldBlockState.isAir())) {
				currentBlockToBreak = null;
				Aoba.getInstance().rotationManager.setGoal(null);
			}
		}
	}

	private BlockPos getNextBlock() {
		// Scan to find next block to begin breaking.
		int rad = radius.getValue().intValue();
		for (int y = rad; y > -rad; y--) {
			for (int x = -rad; x < rad; x++) {
				for (int z = -rad; z < rad; z++) {
					BlockPos blockpos = new BlockPos(MC.player.getBlockX() + x, MC.player.getBlockY() + y,
							MC.player.getBlockZ() + z);
					Block block = MC.level.getBlockState(blockpos).getBlock();
					if (!isBed(block))
						continue;

					return blockpos;
				}
			}
		}
		return null;
	}

	@Override
	public void onTick(Pre event) {
		if (currentBlockToBreak == null) {
			currentBlockToBreak = getNextBlock();
		}

		if (currentBlockToBreak != null) {
			// Check to ensure that the block is not further than we can reach.
			int range = (int) (Math.floor(radius.getValue()) + 1);
			int rangeSqr = range * range;

			if (Vec3.atCenterOf(MC.player.blockPosition()).distanceToSqr(Vec3.atCenterOf(currentBlockToBreak)) > rangeSqr) {
				currentBlockToBreak = null;
			} else {

				RotationGoal rotation = RotationGoal.builder()
						.goal(Rotation.rotationFrom(Vec3.atCenterOf(currentBlockToBreak))).mode(rotationMode.getValue())
						.maxRotation(maxRotation.getValue()).pitchRandomness(pitchRandomness.getValue())
						.yawRandomness(yawRandomness.getValue()).easingFunction(easingFunction.getValue()).build();
				Aoba.getInstance().rotationManager.setGoal(rotation);

				if (triggerOnClick.getValue() && !MC.options.keyAttack.isDown())
					return;

				if (useRaycast.getValue()) {
					HitResult ray = MC.hitResult;

					if (ray != null && ray.getType() == HitResult.Type.BLOCK) {
						BlockHitResult blockResult = (BlockHitResult) ray;

						if (currentBlockToBreak.equals(blockResult.getBlockPos())) {
							breakBlock(currentBlockToBreak);
						}
					}
				} else {
					breakBlock(currentBlockToBreak);
				}
			}

		} else {
			Aoba.getInstance().rotationManager.setGoal(null);
		}
	}

	private void breakBlock(BlockPos pos) {
		MC.player.connection
				.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, Direction.NORTH));
		MC.player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.NORTH));
		MC.player.swing(InteractionHand.MAIN_HAND, MC.player.getMainHandItem().getAttackAnimation(), false);
		MC.player.connection.send(ServerboundPunchPacket.INSTANCE);
	}

	@Override
	public void onTick(Post event) {

	}

	private boolean isBed(Block block) {
		return block instanceof BedBlock;
	}
}