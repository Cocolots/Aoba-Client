/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.components;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.UIProperty;
import net.aoba.gui.types.GridDefinition;
import net.aoba.gui.types.HorizontalAlignment;
import net.aoba.gui.types.TextWrapping;
import net.aoba.gui.types.Thickness;
import net.aoba.gui.types.VerticalAlignment;
import net.aoba.gui.types.GridDefinition.RelativeUnit;
import net.aoba.utils.input.CursorStyle;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;

public class KeybindComponent extends Component implements KeyDownListener {
	private boolean listeningForKey;
	private final StringComponent label;
	private final StringComponent keyTextComponent;

	public static UIProperty<Key> SelectedKeyProperty = new UIProperty<>("SelectedKey", InputConstants.UNKNOWN, false, true, KeybindComponent::OnSelectedKeyPropertyChanged);
	public static UIProperty<String> HeaderProperty = new UIProperty<>("Header", "", false, true, KeybindComponent::OnHeaderPropertyChanged);
	
	private Consumer<Key> onChanged = null;
	private static void OnSelectedKeyPropertyChanged(UIElement sender, Key oldValue, Key newValue) {
		if(sender instanceof KeybindComponent keybind) {
			keybind.keyTextComponent.setProperty(StringComponent.TextProperty, keybind.getKeyDisplayText());
			
			if(keybind.onChanged != null)
				keybind.onChanged.accept(newValue);
		}
	}
	
	private static void OnHeaderPropertyChanged(UIElement sender, String oldValue, String newValue) {
		if(sender instanceof KeybindComponent keybind) {
			keybind.label.setProperty(StringComponent.TextProperty, newValue);
		}
	}
	
	public KeybindComponent() {
		setProperty(UIElement.CursorProperty, CursorStyle.Type);
		
		GridComponent grid = new GridComponent();
		grid.addColumnDefinition(new GridDefinition(1f, RelativeUnit.Relative));
		grid.addColumnDefinition(new GridDefinition(RelativeUnit.Auto));

		label = new StringComponent();
		label.setProperty(StringComponent.TextProperty, getProperty(KeybindComponent.HeaderProperty));
		label.setProperty(UIElement.VerticalAlignmentProperty, VerticalAlignment.Center);
		grid.addChild(label);

		RectangleComponent keyButton = new RectangleComponent();
		keyButton.bindProperty(UIElement.BackgroundProperty, GuiManager.componentBackgroundColor);
		keyButton.bindProperty(UIElement.BorderProperty, GuiManager.componentBorderColor);
		keyButton.bindProperty(UIElement.CornerRadiusProperty, GuiManager.roundingRadius);
		keyButton.setProperty(UIElement.PaddingProperty, new Thickness(10f, 4f, 10f, 4f));
		keyButton.setProperty(UIElement.VerticalAlignmentProperty, VerticalAlignment.Center);

		keyTextComponent = new StringComponent("N/A");
		keyTextComponent.setProperty(UIElement.VerticalAlignmentProperty, VerticalAlignment.Center);
		keyTextComponent.setProperty(UIElement.HorizontalAlignmentProperty, HorizontalAlignment.Center);
		keyTextComponent.setProperty(UIElement.IsHitTestVisibleProperty, false);
		keyTextComponent.setProperty(StringComponent.TextWrappingProperty, TextWrapping.NoWrap);
		keyButton.setContent(keyTextComponent);

		keyButton.setOnClicked(e -> {
			if (e.button == MouseButton.LEFT && e.action == MouseAction.DOWN) {
				setListeningForKey(true);
				e.cancel();
			}
		});

		grid.addChild(keyButton);
		setContent(grid);
	}
	
	private String getKeyDisplayText() {
		Key keybind = getProperty(SelectedKeyProperty);
		if (keybind == null || keybind.equals(InputConstants.UNKNOWN))
			return "N/A";
		return keybind.getDisplayName().getString();
	}


	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (listeningForKey) {
			Key key = event.GetKey() == InputConstants.KEY_ESCAPE
					? InputConstants.UNKNOWN
					: InputConstants.Type.KEYBOARD.getOrCreate(event.GetKey());

			setProperty(SelectedKeyProperty, key);
			setListeningForKey(false);
			event.cancel();
		}
	}

	private void setListeningForKey(boolean state) {
		if (listeningForKey == state)
			return;

		listeningForKey = state;
		if (listeningForKey) {
			GuiManager.requestFocus(this);
			AOBA.eventManager.AddListener(KeyDownListener.class, this);
		} else {
			GuiManager.clearFocus(this);
			AOBA.eventManager.RemoveListener(KeyDownListener.class, this);
		}
	}

	@Override
	protected void onLostFocus() {
		if (listeningForKey) {
			setListeningForKey(false);
			keyTextComponent.setProperty(StringComponent.TextProperty, getKeyDisplayText());
		}
	}
	
	public void setOnChanged(Consumer<Key> consumer) {
		this.onChanged = consumer;
	}
}
