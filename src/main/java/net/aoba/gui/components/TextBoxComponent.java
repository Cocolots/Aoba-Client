package net.aoba.gui.components;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.events.MouseClickEvent;
import net.aoba.event.events.MouseMoveEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.UIProperty;
import net.aoba.gui.colors.Color;
import net.aoba.gui.colors.Colors;
import net.aoba.gui.types.TextWrapping;
import net.aoba.gui.types.Thickness;
import net.aoba.gui.types.VerticalAlignment;
import net.aoba.rendering.Renderer2D;
import net.aoba.rendering.shaders.Shader;
import net.aoba.utils.input.CursorStyle;
import net.aoba.utils.input.TextEditingState;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;
import net.minecraft.client.gui.Font;

public class TextBoxComponent extends Component implements KeyDownListener {
	private boolean listeningForKey;

	private static final Shader CARET_SHADER = Shader.solid(Colors.White);
	private static final Shader PLACEHOLDER_SHADER = Shader.solid(Colors.Gray);
	private static final Shader SELECTION_SHADER = Shader.solid(new Color(0.25f, 0.5f, 1f, 0.35f));
	public static UIProperty<String> HeaderProperty = new UIProperty<String>("Header", "", false, true);
	public static UIProperty<String> TextProperty = new UIProperty<String>("Text", "", false, true,
			TextBoxComponent::onTextPropertyChanged);
	public static UIProperty<String> PlaceholderText = new UIProperty<String>("PlaceholderText", "", false, true,
			TextBoxComponent::onPlaceholderPropertyChanged);
	public static final UIProperty<Float> FontSizeProperty = new UIProperty<>("FontSize", 12f, true, true,
			TextBoxComponent::onFontSizePropertyChanged);

	private final TextEditingState editor = new TextEditingState();
	private boolean isFocused;
	private boolean draggingSelection;
	private boolean applyingEditorText;
	private int caretTick;
	private boolean caretVisible = true;
	private int backspaceDelayTimer;

	private final RectangleComponent box;
	private final StringComponent textComponent;
	private Consumer<String> onTextChanged;

	private static void onTextPropertyChanged(UIElement sender, String oldValue, String newValue) {
		if (sender instanceof TextBoxComponent textBoxComponent) {
			if (!textBoxComponent.applyingEditorText) {
				textBoxComponent.editor.setText(newValue);
			}
			textBoxComponent.refreshDisplayedText();
			if (textBoxComponent.onTextChanged != null)
				textBoxComponent.onTextChanged.accept(newValue);
		}
	}

	private static void onPlaceholderPropertyChanged(UIElement sender, String oldValue, String newValue) {
		if (sender instanceof TextBoxComponent textBoxComponent) {
			textBoxComponent.refreshDisplayedText();
		}
	}

	private static void onFontSizePropertyChanged(UIElement sender, Float oldValue, Float newValue) {
		if (sender instanceof TextBoxComponent textBoxComponent) {
			textBoxComponent.textComponent.setProperty(StringComponent.FontSizeProperty, newValue);
		}
	}

	public TextBoxComponent() {
		setProperty(UIElement.CursorProperty, CursorStyle.Type);
		bindProperty(UIElement.BackgroundProperty, GuiManager.componentBackgroundColor);
		bindProperty(UIElement.BorderProperty, GuiManager.componentBorderColor);
		bindProperty(UIElement.CornerRadiusProperty, GuiManager.roundingRadius);

		box = new RectangleComponent();
		box.setProperty(UIElement.PaddingProperty, new Thickness(4f));

		editor.setText(getProperty(TextProperty));
		textComponent = new StringComponent(editor.getText());
		textComponent.setProperty(UIElement.VerticalAlignmentProperty, VerticalAlignment.Center);
		textComponent.setProperty(StringComponent.TextWrappingProperty, TextWrapping.NoWrap);
		textComponent.setProperty(UIElement.IsHitTestVisibleProperty, false);
		box.setContent(textComponent);
		setContent(box);
		refreshDisplayedText();

		setOnClicked(e -> {
			if (e.button == MouseButton.LEFT && e.action == MouseAction.DOWN) {
				if (!listeningForKey) {
					setListeningForKey(true);
				}
				int position = getCaretPosition(e.mouseX);
				boolean shiftDown = isShiftDown();
				if (shiftDown) {
					editor.moveCaret(position, true);
				} else {
					editor.startSelection(position);
				}
				draggingSelection = true;
				e.cancel();
			}
		});
	}

