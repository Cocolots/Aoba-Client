/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.MouseClickEvent;
import net.aoba.event.events.MouseMoveEvent;
import net.aoba.event.events.MouseScrollEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	private double xpos;
	@Shadow
	private double ypos;

	@Inject(at = { @At("HEAD") }, method = { "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V" }, cancellable = true)
	private void onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba != null && aoba.eventManager != null) {
			int button = buttonInfo.button();
			int mods = buttonInfo.modifiers();
			MouseClickEvent event = new MouseClickEvent(xpos, ypos, button, action, mods);
			aoba.eventManager.Fire(event);

			if (aoba.guiManager.isClickGuiOpen()) {
				ci.cancel();
			}
		}
	}

	@Inject(at = { @At("HEAD") }, method = { "onScroll(JDD)V" }, cancellable = true)
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba != null && aoba.eventManager != null) {
			MouseScrollEvent event = new MouseScrollEvent(horizontal, vertical);
			aoba.eventManager.Fire(event);

			if (event.isCancelled()) {
				ci.cancel();
			}
		}
	}

	@Inject(at = { @At("HEAD") }, method = { "grabMouse()V" }, cancellable = true)
	private void onLockCursor(CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba != null && aoba.guiManager != null) {
			if (aoba.guiManager.isClickGuiOpen())
				ci.cancel();
		}
	}

	@Inject(at = { @At("HEAD") }, method = { "onMove(JDDDD)V" }, cancellable = true)
	private void onCursorPos(long window, double x, double y, double xrel, double yrel, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();
		if (aoba != null && aoba.eventManager != null) {
			double cursorDeltaX = x - this.xpos;
			double cursorDeltaY = y - this.ypos;

			MouseMoveEvent event = new MouseMoveEvent(x, y, cursorDeltaX, cursorDeltaY);
			aoba.eventManager.Fire(event);

			if (event.isCancelled()) {
				// Update the XY but not the delta (used for camera movements);
				this.xpos = x;
				this.ypos = y;
				ci.cancel();
			}

		}
	}
}