/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.combat;

import net.aoba.Aoba;
import net.aoba.event.events.PlayerHealthEvent;
import net.aoba.event.listeners.PlayerHealthListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoSoup extends Module implements PlayerHealthListener {

	private final FloatSetting health = FloatSetting.builder().id("autosoup_health").displayName("Min. Health")
			.description("Minimum health that the AutoSoup will trigger.").defaultValue(6f).minValue(1f).maxValue(20f)
			.step(1f).build();

	private int previousSlot = -1;

	public AutoSoup() {
		super("AutoSoup");

		setCategory(Category.of("Combat"));
		setDescription("Automatically consumes soup when health is low. (KitPVP)");

		addSetting(health);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(PlayerHealthListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(PlayerHealthListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	public void sortInventory() {
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = MC.player.getInventory().getItem(i);
			if (stack.isEmpty() || stack.getItem() == Items.BOWL) {
				int nextSoup = findSoup();
				if (nextSoup >= 0) {
					MC.gameMode.handleInventoryButtonClick(0, nextSoup);
					MC.gameMode.handleInventoryButtonClick(0, i);
				}
			}
		}
	}

	public int findSoup() {
		for (int i = 0; i < 36; i++) {
			ItemStack stack = MC.player.getInventory().getItem(i);
			if (stack != null && stack.getItem() == Items.MUSHROOM_STEW) {
				return i;
			}
		}
		return -1;
	}

	public void setHunger(int hunger) {

	}

	@Override
	public void onHealthChanged(PlayerHealthEvent readPacketEvent) {
		float playerHealth = readPacketEvent.getHealth();

		// If the players HP is below the given threshold.
		if (playerHealth < health.getValue()) {
			// Find the first item in the hotbar that is a Stew item.
			int foodSlot = -1;
			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				Item item = MC.player.getInventory().getItem(i).getItem();

				// Check if it is Mushroom Soup. TODO: Add component check...
				if (item == Items.MUSHROOM_STEW) {
					foodSlot = i;
					break;
				}
			}

			// If a Stew item was found, switch to it and use it.
			if (foodSlot >= 0) {
				previousSlot = MC.player.getInventory().getSelectedSlot();

				MC.player.getInventory().setSelectedSlot(foodSlot);
				MC.options.keyUse.setDown(true);
				MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);

				// Return the player's selected slot back to the previous slot.
				if (previousSlot != -1) {
					MC.options.keyUse.setDown(false);
					MC.player.getInventory().setSelectedSlot(previousSlot);
					previousSlot = -1;
				}
			} else {
				// Otherwise, sort the inventory to try and find some.
				sortInventory();
			}
		}
	}
}