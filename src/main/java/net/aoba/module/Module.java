/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.interfaces.IMultiPlayerGameMode;
import net.aoba.managers.SettingManager;
import net.aoba.mixin.interfaces.IMinecraft;
import net.aoba.settings.Setting;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.KeybindSetting;
import net.aoba.utils.FindItemResult;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Module {
	private String name;
	private String description;
	private Category category;

	public final BooleanSetting state;
	public final KeybindSetting keyBind;
	private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

	private final HashSet<AntiCheat> knownDetectable = new HashSet<AntiCheat>();

	protected static final Minecraft MC = AobaClient.MC;
	protected final IMinecraft IMC = AobaClient.IMC;
	protected static final AobaClient AOBA_CLIENT = Aoba.getInstance();

	/**
	 * Constructor for the module, initializing it with a specified keybind.
	 *
	 * @param name The name to use for this module.
	 */
	public Module(String name) {
		this(name, InputConstants.UNKNOWN);
	}

	public Module(String name, Key keybind) {
		this.name = name;
		keyBind = KeybindSetting.builder().id("key." + name.toLowerCase()).displayName(name + " Key")
				.defaultValue(keybind).build();

		state = BooleanSetting.builder().id("state." + name.toLowerCase()).displayName(name + " State")
				.defaultValue(false).onUpdate(s -> {
					if (s)
						onEnable();
					else
						onDisable();
					onToggle();
				}).build();

		addSetting(keyBind);
		addSetting(state);

		SettingManager.registerSetting(keyBind);
		SettingManager.registerSetting(state);
	}

	/**
	 * Retrieves the name of the module.
	 *
	 * @return The name of the module.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the module.
	 *
	 * @param name The name to assign to the module.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves the description of the module.
	 *
	 * @return The description of the module.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of the module.
	 *
	 * @param description The description to assign to the module.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Retrieves the category of the module.
	 *
	 * @return The {@link Category} assigned to this module.
	 */
	public Category getCategory() {
		return category;
	}

	/**
	 * Sets the category for the module.
	 *
	 * @param category The {@link Category} to assign to this module, categorizing
	 *                 it for organizational purposes.
	 */
	public void setCategory(Category category) {
		this.category = category;
	}

	/**
	 * Retrieves the keybind associated with the module.
	 *
	 * @return The {@link KeybindSetting} that represents the keybinding for this
	 *         module.
	 */
	public KeybindSetting getBind() {
		return keyBind;
	}

	/**
	 * Adds a setting to the module.
	 *
	 * @param setting The {@link Setting} to be added to the module's settings list.
	 */
	public void addSetting(Setting<?> setting) {
		settings.add(setting);
	}

	/**
	 * Adds multiple settings to the module.
	 *
	 * @param settings The array of {@link Setting} objects to be added to the
	 *                 module's settings list.
	 */
	public void addSettings(Setting<?>... settings) {
		for (Setting<?> setting : settings) {
			addSetting(setting);
		}
	}

	/**
	 * Retrieves the list of settings associated with the module.
	 *
	 * @return A {@link List} of {@link Setting} objects that belong to this module.
	 */
	public List<Setting<?>> getSettings() {
		return settings;
	}

	/**
	 * Checks if the module has any settings defined.
	 *
	 * @return {@code true} if the module has one or more settings; {@code false}
	 *         otherwise.
	 */
	public boolean hasSettings() {
		return !settings.isEmpty();
	}

	/**
	 * Called when the module is disabled.
	 * <p>
	 * This method should contain the logic that needs to be executed when the
	 * module is turned off, such as stopping certain processes or resetting states.
	 * </p>
	 */
	public abstract void onDisable();

	/**
	 * Called when the module is enabled.
	 * <p>
	 * This method should contain the logic that needs to be executed when the
	 * module is activated, such as starting certain processes or modifying game
	 * states.
	 * </p>
	 */
	public abstract void onEnable();

	/**
	 * Called when the module's state is toggled.
	 * <p>
	 * This method serves as a general handler that is invoked whenever the module
	 * is enabled or disabled. It can contain logic that needs to be executed
	 * regardless of the current state.
	 * </p>
	 */
	public abstract void onToggle();

	/**
	 * Checks if a specific key is currently pressed.
	 *
	 * @param button The scancode of the key to check. A value of 0 or less
	 *               indicates an invalid key.
	 * @return {@code true} if the specified key is pressed; {@code false}
	 *         otherwise.
	 */
	public boolean isKeyPressed(int button) {
		if (button <= 0)
			return false;

		return InputConstants.isKeyDown(button);
	}

	/**
	 * Toggles the state of the module between enabled and disabled.
	 * <p>
	 * If the module is currently enabled, it will call the {@link #onDisable()}
	 * method to handle any necessary actions during disabling. Conversely, if the
	 * module is disabled, it will call the {@link #onEnable()} method to perform
	 * actions upon enabling.
	 * </p>
	 * The state is then updated accordingly.
	 */
	public void toggle() {
		if (isDetectable(AOBA_CLIENT.moduleManager.antiCheat.getValue()))
			state.setValue(false);
		else
			state.setValue(!state.getValue());
	}

	public boolean isDetectable(AntiCheat anticheat) {
		return knownDetectable.contains(anticheat);
	}

	public void setDetectable(AntiCheat anticheat) {
		setDetectable(anticheat, true);
	}

	/**
	 * Sets multiple anti-cheats as detectable.
	 *
	 * @param anticheats The array of {@link AntiCheat} objects to be set as
	 *                   detectable.
	 */
	public void setDetectable(AntiCheat... anticheats) {
		for (AntiCheat anticheat : anticheats) {
			setDetectable(anticheat, true);
		}
	}

	public void setDetectable(AntiCheat anticheat, boolean state) {
		if (state) {
			knownDetectable.add(anticheat);
		} else {
			knownDetectable.remove(anticheat);
		}
	}

	/**
	 * Returns the current status of the module as a string.
	 */
	public String getStatus() {
		return state.getValue() ? "Enabled" : "Disabled";
	}

	/**
	 * Retrieves the current keybind as a string.
	 */
	public String getKeyBindDisplayName() {
		return keyBind.displayName;
	}

	/**
	 * Resets the module settings to their default values.
	 */
	public void resetSettings() {
		for (Setting<?> setting : settings) {
			setting.resetToDefault();
		}
	}

	/**
	 * Checks if the module belongs to a specified category.
	 *
	 * @param category The {@link Category} to check against this module's category.
	 * @return {@code true} if the module is in the specified category;
	 *         {@code false} otherwise.
	 */
	public final boolean isCategory(Category category) {
		return category.equals(this.category);
	}

	/**
	 * Annotation to provide metadata about a module.
	 * <p>
	 * This annotation can be used to specify module information such as name,
	 * description, category, and keybind.
	 * </p>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ModInfo {
		/**
		 * @return The name of the module.
		 */
		String name();

		/**
		 * @return A brief description of the module's functionality.
		 */
		String description();

		/**
		 * @return The category which the module belongs to.
		 */
		String category();

		/**
		 * @return The keybind integer associated with the module.
		 */
		int bind();
	}

	public static int previousSlot = -1;

	public static FindItemResult findInHotbar(Item... items) {
		return findInHotbar(itemStack -> {
			for (Item item : items) {
				if (itemStack.getItem() == item)
					return true;
			}
			return false;
		});
	}

	public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
		if (testInOffHand(isGood)) {
			return new FindItemResult(45, MC.player.getOffhandItem().getCount());
		}

		if (testInMainHand(isGood)) {
			return new FindItemResult(MC.player.getInventory().getSelectedSlot(),
					MC.player.getMainHandItem().getCount());
		}

		return find(isGood, 0, 8);
	}

	public static FindItemResult find(Predicate<ItemStack> isGood) {
		if (MC.player == null) {
			return new FindItemResult(0, 0);
		}

		return find(isGood, 0, MC.player.getInventory().getContainerSize());
	}

	public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
		if (MC.player == null) {
			return new FindItemResult(0, 0);
		}

		int slot = -1;
		int count = 0;

		for (int i = start; i <= end; i++) {
			ItemStack stack = MC.player.getInventory().getItem(i);

			if (isGood.test(stack)) {
				if (slot == -1) {
					slot = i;
				}
				count += stack.getCount();
			}
		}

		return new FindItemResult(slot, count);
	}

	public static FindItemResult findFastestTool(BlockState state) {
		float bestScore = 1;
		int slot = -1;

		for (int i = 0; i < 9; i++) {
			ItemStack stack = MC.player.getInventory().getItem(i);

			if (stack.isCorrectToolForDrops(state)) {
				float score = stack.getDestroySpeed(state);

				if (score > bestScore) {
					bestScore = score;
					slot = i;
				}
			}
		}

		return new FindItemResult(slot, 1);
	}

	public static boolean testInMainHand(Predicate<ItemStack> predicate) {
		return predicate.test(MC.player.getMainHandItem());
	}

	public static boolean testInOffHand(Predicate<ItemStack> predicate) {
		return predicate.test(MC.player.getOffhandItem());
	}

	public static boolean swap(int slot, boolean swapBack) {
		if (slot == 45) {
			return true;
		}

		if (slot < 0 || slot > 8) {
			return false;
		}

		if (swapBack) {
			if (previousSlot == -1) {
				previousSlot = MC.player.getInventory().getSelectedSlot();
			}
		} else {
			previousSlot = -1;
		}

		MC.player.getInventory().setSelectedSlot(slot);
		((IMultiPlayerGameMode) MC.gameMode).aoba$syncSelected();
		return true;
	}

	public static boolean swapBack() {
		if (previousSlot == -1) {
			return false;
		}

		boolean result = swap(previousSlot, false);
		previousSlot = -1;
		return result;
	}

	public static FindItemResult find(Item... items) {
		return find(itemStack -> {
			for (Item item : items) {
				if (itemStack.getItem() == item)
					return true;
			}
			return false;
		});
	}

	public static void rotatePitch(float degrees) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;

		if (player != null) {
			float currentPitch = player.getXRot();
			float newPitch = currentPitch + degrees;

			newPitch = Math.max(-90.0F, Math.min(90.0F, newPitch));

			player.setXRot(newPitch);

			client.getConnection().send(
					new ServerboundMovePlayerPacket.Rot(player.getYRot(), newPitch, player.onGround(), false));
		}
	}

	public static void sendChatMessage(String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui != null) {
			mc.gui.hud.getChat().addClientSystemMessage(Component.nullToEmpty(ChatFormatting.DARK_PURPLE + "[" + ChatFormatting.LIGHT_PURPLE + "Aoba"
					+ ChatFormatting.DARK_PURPLE + "] " + ChatFormatting.RESET + message));
		}
	}
}