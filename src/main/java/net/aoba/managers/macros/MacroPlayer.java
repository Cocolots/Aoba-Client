/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers.macros;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.Aoba;
import net.aoba.managers.macros.actions.KeyClickMacroEvent;
import net.aoba.managers.macros.actions.MacroEvent;
import net.aoba.managers.macros.actions.MouseClickMacroEvent;
import net.aoba.mixin.interfaces.IKeyboardHandler;
import net.aoba.mixin.interfaces.IMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Class responsible for playing back a Macro
 */
public class MacroPlayer {
	private static final Minecraft MC = Minecraft.getInstance();

	private Macro currentMacro = null;
	private volatile boolean isPlaying = false;
	private Thread playbackThread = null;
	private Runnable curOnFinished;

	public void play(Macro macro) {
		this.play(macro, null);
	}

	/**
	 * Plays a Macro back in a new Thread.
	 *
	 * @param macro Macro to play.
	 * @param onFinished Callback when the Macro is finished.
	 */
	public void play(Macro macro, Runnable onFinished) {
		stop();

		Aoba.getInstance().guiManager.setClickGuiOpen(false);

		currentMacro = macro;
		curOnFinished = onFinished;
		isPlaying = true;
		playbackThread = new Thread(this::execute, "aoba-macro-player");
		playbackThread.setDaemon(true);
		playbackThread.start();
	}

	/**
	 * Stops the currently playing Macro.
	 */
	public void stop() {
		isPlaying = false;
		if (playbackThread != null) {
			playbackThread.interrupt();
			playbackThread = null;
		}
		
		// Unpress any keys that are potentially still being pressed.
		if (currentMacro != null) {
			long window = MC.getWindow().handle();

			HashSet<Integer> keys = new HashSet<>();
			HashSet<Integer> buttons = new HashSet<>();

			for (MacroEvent event : currentMacro.getEvents()) {
				if (event instanceof KeyClickMacroEvent keyEvent) {
					keys.add(keyEvent.getButton());
				} else if (event instanceof MouseClickMacroEvent mouseEvent) {
					buttons.add(mouseEvent.getButton());
				}
			}

			for (int key : keys) {
				KeyEvent keyEvent = new KeyEvent(key, 0, 0);
				((IKeyboardHandler) MC.keyboardHandler).invokeKeyPress(window, InputConstants.RELEASE, keyEvent);
			}

			for (int button : buttons) {
				MouseButtonInfo buttonInfo = new MouseButtonInfo(button, 0);
				((IMouseHandler) MC.mouseHandler).executeOnMouseButton(window, buttonInfo, InputConstants.RELEASE);
			}
		}
		
		if(curOnFinished != null) {
			curOnFinished.run();
		}
		currentMacro = null;
	}



	/**
	 * Returns whether a Macro is currently playing.
	 *
	 * @return True if a Macro is playing, false otherwise.
	 */
	public boolean isPlaying() {
		return isPlaying;
	}

	/**
	 * Executes the currently selected Macro.
	 */
	private void execute() {
		try {
			do {
				Queue<MacroEvent> events = new ArrayDeque<>(currentMacro.getEvents());
				long startTime = System.nanoTime();

				MacroEvent event = events.poll();
				while (event != null && isPlaying) {
					long waitTime = event.getTimestamp() - (System.nanoTime() - startTime);
					if (waitTime > 0) {
						long millis = waitTime / 1_000_000;
						int nanos = (int) (waitTime % 1_000_000);
						Thread.sleep(millis, nanos);
					}

					if (!isPlaying)
						break;

					event.execute();
					event = events.poll();
				}
			} while (isPlaying && currentMacro != null && currentMacro.isLooping());
		} catch (InterruptedException e) {

		} finally {
			this.stop();
		}
	}
}
