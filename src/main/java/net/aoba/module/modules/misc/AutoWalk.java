/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.misc;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;

public class AutoWalk extends Module implements TickListener {
	private boolean previousKeyUp;

	private final BooleanSetting automaticJump = BooleanSetting.builder().id("autowalk_automatic_jump")
			.displayName("Automatically Jump").description("Automatically jumps when you hit a wall.")
			.defaultValue(true).build();

	public AutoWalk() {
		super("AutoWalk");

		setCategory(Category.of("Misc"));
		setDescription("Automatically forward walks for you.");

		addSetting(automaticJump);
	}

	@Override
	public void onDisable() {
		MC.options.keyUp.setDown(previousKeyUp);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
	}

	@Override
	public void onEnable() {
		previousKeyUp = MC.options.keyUp.isDown();
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null || MC.level == null) {
			MC.options.keyUp.setDown(false);
			return;
		}

		MC.options.keyUp.setDown(true);
		if (automaticJump.getValue() && MC.player.horizontalCollision && MC.player.onGround()) {
			MC.player.jumpFromGround();
		}
	}

	@Override
	public void onTick(Post event) {

	}
}
