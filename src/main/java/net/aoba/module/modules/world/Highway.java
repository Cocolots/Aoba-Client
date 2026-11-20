package net.aoba.module.modules.world;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.aoba.utils.FindItemResult;
import net.aoba.utils.entity.EntityUtils;
import net.aoba.utils.player.InteractionUtils;
import net.aoba.utils.player.MiningController;
import net.aoba.utils.player.MiningTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Highway extends Module implements TickListener {
	private final EnumSetting<HighwayDirection> direction = EnumSetting.<HighwayDirection>builder()
			.id("highway_direction").displayName("Direction").description("Cardinal direction used while enabled")
			.defaultValue(HighwayDirection.NORTH).build();

	private final EnumSetting<HighwayMaterial> material = EnumSetting.<HighwayMaterial>builder().id("highway_material")
			.displayName("Floor Material").description("Block used for the highway floor")
			.defaultValue(HighwayMaterial.OBSIDIAN).build();

	private final IntegerSetting width = IntegerSetting.builder().id("highway_width").displayName("Width")
			.description("Highway width in blocks").defaultValue(1).minValue(1).maxValue(3).step(1).build();

	private final IntegerSetting height = IntegerSetting.builder().id("highway_height").displayName("Height")
			.description("Tunnel clearance in blocks").defaultValue(2).minValue(2).maxValue(3).step(1).build();

	private final FloatSetting delay = FloatSetting.builder().id("highway_delay").displayName("Action Delay")
			.description("Minimum delay between mining and placement actions in milliseconds").defaultValue(50f)
			.minValue(0f).maxValue(1000f).step(10f).build();

	private final BooleanSetting autoWalk = BooleanSetting.builder().id("highway_auto_walk").displayName("Auto Walk")
			.description("Walks only after the next path section is clear and supported").defaultValue(true).build();

	private final BooleanSetting autoJump = BooleanSetting.builder().id("highway_auto_jump").displayName("Auto Jump")
			.description("Jumps when auto-walk reaches an obstruction").defaultValue(true).build();

	private final BooleanSetting placeFloor = BooleanSetting.builder().id("highway_place_floor").displayName("Place Floor")
			.description("Places missing floor blocks before walking").defaultValue(true).build();

	private final BooleanSetting dangerStop = BooleanSetting.builder().id("highway_danger_stop").displayName("Danger Stop")
			.description("Stops the module when a configured danger is nearby").defaultValue(true).build();

	private final FloatSetting dangerRadius = FloatSetting.builder().id("highway_danger_radius")
			.displayName("Danger Radius").description("Entity detection radius in blocks").defaultValue(8f).minValue(2f)
			.maxValue(20f).step(1f).build();

	private final BooleanSetting stopOnPlayers = BooleanSetting.builder().id("highway_stop_players")
			.displayName("Stop On Players").description("Stops when another player enters the danger radius")
			.defaultValue(true).build();

	private final BooleanSetting stopOnHostiles = BooleanSetting.builder().id("highway_stop_hostiles")
			.displayName("Stop On Hostiles").description("Stops when a hostile mob enters the danger radius")
			.defaultValue(true).build();

	private final BooleanSetting stopOnLava = BooleanSetting.builder().id("highway_stop_lava").displayName("Stop On Lava")
			.description("Stops when lava is close to the next path section").defaultValue(true).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder().id("highway_rotation_mode")
			.displayName("Rotation Mode").description("Rotates toward mining and placement targets")
			.defaultValue(RotationMode.INSTANT).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("highway_fake_rotation")
			.displayName("Fake Rotation").description("Sends interaction rotations without moving the camera")
			.defaultValue(true).build();

	private final MiningController miner = MiningController.create(MC);
	private HighwayDirection activeDirection = HighwayDirection.NORTH;
	private boolean ownsRotationGoal;
	private long lastPlacementTime;

	public Highway() {
		super("Highway");
		setCategory(Category.of("World"));
		setDescription("Clears and builds a directional highway with automatic danger stops.");

		addSetting(direction);
		addSetting(material);
		addSetting(width);
		addSetting(height);
		addSetting(delay);
		addSetting(autoWalk);
		addSetting(autoJump);
		addSetting(placeFloor);
		addSetting(dangerStop);
		addSetting(dangerRadius);
		addSetting(stopOnPlayers);
		addSetting(stopOnHostiles);
		addSetting(stopOnLava);
		addSetting(rotationMode);
		addSetting(fakeRotation);

		setDetectable(AntiCheat.NoCheatPlus, AntiCheat.Vulcan, AntiCheat.AdvancedAntiCheat, AntiCheat.Verus,
				AntiCheat.Grim, AntiCheat.Matrix, AntiCheat.Negativity, AntiCheat.Karhu);
	}

	@Override
	public void onEnable() {
		activeDirection = direction.getValue();
		lastPlacementTime = 0;
		miner.resetTiming();
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onDisable() {
		MC.options.keyUp.setDown(false);
		miner.reset();
		clearRotation();
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onTick(Pre event) {
		MC.options.keyUp.setDown(false);
		if (MC.player == null || MC.level == null || MC.gameMode == null) {
			miner.reset();
			clearRotation();
			return;
		}

		String danger = findDanger();
		if (danger != null) {
			stopWithReason(danger);
			return;
		}

		BlockPos ahead = MC.player.blockPosition().relative(activeDirection.direction());
		MiningTarget miningTarget = findMiningTarget(ahead);
		if (!state.getValue()) {
			return;
		}
		if (miningTarget != null) {
			if (!updateRotation(miningTarget.pos())) {
				return;
			}
			boolean immediate = MC.player.isCreative() || miningTarget.destroySpeed() == 0f;
			miner.mine(miningTarget.pos(), getFace(miningTarget.pos()), immediate, delay.getValue().longValue(),
					System.currentTimeMillis());
			return;
		}

		finishMining();
		if (placeFloor.getValue()) {
			BlockPos floor = findMissingFloor(ahead);
			if (floor != null) {
				placeFloor(floor);
				return;
			}
		}

		clearRotation();
		if (autoWalk.getValue() && isPathReady(ahead)) {
			MC.player.setYRot(activeDirection.yaw());
			MC.options.keyUp.setDown(true);
			if (autoJump.getValue() && MC.player.horizontalCollision && MC.player.onGround()) {
				MC.player.jumpFromGround();
			}
		}
	}

	@Override
	public void onTick(Post event) {
	}

	private String findDanger() {
		if (!dangerStop.getValue()) {
			return null;
		}

		double radiusSqr = dangerRadius.getValueSqr();
		for (Entity entity : Aoba.getInstance().entityManager.getEntities()) {
			if (entity == MC.player || entity.isRemoved() || !entity.isAlive()
					|| entity.distanceToSqr(MC.player) > radiusSqr) {
				continue;
			}
			if (stopOnHostiles.getValue() && entity instanceof Monster) {
				return "hostile mob nearby";
			}
			if (stopOnPlayers.getValue() && entity instanceof Player player && !EntityUtils.isFriend(player)) {
				return "player nearby";
			}
		}

		if (stopOnLava.getValue()) {
			BlockPos center = MC.player.blockPosition().relative(activeDirection.direction());
			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
				if (MC.level.getFluidState(pos).is(FluidTags.LAVA)) {
					return "lava near the highway path";
				}
			}
		}
		return null;
	}

	private MiningTarget findMiningTarget(BlockPos ahead) {
		for (int y = 0; y < height.getValue(); y++) {
			for (int lateral = minLateral(width.getValue()); lateral <= maxLateral(width.getValue()); lateral++) {
				BlockPos pos = ahead.relative(activeDirection.direction().getClockWise(), lateral).above(y);
				BlockState state = MC.level.getBlockState(pos);
				if (!state.getFluidState().isEmpty()) {
					stopWithReason("fluid blocking the highway path");
					return null;
				}
				if (state.isAir() || state.canBeReplaced()) {
					continue;
				}
				float destroySpeed = state.getDestroySpeed(MC.level, pos);
				if (destroySpeed < 0) {
					stopWithReason("unbreakable block at " + pos.toShortString());
					return null;
				}
				return new MiningTarget(pos, MC.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)),
						destroySpeed);
			}
		}
		return null;
	}

	static int minLateral(int width) {
		return -width / 2;
	}

	static int maxLateral(int width) {
		return width + minLateral(width) - 1;
	}

	private BlockPos findMissingFloor(BlockPos ahead) {
		for (int lateral = minLateral(width.getValue()); lateral <= maxLateral(width.getValue()); lateral++) {
			BlockPos floor = ahead.relative(activeDirection.direction().getClockWise(), lateral).below();
			BlockState state = MC.level.getBlockState(floor);
			if (state.isAir() || state.canBeReplaced()) {
				return floor;
			}
		}
		return null;
	}

	private void placeFloor(BlockPos floor) {
		BlockPos support = floor.below();
		BlockState supportState = MC.level.getBlockState(support);
		if (!supportState.canOcclude()) {
			stopWithReason("missing solid floor support at " + support.toShortString());
			return;
		}

		FindItemResult item = findInHotbar(stack -> Block.byItem(stack.getItem()) == material.getValue().block());
		if (!item.found()) {
			stopWithReason("no " + material.getValue().name().toLowerCase().replace('_', ' ') + " in the hotbar");
			return;
		}
		if (!updateRotation(floor)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (lastPlacementTime != 0 && now - lastPlacementTime < delay.getValue()) {
			return;
		}

		InteractionHand hand = item.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		boolean swapped = hand == InteractionHand.MAIN_HAND && !item.isMainHand() && swap(item.slot(), true);
		if (hand == InteractionHand.MAIN_HAND && !item.isMainHand() && !swapped) {
			stopWithReason("unable to select the floor material");
			return;
		}

		boolean placed;
		try {
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support,
					false);
			placed = InteractionUtils.interactBlock(hit, hand);
		} finally {
			if (swapped) {
				swapBack();
			}
		}

		if (!placed) {
			stopWithReason("floor placement failed");
			return;
		}
		lastPlacementTime = now;
	}

	private boolean isPathReady(BlockPos ahead) {
		for (int lateral = minLateral(width.getValue()); lateral <= maxLateral(width.getValue()); lateral++) {
			BlockPos column = ahead.relative(activeDirection.direction().getClockWise(), lateral);
			BlockState floor = MC.level.getBlockState(column.below());
			if (floor.isAir() || floor.canBeReplaced()) {
				return false;
			}
			for (int y = 0; y < height.getValue(); y++) {
				BlockState state = MC.level.getBlockState(column.above(y));
				if (!state.getFluidState().isEmpty() || (!state.isAir() && !state.canBeReplaced())) {
					return false;
				}
			}
		}
		return true;
	}

	private void finishMining() {
		if (miner.getTarget() == null) {
			return;
		}
		if (MC.level.getBlockState(miner.getTarget()).isAir()) {
			miner.clear();
		} else {
			miner.reset();
		}
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
				.maxRotation(180f).fakeRotation(fakeRotation.getValue()).build());
		ownsRotationGoal = true;
		return false;
	}

	private void clearRotation() {
		if (ownsRotationGoal) {
			Aoba.getInstance().rotationManager.setGoal(null);
			ownsRotationGoal = false;
		}
	}

	private void stopWithReason(String reason) {
		sendChatMessage("Highway stopped: " + reason + ".");
		state.setValue(false);
	}
}