	@Override
	public void update() {
		super.update();
		if (isFocused) {
			caretTick++;
			if (caretTick >= 10) {
				caretVisible = !caretVisible;
				caretTick = 0;
			}

			if (InputConstants.isKeyDown(InputConstants.KEY_BACKSPACE)) {
				backspaceDelayTimer++;
				if (backspaceDelayTimer > 15 && (backspaceDelayTimer - 15) % 2 == 0) {
					String previous = editor.getText();
					editor.backspace();
					if (!previous.equals(editor.getText())) {
						applyEditorText();
					}
				}
			} else {
				backspaceDelayTimer = 0;
			}
		} else {
			caretVisible = false;
			caretTick = 0;
			backspaceDelayTimer = 0;
			editor.clearSelection();
			draggingSelection = false;
		}
	}

	@Override
	public void draw(Renderer2D renderer, float partialTicks) {
		if (isFocused && editor.hasSelection()) {
			float textX = textComponent.getActualSize().x();
			float textY = textComponent.getActualSize().y();
			float textHeight = textComponent.getPreferredSize().height();
			float selectionX = textX + getTextWidth(editor.getText().substring(0, editor.getSelectionStart()));
			float selectionWidth = getTextWidth(editor.getSelectedText());
			renderer.drawBox(selectionX, textY, selectionWidth, textHeight, SELECTION_SHADER);
		}

		super.draw(renderer, partialTicks);

		if (isFocused && caretVisible) {
			float textX = textComponent.getActualSize().x();
			float textY = textComponent.getActualSize().y();
			float textHeight = textComponent.getPreferredSize().height();
			float caretX = textX + getTextWidth(editor.getText().substring(0, editor.getCaret()));
			renderer.drawBox(caretX, textY, 2, textHeight, CARET_SHADER);
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (draggingSelection && isFocused) {
			editor.dragSelection(getCaretPosition(event.getX()));
			caretVisible = true;
			caretTick = 0;
			event.cancel();
		}
		super.onMouseMove(event);
	}

	@Override
	public void onMouseClick(MouseClickEvent event) {
		super.onMouseClick(event);

		if (event.button == MouseButton.LEFT && event.action == MouseAction.UP) {
			draggingSelection = false;
		}
		if (event.button == MouseButton.LEFT && event.action == MouseAction.DOWN) {
			boolean hovered = getProperty(UIElement.IsHoveredProperty);
			if (!hovered && listeningForKey) {
				setListeningForKey(false);
			}
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (!listeningForKey) {
			return;
		}

		caretVisible = true;
		caretTick = 0;
		int key = event.GetKey();
		boolean ctrlDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
				|| InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);

		if (ctrlDown) {
			if (key == InputConstants.KEY_A) {
				editor.selectAll();
			} else if (key == InputConstants.KEY_C) {
				String selected = editor.copy();
				if (!selected.isEmpty()) {
					AobaClient.MC.keyboardHandler.setClipboard(selected);
				}
			} else if (key == InputConstants.KEY_X) {
				String selected = editor.copy();
				if (!selected.isEmpty()) {
					AobaClient.MC.keyboardHandler.setClipboard(selected);
					editor.cut();
					applyEditorText();
				}
			} else if (key == InputConstants.KEY_V) {
				String clipboard = AobaClient.MC.keyboardHandler.getClipboard();
				if (clipboard != null) {
					editor.insert(clipboard);
					applyEditorText();
				}
			} else {
				event.cancel();
				return;
			}
			event.cancel();
			return;
		}

		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_ESCAPE) {
			setListeningForKey(false);
		} else if (key == InputConstants.KEY_LEFT) {
			editor.moveCaretBy(-1, isShiftDown());
		} else if (key == InputConstants.KEY_RIGHT) {
			editor.moveCaretBy(1, isShiftDown());
		} else if (key == InputConstants.KEY_HOME) {
			editor.moveCaret(0, isShiftDown());
		} else if (key == InputConstants.KEY_END) {
			editor.moveCaret(editor.getText().length(), isShiftDown());
		} else if (key == InputConstants.KEY_BACKSPACE) {
			editor.backspace();
			applyEditorText();
		} else if (key == InputConstants.KEY_DELETE) {
			editor.delete();
			applyEditorText();
		} else if (key == InputConstants.KEY_SPACE) {
			editor.insert(" ");
			applyEditorText();
		} else if (keyIsValid(event.GetKeyCode())) {
			char keyCode = (char) event.GetKeyCode();
			keyCode = isShiftDown() ? Character.toUpperCase(keyCode) : Character.toLowerCase(keyCode);
			editor.insert(String.valueOf(keyCode));
			applyEditorText();
		}
		event.cancel();
	}

