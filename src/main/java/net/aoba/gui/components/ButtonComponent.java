/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.components;

import net.aoba.event.events.MouseClickEvent;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.types.Thickness;
import net.aoba.utils.input.CursorStyle;
import net.aoba.rendering.Renderer2D;
import net.aoba.rendering.shaders.Shader;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;

public class ButtonComponent extends Component {
	private Runnable onClick;
	private boolean isPressed = false;
	
	/**
	 * Constructor for button component.
	 *
	 * @param onClick OnClick delegate that will run when the button is pressed.
	 */
	public ButtonComponent(Runnable onClick) {
		this.onClick = onClick;
		this.setProperty(UIElement.CursorProperty, CursorStyle.Click);
		this.setProperty(UIElement.PaddingProperty, new Thickness(4f));
		this.bindProperty(UIElement.BackgroundProperty, GuiManager.buttonBackgroundColor);
		this.bindProperty(UIElement.BorderProperty, GuiManager.buttonBorderColor);
	}

	@Override
	public void draw(Renderer2D renderer, float partialTicks) {
		boolean hovered = getProperty(UIElement.IsHoveredProperty);
		
		Shader bgEffect = isPressed ? GuiManager.buttonPressedBackgroundColor.getValue() : hovered ? GuiManager.buttonHoverBackgroundColor.getValue()
				: getProperty(BackgroundProperty);
		Shader bdEffect = getProperty(BorderProperty);
		Float cornerRadius = getProperty(CornerRadiusProperty);
		Float borderThickness = getProperty(BorderThicknessProperty);
		float radius = cornerRadius != null ? cornerRadius : 0f;
		float thickness = borderThickness != null ? borderThickness : 0f;

		if (bgEffect != null || bdEffect != null) {
			float actualX = getActualSize().x();
			float actualY = getActualSize().y();
			float actualWidth = getActualSize().width();
			float actualHeight = getActualSize().height();

			if (bgEffect != null) {
				renderer.drawRoundedBox(actualX, actualY, actualWidth, actualHeight, radius, bgEffect);
			}

			if (bdEffect != null) {
				renderer.drawRoundedBoxOutline(actualX, actualY, actualWidth, actualHeight, radius, thickness,
						bdEffect);
			}
		}

		super.draw(renderer, partialTicks);
	}

	/**
	 * Sets the OnClick delegate of the button.
	 *
	 * @param onClick Delegate to set.
	 */
	public void setOnClick(Runnable onClick) {
		this.onClick = onClick;
	}

	@Override
	public void onMouseClick(MouseClickEvent event) {
		super.onMouseClick(event);
		if (event.button == MouseButton.LEFT) {
			boolean hovered = getProperty(UIElement.IsHoveredProperty);
			if (hovered) {
				
				if(event.action == MouseAction.DOWN) {
					isPressed = true;
				}else if(event.action == MouseAction.UP) {
					if (onClick != null) {
						onClick.run();
					}
					isPressed = false;
				}
				event.cancel();
			}else {
				isPressed = false;
			}
		}else {
			isPressed = false;
		}
	}
}
