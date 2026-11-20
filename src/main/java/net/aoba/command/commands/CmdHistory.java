/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.command.commands;

import java.util.List;

import net.aoba.Aoba;
import net.aoba.command.Command;
import net.aoba.command.HelpPage;
import net.aoba.command.InvalidSyntaxException;
import net.aoba.managers.CommandManager;

public class CmdHistory extends Command {
	private static final int HISTORY_PER_PAGE = 7;

	public CmdHistory() {
		super("history", "Shows or clears command history.", "[page, clear]");
	}

	@Override
	public void runCommand(String[] parameters) throws InvalidSyntaxException {
		if (parameters.length == 0) {
			showHistory(1);
			return;
		}
		if (parameters.length > 1) {
			throw new InvalidSyntaxException(this);
		}
		if (parameters[0].equalsIgnoreCase("clear")) {
			Aoba.getInstance().commandManager.clearCommandHistory();
			CommandManager.sendChatMessage("Command history cleared.");
			return;
		}
		try {
			showHistory(Integer.parseInt(parameters[0]));
		} catch (NumberFormatException e) {
			throw new InvalidSyntaxException(this);
		}
	}

	private void showHistory(int requestedPage) {
		List<String> history = Aoba.getInstance().commandManager.getCommandHistory();
		HelpPage<String> page = HelpPage.of(history, requestedPage, HISTORY_PER_PAGE);
		CommandManager.sendChatMessage("------------ Command History [Page " + page.page() + " of "
				+ page.pageCount() + "] ------------");
		if (page.entries().isEmpty()) {
			CommandManager.sendChatMessage("No command history.");
			return;
		}

		int firstEntry = (page.page() - 1) * HISTORY_PER_PAGE;
		for (int index = 0; index < page.entries().size(); index++) {
			CommandManager.sendChatMessage(firstEntry + index + 1 + ": " + page.entries().get(index));
		}
	}

	@Override
	public String[] getAutocorrect(String previousParameter) {
		return new String[] { "clear" };
	}
}
