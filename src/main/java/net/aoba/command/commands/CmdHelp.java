/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.command.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.aoba.Aoba;
import net.aoba.command.Command;
import net.aoba.command.HelpPage;
import net.aoba.command.InvalidSyntaxException;
import net.aoba.managers.CommandManager;
import net.aoba.module.Module;
import net.aoba.settings.Setting;
import net.minecraft.ChatFormatting;

public class CmdHelp extends Command {
	private static final int COMMANDS_PER_PAGE = 7;

	public CmdHelp() {
		super("help", "Shows commands and module settings.", "[page, module]");
	}

	@Override
	public void runCommand(String[] parameters) throws InvalidSyntaxException {
		if (parameters.length == 0) {
			showCommands(1);
			return;
		}
		if (parameters.length > 1) {
			throw new InvalidSyntaxException(this);
		}
		if (parameters[0].chars().allMatch(Character::isDigit)) {
			try {
				showCommands(Integer.parseInt(parameters[0]));
			} catch (NumberFormatException e) {
				throw new InvalidSyntaxException(this);
			}
			return;
		}
		showModule(parameters[0]);
	}

	private void showCommands(int requestedPage) {
		List<String> names = new ArrayList<>(Aoba.getInstance().commandManager.getCommands().keySet());
		names.sort(String.CASE_INSENSITIVE_ORDER);
		HelpPage<String> page = HelpPage.of(names, requestedPage, COMMANDS_PER_PAGE);
		String prefix = CommandManager.PREFIX.getValue();

		CommandManager.sendChatMessage("------------ Help [Page " + page.page() + " of " + page.pageCount()
				+ "] ------------");
		CommandManager.sendChatMessage("Use " + ChatFormatting.LIGHT_PURPLE + prefix + " help [page|module]"
				+ ChatFormatting.RESET + " for command pages or module details.");
		for (String name : page.entries()) {
			Command command = Aoba.getInstance().commandManager.getCommandBySyntax(name);
			CommandManager.sendChatMessage(prefix + " " + name + " " + command.getSyntax() + " — "
					+ command.getDescription());
		}
		if (names.isEmpty()) {
			CommandManager.sendChatMessage("No commands registered.");
		}
	}

	private void showModule(String name) {
		Module module = Aoba.getInstance().moduleManager.getModuleByName(name);
		if (module == null) {
			CommandManager.sendChatMessage("Could not find module '" + name + "'.");
			return;
		}

		CommandManager.sendChatMessage("------------ " + ChatFormatting.LIGHT_PURPLE + module.getName()
				+ ChatFormatting.RESET + " ------------");
		CommandManager.sendChatMessage("Description: " + module.getDescription());
		CommandManager.sendChatMessage("Category: " + module.getCategory().getName());
		CommandManager.sendChatMessage("State: " + module.getStatus());
		CommandManager.sendChatMessage("Keybind: " + module.getBind().getValue());
		for (Setting<?> setting : module.getSettings()) {
			if (setting == module.state || setting == module.getBind()) {
				continue;
			}
			CommandManager.sendChatMessage(setting.displayName + ": " + setting.getValue()
					+ (setting.description == null || setting.description.isBlank() ? "" : " — " + setting.description));
		}
	}

	@Override
	public String[] getAutocorrect(String previousParameter) {
		return Aoba.getInstance().moduleManager.modules.stream().map(Module::getName)
				.sorted(Comparator.comparing(String::toLowerCase)).toArray(String[]::new);
	}
}
