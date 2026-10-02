/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.aoba.Aoba;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.events.TickEvent;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.gui.colors.Color;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.EasingFunction;
import net.aoba.managers.rotation.goals.Vec3dGoal;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.ShaderSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.RangeSetting;
import net.aoba.utils.types.Range;
import net.aoba.utils.BlockPlacement;
import net.aoba.utils.FindItemResult;
import net.aoba.utils.entity.DamageUtils;
import net.aoba.utils.entity.TargetPriority;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoCrystal extends Module implements TickListener, Render3DListener {
	private final FloatSetting radius = FloatSetting.builder().id("autocrystal_radius").displayName("Radius")
			.description("Radius, in blocks, that you can place/attack a crystal.").defaultValue(5f).minValue(1f)
			.maxValue(15f).step(1f).build();

	private final FloatSetting placeRadius = FloatSetting.builder().id("autocrystal_place_radius")
			.displayName("Place Radius").description("Radius, in blocks, that you can place/attack a crystal.")
			.defaultValue(5f).minValue(1f).maxValue(15f).step(1f).build();

	private final BooleanSetting targetFriends = BooleanSetting.builder().id("autocrystal_target_friends")
			.displayName("Target Friends").description("Target friends.").defaultValue(false).build();

	private final RangeSetting attackDelay = RangeSetting.builder().id("autocrystal_attack_delay")
			.displayName("Attack Delay").description("Random delay in milliseconds between attacks (min, max).")
			.defaultValue(new Range(400f, 600f)).minValue(0f).maxValue(2000f).step(50f).build();

	private final BooleanSetting autoSwitch = BooleanSetting.builder().id("autocrystal_auto_switch")
			.displayName("Auto Switch").description("Automatically switch to End Crystal.").defaultValue(true).build();

	private final RangeSetting placeDelay = RangeSetting.builder().id("autocrystal_place_delay")
			.displayName("Place Delay").description("Random delay in milliseconds between placing crystals (min, max).")
			.defaultValue(new Range(400f, 600f)).minValue(0f).maxValue(2000f).step(50f).build();

	private final BooleanSetting multiPlace = BooleanSetting.builder().id("autocrystal_multi_place")
			.displayName("MultiPlace").description("Allows placing multiple crystals simultaneously.")
			.defaultValue(false).build();

	private final BooleanSetting antiSuicide = BooleanSetting.builder().id("autocrystal_anti_suicide")
			.displayName("AntiSuicide").description("Prevents attacking crystals if it would result in player's death.")
			.defaultValue(true).build();

	private final FloatSetting minDamage = FloatSetting.builder().id("autocrystal_min_damage").displayName("Min Damage")
			.description("Minimum damage a crystal must deal to be placed or attacked.").defaultValue(6f).minValue(0f)
			.maxValue(36f).step(0.5f).build();

	private final BooleanSetting ignoreWalls = BooleanSetting.builder().id("autocrystal_ignore_walls")
			.displayName("Ignore Walls").description("Ignore walls when targeting enemies.").defaultValue(true).build();

	private final EnumSetting<TargetPriority> targetPriority = EnumSetting.<TargetPriority>builder()
			.id("autocrystal_target_priority").displayName("Target Priority").description("Priority to target players.")
			.defaultValue(TargetPriority.CLOSEST).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("autocrystal_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates.").defaultValue(RotationMode.NONE).build();

	private final BooleanSetting useRaycast = BooleanSetting.builder().id("autocrystal_use_raycast").displayName("Use Raycast")
			.description(
					"Whether a raycast will be used to ensure that the player is aiming at the crystal before attacking it.")
			.defaultValue(true).build();

	private final BooleanSetting attackOnClick = BooleanSetting.builder().id("autocrystal_attack_on_click")
			.displayName("Attack On Click").description("Only attack crystals while left-click is held.")
			.defaultValue(false).build();

	private final BooleanSetting placeOnClick = BooleanSetting.builder().id("autocrystal_place_on_click")
			.displayName("Place On Click").description("Only place crystals while right-click is held.")
			.defaultValue(false).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("autocrystal_max_rotation")
			.displayName("Max Rotation").description("The max speed that Aimbot will rotate").defaultValue(10.0f)
			.minValue(1.0f).maxValue(360.0f).build();

	private final EnumSetting<EasingFunction> easingFunction = EnumSetting.<EasingFunction>builder()
			.id("autocrystal_easing").displayName("Easing")
			.description("Easing curve applied to the rotation speed as it approaches the target.")
			.defaultValue(EasingFunction.SineEaseInOut).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("autocrystal_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("The randomness of the player's yaw").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("autocrystal_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("The randomness of the player's pitch").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("autocrystal_fake_rotation")
			.displayName("Fake Rotation")
			.description("Spoofs the client's rotation so that the player appears rotated on the server")
			.defaultValue(false).build();

	private final BooleanSetting moveFix = BooleanSetting.builder().id("autocrystal_move_fix").displayName("Move Fix")
			.description("Corrects movement to match spoofed rotation by using the server yaw for velocity.")
			.defaultValue(false).build();

	private final FloatSetting maxSelfDamage = FloatSetting.builder().id("autocrystal_max_self_damage")
			.displayName("Max Self Damage")
			.description("Maximum self-damage the player can take from a single crystal.").defaultValue(4f).minValue(0f)
			.maxValue(20f).step(0.5f).build();

	private final FloatSetting enemyRange = FloatSetting.builder().id("autocrystal_enemy_range")
			.displayName("Enemy Range").description("Maximum distance an enemy can be to be considered a target.")
			.defaultValue(12f).minValue(0f).maxValue(32f).step(1f).build();

	private final ShaderSetting color = ShaderSetting.builder().id("autocrystal_color").displayName("Color")
			.description("Color").defaultValue(Shader.solid(new Color(0, 1f, 1f))).build();

	private static final long BOX_DISPLAY_TIME_MS = 3000;

	private long lastAttackTime;
	private long lastPlaceTime;
	private long nextAttackDelayMs;
	private long nextPlaceDelayMs;
	private final Map<BlockPos, Long> displayedBoxes = new HashMap<>();

	public AutoCrystal() {
		super("AutoCrystal");

		setCategory(Category.of("Combat"));
		setDescription("Attacks anything within your personal space with a End Crystal.");

		addSetting(ignoreWalls);
		addSetting(radius);
		addSetting(placeRadius);
		addSetting(targetFriends);
		addSetting(attackDelay);
		addSetting(autoSwitch);
		addSetting(placeDelay);
		addSetting(multiPlace);
		addSetting(antiSuicide);
		addSetting(minDamage);
		addSetting(maxSelfDamage);
		addSetting(enemyRange);
		addSetting(targetPriority);
		addSetting(rotationMode);
		addSetting(useRaycast);
		addSetting(attackOnClick);
		addSetting(placeOnClick);
		addSetting(maxRotation);
		addSetting(easingFunction);
		addSetting(yawRandomness);
		addSetting(pitchRandomness);
		addSetting(fakeRotation);
		addSetting(moveFix);
		addSetting(color);

		lastAttackTime = 0;
		lastPlaceTime = 0;
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
		Aoba.getInstance().rotationManager.setGoal(null);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(Render3DListener.class, this);
		nextAttackDelayMs = (long) attackDelay.randomValue();
		nextPlaceDelayMs = (long) placeDelay.randomValue();
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onTick(TickEvent.Pre event) {
		long currentTime = System.currentTimeMillis();

		// Find a target to attack first before finding a place target.
		EndCrystal attackTarget = pickAttackTarget();
		BlockPlacement placeTarget = (attackTarget == null) ? pickPlaceTarget() : null;

		// Rotate towards either attack target or placement.
		Vec3 rotationGoal = null;
		if (attackTarget != null) {
			rotationGoal = attackTarget.position().add(0, attackTarget.getBoundingBox().getYsize() / 2.0, 0);
		} else if (placeTarget != null) {
			Direction face = placeTarget.placementFace();
			rotationGoal = Vec3.atCenterOf(placeTarget.placementPos()).add(face.getStepX() * 0.5, face.getStepY() * 0.5,
					face.getStepZ() * 0.5);
		}

		// Set rotation goal.
		if (rotationGoal != null) {
			Aoba.getInstance().rotationManager.setGoal(Vec3dGoal.builder().goal(rotationGoal)
					.mode(rotationMode.getValue()).maxRotation(maxRotation.getValue())
					.pitchRandomness(pitchRandomness.getValue()).yawRandomness(yawRandomness.getValue())
					.fakeRotation(fakeRotation.getValue()).moveFix(moveFix.getValue()).easingFunction(easingFunction.getValue()).build());
		} else {
			Aoba.getInstance().rotationManager.setGoal(null);
		}

		// Attack the crystal if one is in range and the cooldown has elapsed.
		boolean canAttack = !attackOnClick.getValue() || MC.options.keyAttack.isDown();
		if (canAttack && attackTarget != null
				&& (nextAttackDelayMs <= 0 || currentTime - lastAttackTime >= nextAttackDelayMs)) {
			attackCrystal(attackTarget);
			lastAttackTime = currentTime;
			nextAttackDelayMs = (long) attackDelay.randomValue();
			return;
		}

		// Otherwise, place a new crystal if there's a valid spot and the cooldown has
		// elapsed.
		boolean canPlace = !placeOnClick.getValue() || MC.options.keyUse.isDown();
		if (canPlace && placeTarget != null
				&& (nextPlaceDelayMs <= 0 || currentTime - lastPlaceTime >= nextPlaceDelayMs)) {
			placeCrystal(placeTarget);
			lastPlaceTime = currentTime;
			nextPlaceDelayMs = (long) placeDelay.randomValue();
			return;
		}
	}

	@Override
	public void onTick(TickEvent.Post event) {

	}

	/**
	 * Finds the best placement to place an end crystal based on the target
	 * priority.
	 * 
	 * @return Placement record with the obsidian/bedrock support and the click
	 *         face.
	 */
	private BlockPlacement pickPlaceTarget() {
		// Skip if no end crystal is found in inventory.
		FindItemResult result = find(Items.END_CRYSTAL);
		if (!result.found() || !result.isHotbar())
			return null;

		for (Player player : Aoba.getInstance().entityManager.getPlayers()) {
			if (player == MC.player || MC.player.distanceToSqr(player) > radius.getValueSqr())
				continue;
			if (!targetFriends.getValue() && Aoba.getInstance().friendsList.contains(player))
				continue;

			// Find ideal placement for the player.
			Optional<BlockPlacement> candidate = findPlacement(player);
			if (candidate.isEmpty())
				continue;

			// Calculate damage and skip if below minimum damage.
			BlockPlacement placement = candidate.get();
			double damage = DamageUtils.crystalDamage(player,
					Vec3.atLowerCornerOf(placement.placementPos().offset(0, 1, 0)));
			if (damage < minDamage.getValue())
				continue;

			return placement;
		}
		return null;
	}

	/**
	 * Places an end crystal at a given placement.
	 * 
	 * @param placement Pre-computed placement (support block + click face).
	 */
	private void placeCrystal(BlockPlacement placement) {
		// Skip if there are no end crystal in the hotbar.
		FindItemResult result = find(Items.END_CRYSTAL);
		if (!result.found() || !result.isHotbar())
			return;

		BlockPos placePos = placement.placementPos();
		Direction clickFace = placement.placementFace();

		Vec3 hitPos = Vec3.atCenterOf(placePos).add(clickFace.getStepX() * 0.5, clickFace.getStepY() * 0.5,
				clickFace.getStepZ() * 0.5);

		// Check if raycast is over the placement position.
		BlockHitResult hit;
		if (useRaycast.getValue()) {
			hit = InteractionUtils.raycastBlock(placePos, clickFace);
		} else
			hit = new BlockHitResult(hitPos, clickFace, placePos, false);

		if (hit == null)
			return;

		// Swap to the end crystal if enabled.
		if (autoSwitch.getValue())
			swap(result.slot(), false);

		// Place the crystal
		if (multiPlace.getValue() && !useRaycast.getValue()) {
			performMultiPlace(placePos);
		} else {
			SwingAnimation swingAnimation = MC.player.getMainHandItem().getInteractAnimation();
			InteractionResult useResult = MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, hit);
			if (useResult instanceof InteractionResult.Success success
					&& success.swingSource() == InteractionResult.SwingSource.PREDICTED)
				MC.player.swing(InteractionHand.MAIN_HAND, swingAnimation, false);
		}

		displayedBoxes.put(placePos, System.currentTimeMillis());
	}

	/**
	 * Places multiple crystals per tick around a given position.
	 * 
	 * @param initialPos Position to place.
	 */
	private void performMultiPlace(BlockPos initialPos) {
		BlockPos[] positions = { initialPos, initialPos.above(), initialPos.east(), initialPos.west(),
				initialPos.north(), initialPos.south() };

		for (BlockPos pos : positions) {
			Direction clickFace = InteractionUtils.getClosestDirection(pos);
			if (clickFace == null)
				continue;
			Vec3 hitPos = Vec3.atCenterOf(pos).add(clickFace.getStepX() * 0.5, clickFace.getStepY() * 0.5,
					clickFace.getStepZ() * 0.5);
			BlockHitResult hitResult = new BlockHitResult(hitPos, clickFace, pos, false);
			SwingAnimation swingAnimation = MC.player.getMainHandItem().getInteractAnimation();
			InteractionResult useResult = MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, hitResult);
			if (useResult instanceof InteractionResult.Success success
					&& success.swingSource() == InteractionResult.SwingSource.PREDICTED)
				MC.player.swing(InteractionHand.MAIN_HAND, swingAnimation, false);
		}
	}

	/**
	 * Finds the best ender crystal placement position based from a player
	 * 
	 * @param player Targetted player to place the crystal next.
	 * @return Placement record (support block + click face) if a valid spot exists.
	 */
	private Optional<BlockPlacement> findPlacement(Player player) {
		BlockPos playerPos = player.blockPosition();
		double maxDamage = 0;
		double minDistance = Double.MAX_VALUE;
		BlockPos bestPos = null;

		int placeExtent = placeRadius.getValue().intValue();
		for (int x = -placeExtent; x <= placeExtent; x++) {
			for (int y = -placeExtent; y <= placeExtent; y++) {
				for (int z = -placeExtent; z <= placeExtent; z++) {
					BlockPos pos = playerPos.offset(x, y, z);
					BlockState blockState = MC.level.getBlockState(pos);
					Block block = blockState.getBlock();
					if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK)
						continue;

					// Ensure the block above is air
					BlockPos abovePos = pos.above();
					BlockState aboveBlockState = MC.level.getBlockState(abovePos);
					if (!aboveBlockState.isAir())
						continue;

					// Ensure there is air two blocks above the potential placement
					BlockPos abovePos2 = pos.above(2);
					BlockState aboveBlockState2 = MC.level.getBlockState(abovePos2);
					if (!aboveBlockState2.isAir())
						continue;

					if (pos.getY() > playerPos.getY() + 1)
						continue;

					// Skip if the position is behind a wall.
					if (!ignoreWalls.getValue() && InteractionUtils.isBehindWall(pos))
						continue;

					double damage = DamageUtils.crystalDamage(player, Vec3.atLowerCornerOf(pos));
					double distance = playerPos.distSqr(pos);

					if (pos.getY() == playerPos.getY()) {
						damage *= 1.5;
					}

					if (damage > maxDamage || (damage == maxDamage && distance < minDistance)) {
						maxDamage = damage;
						minDistance = distance;
						bestPos = pos;
					}
				}
			}
		}

		if (bestPos == null)
			return Optional.empty();

		Direction clickFace = InteractionUtils.getClosestDirection(bestPos);
		if (clickFace == null)
			return Optional.empty();

		return Optional.of(new BlockPlacement(bestPos, bestPos, clickFace));
	}

	/**
	 * Gets the best end crystal to attack based on the target priority.
	 * 
	 * @return End Crystal to attack.
	 */
	private EndCrystal pickAttackTarget() {
		double maxSelfDamageAllowed = maxSelfDamage.getValue();
		double minDamageRequired = minDamage.getValue();
		double rangeSqr = enemyRange.getValueSqr();

		EndCrystal chosenCrystal = null;
		double chosenDamage = 0;
		double chosenDistanceSq = Double.MAX_VALUE;
		double chosenHealth = Double.MAX_VALUE;

		for (Entity entity : Aoba.getInstance().entityManager.getEntities()) {
			if (!(entity instanceof EndCrystal crystal))
				continue;

			// Skip if beyond radius.
			double crystalDistanceSq = MC.player.distanceToSqr(crystal);
			if (crystalDistanceSq >= rangeSqr)
				continue;

			// Skip if behind a wall.
			if (!ignoreWalls.getValue() && InteractionUtils.isBehindWall(crystal))
				continue;

			// Skip if the crystal will kill the player.
			if (antiSuicide.getValue()
					&& DamageUtils.crystalDamage(MC.player, crystal.position()) > maxSelfDamageAllowed)
				continue;

			for (Player player : MC.level.players()) {
				if (player == MC.player
						|| (!targetFriends.getValue() && Aoba.getInstance().friendsList.contains(player)))
					continue;

				// Skip if it does not meet the minimum crystal damage.
				double damage = DamageUtils.crystalDamage(player, crystal.position());
				if (damage < minDamageRequired)
					continue;

				double distanceToPlayerSqr = MC.player.distanceToSqr(player);
				double playerHealth = player.getHealth();

				// Find the best crystal
				boolean isCrystalBetter;
				switch (targetPriority.getValue()) {
				case CLOSEST:
					isCrystalBetter = chosenCrystal == null || distanceToPlayerSqr < chosenDistanceSq;
					break;
				case HIGHEST_DAMAGE:
					isCrystalBetter = chosenCrystal == null || damage > chosenDamage;
					break;
				case LOWEST_HEALTH:
					isCrystalBetter = chosenCrystal == null || playerHealth < chosenHealth;
					break;
				case MOST_HEALTH:
					isCrystalBetter = chosenCrystal == null || playerHealth > chosenHealth;
					break;
				default:
					isCrystalBetter = false;
					break;
				}

				if (isCrystalBetter) {
					chosenCrystal = crystal;
					chosenDamage = damage;
					chosenDistanceSq = distanceToPlayerSqr;
					chosenHealth = playerHealth;
				}
			}
		}

		return chosenCrystal;
	}

	/**
	 * Attacks an end crystal.
	 * 
	 * @param target End Crystal target.
	 */
	private void attackCrystal(EndCrystal target) {
		// Verify our look direction actually intersects the chosen crystal.
		if (useRaycast.getValue()) {
			double reach = MC.player.entityInteractionRange();
			Vec3 eyePos = MC.player.getEyePosition();
			Vec3 lookVec = MC.player.getViewVector(1.0F);
			Vec3 lookEnd = eyePos.add(lookVec.scale(reach));
			AABB searchBox = MC.player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0, 1.0, 1.0);
			EntityHitResult hit = ProjectileUtil.getEntityHitResult(MC.player, eyePos, lookEnd, searchBox,
					e -> e == target, eyePos.distanceToSqr(lookEnd));
			if (hit == null || hit.getEntity() != target)
				return;
		}

		InteractionUtils.attack(target);
	}

	@Override
	public void onRender(Render3DEvent event) {
		long currentTime = System.currentTimeMillis();

		displayedBoxes.entrySet().removeIf(entry -> {
			long renderTime = entry.getValue();
			return currentTime - renderTime >= BOX_DISPLAY_TIME_MS;
		});

		for (Map.Entry<BlockPos, Long> entry : displayedBoxes.entrySet()) {
			BlockPos pos = entry.getKey();
			event.getRenderer().drawBox(new AABB(pos), color.getValue(), 1.0f);
		}
	}
}
