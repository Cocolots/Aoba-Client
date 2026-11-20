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
import net.aoba.module.Category;
import net.aoba.module.Module;

public class Safewalk extends Module implements TickListener {

	public Safewalk() {
		super("Safewalk");
		setCategory(Category.of("Movement"));
		setDescription("Permanently keeps player in sneaking mode.");
	}

	@Override
	public void onDisable() {
		MC.options.keyShift.setDown(false);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
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
		if (MC.player != null) {
			MC.options.keyShift.setDown(true);
		}
	}

	@Override
	public void onTick(Post event) {

	}
}
