/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.navigation.huds;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.AobaClient;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.colors.Colors;
import net.aoba.gui.navigation.HudWindow;
import net.aoba.gui.types.Rectangle;
import net.aoba.gui.types.ResizeMode;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.rendering.Renderer2D;
import net.aoba.rendering.shaders.Shader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;

public class ModuleSelectorHud extends HudWindow {

	private static final float ROW_HEIGHT = 30.0f;
	private static final Shader DISABLED_MODULE_SHADER = Shader.solid(Colors.White);
	private static final Shader ENABLED_MODULE_SHADER = Shader.solid(Colors.Green);
	
	private final KeyMapping keybindUp;
	private final KeyMapping keybindDown;
	private final KeyMapping keybindLeft;
	private final KeyMapping keybindRight;

	private int index = 0;
	private int indexMods = 0;
	private boolean isCategoryMenuOpen = false;

	private final List<Category> categories = new ArrayList<>();
	private final ArrayList<Module> modules = new ArrayList<Module>();

	public ModuleSelectorHud() {
		super("ModuleSelectorHud", 0, 0);

		categories.addAll(Category.getAllCategories().values());

		float newHeight = categories.size() * ROW_HEIGHT;
		setProperty(UIElement.MinHeightProperty, newHeight);
		setProperty(UIElement.HeightProperty, newHeight);
		setProperty(UIElement.MinWidthProperty, 180f);

		resizeMode = ResizeMode.Width;

		keybindUp = new KeyMapping("key.tabup", InputConstants.KEY_UP, AobaClient.AOBA_CATEGORY);
		keybindDown = new KeyMapping("key.tabdown", InputConstants.KEY_DOWN, AobaClient.AOBA_CATEGORY);
		keybindLeft = new KeyMapping("key.tableft", InputConstants.KEY_LEFT, AobaClient.AOBA_CATEGORY);
		keybindRight = new KeyMapping("key.tabright", InputConstants.KEY_RIGHT, AobaClient.AOBA_CATEGORY);
	}

	@Override
	public void update() {
		if (keybindUp.isDown()) {
			if (!isCategoryMenuOpen) {
				if (index == 0) {
					index = categories.size() - 1;
				} else {
					index -= 1;
				}
			} else {
				if (indexMods == 0) {
					indexMods = modules.size() - 1;
				} else {
					indexMods -= 1;
				}
			}
			keybindUp.setDown(false);
		} else if (keybindDown.isDown()) {
			if (!isCategoryMenuOpen) {
				index = (index + 1) % categories.size();
			} else {
				indexMods = (indexMods + 1) % modules.size();
			}
			keybindDown.setDown(false);
		} else if (keybindRight.isDown()) {
			if (!isCategoryMenuOpen) {
				isCategoryMenuOpen = true;
				if (modules.isEmpty()) {
					for (Module module : AOBA.moduleManager.modules) {
						if (module.isCategory(categories.get(index))) {
							modules.add(module);
						}
					}
				}
			} else {
				modules.get(indexMods).toggle();
			}
			keybindRight.setDown(false);
		} else if (keybindLeft.isDown()) {
			if (isCategoryMenuOpen) {
				indexMods = 0;
				modules.clear();
				isCategoryMenuOpen = false;
			}
			keybindLeft.setDown(false);
		}
	}

	@Override
	public void draw(Renderer2D renderer, float partialTicks) {
		super.draw(renderer, partialTicks);

		// Gets the client and window.
		Rectangle pos = getActualSize();

		float x = pos.x();
		float y = pos.y();
		float width = pos.width();
		float height = pos.height();

			// Draws background depending on components width and height
		renderer.drawRoundedBox( x, y, width, height, GuiManager.roundingRadius.getValue(),
					GuiManager.windowBackgroundColor.getValue());
		renderer.drawRoundedBoxOutline( x, y, width, height, GuiManager.roundingRadius.getValue(),
					1.0f, GuiManager.windowBorderColor.getValue());

			// For every category, draw a cell for it.
			Font font = GuiManager.fontSetting.getValue().getRenderer();
			for (int i = 0; i < categories.size(); i++) {
				renderer.drawString(">>", x + width - 24, y + (ROW_HEIGHT * i) + 8,
						GuiManager.foregroundColor.getValue(), font);

				// Draws the name of the category dependent on whether it is selected.
				if (index == i)
					renderer.drawString("> " + categories.get(i).getName(), x + 8,
							y + (ROW_HEIGHT * i) + 8, GuiManager.foregroundColor.getValue(), font);
				else
					renderer.drawString(categories.get(i).getName(), x + 8, y + (ROW_HEIGHT * i) + 8,
							DISABLED_MODULE_SHADER, font);
			}

			// If any particular category menu is open.
			if (isCategoryMenuOpen) {
				// Draw the table underneath
				renderer.drawRoundedBox(x + width, y + (ROW_HEIGHT * index), 165,
						ROW_HEIGHT * modules.size(), GuiManager.roundingRadius.getValue(),
						GuiManager.windowBackgroundColor.getValue());
				renderer.drawRoundedBoxOutline(x + width, y + (ROW_HEIGHT * index), 165,
						ROW_HEIGHT * modules.size(), GuiManager.roundingRadius.getValue(),
						1.0f, GuiManager.windowBorderColor.getValue());

				// For every mod, draw a cell for it.
				for (int i = 0; i < modules.size(); i++) {
					if (indexMods == i) {
						if (modules.get(i).state.getValue()) {
							renderer.drawString("> " + modules.get(i).getName(), x + width + 5,
									y + (i * ROW_HEIGHT) + (index * ROW_HEIGHT) + 8, ENABLED_MODULE_SHADER, font);
						} else {
							renderer.drawString("> " + modules.get(i).getName(), x + width + 5,
									y + (i * ROW_HEIGHT) + (index * ROW_HEIGHT) + 8,
									GuiManager.foregroundColor.getValue(), font);
						}
					} else {
						renderer.drawString(modules.get(i).getName(), x + width + 5,
								y + (i * ROW_HEIGHT) + (index * ROW_HEIGHT) + 8,
								modules.get(i).state.getValue() ? ENABLED_MODULE_SHADER : DISABLED_MODULE_SHADER, font);
					}
				}
			}
	}
}