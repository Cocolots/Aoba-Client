/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.misc;

import net.aoba.Aoba;
import net.aoba.event.events.FoodLevelEvent;
import net.aoba.event.events.ItemUsedEvent;
import net.aoba.event.events.PlayerHealthEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.FoodLevelListener;
import net.aoba.event.listeners.ItemUsedListener;
import net.aoba.event.listeners.PlayerHealthListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.utils.player.ConsumableSession;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends Module implements FoodLevelListener, PlayerHealthListener, ItemUsedListener, TickListener {
	private static final long USE_TIMEOUT_MS = 10_000;

	private final ConsumableSession session = new ConsumableSession();
	private ItemStack lastUsedItemStack;
	private boolean fillingToFull;

	private final BooleanSetting swapBack = BooleanSetting.builder().id("autoeat_swap_back").displayName("Swap Back")
			.description("Whether the player's slot will be switched back to their previous slot after eating.")
			.defaultValue(true).build();

	private final BooleanSetting fillToFull = BooleanSetting.builder().id("autoeat_fill_to_hull")
			.displayName("Fill To Full").description("Whether to fill the player's hunger to full.").defaultValue(true)
			.build();

	private final FloatSetting hungerSetting = FloatSetting.builder().id("autoeat_hunger").displayName("Hunger")
			.description("Determines when AutoEat will trigger.").defaultValue(10f).minValue(1f).maxValue(20f).step(1f)
			.build();

	private final FloatSetting healthSetting = FloatSetting.builder().id("autoeat_health").displayName("Health")
			.description("Determines when AutoEat will trigger based on health.").defaultValue(10f).minValue(1f)
			.maxValue(20f).step(1f).build();

	private final BooleanSetting prioritizeGapples = BooleanSetting.builder().id("autoeat_prioritize_gapples")
			.displayName("Prioritize Gapples").description("Prioritizes enchanted golden apples and golden apples.")
			.defaultValue(true).build();

	public AutoEat() {
		super("AutoEat");

		setCategory(Category.of("Misc"));
		setDescription("Automatically eats the best food in your inventory.");

		addSetting(fillToFull);
		addSetting(swapBack);
		addSetting(hungerSetting);
		addSetting(healthSetting);
		addSetting(prioritizeGapples);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(FoodLevelListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(PlayerHealthListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(ItemUsedListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		stopSession(false);
		fillingToFull = false;
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(FoodLevelListener.class, this);
		Aoba.getInstance().eventManager.AddListener(PlayerHealthListener.class, this);
		Aoba.getInstance().eventManager.AddListener(ItemUsedListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		fillingToFull = false;
	}

	@Override
	public void onToggle() {
	}

	public void setHunger(int hunger) {
		hungerSetting.setValue((float) hunger);
	}

	public void setHealth(float health) {
		healthSetting.setValue(health);
	}

	private void eatIfNecessary() {
		if (MC.player == null || session.isActive()) {
			return;
		}

		int foodLevel = MC.player.getFoodData().getFoodLevel();
		boolean continueFilling = fillingToFull
				&& AutoEatPlanner.shouldContinue(foodLevel, hungerSetting.getValue(), true);
		if (!continueFilling) {
			fillingToFull = false;
		}
		if (!continueFilling && !AutoEatPlanner.shouldStart(foodLevel, MC.player.getHealth(), hungerSetting.getValue(),
				healthSetting.getValue())) {
			return;
		}

		int foodSlot = -1;
		int bestScore = -1;
		ItemStack selectedStack = null;
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = MC.player.getInventory().getItem(slot);
			FoodProperties food = getFood(stack);
			if (food == null) {
				continue;
			}

			Item item = stack.getItem();
			int score = AutoEatPlanner.foodScore(food.nutrition(), item == Items.GOLDEN_APPLE,
					item == Items.ENCHANTED_GOLDEN_APPLE, prioritizeGapples.getValue());
			if (score > bestScore) {
				bestScore = score;
				foodSlot = slot;
				selectedStack = stack;
			}
		}

		if (selectedStack == null && getFood(MC.player.getOffhandItem()) != null) {
			foodSlot = InteractionUtils.OFFHAND;
			selectedStack = MC.player.getOffhandItem();
		}
		if (selectedStack == null) {
			fillingToFull = false;
			return;
		}

		int selectedSlot = MC.player.getInventory().getSelectedSlot();
		if (!session.start(selectedSlot, foodSlot, System.currentTimeMillis())) {
			return;
		}
		if (foodSlot != InteractionUtils.OFFHAND && foodSlot != selectedSlot && !swap(foodSlot, false)) {
			session.clear();
			return;
		}

		lastUsedItemStack = selectedStack.copy();
		fillingToFull = fillToFull.getValue();
		MC.options.keyUse.setDown(true);
	}

	private FoodProperties getFood(ItemStack stack) {
		return stack == null || stack.isEmpty() ? null : stack.getItem().components().get(DataComponents.FOOD);
	}

	private void stopSession(boolean restoreSlot) {
		MC.options.keyUse.setDown(false);
		lastUsedItemStack = null;

		if (session.isActive()) {
			int itemSlot = session.getItemSlot();
			int previousSlot = session.getPreviousSlot();
			session.stop();
			if (restoreSlot && swapBack.getValue() && previousSlot >= 0 && itemSlot != InteractionUtils.OFFHAND
					&& previousSlot != itemSlot && MC.player != null) {
				swap(previousSlot, false);
			}
		}
	}

	@Override
	public void onFoodLevelChanged(FoodLevelEvent event) {
		eatIfNecessary();
	}

	@Override
	public void onHealthChanged(PlayerHealthEvent event) {
		eatIfNecessary();
	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null) {
			stopSession(false);
			return;
		}
		if (session.isExpired(System.currentTimeMillis(), USE_TIMEOUT_MS)) {
			stopSession(true);
			fillingToFull = false;
		}
		eatIfNecessary();
	}

	@Override
	public void onTick(Post event) {
	}

	@Override
	public void onItemUsed(ItemUsedEvent.Pre event) {
	}

	@Override
	public void onItemUsed(ItemUsedEvent.Post event) {
		if (lastUsedItemStack == null || !ItemStack.isSameItem(event.getItemStack(), lastUsedItemStack)) {
			return;
		}

		boolean continueEating = MC.player != null && AutoEatPlanner.shouldContinue(
				MC.player.getFoodData().getFoodLevel(), hungerSetting.getValue(), fillToFull.getValue());
		stopSession(true);
		fillingToFull = continueEating && fillToFull.getValue();
	}
}
