/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.command.commands;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

import net.aoba.Aoba;
import net.aoba.command.Command;
import net.aoba.command.InvalidSyntaxException;
import net.aoba.managers.CommandManager;

public class CmdClickgui extends Command {

    public CmdClickgui() {
        super("clickgui", "Allows the player to see chest locations through ESP", "[set/open] [value]");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        switch (parameters[0]) {
            case "set":
                if (parameters.length != 2)
                    throw new InvalidSyntaxException(this);
                char keybind = Character.toLowerCase(parameters[1].charAt(0));
                CommandManager.sendChatMessage("Setting keybind to " + keybind + ".");
                String inputKey = "key.keyboard." + keybind;
                Key key = InputConstants.getKey(inputKey);
                Aoba.getInstance().guiManager.clickGuiButton.setValue(key);
                break;
            case "open":
            	CommandManager.sendChatMessage("Opening ClickGUI.");
                Aoba.getInstance().guiManager.setClickGuiOpen(true);
                break;
            default:
                throw new InvalidSyntaxException(this);
        }
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        switch (previousParameter) {
            default:
                return new String[]{"set", "open"};
        }
    }
}
