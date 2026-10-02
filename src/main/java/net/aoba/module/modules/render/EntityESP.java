/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.render;

import java.util.HashSet;
import java.util.Set;

import net.aoba.Aoba;
import net.aoba.event.events.Render3DEvent;
import net.aoba.event.listeners.Render3DListener;
import net.aoba.gui.colors.Color;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.types.EntitiesSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.ShaderSetting;
import net.aoba.utils.entity.EntityUtils;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityESP extends Module implements Render3DListener {
	public enum DrawMode {
		BoundingBox, Model
	}

	private final EnumSetting<DrawMode> drawMode = EnumSetting.<DrawMode>builder().id("entityesp_draw_mode")
			.displayName("Draw Mode").description("Draw Mode").defaultValue(DrawMode.Model).build();

	private final ShaderSetting color_friend = ShaderSetting.builder().id("entityesp_color_friend")
			.displayName("Friend Color").description("Color used for players in your friends list.")
			.defaultValue(Shader.solid(new Color(0f, 1f, 0f, 1f))).build();

	private final ShaderSetting color_enemy = ShaderSetting.builder().id("entityesp_color_enemy")
			.displayName("Enemy Color").description("Color used for players not in your friends list.")
			.defaultValue(Shader.solid(new Color(1f, 1f, 0f, 1f))).build();

	private final ShaderSetting color_animal = ShaderSetting.builder().id("entityesp_color_animal")
			.displayName("Animal Color").description("Color used for passive animals.")
			.defaultValue(Shader.solid(new Color(0f, 1f, 0f, 0.3f))).build();

	private final ShaderSetting color_monster = ShaderSetting.builder().id("entityesp_color_monster")
			.displayName("Monster Color").description("Color used for hostile mobs.")
			.defaultValue(Shader.solid(new Color(1f, 0f, 0f, 0.3f))).build();

	private final ShaderSetting color_other = ShaderSetting.builder().id("entityesp_color_other")
			.displayName("Other Color").description("Color used for any other living entity.")
			.defaultValue(Shader.solid(new Color(0f, 0f, 1f, 0.3f))).build();

	private final EntitiesSetting showEntities = EntitiesSetting.builder().id("entityesp_show_entities")
			.displayName("Show Entities")
			.description("Entity types that EntityESP will draw. Color is still picked per category.")
			.defaultValue(defaultEntitySet()).build();

	private final FloatSetting lineThickness = FloatSetting.builder().id("entityesp_linethickness")
			.displayName("Line Thickness").description("Adjust the thickness of the ESP box lines").defaultValue(2f)
			.minValue(0f).maxValue(5f).step(0.1f).build();

	public EntityESP() {
		super("EntityESP");
		setCategory(Category.of("Render"));
		setDescription("Allows the player to see entities with an ESP.");

		addSetting(drawMode);
		addSetting(showEntities);
		addSetting(color_friend);
		addSetting(color_enemy);
		addSetting(color_animal);
		addSetting(color_monster);
		addSetting(color_other);
		addSetting(lineThickness);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(Render3DListener.class, this);
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onRender(Render3DEvent event) {
		float partialTicks = event.getRenderer().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Frustum frustum = event.getRenderer().getFrustum();
		Vec3 cameraPosition = event.getRenderer().getCamera().position();

		for (Entity entity : Aoba.getInstance().entityManager.getEntities()) {
			if (entity == MC.player)
				continue;
			
			if (entity instanceof LivingEntity) {
				if (!MC.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraPosition.x(), cameraPosition.y(), cameraPosition.z(), partialTicks))
					continue;

				Shader effect = getColorForEntity(entity);
				if (effect != null) {
					switch (drawMode.getValue()) {
					case BoundingBox:
						double interpolatedX = Mth.lerp(partialTicks, entity.xo, entity.getX());
						double interpolatedY = Mth.lerp(partialTicks, entity.yo, entity.getY());
						double interpolatedZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

						AABB boundingBox = entity.getBoundingBox().move(interpolatedX - entity.getX(),
								interpolatedY - entity.getY(), interpolatedZ - entity.getZ());
						event.getRenderer().drawBox(boundingBox, effect, lineThickness.getValue());
						break;
					case Model:
						event.getRenderer().drawEntityModel(entity, effect);
						break;
					}
				}
			}
		}
	}

	private Shader getColorForEntity(Entity entity) {
		if (!showEntities.getValue().contains(entity.getType()))
			return null;

		if (entity instanceof Player player)
			return EntityUtils.isFriend(player) ? color_friend.getValue() : color_enemy.getValue();
		if (entity instanceof Animal)
			return color_animal.getValue();
		if (entity instanceof Enemy)
			return color_monster.getValue();
		return color_other.getValue();
	}

	private static Set<EntityType<?>> defaultEntitySet() {
		HashSet<EntityType<?>> set = new HashSet<>();
		set.add(EntityTypes.PLAYER);
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type.getCategory() != MobCategory.MISC)
				set.add(type);
		}
		return set;
	}
}
