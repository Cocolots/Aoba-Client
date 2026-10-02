/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.screens.addons;

import java.nio.file.Path;
import java.util.List;
import net.aoba.api.IAddon;
import net.aoba.gui.screens.AobaPanorama;
import net.aoba.gui.screens.addons.AddonSelectionList.NormalEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import com.mojang.blaze3d.Blaze3D;

public class AddonScreen extends Screen {
	protected static final AobaPanorama AOBA_ROTATING_PANORAMA_RENDERER = new AobaPanorama();

	private final Screen parentScreen;

	// Widget
	private AddonSelectionList addonListSelector;
	private EditBox descriptionWidget;

	// Selected Mod
	private IAddon selectedAddon;

	public AddonScreen(Screen parentScreen) {
		super(Component.nullToEmpty("Addon Manager"));
		this.parentScreen = parentScreen;
	}

	public void init() {
		super.init();

		// Left Side
		Path addonsPath = Minecraft.getInstance().getResourcePackDirectory().getParent().resolve("mods");
		addRenderableWidget(Button
				.builder(Component.nullToEmpty("Open Addons Folder"), button -> Blaze3D.openPath(addonsPath))
				.bounds(16, height - 40, 120, 20).build());

		addRenderableWidget(Button.builder(Component.nullToEmpty("Done"), b -> minecraft.gui.setScreen(parentScreen))
				.bounds(146, height - 40, 120, 20).build());

		addonListSelector = new AddonSelectionList(this, minecraft, 250, height, 16, 48);
		addonListSelector.setRectangle(250, height - 80, 16, 32);
		addRenderableWidget(addonListSelector);

		// Right Side
		descriptionWidget = new EditBox(font, 282, 112, width - 314, height - 160,
				Component.nullToEmpty(""));
		descriptionWidget.setEditable(false);
		descriptionWidget.setCanLoseFocus(true);
		addRenderableWidget(descriptionWidget);

		// Set the selected addon if any exist.
		List<NormalEntry> entries = addonListSelector.getAddons();
		if (entries.size() > 0) {
			setSelected(entries.getFirst());
		}
	}

	@Override
	public void tick() {

	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, "Addons", 145, 16, 0xFFFFFFFF);

		// Draw Addon Information
		if (selectedAddon != null) {
			AddonSelectionList.NormalEntry entry = (AddonSelectionList.NormalEntry) addonListSelector.getSelected();
			if (entry != null) {
				// Draw the border
				graphics.fill(281, 59, 331, 109, 0xFFFFFFFF);
				graphics.fill(282, 60, 330, 108, 0xFF000000);

				// Draw the texture
				graphics.blit(entry.getIcon(), 282, 60, 48, 48, 0f, 0f, 1f, 1f);
			}
			graphics.text(font, selectedAddon.getName(), 338, 68, 0xFFFFFFFF);
			graphics.text(font, selectedAddon.getVersion(), 338, 80, 0xFFFFFFFF);
			graphics.text(font, "By " + selectedAddon.getAuthor(), 338, 92, 0xFFFFFFFF);
		}

	}

	public void setSelected(AddonSelectionList.NormalEntry selected) {
		addonListSelector.setSelected(selected);
		selectedAddon = selected.getAddon();

		if (descriptionWidget != null) {
			if (selectedAddon != null) {
				descriptionWidget.visible = true;
				descriptionWidget.setTextColorUneditable(CommonColors.WHITE);
				descriptionWidget.setValue(selectedAddon.getDescription());
			} else
				descriptionWidget.visible = false;
		}
	}

	@Override
	protected void extractPanorama(final GuiGraphicsExtractor graphics, final float a){
		try {
			AOBA_ROTATING_PANORAMA_RENDERER.extractRenderState(graphics, this.width, this.height, true);
		} catch (IllegalStateException e) {
		}
	}
}
