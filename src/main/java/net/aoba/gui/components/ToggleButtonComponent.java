/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.components;

import java.util.function.Consumer;

import net.aoba.event.events.MouseClickEvent;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.UIProperty;
import net.aoba.gui.types.Thickness;
import net.aoba.utils.input.CursorStyle;
import net.aoba.rendering.Renderer2D;
import net.aoba.rendering.shaders.Shader;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;

public class ToggleButtonComponent extends Component {
	private Runnable onClick;
	private Consumer<Boolean> onChanged;
	private boolean isPressed = false;
	
	public static final UIProperty<Boolean> IsCheckedProperty = new UIProperty<>("IsChecked", false, false, false, ToggleButtonComponent::onIsCheckedChanged);

	private static void onIsCheckedChanged(UIElement sender, Boolean oldValue, Boolean newValue) {
		if(sender instanceof ToggleButtonComponent toggleButton) {
			if(toggleButton.onChanged != null)
				toggleButton.onChanged.accept(newValue);
		}
	}
	
	/**
	 * Constructor for button component.
	 *
	 * @param onClick OnClick delegate that will run when the button is pressed.
	 */
	public ToggleButtonComponent() {
		this.setProperty(UIElement.CursorProperty, CursorStyle.Click);
		this.setProperty(UIElement.PaddingProperty, new Thickness(4f));
		this.bindProperty(UIElement.BackgroundProperty, GuiManager.componentBackgroundColor);
		this.bindProperty(UIElement.BorderProperty, GuiManager.componentBorderColor);
	}
	
	/**
	 * Constructor for button component.
	 *
	 * @param onClick OnClick delegate that will run when the button is pressed.
	 */
	public ToggleButtonComponent(Runnable onClick) {
		this();
		this.onClick = onClick;
	}

	@Override
	public void draw(Renderer2D renderer, float partialTicks) {
		boolean hovered = getProperty(UIElement.IsHoveredProperty);
		boolean isChecked = getProperty(IsCheckedProperty);
		
		Shader bgEffect = (isPressed || isChecked) ? GuiManager.buttonPressedBackgroundColor.getValue() 
					: hovered ? GuiManager.componentHoveredBackgroundColor.getValue()
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
	
	public void setOnChanged(Consumer<Boolean> onChanged) {
		this.onChanged = onChanged;
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
					Boolean isChecked = getProperty(IsCheckedProperty);
					setProperty(IsCheckedProperty, !Boolean.TRUE.equals(isChecked));
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
