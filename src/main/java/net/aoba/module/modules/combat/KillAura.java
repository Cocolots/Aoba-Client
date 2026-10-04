/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.combat;

import java.util.ArrayList;
import java.util.Set;
import net.aoba.Aoba;
import net.aoba.event.events.SubtickEvent;
import net.aoba.event.events.TickEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.listeners.SubtickListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.EasingFunction;
import net.aoba.managers.rotation.goals.EntityGoal;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EntitiesSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.RangeSetting;
import net.aoba.utils.entity.BodyPart;
import net.aoba.utils.entity.EntityUtils;
import net.aoba.utils.entity.TargetPriority;
import net.aoba.utils.player.InteractionUtils;
import net.aoba.utils.types.Range;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KillAura extends Module implements SubtickListener, TickListener {
	private final EnumSetting<TargetPriority> targetPriority = EnumSetting.<TargetPriority>builder()
			.id("killaura_target_priority").displayName("Target Priority")
			.description("The priority used to pick which target to attack.").defaultValue(TargetPriority.CLOSEST)
			.build();

	private final FloatSetting radius = FloatSetting.builder().id("killaura_radius").displayName("Radius")
			.description("Radius that KillAura will target entities.").defaultValue(5f).minValue(0.1f).maxValue(10f)
			.step(0.1f).build();

	private final FloatSetting fov = FloatSetting.builder().id("killaura_fov").displayName("FOV")
			.description("Angular cone in front of the player that targets must fall within.").defaultValue(360.0f)
			.minValue(1.0f).maxValue(360.0f).step(1.0f).build();

	private final EnumSetting<BodyPart> bodyPart = EnumSetting.<BodyPart>builder().id("killaura_body_part")
			.displayName("Body Part").description("The part of the target's body to aim at.")
			.defaultValue(BodyPart.HEAD).build();

	private final EntitiesSetting targetEntities = EntitiesSetting.builder().id("killaura_target_entities")
			.displayName("Target Entities")
			.description("Entity types that KillAura will target.")
			.defaultValue(Set.of(EntityTypes.PLAYER)).build();

	private final BooleanSetting targetFriends = BooleanSetting.builder().id("killaura_target_friends")
			.displayName("Target Friends").description("Target Friends.").defaultValue(false).build();

	private final BooleanSetting ignoreDead = BooleanSetting.builder().id("killaura_ignore_dead")
			.displayName("Ignore Dead").description("Skip entities that are dead or dying.").defaultValue(true).build();

	private final BooleanSetting ignoreInvisible = BooleanSetting.builder().id("killaura_ignore_invisible")
			.displayName("Ignore Invisible").description("Skip entities that are invisible.").defaultValue(true)
			.build();

	private final BooleanSetting ignoreSleeping = BooleanSetting.builder().id("killaura_ignore_sleeping")
			.displayName("Ignore Sleeping").description("Skip players that are sleeping.").defaultValue(true).build();

	private final BooleanSetting ignoreNPCs = BooleanSetting.builder().id("killaura_ignore_npcs")
			.displayName("Ignore NPCs").description("Attempts to ignore NPCs based on the entity UUID.")
			.defaultValue(true).build();

	private final RangeSetting attackDelay = RangeSetting.builder().id("killaura_attack_delay")
			.displayName("Attack Delay").description("Random delay in milliseconds between attacks (min, max).")
			.defaultValue(new Range(50f, 150f)).minValue(0f).maxValue(1000f).step(10f).build();

	private final BooleanSetting useRaycast = BooleanSetting.builder().id("killaura_use_raycast").displayName("Use Raycast")
			.description(
					"Whether a raycast will be used to ensure that KillAura will not hit a player outside of the view")
			.defaultValue(true).build();

	private final BooleanSetting triggerOnClick = BooleanSetting.builder().id("killaura_trigger_on_click")
			.displayName("Trigger On Click").description("Only attack while the attack key is held down.")
			.defaultValue(false).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("killaura_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates.").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("killaura_max_rotation")
			.displayName("Max Rotation").description("The max speed that KillAura will rotate").defaultValue(10.0f)
			.minValue(1.0f).maxValue(360.0f).build();

	private final EnumSetting<EasingFunction> easingFunction = EnumSetting.<EasingFunction>builder()
			.id("killaura_easing").displayName("Easing")
			.description("Easing curve applied to the rotation speed as it approaches the target.")
			.defaultValue(EasingFunction.SineEaseInOut).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("killaura_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("The randomness of the player's yaw").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("killaura_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("The randomness of the player's pitch").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final BooleanSetting fakeRotation = BooleanSetting.builder().id("killaura_fake_rotation")
			.displayName("Fake Rotation")
			.description("Spoofs the client's rotation so that the player appears rotated on the server")
			.defaultValue(false).build();

	private final BooleanSetting moveFix = BooleanSetting.builder().id("killaura_move_fix").displayName("Move Fix")
			.description("Corrects movement to match spoofed rotation by using the server yaw for velocity.")
			.defaultValue(false).build();

	private LivingEntity entityToAttack;
	private float timeSinceAttackMs;
	private float nextAttackDelayMs;

	public KillAura() {
		super("KillAura");

		setCategory(Category.of("Combat"));
		setDescription("Attacks anything within your personal space.");

		addSetting(targetPriority);
		addSetting(attackDelay);
		addSetting(radius);
		addSetting(fov);
		addSetting(bodyPart);
		addSetting(targetEntities);
		addSetting(targetFriends);
		addSetting(ignoreDead);
		addSetting(ignoreInvisible);
		addSetting(ignoreSleeping);
		addSetting(ignoreNPCs);
		addSetting(useRaycast);
		addSetting(triggerOnClick);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(easingFunction);
		addSetting(yawRandomness);
		addSetting(pitchRandomness);
		addSetting(fakeRotation);
		addSetting(moveFix);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(SubtickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().rotationManager.setGoal(null);
		entityToAttack = null;
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(SubtickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		timeSinceAttackMs = 0f;
		nextAttackDelayMs = attackDelay.randomValue();
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onSubtick(SubtickEvent event) {
		timeSinceAttackMs += event.getDelta();

		float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 playerPos = MC.player.getPosition(partialTick);

		ArrayList<LivingEntity> hitList = new ArrayList<LivingEntity>();
		entityToAttack = null;

		float radiusSqr = radius.getValueSqr();
		Set<EntityType<?>> allowed = targetEntities.getValue();
		if (!allowed.isEmpty()) {
			for (Entity entity : Aoba.getInstance().entityManager.getEntities()) {
				if (entity instanceof LivingEntity living) {
					if (entity == MC.player)
						continue;

					if (!allowed.contains(entity.getType()))
						continue;

					if (playerPos.distanceToSqr(entity.getPosition(partialTick)) > radiusSqr)
						continue;

					if (!shouldTarget(living, partialTick))
						continue;

					hitList.add(living);
				}
			}
		}

		for (LivingEntity entity : hitList) {
			if (entityToAttack == null) {
				entityToAttack = entity;
			} else if (targetPriority.getValue() == TargetPriority.LOWEST_HEALTH) {
				if (entity.getHealth() <= entityToAttack.getHealth()) {
					entityToAttack = entity;
				}
			} else if (targetPriority.getValue() == TargetPriority.MOST_HEALTH) {
				if (entity.getHealth() >= entityToAttack.getHealth()) {
					entityToAttack = entity;
				}
			} else if (playerPos.distanceToSqr(entity.getPosition(partialTick)) <= playerPos
					.distanceToSqr(entityToAttack.getPosition(partialTick))) {
				entityToAttack = entity;
			}
		}

		if (entityToAttack != null) {
			EntityGoal rotation = EntityGoal.builder().goal(entityToAttack).mode(rotationMode.getValue())
					.maxRotation(maxRotation.getValue()).pitchRandomness(pitchRandomness.getValue())
					.yawRandomness(yawRandomness.getValue()).fakeRotation(fakeRotation.getValue())
					.moveFix(moveFix.getValue()).bodyPart(bodyPart.getValue()).easingFunction(easingFunction.getValue()).build();
			Aoba.getInstance().rotationManager.setGoal(rotation);
		} else {
			Aoba.getInstance().rotationManager.setGoal(null);
		}
	}

	@Override
	public void onTick(TickEvent.Pre event) {
		if (triggerOnClick.getValue() && !MC.options.keyAttack.isDown())
			return;

		LivingEntity target = entityToAttack;
		if (target == null)
			return;
		if (target.isRemoved() || !target.isAlive())
			return;

		if (MC.player.getAttackStrengthScale(0) != 1 || timeSinceAttackMs < nextAttackDelayMs)
			return;

		boolean attacked = false;
		if (useRaycast.getValue()) {
			HitResult ray = InteractionUtils.raycastEntity(target, radius.getValue());

			if (ray != null && ray.getType() == HitResult.Type.ENTITY) {
				EntityHitResult entityResult = (EntityHitResult) ray;
				Entity ent = entityResult.getEntity();
				InteractionUtils.attack(ent);
				attacked = true;
			}
		} else {
			InteractionUtils.attack(target);
			attacked = true;
		}

		if (attacked) {
			timeSinceAttackMs = 0f;
			nextAttackDelayMs = attackDelay.randomValue();
		}
	}

	@Override
	public void onTick(Post event) {

	}

	/**
	 * Whether the KillAura will target a specific entity based on the current
	 * settings.
	 * 
	 * @param entity Entity to target.
	 * @return True if the target can be targeted, false otherwise.
	 */
	private boolean shouldTarget(LivingEntity entity, float partialTick) {
		if (!EntityUtils.isInFOV(entity, bodyPart.getValue(), fov.getValue(), partialTick))
			return false;

		if (ignoreDead.getValue() && !entity.isAlive())
			return false;

		if (ignoreInvisible.getValue() && entity.isInvisible())
			return false;

		if (ignoreSleeping.getValue() && entity.isSleeping())
			return false;

		if (entity instanceof Player player) {
			if (!targetFriends.getValue() && EntityUtils.isFriend(player))
				return false;
			if (ignoreNPCs.getValue() && EntityUtils.isNPC(player))
				return false;
		}
		return true;
	}
}
