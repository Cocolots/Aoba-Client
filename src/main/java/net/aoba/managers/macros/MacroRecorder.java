/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.macros;

import java.util.HashSet;
import java.util.LinkedList;
import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.Aoba;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.events.KeyUpEvent;
import net.aoba.event.events.MouseClickEvent;
import net.aoba.event.events.MouseMoveEvent;
import net.aoba.event.events.MouseScrollEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.event.listeners.KeyUpListener;
import net.aoba.event.listeners.MouseClickListener;
import net.aoba.event.listeners.MouseMoveListener;
import net.aoba.event.listeners.MouseScrollListener;
import net.aoba.managers.macros.actions.KeyClickMacroEvent;
import net.aoba.managers.macros.actions.MacroEvent;
import net.aoba.managers.macros.actions.MouseClickMacroEvent;
import net.aoba.managers.macros.actions.MouseMoveMacroEvent;
import net.aoba.managers.macros.actions.MouseScrollMacroEvent;

/**
 * Class responsible for recording Macros
 */
public class MacroRecorder
		implements MouseClickListener, MouseMoveListener, MouseScrollListener, KeyDownListener, KeyUpListener {

	private static final long SAMPLE_INTERVAL_NS = 4_000_000; // 1ms = 1000Hz, default 4000hz

	private LinkedList<MacroEvent> currentMacro = new LinkedList<MacroEvent>();
	private long startTime = 0;
	private long lastMoveTime = 0;
	private boolean recording = false;

	private final HashSet<Integer> heldKeys = new HashSet<>();
	private final HashSet<Integer> heldMouseButtons = new HashSet<>();

	/**
	 * Begins recording a Macro
	 */
	public void startRecording() {
		if (!recording) {
			currentMacro = new LinkedList<MacroEvent>();
			recording = true;
			startTime = System.nanoTime();

			Aoba.getInstance().eventManager.AddListener(MouseClickListener.class, this);
			Aoba.getInstance().eventManager.AddListener(MouseMoveListener.class, this);
			Aoba.getInstance().eventManager.AddListener(MouseScrollListener.class, this);
			Aoba.getInstance().eventManager.AddListener(KeyDownListener.class, this);
			Aoba.getInstance().eventManager.AddListener(KeyUpListener.class, this);
		}
	}

	/**
	 * Stops recording a Macro
	 */
	public void stopRecording() {
		if (recording) {
			long timeStamp = System.nanoTime() - startTime;

			// Set the macro to release all keys currently pressed at the end.
			for (int key : heldKeys)
				currentMacro.add(new KeyClickMacroEvent(timeStamp, key, 0, InputConstants.RELEASE, 0));
			for (int button : heldMouseButtons)
				currentMacro.add(new MouseClickMacroEvent(timeStamp, button, InputConstants.RELEASE, 0));
			heldKeys.clear();
			heldMouseButtons.clear();

			recording = false;
			startTime = 0;

			Aoba.getInstance().eventManager.RemoveListener(MouseClickListener.class, this);
			Aoba.getInstance().eventManager.RemoveListener(MouseMoveListener.class, this);
			Aoba.getInstance().eventManager.RemoveListener(MouseScrollListener.class, this);
			Aoba.getInstance().eventManager.RemoveListener(KeyDownListener.class, this);
			Aoba.getInstance().eventManager.RemoveListener(KeyUpListener.class, this);
		}
	}

	/**
	 * Returns the recorder to its default state.
	 */
	public void reset() {
		heldKeys.clear();
		heldMouseButtons.clear();
		recording = false;
		startTime = 0;
		currentMacro.clear();
		currentMacro = null;
	}
	
	/**
	 * Constructs and returns a macro built from this recorder.
	 */
	public Macro constructMacro() {
		if (!recording && currentMacro != null) {
			Macro macro = new Macro(currentMacro);
			return macro;
		}
		return null;
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		if (event.GetKey() != Aoba.getInstance().guiManager.clickGuiButton.getValue().getValue()
				&& event.GetKey() != 256
				&& !Aoba.getInstance().guiManager.isClickGuiOpen()) {
			heldKeys.remove(Integer.valueOf(event.GetKey()));
			long timeStamp = System.nanoTime() - startTime;
			currentMacro.add(new KeyClickMacroEvent(timeStamp, event.GetKey(), event.GetKeyCode(), event.GetAction(),
					event.GetModifiers()));
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (event.GetKey() != Aoba.getInstance().guiManager.clickGuiButton.getValue().getValue()
				&& event.GetKey() != 256
				&& !Aoba.getInstance().guiManager.isClickGuiOpen()) {
			heldKeys.add(event.GetKey());
			long timeStamp = System.nanoTime() - startTime;
			currentMacro.add(new KeyClickMacroEvent(timeStamp, event.GetKey(), event.GetKeyCode(), event.GetAction(),
					event.GetModifiers()));
		}
	}


	@Override
	public void onMouseScroll(MouseScrollEvent event) {
		if (!Aoba.getInstance().guiManager.isClickGuiOpen()) {
			long timeStamp = System.nanoTime() - startTime;
			currentMacro.add(new MouseScrollMacroEvent(timeStamp, event.GetHorizontal(), event.GetVertical()));
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent mouseMoveEvent) {
		if (!Aoba.getInstance().guiManager.isClickGuiOpen()) {
			if (mouseMoveEvent.getX() == 0 && mouseMoveEvent.getY() == 0)
				return;

			long now = System.nanoTime();
			if (now - lastMoveTime < SAMPLE_INTERVAL_NS)
				return;

			lastMoveTime = now;
			long timeStamp = now - startTime;
			currentMacro.add(new MouseMoveMacroEvent(timeStamp, mouseMoveEvent.getX(), mouseMoveEvent.getY()));
		}
	}

	@Override
	public void onMouseClick(MouseClickEvent mouseClickEvent) {
		if (!Aoba.getInstance().guiManager.isClickGuiOpen()) {
			if (mouseClickEvent.action == InputConstants.PRESS)
				heldMouseButtons.add(mouseClickEvent.button);
			else if (mouseClickEvent.action == InputConstants.RELEASE)
				heldMouseButtons.remove(Integer.valueOf(mouseClickEvent.button));
			long timeStamp = System.nanoTime() - startTime;
			currentMacro.add(new MouseClickMacroEvent(timeStamp, mouseClickEvent.button, mouseClickEvent.action,
					mouseClickEvent.mods));
		}
	}
}
