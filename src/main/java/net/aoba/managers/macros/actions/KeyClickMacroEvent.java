/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.macros.actions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.aoba.mixin.interfaces.IKeyboardHandler;
import net.minecraft.client.input.KeyEvent;

public class KeyClickMacroEvent extends MacroEvent {

	private int button = 0;
	private int keycode = 0;
	private int action= 0;
	private int mods= 0;

	public KeyClickMacroEvent() {

	}

	public KeyClickMacroEvent(long timestamp, int button, int keycode, int action, int mods) {
		super(timestamp);
		this.button = button;
		this.keycode = keycode;
		this.action = action;
		this.mods = mods;
	}

	@Override
	public void write(DataOutputStream fs) throws IOException {
		super.write(fs);
		fs.writeInt(button);
		fs.writeInt(keycode);
		fs.writeInt(action);
		fs.writeInt(mods);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		super.read(in);
		button = in.readInt();
		keycode = in.readInt();
		action = in.readInt();
		mods = in.readInt();
	}

	public int getButton() {
		return button;
	}

	public int getAction() {
		return action;
	}

	@Override
	public void execute() {
		KeyEvent keyEvent = new KeyEvent(button, keycode, mods);
		((IKeyboardHandler) MC.keyboardHandler).invokeKeyPress(MC.getWindow().handle(), action, keyEvent);
	}
}
