/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.settings.types;

import java.util.function.Consumer;
import net.aoba.Aoba;
import net.aoba.gui.colors.Color;
import net.aoba.rendering.shaders.Shader;
import net.aoba.settings.Setting;

public class ShaderSetting extends Setting<Shader> {

    protected ShaderSetting(String ID, String displayName, String description, Shader default_value, Consumer<Shader> onUpdate) {
        super(ID, displayName, description, default_value);
        type = TYPE.SHADER;
    }

    @Override
    protected boolean isValueValid(Shader value) {
        return value != null;
    }

    public String getShaderId() {
        Shader v = getValue();
        return v != null ? v.id() : "solid";
    }

    public void setShaderId(String shaderId) {
        Shader template = Aoba.getInstance().shaderManager.getShader(shaderId);
        if (template != null) {
            Shader newShader = template.copy();
            setValue(newShader);
        }
    }

    public String getSerializedValue() {
        StringBuilder serialized = new StringBuilder(getShaderId());
        Shader shader = getValue();
        if (shader != null) {
            for (float value : shader.uniformValues()) {
                serialized.append(",").append(value);
            }
        }
        return serialized.toString();
    }

    public void setSerializedValue(String value) {
        String[] parts = value.split(",");
        if (parts.length == 0)
            return;

        setShaderId(parts[0]);
        Shader shader = getValue();
        if (shader != null && parts.length > 1) {
            float[] uniforms = shader.uniformValues();
            for (int i = 0; i < Math.min(parts.length - 1, uniforms.length); i++) {
                try {
                    uniforms[i] = Float.parseFloat(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                }
            }
            update();
        }
    }

    public static ShaderSetting.BUILDER builder() {
    	return new ShaderSetting.BUILDER();
    }

    public static class BUILDER extends Setting.BUILDER<ShaderSetting.BUILDER, ShaderSetting, Shader> {
		protected BUILDER() {
        }

		@Override
		public ShaderSetting build() {
		    Shader def = defaultValue != null ? defaultValue : Shader.solid(new Color(255, 255, 255));
			return new ShaderSetting(id, displayName, description, def, onUpdate);
		}
	}
}
