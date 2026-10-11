/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.command.commands;

import net.aoba.Aoba;
import net.aoba.command.Command;
import net.aoba.managers.CommandManager;
import net.aoba.module.Module;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class CmdHelp extends Command {

    int indexesPerPage = 5;

    public CmdHelp() {
        super("help", "Shows the avaiable commands.", "[page, module]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length <= 0) {
            ShowCommands(1);
        } else if (StringUtils.isNumeric(parameters[0])) {
            int page = Integer.parseInt(parameters[0]);
            ShowCommands(page);
        } else {
            Module module = Aoba.getInstance().moduleManager.getModuleByName(parameters[0]);
            if (module == null) {
                CommandManager.sendChatMessage("Could not find Module '" + parameters[0] + "'.");
            } else {
                String title = "------------ " + ChatFormatting.LIGHT_PURPLE + module.getName() + " Help" + ChatFormatting.RESET + " ------------";
                String unformatted_title = "------------ " + module.getName() + " Help ------------";
                CommandManager.sendChatMessage(title);
                CommandManager.sendChatMessage("Name: " + ChatFormatting.LIGHT_PURPLE + module.getName() + ChatFormatting.RESET);
                CommandManager.sendChatMessage("Description: " + ChatFormatting.LIGHT_PURPLE + module.getDescription() + ChatFormatting.RESET);
                CommandManager.sendChatMessage("Keybind: " + ChatFormatting.LIGHT_PURPLE + module.getBind().getValue().getKey().getName() + ChatFormatting.RESET);
                CommandManager.sendChatMessage("-".repeat(unformatted_title.length() - 2)); // mc font characters are not the same width but eh..
            }
        }

    }

    private void ShowCommands(int page) {
        String title = "------------ Help [Page " + page + " of 5] ------------";  // TODO: remove hardcoded page length
        CommandManager.sendChatMessage(title);
        CommandManager.sendChatMessage("Use " + ChatFormatting.LIGHT_PURPLE + ".aoba help [n]" + ChatFormatting.RESET + " to get page n of help.");

        // Fetch the commands and dislays their syntax on the screen.
        Map<String, Command> commands = Aoba.getInstance().commandManager.getCommands();
        Set<String> keySet = commands.keySet();
        ArrayList<String> listOfCommands = new ArrayList<String>(keySet);

        for (int i = (page - 1) * indexesPerPage; i <= (page * indexesPerPage); i++) {
            if (i >= 0 && i < Aoba.getInstance().commandManager.getNumOfCommands()) {
                CommandManager.sendChatMessage(" .aoba " + listOfCommands.get(i));
            }
        }
        CommandManager.sendChatMessage("-".repeat(title.length() - 2)); // mc font characters are not the same width but eh..
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        CommandManager cm = Aoba.getInstance().commandManager;
        int numCmds = cm.getNumOfCommands();
        String[] commands = new String[numCmds];

        Set<String> cmds = Aoba.getInstance().commandManager.getCommands().keySet();
        int i = 0;
        for (String x : cmds)
            commands[i++] = x;

        return commands;
    }

}
