/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.macros;

import java.io.File;
import java.util.LinkedList;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.aoba.managers.macros.actions.MacroEvent;
import net.minecraft.client.Minecraft;
import net.aoba.utils.input.Keybind;

/**
 * Represents a Macro that contains a list of events.
 */
public class Macro {
	private String name;
	private String filePath;
	private LinkedList<MacroEvent> events;
	private boolean looping = false;
	private Keybind keybind = Keybind.from(InputConstants.UNKNOWN);

	public Macro() {
	}

	public Macro(LinkedList<MacroEvent> events)
	{
		this.events = events;
	}

	public Macro(LinkedList<MacroEvent> events, boolean looping) {
		this.events = events;
		this.looping = looping;
	}

	public Macro(LinkedList<MacroEvent> events, boolean looping, Keybind keybind) {
		this.events = events;
		this.looping = looping;
		this.keybind = keybind;
	}

	public Macro(Macro other) {
		this.events = new LinkedList<>(other.events);
		this.looping = other.looping;
		this.keybind = other.keybind;
		if (other.name != null)
			setName(other.name);
	}

	/**
	 * Getter for the name of the Macro
	 * 
	 * @return Name of the Macro
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getter for the file path of the Macro
	 * 
	 * @return File path of the Macro
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Getter for the events in the Macro
	 * 
	 * @return LinkedList of events
	 */
	public LinkedList<MacroEvent> getEvents() {
		return events;
	}

	/**
	 * Getter for whether the macro is looping or not.
	 * @return True if the macro is looping, false otherwise.
	 */
	public boolean isLooping() {
		return this.looping;
	}

	/**
	 * Setter for whether the macro should loop.
	 * @param Whether the macro should loop.
	 */
	public void setLooping(boolean looping) {
		this.looping = looping;
	}
	
	/**
	 * Getter for the keybind assigned to this Macro.
	 * @return The assigned keybind.
	 */
	public Keybind getKeybind() {
		return this.keybind;
	}

	/**
	 * Setter for the keybind assigned to this Macro.
	 * @param keybind The Key to assign.
	 */
	public void setKeybind(Keybind keybind) {
		this.keybind = keybind;
	}

	/**
	 * Setter for the name of the Macro
	 * @param name Value to set Macro's name to.
	 */
	public void setName(String name) {
		this.name = name;
		Minecraft MC = Minecraft.getInstance();
		filePath = MC.gameDirectory + File.separator + "aoba" + File.separator + "macros" + File.separator + name
				+ ".macro";
	}
}
