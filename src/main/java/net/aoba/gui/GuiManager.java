/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.events.MouseClickEvent;
import net.aoba.event.events.MouseMoveEvent;
import net.aoba.event.events.MouseScrollEvent;
import org.joml.Matrix3x2fStack;
import net.aoba.event.events.Render2DEvent;
import net.aoba.event.events.TickEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.event.listeners.MouseClickListener;
import net.aoba.event.listeners.MouseMoveListener;
import net.aoba.event.listeners.MouseScrollListener;
import net.aoba.event.listeners.Render2DListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.gui.colors.Color;
import net.aoba.gui.colors.Colors;
import net.aoba.gui.font.FontManager;
import net.aoba.gui.components.GridComponent;
import net.aoba.gui.components.ImageComponent;
import net.aoba.gui.components.ModuleComponent;
import net.aoba.gui.components.TabComponent;
import net.aoba.gui.components.TabItemComponent;
import net.aoba.gui.components.StackPanelComponent;
import net.aoba.gui.components.StringComponent;
import net.aoba.gui.navigation.HudWindow;
import net.aoba.gui.navigation.Page;
import net.aoba.gui.navigation.Popup;
import net.aoba.gui.navigation.Window;
import net.aoba.gui.navigation.huds.ArmorHud;
import net.aoba.gui.navigation.huds.CoordsHud;
import net.aoba.gui.navigation.huds.DayHud;
import net.aoba.gui.navigation.huds.FPSHud;
import net.aoba.gui.navigation.huds.InputHud;
import net.aoba.gui.navigation.huds.ModuleArrayListHud;
import net.aoba.gui.navigation.huds.ModuleSelectorHud;
import net.aoba.gui.navigation.huds.NetherCoordsHud;
import net.aoba.gui.navigation.huds.PingHud;
import net.aoba.gui.navigation.huds.RadarHud;
import net.aoba.gui.navigation.huds.SpeedHud;
import net.aoba.gui.navigation.huds.TimeHud;
import net.aoba.gui.navigation.huds.WatermarkHud;
import net.aoba.gui.navigation.windows.AntiCheatWindow;
import net.aoba.gui.navigation.windows.AuthCrackerWindow;
import net.aoba.gui.navigation.windows.FriendsWindow;
import net.aoba.gui.navigation.windows.PathfindingWindow;
import net.aoba.gui.navigation.windows.SearchWindow;
import net.aoba.gui.navigation.windows.UIOptionsWindow;
import net.aoba.gui.types.GridDefinition;
import net.aoba.gui.types.Rectangle;
import net.aoba.gui.types.Size;
import net.aoba.gui.types.SizeToContent;
import net.aoba.gui.types.VerticalAlignment;
import net.aoba.gui.types.GridDefinition.RelativeUnit;
import net.aoba.gui.navigation.windows.MacroWindow;
import net.aoba.gui.navigation.windows.SettingsWindow;
import net.aoba.gui.navigation.windows.ToggleHudsTab;
import net.aoba.managers.SettingManager;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.ShaderSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.FontSetting;
import net.aoba.settings.types.KeybindSetting;
import net.aoba.utils.input.CursorStyle;
import net.aoba.utils.input.Input;
import net.aoba.rendering.Renderer2D;
import net.aoba.rendering.shaders.Shader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiManager implements KeyDownListener, TickListener, Render2DListener, MouseClickListener,
		MouseMoveListener, MouseScrollListener {
	private static final Minecraft MC = Minecraft.getInstance();
	
	public KeybindSetting clickGuiButton = KeybindSetting.builder().id("key.clickgui").displayName("ClickGUI Key")
			.defaultValue(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RSHIFT)).build();
	
	private static CursorStyle currentCursor = CursorStyle.Default;
	private static String tooltip = null;
	private final KeyMapping esc = new KeyMapping("key.esc", InputConstants.KEY_ESCAPE, AobaClient.AOBA_CATEGORY);

	private boolean clickGuiOpen = false;
	private static boolean isKeyboardInputActive = false;
	private static UIElement focusedElement = null;
	private final HashMap<Object, Window> pinnedHuds = new HashMap<Object, Window>();
	private Popup popup;

	// Tab Control and Pages
	public TabComponent clickGuiTabControl;
	private final List<Page> pages = new ArrayList<>();
	public Page modulesPane = new Page("Modules");
	public Page toolsPane = new Page("Tools");
	public Page hudPane = new Page("Hud");

	// Global HUD Settings
	public static BooleanSetting enableCustomTitle = BooleanSetting.builder().id("enable_custom_title")
			.displayName("Enable Custom Title Screen").defaultValue(true).build();

	public static BooleanSetting enableTooltips = BooleanSetting.builder().id("enable_tooltips")
			.displayName("Enable Tooltips").defaultValue(true).build();

	public static ShaderSetting foregroundColor = ShaderSetting.builder().id("ui_foreground_color")
			.displayName("UI Foreground Color").description("Color of the foreground.")
			.defaultValue(Shader.solid(Colors.White)).build();

	public static ShaderSetting foregroundHeaderColor = ShaderSetting.builder().id("ui_header_foreground_color")
			.displayName("UI Header Foreground Color").description("Color of the header foreground.")
			.defaultValue(Shader.solid(new Color(210, 80, 255))).build();

	public static ShaderSetting foregroundAccentColor = ShaderSetting.builder().id("ui_accent_foreground_color")
			.displayName("UI Accent Foreground Color").description("Color of the accented foreground.")
			.defaultValue(Shader.solid(Colors.Gray)).build();

	public static ShaderSetting windowBorderColor = ShaderSetting.builder().id("ui_window_border_color")
			.displayName("UI Border Color").description("Color of the borders.")
			.defaultValue(Shader.solid(new Color(0, 0, 0))).build();

	public static ShaderSetting panelBorderColor = ShaderSetting.builder().id("ui_panel_border_color")
			.displayName("UI Panel Border Color").description("Accent color of the background.")
			.defaultValue(Shader.solid(new Color(100, 100, 100, 100))).build();

	public static ShaderSetting componentBorderColor = ShaderSetting.builder().id("ui_component_border_color")
			.displayName("UI Component Border Color").description("Background colors of components (i.e TextBox).")
			.defaultValue(Shader.solid(new Color(115, 115, 115, 200))).build();

	public static ShaderSetting windowBackgroundColor = ShaderSetting.builder().id("ui_window_background_color")
			.displayName("UI Background Color").description("Color of the background.")
			.defaultValue(Shader.blur(new Color(55, 55, 55, 255), 60f, 5f)).build();

	public static ShaderSetting panelBackgroundColor = ShaderSetting.builder().id("ui_panel_background_color")
			.displayName("UI Panel Background Color").description("Accent color of the background.")
			.defaultValue(Shader.solid(new Color(90, 90, 90, 60))).build();

	public static ShaderSetting componentBackgroundColor = ShaderSetting.builder().id("ui_component_background_color")
			.displayName("UI Component Background Color").description("Background colors of components (i.e TextBox).")
			.defaultValue(Shader.solid(new Color(20, 20, 20, 95))).build();

	public static ShaderSetting buttonBackgroundColor = ShaderSetting.builder().id("ui_button_background_color")
			.displayName("UI Button Background Color").description("Background color of buttons.")
			.defaultValue(Shader.gradient(new Color(180, 50, 220), new Color(100, 20, 160), 90f)).build();

	public static ShaderSetting buttonHoverBackgroundColor = ShaderSetting.builder()
			.id("ui_button_hover_background_color").displayName("UI Button Hover Background Color")
			.description("Background color of buttons when hovered.")
			.defaultValue(Shader.gradient(new Color(210, 80, 255), new Color(130, 40, 190), 90f)).build();

	public static ShaderSetting buttonBorderColor = ShaderSetting.builder().id("ui_button_border_color")
			.displayName("UI Button Border Color").description("Border color of buttons.")
			.defaultValue(Shader.gradient(new Color(220, 100, 255), new Color(140, 50, 200), 90f)).build();

	public static FloatSetting roundingRadius = FloatSetting.builder().id("hud_rounding_radius")
			.displayName("Corner Rounding").description("The radius of the rounding on hud.").defaultValue(12f)
			.minValue(0f).maxValue(20f).step(1f).build();

	public static FloatSetting lineThickness = FloatSetting.builder().id("ui_line_thickness")
			.displayName("Line Thickness").description("The thickness of borders and outlines.").defaultValue(0.5f)
			.minValue(0.5f).maxValue(4f).step(0.5f).build();

	public static FontSetting fontSetting = FontSetting.builder().id("aoba_font").displayName("Font")
			.description("The font that Aoba will use.").fontName(FontManager.DEFAULT_FONT).build();

	public ModuleSelectorHud moduleSelector;
	public ArmorHud armorHud;
	public RadarHud radarHud;
	public TimeHud timeHud;
	public DayHud dayHud;
	public ModuleArrayListHud moduleArrayListHud;
	public WatermarkHud watermarkHud;
	public CoordsHud coordsHud;
	public NetherCoordsHud netherCoordsHud;
	public FPSHud fpsHud;
	public PingHud pingHud;
	public SpeedHud speedHud;
	public InputHud inputHud;

	public GuiManager() {
		clickGuiTabControl = new TabComponent();

		SettingManager.registerGlobalSetting(windowBorderColor);
		SettingManager.registerGlobalSetting(windowBackgroundColor);
		SettingManager.registerGlobalSetting(panelBorderColor);
		SettingManager.registerGlobalSetting(panelBackgroundColor);
		SettingManager.registerGlobalSetting(componentBackgroundColor);
		SettingManager.registerGlobalSetting(foregroundColor);
		SettingManager.registerGlobalSetting(foregroundHeaderColor);
		SettingManager.registerGlobalSetting(foregroundAccentColor);
		SettingManager.registerGlobalSetting(buttonBackgroundColor);
		SettingManager.registerGlobalSetting(buttonHoverBackgroundColor);
		SettingManager.registerGlobalSetting(buttonBorderColor);
		SettingManager.registerGlobalSetting(roundingRadius);
		SettingManager.registerGlobalSetting(lineThickness);
		SettingManager.registerGlobalSetting(fontSetting);

		SettingManager.registerSetting(clickGuiButton);
		Aoba.getInstance().eventManager.AddListener(KeyDownListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(Render2DListener.class, this);
		Aoba.getInstance().eventManager.AddListener(MouseClickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(MouseMoveListener.class, this);
		Aoba.getInstance().eventManager.AddListener(MouseScrollListener.class, this);
	}

	public void Initialize() {
		System.out.println("Initializing");
		toolsPane.addWindow(new AuthCrackerWindow());
		toolsPane.addWindow(new PathfindingWindow());
		toolsPane.addWindow(new MacroWindow());
		
		moduleSelector = new ModuleSelectorHud();
		armorHud = new ArmorHud(0, 0);
		radarHud = new RadarHud(0, 0);
		timeHud = new TimeHud(0, 0);
		dayHud = new DayHud(0, 0);
		moduleArrayListHud = new ModuleArrayListHud(0, 0);
		watermarkHud = new WatermarkHud(0, 0);
		coordsHud = new CoordsHud(0, 0);
		netherCoordsHud = new NetherCoordsHud(0, 0);
		fpsHud = new FPSHud(0, 0);
		pingHud = new PingHud(0, 0);
		speedHud = new SpeedHud(0, 0);
		inputHud = new InputHud(0,0);

		ArrayList<HudWindow> huds = Lists.newArrayList(moduleSelector, armorHud, radarHud, timeHud, dayHud,
				moduleArrayListHud, watermarkHud, coordsHud, netherCoordsHud, fpsHud, pingHud, speedHud, inputHud);
		hudPane.addWindow(new UIOptionsWindow());
		hudPane.addWindow(new ToggleHudsTab(huds));
		Map<String, Category> categories = Category.getAllCategories();
		float xOffset = 50;

		for (Category category : categories.values()) {
			Window tab = new Window(category.getName(), xOffset, 75.0f);
			tab.setSizeToContent(SizeToContent.Height);
			StackPanelComponent stackPanel = new StackPanelComponent();
			stackPanel.setSpacing(2f);

			GridComponent gridComponent = new GridComponent();
			gridComponent.setProperty(GridComponent.HorizontalSpacingProperty, 8f);
			gridComponent.setProperty(UIElement.IsHitTestVisibleProperty, false);
			gridComponent.addColumnDefinition(new GridDefinition(RelativeUnit.Auto));
			gridComponent.addColumnDefinition(new GridDefinition(1, RelativeUnit.Relative)); // Fill all remaining space

			ImageComponent img = new ImageComponent();
			img.setProperty(ImageComponent.ImageProperty, category.getIcon());
			img.setProperty(UIElement.IsHitTestVisibleProperty, false);
			img.setProperty(UIElement.WidthProperty, 16f);
			img.setProperty(UIElement.HeightProperty, 16f);
			img.bindProperty(UIElement.ForegroundProperty, GuiManager.foregroundHeaderColor);
			gridComponent.addChild(img);

			StringComponent title = new StringComponent();
			title.setProperty(StringComponent.TextProperty, category.getName());
			title.setProperty(UIElement.FontWeightProperty, FontManager.WEIGHT_BOLD);
			title.setProperty(UIElement.VerticalAlignmentProperty, VerticalAlignment.Center);
			title.setProperty(UIElement.IsHitTestVisibleProperty, false);
			title.bindProperty(UIElement.ForegroundProperty, GuiManager.foregroundHeaderColor);
			gridComponent.addChild(title);

			stackPanel.addChild(gridComponent);

			// Loop through modules and add them to the correct category
			for (Module module : Aoba.getInstance().moduleManager.modules) {
				if (module.getCategory().equals(category)) {
					ModuleComponent button = new ModuleComponent();
					button.setProperty(ModuleComponent.ModuleProperty, module);
					stackPanel.addChild(button);
				}
			}

			tab.setContent(stackPanel);
			tab.setProperty(UIElement.MinWidthProperty, 200f);
			modulesPane.addWindow(tab);

			xOffset += tab.getProperty(UIElement.MinWidthProperty) + 10;
		}

		SearchWindow searchWindow = new SearchWindow();
		searchWindow.position.setX(xOffset);
		searchWindow.position.setY(73f);
		modulesPane.addWindow(searchWindow);
		modulesPane.addWindow(new SettingsWindow());
		modulesPane.addWindow(new AntiCheatWindow());
		modulesPane.addWindow(new FriendsWindow());

		pages.add(modulesPane);
		pages.add(toolsPane);
		pages.add(hudPane);

		clickGuiTabControl.addChild(new TabItemComponent("Modules"));
		clickGuiTabControl.addChild(new TabItemComponent("Tools"));
		clickGuiTabControl.addChild(new TabItemComponent("Hud"));
		clickGuiTabControl.setOnSelectionChanged(idx -> {
			for (int i = 0; i < pages.size(); i++)
				pages.get(i).setVisible(i == idx);
		});
		clickGuiTabControl.initialize();

		modulesPane.initialize();
		toolsPane.initialize();
		hudPane.initialize();

		clickGuiTabControl.setProperty(TabComponent.SelectedIndexProperty, 0);
		modulesPane.setVisible(true);
	}

	public static CursorStyle getCursor() {
		return currentCursor;
	}

	public static void setCursor(CursorStyle cursor) {
		currentCursor = cursor;
		Input.setCursorStyle(currentCursor);
	}

	public static String getTooltip() {
		return tooltip;
	}

	public static void setTooltip(String tt) {
		if (!Objects.equals(tooltip, tt))
			tooltip = tt;
	}

	public static boolean isKeyboardInputActive() {
		return isKeyboardInputActive;
	}

	public static void setKeyboardInputActive(boolean state) {
		isKeyboardInputActive = state;
	}

	public static UIElement getFocusedElement() {
		return focusedElement;
	}

	public static void requestFocus(UIElement element) {
		if (focusedElement == element)
			return;

		if (focusedElement != null) {
			focusedElement.onLostFocus();
		}

		focusedElement = element;
		isKeyboardInputActive = element != null;

		if (focusedElement != null) {
			focusedElement.onGotFocus();
		}
	}

	public static void clearFocus(UIElement element) {
		if (focusedElement == element) {
			focusedElement = null;
			isKeyboardInputActive = false;

			if (element != null) {
				element.onLostFocus();
			}
		}
	}

	public void addWindow(Window hud, String pageName) {
		for (Page page : pages) {
			if (page.getTitle().equals(pageName)) {
				page.addWindow(hud);
				page.moveToFront(hud);
				hud.initialize();
				break;
			}
		}
	}

	public void removeWindow(Window hud, String pageName) {
		for (Page page : pages) {
			if (page.getTitle().equals(pageName)) {
				page.removeWindow(hud);
				break;
			}
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (clickGuiButton.getValue().getValue() == event.GetKey() && MC.gui.screen() == null && !isKeyboardInputActive) {
			setClickGuiOpen(!clickGuiOpen);
			toggleMouse();
		}
	}

	public void setHudActive(HudWindow hud, boolean state) {
		if (state) {
			pinnedHuds.put(hud.getClass(), hud);
			hud.activated.silentSetValue(true);
			hudPane.addWindow(hud);
			hud.setProperty(UIElement.IsVisibleProperty, true);
		} else {
			pinnedHuds.remove(hud.getClass());
			hud.activated.silentSetValue(false);
			hudPane.removeWindow(hud);
			hud.setProperty(UIElement.IsVisibleProperty, false);
		}
	}

	@Override
	public void onTick(TickEvent.Pre event) {

	}

	@Override
	public void onTick(TickEvent.Post event) {
		if (clickGuiOpen) {
			clickGuiTabControl.update();
			int selectedIdx = clickGuiTabControl.getSelectedIndex();
			if (selectedIdx >= 0 && selectedIdx < pages.size())
				pages.get(selectedIdx).update();
		}

		/**
		 * Updates each of the Tab GUIs that are currently on the screen.
		 */
		for (Window hud : pinnedHuds.values()) {
			hud.update();
		}

		if (esc.isDown() && clickGuiOpen) {
			clickGuiOpen = false;
			toggleMouse();
		}
	}

	@Override
	public void onRender(Render2DEvent event) {
		Renderer2D renderer = event.getRenderer();
		GuiGraphicsExtractor drawContext = renderer.getDrawContext();
		float tickDelta = renderer.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		Matrix3x2fStack matrixStack = drawContext.pose();
		matrixStack.pushMatrix();

		int guiScale = MC.getWindow().calculateScale(MC.options.guiScale().get(), MC.isEnforceUnicode());
		matrixStack.scale(1.0f / guiScale, 1.0f / guiScale);

		com.mojang.blaze3d.platform.Window window = MC.getWindow();
		// Render ClickGUI and Topbar
		if (clickGuiOpen) {
			renderer.drawBox(0, 0, window.getScreenWidth(), window.getScreenHeight(),
					Shader.solid(new Color(26, 26, 26, 100)));

			// Measure and position TabComponent at top-center
			clickGuiTabControl.measureCore(new Size(window.getScreenWidth(), window.getScreenHeight()));
			Size tabSize = clickGuiTabControl.getPreferredSize();
			float tabX = (window.getScreenWidth() - tabSize.width()) / 2f;
			clickGuiTabControl.arrange(new Rectangle(tabX, 25, tabSize.width(), tabSize.height()));
			clickGuiTabControl.draw(renderer, tickDelta);

			// Draw selected page's windows
			int selectedIdx = clickGuiTabControl.getSelectedIndex();
			if (selectedIdx >= 0 && selectedIdx < pages.size())
				pages.get(selectedIdx).render(renderer, tickDelta);
		}

		// Render HUDS
		if (!clickGuiOpen) {
			for (Window hud : pinnedHuds.values()) {
				hud.draw(renderer, tickDelta);
			}
		}

		// Draw Popup on top of all UI elements
		if (popup != null) {
			popup.draw(renderer, tickDelta);
		}

		// Draw Tooltip on top of all UI elements
		if (tooltip != null && enableTooltips.getValue()) {
			int mouseX = (int) MC.mouseHandler.xpos();
			int mouseY = (int) MC.mouseHandler.ypos();
			Font tooltipFont = fontSetting.getValue().getRenderer();
			int tooltipWidth = Renderer2D.getStringWidth(tooltip, tooltipFont) + 2;
			int tooltipHeight = 10;

			renderer.drawRoundedBox(mouseX + 12, mouseY + 12, (tooltipWidth + 4) * 2, (tooltipHeight + 4) * 2,
					roundingRadius.getValue(), windowBackgroundColor.getValue());
			renderer.drawString(tooltip, mouseX + 18, mouseY + 18, foregroundColor.getValue(), tooltipFont);
		}
		matrixStack.popMatrix();
	}

	/**
	 * Gets whether or not the Click GUI is currently open.
	 *
	 * @return State of the Click GUI.
	 */
	public boolean isClickGuiOpen() {
		return clickGuiOpen;
	}

	public void setClickGuiOpen(boolean state) {
		clickGuiOpen = state;
		setTooltip(null);
		if (!state) {
			requestFocus(null);
			closePopup();
		}
	}

	public Popup getPopup() {
		return popup;
	}

	public void openPopup(UIElement target, UIElement content, Popup.PlacementMode mode) {
		closePopup();
		popup = new Popup();
		popup.open(target, content, mode);
	}

	public void openPopup(UIElement target, UIElement content) {
		openPopup(target, content, Popup.PlacementMode.Bottom);
	}

	public void closePopup() {
		if (popup != null) {
			popup.close();
			popup = null;
		}
	}

	@Override
	public void onMouseClick(MouseClickEvent event) {
		if (popup != null) {
			popup.onMouseClick(event);
			if (event.isCancelled())
				return;
		}

		if (clickGuiOpen) {
			clickGuiTabControl.onMouseClick(event);
			if (!event.isCancelled()) {
				int selectedIdx = clickGuiTabControl.getSelectedIndex();
				if (selectedIdx >= 0 && selectedIdx < pages.size())
					pages.get(selectedIdx).onMouseClick(event);
			}
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (popup != null) {
			popup.onMouseMove(event);
		}

		if (clickGuiOpen) {
			clickGuiTabControl.onMouseMove(event);
			int selectedIdx = clickGuiTabControl.getSelectedIndex();
			if (selectedIdx >= 0 && selectedIdx < pages.size())
				pages.get(selectedIdx).onMouseMove(event);
		}
	}

	@Override
	public void onMouseScroll(MouseScrollEvent event) {
		if (popup != null) {
			popup.onMouseScroll(event);
			if (event.isCancelled())
				return;
		}

		if (clickGuiOpen) {
			int selectedIdx = clickGuiTabControl.getSelectedIndex();
			if (selectedIdx >= 0 && selectedIdx < pages.size())
				pages.get(selectedIdx).onMouseScroll(event);
		}
	}

	/**
	 * Locks and unlocks the Mouse.
	 */
	public void toggleMouse() {
		if (MC.mouseHandler.isMouseGrabbed()) {
			MC.mouseHandler.releaseMouse();
		} else {
			MC.mouseHandler.grabMouse();
		}
	}
}
