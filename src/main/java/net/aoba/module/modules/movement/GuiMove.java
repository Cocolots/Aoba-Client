/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.movement;

import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;

public class GuiMove extends Module implements TickListener {
	public GuiMove() {
		super("GuiMove");
		setCategory(Category.of("Movement"));
		setDescription("Lets the player move while inside of menus using arrow keys..");

		setDetectable(AntiCheat.Karhu);
	}

	@Override
	public void onDisable() {
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

	}

	@Override
	public void onTick(Post event) {
		if (MC.gui.screen() != null && !(MC.gui.screen() instanceof ChatScreen)) {
			for (KeyMapping k : new KeyMapping[] { MC.options.keyUp, MC.options.keyDown, MC.options.keyLeft,
					MC.options.keyRight, MC.options.keyJump, MC.options.keySprint })
				k.setDown(isKeyPressed(InputConstants.getKey(k.saveString()).getValue()));

			float deltaX = 0;
			float deltaY = 0;

			if (isKeyPressed(InputConstants.KEY_DOWN))
				deltaY += 10f;

			if (isKeyPressed(InputConstants.KEY_UP))
				deltaY -= 10f;

			if (isKeyPressed(InputConstants.KEY_RIGHT))
				deltaX += 10f;

			if (isKeyPressed(InputConstants.KEY_LEFT))
				deltaX -= 10f;

			if (deltaX != 0 || deltaY != 0)
				MC.player.turn(deltaX, deltaY);
		}
	}

}
