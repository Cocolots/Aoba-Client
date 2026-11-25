/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers;

import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;

import net.aoba.api.IAddon;
import net.aoba.command.Command;
import net.aoba.command.CommandHistory;
import net.aoba.command.CommandParseException;
import net.aoba.command.CommandRegistry;
import net.aoba.command.CommandSuggester;
import net.aoba.command.CommandTokenizer;
import net.aoba.command.InvalidSyntaxException;
import net.aoba.command.commands.*;
import net.aoba.settings.types.StringSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CommandManager {
	private static final int HISTORY_LIMIT = 100;
	private final CommandRegistry registry = new CommandRegistry();
	private final CommandHistory commandHistory = new CommandHistory(HISTORY_LIMIT);

	public static StringSetting PREFIX = StringSetting.builder().id("aoba_prefix").displayName("Prefix")
			.defaultValue(".aoba").build();

	public final CmdAimbot aimbot = new CmdAimbot();
	public final CmdAutoEat autoeat = new CmdAutoEat();
	public final CmdChestESP chestesp = new CmdChestESP();
	public final CmdClickgui clickgui = new CmdClickgui();
	public final CmdEntityESP entityesp = new CmdEntityESP();
	public final CmdFastBreak fastbreak = new CmdFastBreak();
	public final CmdFly fly = new CmdFly();
	public final CmdFreecam freecam = new CmdFreecam();
	public final CmdFont font = new CmdFont();
	public final CmdFullbright fullbright = new CmdFullbright();
	public final CmdHelp help = new CmdHelp();
	public final CmdHud hud = new CmdHud();
	public final CmdItemESP itemesp = new CmdItemESP();
	public final CmdNoclip noclip = new CmdNoclip();
	public final CmdNoFall nofall = new CmdNoFall();
	public final CmdNoSlowdown noslowdown = new CmdNoSlowdown();
	public final CmdNuker nuker = new CmdNuker();
	public final CmdPOV pov = new CmdPOV();
	public final CmdReach reach = new CmdReach();
	public final CmdSpam spam = new CmdSpam();
	public final CmdSprint sprint = new CmdSprint();
	public final CmdStep step = new CmdStep();
	public final CmdTileBreaker tilebreaker = new CmdTileBreaker();
	public final CmdTimer timer = new CmdTimer();
	public final CmdTP tp = new CmdTP();
	public final CmdTracer tracer = new CmdTracer();
	public final CmdXRay xray = new CmdXRay();
	public final CmdHistory history = new CmdHistory();
	public final CmdDiscord discord = new CmdDiscord();

	public CommandManager(List<IAddon> addons) {
		SettingManager.registerSetting(PREFIX);
		registerDefaults();

		if (addons == null) {
			return;
		}
		for (IAddon addon : addons) {
			if (addon == null || addon.commands() == null) {
				continue;
			}
			for (Command command : addon.commands()) {
				if (command == null) {
					continue;
				}
				if (!registry.registerIfAbsent(command)) {
					LogUtils.getLogger().warn("Duplicate addon command '{}'; registration skipped", command.getName());
				}
			}
		}
	}

	private void registerDefaults() {
		List<Command> defaults = List.of(aimbot, autoeat, chestesp, clickgui, entityesp, fastbreak, fly, freecam, font,
				fullbright, help, hud, itemesp, noclip, nofall, noslowdown, nuker, pov, reach, spam, sprint, step,
				tilebreaker, timer, tp, tracer, xray, history, discord);
		for (Command command : defaults) {
			registry.register(command);
		}
	}

	public void registerCommand(Command command, String... aliases) {
		registry.register(command, aliases);
	}

	public Command getCommandBySyntax(String syntax) {
		return registry.find(syntax).orElse(null);
	}

	public Map<String, Command> getCommands() {
		return registry.getCommands();
	}

	public int getNumOfCommands() {
		return registry.size();
	}

	public void command(String[] commandIn) {
		if (commandIn == null || commandIn.length == 0) {
			sendInvalidCommandMessage();
			return;
		}
		dispatch(String.join(" ", commandIn));
	}

	public boolean dispatch(String input) {
		var bodyOptional = CommandTokenizer.removePrefix(input, PREFIX.getValue());
		if (bodyOptional.isEmpty()) {
			return false;
		}
		commandHistory.add(input);

		List<String> tokens;
		try {
			tokens = CommandTokenizer.tokenize(bodyOptional.get());
		} catch (CommandParseException e) {
			sendChatMessage("Command parse error: " + e.getMessage());
			return true;
		}
		if (tokens.isEmpty()) {
			sendInvalidCommandMessage();
			return true;
		}

		Command command = getCommandBySyntax(tokens.get(0));
		if (command == null) {
			sendInvalidCommandMessage();
			return true;
		}

		try {
			command.runCommand(tokens.subList(1, tokens.size()).toArray(String[]::new));
		} catch (InvalidSyntaxException e) {
			e.PrintToChat();
		} catch (RuntimeException e) {
			LogUtils.getLogger().error("Command '{}' failed", command.getName(), e);
			sendChatMessage("Command failed: " + e.getMessage());
		}
		return true;
	}

	public CommandSuggester.Result suggest(String input, int cursor) {
		return CommandSuggester.suggest(input, cursor, PREFIX.getValue(), registry);
	}

	public List<String> getCommandHistory() {
		return commandHistory.getEntries();
	}

	public void clearCommandHistory() {
		commandHistory.clear();
	}

	private void sendInvalidCommandMessage() {
		sendChatMessage("Invalid Command! Type " + ChatFormatting.LIGHT_PURPLE + PREFIX.getValue() + " help"
				+ ChatFormatting.RESET + " for a list of commands.");
	}

	public static void sendChatMessage(String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.gui != null && mc.gui.hud != null) {
			mc.gui.hud.getChat().addClientSystemMessage(Component.nullToEmpty(ChatFormatting.DARK_PURPLE + "["
					+ ChatFormatting.LIGHT_PURPLE + "Aoba" + ChatFormatting.DARK_PURPLE + "] "
					+ ChatFormatting.RESET + message));
		}
	}
}
