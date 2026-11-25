/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.settings;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;

import net.aoba.utils.TextUtils;

public abstract class Setting<T> {
	public enum TYPE {
		BOOLEAN, FLOAT, STRING, INTEGER, STRINGLIST, INDEXEDSTRINGLIST, KEYBIND, COLOR, SHADER, BLOCKS, ENTITIES, ENUM, RECTANGLE,
		VEC3D, HOTBAR, FONT, RANGE
	}

	public TYPE type;
	public final String ID;
	public final String displayName;
	public final String description;
	protected final T default_value;
	protected T value;

	private final HashSet<Consumer<T>> onUpdate = new HashSet<Consumer<T>>();

	public Setting(String ID, String description, T default_value) {
		this.ID = ID;
		displayName = TextUtils.idToName(ID);
		this.description = description;
		this.default_value = default_value;
		value = default_value;
	}

	public Setting(String ID, String displayName, String description, T default_value) {
		this.ID = ID;
		this.displayName = displayName;
		this.description = description;
		this.default_value = default_value;
		value = default_value;
	}

	public Setting(String ID, String displayName, String description, T default_value, Consumer<T> onUpdate) {
		this.ID = ID;
		this.displayName = displayName;
		this.description = description;
		this.default_value = default_value;
		this.onUpdate.add(onUpdate);
		value = default_value;
	}

	public Setting(String ID, String description, T default_value, Consumer<T> onUpdate) {
		this.ID = ID;
		displayName = TextUtils.idToName(ID);
		this.description = description;
		this.default_value = default_value;
		this.onUpdate.add(onUpdate);
		value = default_value;
	}

	/**
	 * Getter for the current value.
	 * 
	 * @return The value currently stored in the Setting.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Setter for the current value.
	 * 
	 * @param value The value to set.
	 */
	public void setValue(T value) {
		if (Objects.equals(this.value, value))
			return;

		if (!isValueValid(value))
			return;

		this.value = value;
		update();
	}

	/**
	 * Resets the value of the setting to the default value.
	 */
	public void resetToDefault() {
		resetValue();
	}

	/**
	 * Setter for the current value that does not call update.
	 *
	 * @param value The value to set.
	 */
	public void silentSetValue(T value) {
		if (isValueValid(value)) {
			this.value = value;
		}
	}

	/**
	 * Resets the value of the setting to the default value.
	 */
	public void resetValue() {
		setValue(default_value);
	}

	/**
	 * Function that handles when the value is updated.
	 */
	public void update() {
		Consumer<T>[] snapshot = onUpdate.toArray(new Consumer[0]);
		for (Consumer<T> consumer : snapshot) {
			if (consumer != null) {
				try {
					consumer.accept(value);
				} catch (Exception e) {
					LogUtils.getLogger().error("Error in setting listener for " + ID, e);
				}
			}
		}
	}

	public void addOnUpdate(Consumer<T> consumer) {
		onUpdate.add(consumer);
	}

	public void removeOnUpdate(Consumer<T> consumer) {
		onUpdate.remove(consumer);
	}

	/**
	 * Getter for the default value.
	 *
	 * @return
	 */
	public T getDefaultValue() {
		return default_value;
	}

	/**
	 * Function that will check if a value is valid for this particular setting.
	 *
	 * @param value The value to test.
	 * @return True if the value is valid.
	 */
	protected abstract boolean isValueValid(T value);

	/**
	 * Abstract builder class for easier setting creation.
	 * 
	 * @param <S>
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public abstract static class BUILDER<B extends BUILDER<?, ?, ?>, S extends Setting<T>, T> {
		protected String id;
		protected String displayName;
		protected String description;
		protected T defaultValue;
		protected Consumer<T> onUpdate;

		protected BUILDER() {
		}

		public B id(String id) {
			this.id = id;
			return (B) this;
		}

		public B displayName(String displayName) {
			this.displayName = displayName;
			return (B) this;
		}

		public B description(String description) {
			this.description = description;
			return (B) this;
		}

		public B defaultValue(T defaultValue) {
			this.defaultValue = defaultValue;
			return (B) this;
		}

		public B onUpdate(Consumer<T> onUpdate) {
			this.onUpdate = onUpdate;
			return (B) this;
		}

		public abstract S build();
	}
}
