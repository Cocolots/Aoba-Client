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

import net.aoba.mixin.interfaces.IMouseHandler;
import net.minecraft.client.MouseHandler;

public class MouseMoveMacroEvent extends MacroEvent {
	private double x = 0;
	private double y = 0;
	
	public MouseMoveMacroEvent() {
		
	}
	
	public MouseMoveMacroEvent(long timestamp, double x, double y) {
		super(timestamp);
		this.x = x;
		this.y = y;
	}

	@Override
	public void write(DataOutputStream fs) throws IOException {
		super.write(fs);
		fs.writeDouble(x);
		fs.writeDouble(y);
	}

	@Override
	public void read(DataInputStream in)throws IOException {
		super.read(in);
		x = in.readDouble();
		y = in.readDouble();
	}

	@Override
	public void execute() {
		MouseHandler mouse = MC.mouseHandler;
		IMouseHandler iMouse = (IMouseHandler)mouse;
		if(iMouse != null) {
			iMouse.executeOnCursorPos(MC.getWindow().handle(), x, y, x - mouse.xpos(), y - mouse.ypos());
		}
	}
}
