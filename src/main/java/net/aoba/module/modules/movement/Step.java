/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.movement;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends Module implements TickListener {

	private final FloatSetting stepHeight = FloatSetting.builder().id("step_height").displayName("Height")
			.description("Height that the player will step up.").defaultValue(1f).minValue(0f).maxValue(2f).step(0.5f)
			.build();

	public Step() {
		super("Step");
		setCategory(Category.of("Movement"));
		setDescription("Steps up blocks.");

		stepHeight.addOnUpdate((i) -> {
			if (state.getValue() && MC != null && MC.player != null) {
				AttributeInstance attribute = MC.player.getAttribute(Attributes.STEP_HEIGHT);
				attribute.setBaseValue(stepHeight.getValue());
			}
		});

		addSetting(stepHeight);

		setDetectable(
		    AntiCheat.NoCheatPlus,
		    AntiCheat.Vulcan,
		    AntiCheat.AdvancedAntiCheat,
		    AntiCheat.Verus,
		    AntiCheat.Grim,
		    AntiCheat.Matrix,
		    AntiCheat.Negativity,
		    AntiCheat.Karhu,
		    AntiCheat.Buzz
		);
	}

	@Override
	public void onDisable() {
		if (MC != null && MC.player != null) {
			AttributeInstance attribute = MC.player.getAttribute(Attributes.STEP_HEIGHT);
			attribute.setBaseValue(0.5f);
		}
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
	}

	@Override
	public void onEnable() {
		if (MC != null && MC.player != null) {
			AttributeInstance attribute = MC.player.getAttribute(Attributes.STEP_HEIGHT);
			attribute.setBaseValue(stepHeight.getValue());
		}
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onTick(Pre event) {
		if (MC != null && MC.player != null) {
			AttributeInstance attribute = MC.player.getAttribute(Attributes.STEP_HEIGHT);
			attribute.setBaseValue(stepHeight.getValue());
		}
	}

	@Override
	public void onTick(Post event) {
	}

	@Override
	public void onToggle() {

	}

	public float getStepHeight() {
		return stepHeight.getValue();
	}

	public void setStepHeight(float height) {
		stepHeight.setValue(height);
	}
}
