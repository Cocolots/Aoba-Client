/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.utils.input;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

public enum CursorStyle {
    Default,
    Click,
    Type,
	HorizonalResize,
	VerticalResize;

    public CursorType getCursorType() {
        return switch (this) {
            case Click -> CursorTypes.POINTING_HAND;
            case Type -> CursorTypes.IBEAM;
            case HorizonalResize -> CursorTypes.RESIZE_EW;
            case VerticalResize -> CursorTypes.RESIZE_NS;
            case Default -> CursorTypes.ARROW;
        };
    }
}
