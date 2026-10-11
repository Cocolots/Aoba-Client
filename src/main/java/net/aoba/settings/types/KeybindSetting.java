/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.settings.types;

import net.aoba.settings.Setting;
import net.aoba.utils.input.Keybind;
import java.util.function.Consumer;

public class KeybindSetting extends Setting<Keybind> {
    protected KeybindSetting(String ID, String displayName, String description, Keybind default_value, Consumer<Keybind> onUpdate) {
        super(ID, displayName, description, default_value, onUpdate);
        type = TYPE.KEYBIND;
    }
   
    /**
     * Checks whether or not a value is with this setting's valid range.
     */
    @Override
    protected boolean isValueValid(Keybind value) {
        return true;
    }
    
    public static KeybindSetting.BUILDER builder() {
    	return new KeybindSetting.BUILDER();
    }
    
    public static class BUILDER extends Setting.BUILDER<KeybindSetting.BUILDER, KeybindSetting, Keybind> {
		protected BUILDER() {
        }

		@Override
		public KeybindSetting build() {
			return new KeybindSetting(id, displayName, description, defaultValue, onUpdate);
		}
		
		public KeybindSetting.BUILDER setMomentary(boolean value) {
			return this;
		}
	}
}