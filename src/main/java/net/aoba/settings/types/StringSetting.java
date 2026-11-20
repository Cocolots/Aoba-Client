/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.settings.types;

import java.util.function.Consumer;

import net.aoba.settings.Setting;

public class StringSetting extends Setting<String> {
	protected StringSetting(String ID, String displayName, String description, String default_value,
			Consumer<String> onUpdate) {
		super(ID, displayName, description, default_value, onUpdate);
		type = TYPE.STRING;
	}

	/**
	 * Checks whether or not a value is with this setting's valid range.
	 */
	@Override
	protected boolean isValueValid(String value) {
		return true;
	}

	public static StringSetting.BUILDER builder() {
		return new StringSetting.BUILDER();
	}

	public static class BUILDER extends Setting.BUILDER<StringSetting.BUILDER, StringSetting, String> {
		protected BUILDER() {
        }

		@Override
		public StringSetting build() {
			return new StringSetting(id, displayName, description, defaultValue, onUpdate);
		}
	}
}