	private boolean isShiftDown() {
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}

	private void applyEditorText() {
		applyingEditorText = true;
		try {
			setProperty(TextProperty, editor.getText());
		} finally {
			applyingEditorText = false;
		}
	}

	private float getTextWidth(String text) {
		return Renderer2D.getStringWidth(text, getFont(), textComponent.getProperty(StringComponent.FontSizeProperty));
	}

	private Font getFont() {
		return textComponent.getProperty(UIElement.FontProperty).getRenderer(
				textComponent.getProperty(StringComponent.FontSizeProperty),
				textComponent.getProperty(UIElement.FontWeightProperty));
	}

	private int getCaretPosition(double mouseX) {
		String text = editor.getText();
		float textX = textComponent.getActualSize().x();
		float relativeX = (float) mouseX - textX;
		if (relativeX <= 0) {
			return 0;
		}

		for (int i = 0; i < text.length(); i++) {
			float current = getTextWidth(text.substring(0, i));
			float next = getTextWidth(text.substring(0, i + 1));
			if (relativeX < (current + next) / 2f) {
				return i;
			}
		}
		return text.length();
	}

	private void refreshDisplayedText() {
		String text = getProperty(TextProperty);
		boolean empty = text == null || text.isEmpty();
		if (empty && !isFocused) {
			String placeholder = getProperty(PlaceholderText);
			textComponent.setProperty(StringComponent.TextProperty, placeholder == null ? "" : placeholder);
			textComponent.setProperty(UIElement.ForegroundProperty, PLACEHOLDER_SHADER);
		} else {
			textComponent.setProperty(StringComponent.TextProperty, text == null ? "" : text);
			textComponent.clearProperty(UIElement.ForegroundProperty);
		}
	}

	private boolean keyIsValid(int key) {
		return key == 45 || (key >= 48 && key <= 57) || (key >= 65 && key <= 90) || (key >= 97 && key <= 122);
	}

	private void setListeningForKey(boolean state) {
		if (listeningForKey == state)
			return;

		listeningForKey = state;
		isFocused = state;
		if (listeningForKey) {
			GuiManager.requestFocus(this);
			caretVisible = true;
			caretTick = 0;
			Aoba.getInstance().eventManager.AddListener(KeyDownListener.class, this);
		} else {
			editor.clearSelection();
			draggingSelection = false;
			GuiManager.clearFocus(this);
			Aoba.getInstance().eventManager.RemoveListener(KeyDownListener.class, this);
		}
		refreshDisplayedText();
	}

	@Override
	protected void onLostFocus() {
		if (listeningForKey) {
			listeningForKey = false;
			isFocused = false;
			editor.clearSelection();
			draggingSelection = false;
			Aoba.getInstance().eventManager.RemoveListener(KeyDownListener.class, this);
			refreshDisplayedText();
		}
	}

	public void setOnTextChanged(Consumer<String> onTextChanged) {
		this.onTextChanged = onTextChanged;
	}

	@Override
	protected void onVisibilityChanged(Boolean oldValue, Boolean newValue) {
		super.onVisibilityChanged(oldValue, newValue);
		if (!newValue)
			setListeningForKey(false);
	}
}
