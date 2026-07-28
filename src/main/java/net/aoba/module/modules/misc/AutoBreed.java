/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.misc;

import java.util.HashMap;
import java.util.Map;

import net.aoba.Aoba;
import net.aoba.event.events.BreedEvent;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.BreedListener;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.gui.colors.Color;
import net.aoba.managers.rotation.RotationMode;
import net.aoba.managers.rotation.goals.EntityGoal;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.ColorSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.utils.FindItemResult;
import net.aoba.utils.entity.BodyPart;
import net.aoba.utils.entity.EntityUtils;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

public class AutoBreed extends Module implements TickListener, Render3DListener, BreedListener {

	private static final long BREED_COOLDOWN = 6000;

	private final FloatSetting radius = FloatSetting.builder().id("autobreed_radius").displayName("Radius")
			.description("Radius that AutoBreed will trigger on Mobs.").defaultValue(3f).minValue(0.1f).maxValue(10f)
			.step(0.1f).build();

	private final BooleanSetting useRaycast = BooleanSetting.builder().id("autobreed_use_raycast")
			.displayName("Use Raycast").description("Whether a raycast will be used when interacting with animals.")
			.defaultValue(false).build();

	private final ColorSetting highlightColor = ColorSetting.builder().id("autobreed_highlight_color")
			.displayName("Highlight Color").description("Color of the highlight shown over animals that have been fed.")
			.defaultValue(new Color(1f, 0.45f, 0.8f, 1f)).build();

	private final EnumSetting<RotationMode> rotationMode = EnumSetting.<RotationMode>builder()
			.id("autobreed_rotation_mode").displayName("Rotation Mode")
			.description("Controls how the player's view rotates.").defaultValue(RotationMode.NONE).build();

	private final FloatSetting maxRotation = FloatSetting.builder().id("autobreed_max_rotation")
			.displayName("Max Rotation").description("The max speed that AutoBreed will rotate").defaultValue(10.0f)
			.minValue(1.0f).maxValue(360.0f).build();

	private final FloatSetting yawRandomness = FloatSetting.builder().id("autobreed_yaw_randomness")
			.displayName("Yaw Rotation Jitter").description("The randomness of the player's yaw").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final FloatSetting pitchRandomness = FloatSetting.builder().id("autobreed_pitch_randomness")
			.displayName("Pitch Rotation Jitter").description("The randomness of the player's pitch").defaultValue(0.0f)
			.minValue(0.0f).maxValue(10.0f).step(0.1f).build();

	private final Map<Integer, Long> entityTimerMap = new HashMap<>();

	private EntityGoal currentGoal;

	public AutoBreed() {
		super("AutoBreed");

		setCategory(Category.of("Misc"));
		setDescription("Automatically breeds animals that are near you.");

		addSetting(radius);
		addSetting(useRaycast);
		addSetting(highlightColor);
		addSetting(rotationMode);
		addSetting(maxRotation);
		addSetting(yawRandomness);
		addSetting(pitchRandomness);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(BreedListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
		reset();
		entityTimerMap.clear();
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(BreedListener.class, this);
		Aoba.getInstance().eventManager.AddListener(Render3DListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onTick(Pre event) {

	}

	@Override
	public void onTick(Post event) {
		if (MC.player == null || MC.level == null)
			return;

		long now = MC.level.getGameTime();

		// Remove any entities whose cooldown is above the threshold.
		entityTimerMap.entrySet()
				.removeIf(e -> now - e.getValue() >= BREED_COOLDOWN || MC.level.getEntity(e.getKey()) == null);

		Animal foundEntity = null;
		double closestDistSqr = Double.MAX_VALUE;
		FindItemResult foundFood = null;
		for (Entity entity : Aoba.getInstance().entityManager.getEntities()) {
			if (!(entity instanceof Animal animal))
				continue;

			// Skip entities that are babies, dying, or currently on cooldown.
			if (animal.isBaby() || !animal.isAlive() || !canBreed(animal, now))
				continue;

			// Skip any outside of the players radius.
			double distSqr = MC.player.distanceToSqr(animal);
			if (distSqr > radius.getValueSqr() || distSqr >= closestDistSqr)
				continue;

			// Try to find a food that matches the animals food predicate.
			FindItemResult food = findInHotbar(animal::isFood);
			if (!food.found())
				continue;

			// Track entity.
			closestDistSqr = distSqr;
			foundEntity = animal;
			foundFood = food;
		}

		// Clear goal is no entity was found.
		if (foundEntity == null) {
			reset();
			return;
		}

		// Set the rotation goal.
		currentGoal = EntityGoal.builder().goal(foundEntity).mode(rotationMode.getValue())
				.maxRotation(maxRotation.getValue()).pitchRandomness(pitchRandomness.getValue())
				.yawRandomness(yawRandomness.getValue()).build();
		Aoba.getInstance().rotationManager.setGoal(currentGoal);

		// Swap to the food item.
		if (!swap(foundFood.slot(), false))
			return;

		InteractionHand hand = foundFood.getHand();
		if (hand == null)
			return;

		// Try and raycast if needed.
		EntityHitResult hitResult = useRaycast.getValue()
				? InteractionUtils.raycastEntity(foundEntity, radius.getValue())
				: new EntityHitResult(foundEntity, EntityUtils.getBodyPartPosition(foundEntity, BodyPart.CHEST, 1.0f));
		if (hitResult == null)
			return;

		// Feed animal
		MC.player.swing(hand);
		MC.gameMode.interact(MC.player, foundEntity, hitResult, hand);
		entityTimerMap.put(foundEntity.getId(), now);
	}

	@Override
	public void onBreed(BreedEvent entityEvent) {
		if (MC.level == null)
			return;

		if (entityEvent.getEntity() instanceof Animal animal)
			entityTimerMap.put(animal.getId(), MC.level.getGameTime());
	}

	@Override
	public void onRender(Render3DEvent event) {
		if (MC.level == null)
			return;

		long now = MC.level.getGameTime();
		Color base = highlightColor.getValue();

		for (Map.Entry<Integer, Long> entry : entityTimerMap.entrySet()) {
			Entity entity = MC.level.getEntity(entry.getKey());
			if (entity == null)
				continue;

			// Lerp color opacity and render model.
			float fade = 1.0f - ((now - entry.getValue()) / (float) BREED_COOLDOWN);
			Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(),
					base.getAlpha() * Mth.clamp(fade, 0f, 1f));
			event.getRenderer().drawEntityModel(entity, Shader.solid(color));
		}
	}

	private boolean canBreed(Entity entity, long now) {
		Long fedAt = entityTimerMap.get(entity.getId());
		if (fedAt != null) {
			long timeSince = now - fedAt;
			return timeSince >= BREED_COOLDOWN;
		}
		return true;
	}

	private void reset() {
		if (currentGoal == null)
			return;

		if (Aoba.getInstance().rotationManager.getGoal() == currentGoal)
			Aoba.getInstance().rotationManager.setGoal(null);

		currentGoal = null;
	}
}
