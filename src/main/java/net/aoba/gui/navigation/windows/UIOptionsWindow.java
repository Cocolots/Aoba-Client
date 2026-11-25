/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.navigation.windows;

import java.io.IOException;
import java.util.List;

import net.aoba.Aoba;
import net.aoba.gui.GuiManager;
import net.aoba.gui.UIElement;
import net.aoba.gui.components.ButtonComponent;
import net.aoba.gui.components.CheckboxComponent;
import net.aoba.gui.components.ComboBoxComponent;
import net.aoba.gui.components.ExpanderComponent;
import net.aoba.gui.components.KeybindComponent;
import net.aoba.gui.components.ScrollComponent;
import net.aoba.gui.components.ShaderComponent;
import net.aoba.gui.components.SliderComponent;
import net.aoba.gui.components.StackPanelComponent;
import net.aoba.gui.components.StringComponent;
import net.aoba.gui.components.TextBoxComponent;
import net.aoba.gui.font.FontManager;
import net.aoba.gui.navigation.Window;
import net.aoba.gui.themes.GuiThemes;
import net.aoba.gui.themes.ThemePreset;
import net.aoba.gui.types.BindingMode;
import net.aoba.gui.types.SizeToContent;
import net.aoba.settings.types.ShaderSetting;

public class UIOptionsWindow extends Window {
	public UIOptionsWindow() {
		super("UI Settings", 600, 200);
		sizeToContent = SizeToContent.Both;
		setProperty(UIElement.MinWidthProperty, 420.0f);

		StackPanelComponent stackPanel = new StackPanelComponent();
		stackPanel.setSpacing(8f);

		// Theme presets
		stackPanel.addChild(createHeader("Theme Presets"));
		ComboBoxComponent themeCombo = new ComboBoxComponent();
		themeCombo.setProperty(ComboBoxComponent.ItemsSourceProperty, GuiThemes.themes());
		ThemePreset selectedTheme = GuiThemes.findById(GuiManager.themePreset.getValue())
				.orElse(GuiThemes.defaultTheme());
		themeCombo.setProperty(ComboBoxComponent.SelectedItemProperty, selectedTheme);
		themeCombo.setOnItemChanged(item -> {
			if (item instanceof ThemePreset theme) {
				GuiThemes.apply(theme);
			}
		});
		stackPanel.addChild(themeCombo);

		StringComponent themeNote = new StringComponent("Fonts and keybinds are preserved.");
		themeNote.bindProperty(UIElement.ForegroundProperty, GuiManager.foregroundAccentColor);
		stackPanel.addChild(themeNote);

		TextBoxComponent themeNameInput = new TextBoxComponent();
		themeNameInput.setProperty(TextBoxComponent.PlaceholderText, "New theme name...");
		StringComponent themeStatus = new StringComponent("Themes are stored in .minecraft/aoba/themes.");
		themeStatus.bindProperty(UIElement.ForegroundProperty, GuiManager.foregroundAccentColor);
		ButtonComponent saveThemeButton = new ButtonComponent(() -> {
			try {
				ThemePreset savedTheme = GuiThemes.saveCurrent(themeNameInput.getProperty(TextBoxComponent.TextProperty));
				themeCombo.setProperty(ComboBoxComponent.ItemsSourceProperty, GuiThemes.themes());
				themeCombo.setProperty(ComboBoxComponent.SelectedItemProperty, savedTheme);
				themeNameInput.setProperty(TextBoxComponent.TextProperty, "");
				themeStatus.setProperty(StringComponent.TextProperty, "Saved " + savedTheme.displayName() + ".");
			} catch (IOException | IllegalArgumentException e) {
				themeStatus.setProperty(StringComponent.TextProperty, "Could not save theme: " + e.getMessage());
			}
		});
		saveThemeButton.setContent(new StringComponent("Save Current Theme"));
		stackPanel.addChild(themeNameInput);
		stackPanel.addChild(saveThemeButton);
		stackPanel.addChild(themeStatus);

		// Font
		stackPanel.addChild(createHeader("HUD Font"));
		List<String> fontNames = Aoba.getInstance().fontManager.fonts.keySet().stream().toList();
		ComboBoxComponent fontCombo = new ComboBoxComponent();
		fontCombo.setProperty(ComboBoxComponent.ItemsSourceProperty, fontNames);
		fontCombo.setProperty(ComboBoxComponent.ItemsTemplate, name -> {
			StringComponent label = new StringComponent((String) name);
			label.setProperty(UIElement.FontProperty, Aoba.getInstance().fontManager.getFont((String) name));
			return label;
		});
		fontCombo.setProperty(ComboBoxComponent.SelectedItemProperty, GuiManager.fontSetting.getFontName());
		fontCombo.setOnItemChanged(s -> {
			if (s instanceof String name) {
				GuiManager.fontSetting.setFontName(name);
			}
		});
		stackPanel.addChild(fontCombo);

		// Colors
		stackPanel.addChild(createHeader("Colors"));
		StackPanelComponent colorStack = new StackPanelComponent();
		colorStack.setSpacing(8f);
		addShaderEditor(colorStack, GuiManager.foregroundColor);
		addShaderEditor(colorStack, GuiManager.foregroundHeaderColor);
		addShaderEditor(colorStack, GuiManager.foregroundAccentColor);
		addShaderEditor(colorStack, GuiManager.windowBorderColor);
		addShaderEditor(colorStack, GuiManager.panelBorderColor);
		addShaderEditor(colorStack, GuiManager.componentBorderColor);
		addShaderEditor(colorStack, GuiManager.windowBackgroundColor);
		addShaderEditor(colorStack, GuiManager.panelBackgroundColor);
		addShaderEditor(colorStack, GuiManager.componentBackgroundColor);
		addShaderEditor(colorStack, GuiManager.componentHoveredBackgroundColor);
		addShaderEditor(colorStack, GuiManager.buttonBackgroundColor);
		addShaderEditor(colorStack, GuiManager.buttonHoverBackgroundColor);
		addShaderEditor(colorStack, GuiManager.buttonPressedBackgroundColor);
		addShaderEditor(colorStack, GuiManager.buttonBorderColor);

		ScrollComponent colorScroll = new ScrollComponent();
		colorScroll.setContent(colorStack);
		colorScroll.setProperty(UIElement.ClipToBoundsProperty, true);
		colorScroll.setProperty(UIElement.MaxHeightProperty, 320.0f);
		stackPanel.addChild(colorScroll);

		// Styling
		stackPanel.addChild(createHeader("Styling"));
		SliderComponent roundingRadiusSlider = new SliderComponent();
		roundingRadiusSlider.setProperty(SliderComponent.HeaderProperty, GuiManager.roundingRadius.displayName);
		roundingRadiusSlider.setProperty(SliderComponent.MinimumProperty, GuiManager.roundingRadius.min_value);
		roundingRadiusSlider.setProperty(SliderComponent.MaximumProperty, GuiManager.roundingRadius.max_value);
		roundingRadiusSlider.bindProperty(SliderComponent.ValueProperty, GuiManager.roundingRadius, BindingMode.TwoWay);
		roundingRadiusSlider.setProperty(SliderComponent.StepProperty, GuiManager.roundingRadius.step);
		stackPanel.addChild(roundingRadiusSlider);

		SliderComponent lineThicknessSlider = new SliderComponent();
		lineThicknessSlider.setProperty(SliderComponent.HeaderProperty, GuiManager.lineThickness.displayName);
		lineThicknessSlider.setProperty(SliderComponent.MinimumProperty, GuiManager.lineThickness.min_value);
		lineThicknessSlider.setProperty(SliderComponent.MaximumProperty, GuiManager.lineThickness.max_value);
		lineThicknessSlider.bindProperty(SliderComponent.ValueProperty, GuiManager.lineThickness, BindingMode.TwoWay);
		lineThicknessSlider.setProperty(SliderComponent.StepProperty, GuiManager.lineThickness.step);
		stackPanel.addChild(lineThicknessSlider);

		// Interface behavior
		stackPanel.addChild(createHeader("Interface"));
		CheckboxComponent customTitleScreenCheckbox = new CheckboxComponent();
		customTitleScreenCheckbox.setProperty(CheckboxComponent.HeaderProperty, GuiManager.enableCustomTitle.displayName);
		customTitleScreenCheckbox.bindProperty(CheckboxComponent.IsCheckedProperty, GuiManager.enableCustomTitle,
				BindingMode.TwoWay);
		stackPanel.addChild(customTitleScreenCheckbox);

		CheckboxComponent tooltipCheckbox = new CheckboxComponent();
		tooltipCheckbox.setProperty(CheckboxComponent.HeaderProperty, GuiManager.enableTooltips.displayName);
		tooltipCheckbox.bindProperty(CheckboxComponent.IsCheckedProperty, GuiManager.enableTooltips, BindingMode.TwoWay);
		stackPanel.addChild(tooltipCheckbox);

		// Keybinds
		stackPanel.addChild(createHeader("Keybinds"));
		KeybindComponent clickGuiButtonComponent = new KeybindComponent();
		clickGuiButtonComponent.bindProperty(KeybindComponent.SelectedKeyProperty,
				Aoba.getInstance().guiManager.clickGuiButton, BindingMode.TwoWay);
		clickGuiButtonComponent.setProperty(KeybindComponent.HeaderProperty,
				Aoba.getInstance().guiManager.clickGuiButton.displayName);
		stackPanel.addChild(clickGuiButtonComponent);

		setContent(stackPanel);
	}

	private static StringComponent createHeader(String text) {
		StringComponent header = new StringComponent(text);
		header.setProperty(UIElement.FontWeightProperty, FontManager.WEIGHT_BOLD);
		header.bindProperty(UIElement.ForegroundProperty, GuiManager.foregroundHeaderColor);
		return header;
	}

	private static void addShaderEditor(StackPanelComponent parent, ShaderSetting setting) {
		ShaderComponent shader = new ShaderComponent();
		shader.bindProperty(ShaderComponent.ShaderProperty, setting, BindingMode.TwoWay);
		ExpanderComponent expander = new ExpanderComponent(setting.displayName);
		expander.setProperty(ExpanderComponent.ExpanderContentProperty, shader);
		parent.addChild(expander);
	}
}
