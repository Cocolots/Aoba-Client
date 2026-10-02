/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.utils.input;

import static net.aoba.AobaClient.MC;

public class Input {
	private static CursorStyle lastCursorStyle = CursorStyle.Default;

	public static void setCursorStyle(CursorStyle style) {

		if (lastCursorStyle != style) {
			MC.getWindow().selectCursor(style.getCursorType());
			lastCursorStyle = style;
		}
	}
}
