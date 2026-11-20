/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.aoba.Aoba;
import net.aoba.event.events.BlockStateEvent;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.BlockStateListener;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.gui.colors.Color;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.BlocksSetting;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.ShaderSetting;
import net.aoba.utils.player.MiningController;
import net.aoba.utils.player.MiningPriority;
import net.aoba.utils.player.MiningTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Nuker extends Module implements Render3DListener, TickListener, BlockStateListener {
	private final BooleanSetting creative = BooleanSetting.builder().id("nuker_creative").displayName("Creative")
			.description("Uses instant-break packet ordering regardless of the current game mode.").defaultValue(false)
			.build();

	private final ShaderSetting color = ShaderSetting.builder().id("nuker_color").displayName("Color")
			.description("Target highlight color").defaultValue(Shader.solid(new Color(0f, 1f, 1f, 1f))).build();

	private final FloatSetting radius = FloatSetting.builder().id("nuker_radius").displayName("Radius")
			.description("Maximum block interaction radius").defaultValue(5f).minValue(1f).maxValue(15f).step(1f)
			.build();

	private final FloatSetting delay = FloatSetting.builder().id("nuker_delay").displayName("Break Delay")
			.description("Minimum delay between mining actions in milliseconds").defaultValue(50f).minValue(0f)
			.maxValue(1000f).step(10f).build();

	private final BlocksSetting blacklist = BlocksSetting.builder().id("nuker_blacklist").displayName("Blacklist")
			.description("Blocks that will not be broken by Nuker").defaultValue(new HashSet<Block>()).build();

	private final EnumSetting<MiningPriority> priority = EnumSetting.<MiningPriority>builder().id("nuker_priority")
			.displayName("Priority").description("Order used to select surrounding blocks")
			.defaultValue(MiningPriority.CLOSEST).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder().id("nuker_rotation_mode")
			.displayName("Rotation Mode").description("Rotates toward the targeted block before mining")
			.defaultValue(RotationMode.INSTANT).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("nuker_max_rotation").displayName("Max Rotation")
			.description("Maximum rotation speed in degrees per tick").defaultValue(180f).minValue(1f).maxValue(360f)
			.step(1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("nuker_fake_rotation")
			.displayName("Fake Rotation").description("Sends mining rotations without moving the camera")
			.defaultValue(true).build();

	private final MiningController miner = MiningController.create(MC);
	private boolean ownsRotationGoal;

	public Nuker() {
		super("Nuker");
		setCategory(Category.of("World"));
		setDescription("Destroys blocks around the player with controlled mining and rotations.");

		addSetting(creative);
		addSetting(radius);
		addSetting(delay);
		addSetting(color);
		addSetting(blacklist);
		addSetting(priority);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(fakeRotation);

		setDetectable(AntiCheat.NoCheatPlus, AntiCheat.Vulcan, AntiCheat.AdvancedAntiCheat, AntiCheat.Verus,
				AntiCheat.Grim, AntiCheat.Matrix, AntiCheat.Negativity, AntiCheat.Karhu);
	}

	public void setRadius(int radius) {
		this.radius.setValue((float) radius);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(BlockStateListener.class, this);
		miner.reset();
		clearRotation();
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(Render3DListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(BlockStateListener.class, this);
		miner.resetTiming();
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onRender(Render3DEvent event) {
		if (miner.getTarget() != null) {
			event.getRenderer().drawBox(new AABB(miner.getTarget()), color.getValue(), 1.0f);
		}
	}

	@Override
	public void onBlockStateChanged(BlockStateEvent event) {
		if (event.getBlockPos().equals(miner.getTarget()) && event.getBlockState().isAir()) {
			miner.clear();
			clearRotation();
		}
	}

	private List<MiningTarget> findTargets() {
		List<MiningTarget> targets = new ArrayList<>();
		double effectiveRadius = Math.min(radius.getValue(), MC.player.blockInteractionRange());
		double radiusSqr = effectiveRadius * effectiveRadius;
		int rad = (int) Math.ceil(effectiveRadius);
		BlockPos origin = MC.player.blockPosition();
		Vec3 eye = MC.player.getEyePosition();

		for (int x = -rad; x <= rad; x++) {
			for (int y = -rad; y <= rad; y++) {
				for (int z = -rad; z <= rad; z++) {
					BlockPos pos = origin.offset(x, y, z);
					double distanceSqr = eye.distanceToSqr(Vec3.atCenterOf(pos));
					if (distanceSqr > radiusSqr) {
						continue;
					}

					BlockState state = MC.level.getBlockState(pos);
					float destroySpeed = state.getDestroySpeed(MC.level, pos);
					if (state.isAir() || !state.getFluidState().isEmpty() || destroySpeed < 0
							|| blacklist.getValue().contains(state.getBlock())) {
						continue;
					}
					targets.add(new MiningTarget(pos, distanceSqr, destroySpeed));
				}
			}
		}

		targets.sort(MiningTarget.comparator(priority.getValue()));
		return targets;
	}

	private MiningTarget selectTarget(List<MiningTarget> targets) {
		BlockPos active = miner.getTarget();
		if (active != null) {
			for (MiningTarget target : targets) {
				if (target.pos().equals(active)) {
					return target;
				}
			}
			miner.reset();
			clearRotation();
		}
		return targets.isEmpty() ? null : targets.get(0);
	}

	private Direction getFace(BlockPos pos) {
		Vec3 offset = MC.player.getEyePosition().subtract(Vec3.atCenterOf(pos));
		return Direction.getNearest(BlockPos.containing(offset), Direction.UP);
	}

	private boolean updateRotation(BlockPos pos) {
		if (rotationMode.getValue() == RotationMode.NONE) {
			clearRotation();
			return true;
		}

		Vec3 center = Vec3.atCenterOf(pos);
		if (ownsRotationGoal && Aoba.getInstance().rotationManager.getGoal() instanceof Vec3dGoal goal
				&& center.equals(goal.getGoal())) {
			return true;
		}

		Aoba.getInstance().rotationManager.setGoal(Vec3dGoal.builder().goal(center).mode(rotationMode.getValue())
				.maxRotation(maxRotation.getValue()).fakeRotation(fakeRotation.getValue()).build());
		ownsRotationGoal = true;
		return false;
	}

	private void clearRotation() {
		if (ownsRotationGoal) {
			Aoba.getInstance().rotationManager.setGoal(null);
			ownsRotationGoal = false;
		}
	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null || MC.level == null || MC.gameMode == null) {
			miner.reset();
			clearRotation();
			return;
		}

		MiningTarget target = selectTarget(findTargets());
		if (target == null) {
			miner.reset();
			clearRotation();
			return;
		}
		if (!updateRotation(target.pos())) {
			return;
		}

		boolean immediate = creative.getValue() || target.destroySpeed() == 0f;
		miner.mine(target.pos(), getFace(target.pos()), immediate, delay.getValue().longValue(),
				System.currentTimeMillis());
	}

	@Override
	public void onTick(Post event) {
	}
}
