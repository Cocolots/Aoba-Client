/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.macros;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.logging.LogUtils;

import net.aoba.Aoba;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.gui.GuiManager;
import net.aoba.managers.macros.actions.KeyClickMacroEvent;
import net.aoba.managers.macros.actions.MacroEvent;
import net.aoba.managers.macros.actions.MouseClickMacroEvent;
import net.aoba.managers.macros.actions.MouseMoveMacroEvent;
import net.aoba.managers.macros.actions.MouseScrollMacroEvent;
import net.aoba.utils.types.ObservableList;
import net.minecraft.client.Minecraft;

/**
 * Represents the Manager responsible for maintaining, creating, saving, and
 * loading Macros.
 */
public class MacroManager implements KeyDownListener {
	private static final Minecraft MC = Minecraft.getInstance();

	// Byte IDs — 0x00 is reserved as the flags terminator.
	private static final byte FLAG_TERMINATOR = 0x00;
	private static final byte FLAG_LOOPING = 0x01;
	private static final byte FLAG_KEYBIND = 0x02;

	private static final byte EVENT_KEY = 0x01;
	private static final byte EVENT_CLICK = 0x02;
	private static final byte EVENT_MOVE = 0x03;
	private static final byte EVENT_SCROLL = 0x04;

	private final ObservableList<Macro> macros = new ObservableList<>();

	private final MacroRecorder recorder;
	private final MacroPlayer player;

	public MacroManager() {
		recorder = new MacroRecorder();
		player = new MacroPlayer();
		load();
		Aoba.getInstance().eventManager.AddListener(KeyDownListener.class, this);
	}

	/**
	 * Loads all of the macros found in the runDirectory/aoba/macros
	 */
	private void load() {
		File macroDirectory = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "macros");
		if (macroDirectory.exists() && macroDirectory.isDirectory()) {
			LogUtils.getLogger().info("Found Macro Directory: " + macroDirectory.getAbsolutePath());
			File[] files = macroDirectory.listFiles((dir, name) -> name.endsWith(".macro"));
			if (files != null) {
				for (File file : files) {
					try {
						Macro macro = loadMacroFromFile(file);
						macros.add(macro);
					} catch (Exception e) {

					}
				}
			}
		}
	}

	/**
	 * Loads a Macro from a file.
	 * 
	 * @param file File to read from.
	 * @return Loaded Macro from the file.
	 */
	private Macro loadMacroFromFile(File file) {
		String name = file.getName();
		name = name.substring(0, name.length() - 6);
		String filePath = file.getPath();
		LinkedList<MacroEvent> events = new LinkedList<MacroEvent>();
		boolean looping = false;
		Key keybind = InputConstants.UNKNOWN;

		try (DataInputStream in = new DataInputStream(new FileInputStream(filePath))) {
			// Read flags until we hit the terminator (0x00).
			byte flag;
			while ((flag = in.readByte()) != FLAG_TERMINATOR) {
				switch (flag) {
					case FLAG_LOOPING -> looping = true;
					case FLAG_KEYBIND -> keybind = InputConstants.Type.KEYBOARD.getOrCreate(in.readInt());
					default -> {}
				}
			}

			// Read events — each is a single byte ID followed by its data.
			byte eventId;
			while (true) {
				eventId = in.readByte();
				MacroEvent event = switch (eventId) {
					case EVENT_KEY -> new KeyClickMacroEvent();
					case EVENT_CLICK -> new MouseClickMacroEvent();
					case EVENT_MOVE -> new MouseMoveMacroEvent();
					case EVENT_SCROLL -> new MouseScrollMacroEvent();
					default -> null;
				};
				if (event != null) {
					event.read(in);
					events.add(event);
				}
			}
		} catch (EOFException eof) {
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Macro newMacro = new Macro(events, looping, keybind);
		newMacro.setName(name);
		return newMacro;
	}

	public void save() {
		try {
			File macrosFolder = new File(MC.gameDirectory + File.separator + "aoba" + File.separator + "macros");
			if (!macrosFolder.exists() && !macrosFolder.mkdirs()) {
				throw new IOException("Failed to create macro folder: " + macrosFolder.getAbsolutePath());
			}

			for (Macro macro : macros) {
				File macroFile = new File(macro.getFilePath());
				if (!macroFile.exists() && !macroFile.createNewFile()) {
					throw new IOException("Failed to create macro file: " + macroFile.getAbsolutePath());
				}

				try (DataOutputStream out = new DataOutputStream(new FileOutputStream(macroFile))) {
					// Write flags.
					if (macro.isLooping())
						out.writeByte(FLAG_LOOPING);
					if (macro.getKeybind() != InputConstants.UNKNOWN) {
						out.writeByte(FLAG_KEYBIND);
						out.writeInt(macro.getKeybind().getValue());
					}
					out.writeByte(FLAG_TERMINATOR);

					// Write events.
					for (MacroEvent event : macro.getEvents()) {
						byte id =  switch (event) {
							case KeyClickMacroEvent e -> EVENT_KEY;
							case MouseClickMacroEvent e -> EVENT_CLICK;
							case MouseMoveMacroEvent e -> EVENT_MOVE;
							case MouseScrollMacroEvent e -> EVENT_SCROLL;
							default -> -1;
						};

						if (id != -1) {
							out.writeByte(id);
							event.write(out);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds a Macro to the Macro Manager.
	 * 
	 * @param macro Macro to add.
	 */
	public void addMacro(Macro macro) {
		macros.add(macro);
	}

	/**
	 * Removes a Macro from the Macro Manager.
	 *
	 * @param macro Macro to remove.
	 */
	public void removeMacro(Macro macro) {
		macros.remove(macro);
	}

	/**
	 * Returns the instance of the MacroRecorder.
	 * 
	 * @return MacroRecorder
	 */
	public MacroRecorder getRecorder() {
		return recorder;
	}

	/**
	 * Returns the instance of the MacroPlayer
	 * 
	 * @return MacroPlayer
	 */
	public MacroPlayer getPlayer() {
		return player;
	}

	/**
	 * Returns a list of all of the Macros
	 *
	 * @return List of all Macros.
	 */
	public List<Macro> getMacros() {
		return macros;
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (MC.gui.screen() != null)
			return;
		if (GuiManager.isKeyboardInputActive())
			return;

		int key = event.GetKey();
		for (Macro macro : macros) {
			Key bind = macro.getKeybind();
			if (bind != InputConstants.UNKNOWN && bind.getValue() == key) {
				if (player.isPlaying()) {
					player.stop();
				} else {
					player.play(macro);
				}
				break;
			}
		}
	}
}
