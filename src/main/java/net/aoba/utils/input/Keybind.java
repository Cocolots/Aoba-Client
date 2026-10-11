package net.aoba.utils.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

public class Keybind {
	private Key key;
	private boolean isMomentary = false;
	
	public Keybind(Key key, boolean isMomentary) {
		this.key = key;
		this.isMomentary = isMomentary;
	}
	
	public Key getKey() {
		return this.key;
	}
	
	public void setKey(Key key) {
		this.key = key;
	}
	
	public int getKeyCode() {
		return this.key.getValue();
	}
	
	public void setKeyCode(int code) {
		Key key = InputConstants.Type.KEYBOARD.getOrCreate(code);
		setKey(key);
	}
	
	public boolean getIsMomentary() {
		return this.isMomentary;
	}
	
	public void setIsMomentary(boolean value) {
		this.isMomentary = value;
	}
	
	public static Keybind from(Key key) {
		return new Keybind(key, false);
	}
	
	public static Keybind from(Key key, boolean momentary) {
		return new Keybind(key, momentary);
	}
	
	public static Keybind from(int keyCode) {
		Key key = InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
		return new Keybind(key, false);
	}
	
	public static Keybind from(int keyCode, boolean momentary) {
		Key key = InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
		return new Keybind(key, momentary);
	}
}
