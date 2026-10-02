package net.aoba.mixin;

import static net.aoba.AobaClient.MC;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.OptionalDouble;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.aoba.Aoba;
import net.aoba.gui.GuiManager;
import net.aoba.gui.colors.Colors;
import net.aoba.module.modules.render.Tooltips;
import net.aoba.rendering.shaders.Shader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(value = { AbstractContainerScreen.class })
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
	@Unique
	private Runnable postRender;
	@Unique
	private final MapRenderState mapRenderState = new MapRenderState();

	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	@Nullable
	protected Slot hoveredSlot;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;

	private boolean hasItems(ItemStack itemStack) {
		ItemContainerContents compoundTag = itemStack.get(DataComponents.CONTAINER);
		return compoundTag != null;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		Tooltips tooltips = Aoba.getInstance().moduleManager.tooltips;

		if (tooltips.state.getValue() && hoveredSlot != null && !hoveredSlot.getItem().isEmpty()
				&& minecraft.player.inventoryMenu.getCarried().isEmpty()) {
			if (hasItems(hoveredSlot.getItem()) && tooltips.getStorage()) {
				renderShulkerToolTip(context, mouseX, mouseY, 0, 0, hoveredSlot.getItem());
			} else if (hoveredSlot.getItem().getItem() == Items.FILLED_MAP && tooltips.getMap()) {
				drawMapPreview(context, hoveredSlot.getItem(), mouseX, mouseY);
			}
		}
	}

	public boolean renderShulkerToolTip(GuiGraphicsExtractor context, int offsetX, int offsetY, int mouseX, int mouseY,
			ItemStack stack) {
		try {
			ItemContainerContents compoundTag = stack.get(DataComponents.CONTAINER);
			if (compoundTag == null)
				return false;

			float[] colors = new float[] { 1F, 1F, 1F };
			Item focusedItem = stack.getItem();
			if (focusedItem instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
				try {
					ShulkerBoxBlock shulkerBoxBlock = (ShulkerBoxBlock) bi.getBlock();

					Color c = new Color(Objects.requireNonNull(shulkerBoxBlock.getColor()).getTextureDiffuseColor());
					colors = new float[] { c.getRed() / 255f, c.getGreen() / 255f, c.getRed() / 255f,
							c.getAlpha() / 255f };
				} catch (NullPointerException npe) {
					colors = new float[] { 1F, 1F, 1F };
				}
			}

			int tooltipWidth = 150;
			int nameHeight = 12;

			Font aobaFont = GuiManager.fontSetting.getValue().getRenderer();
			Aoba.getInstance().render2D.drawStringWithScale(stack.getHoverName().getString(), offsetX + 10, offsetY - 10,
					Shader.solid(Colors.White), 1.0f, aobaFont);

			extractRenderState(context, compoundTag.itemCopies().toList(), offsetX, offsetY + nameHeight, mouseX, mouseY, colors);

			context.fill(offsetX, offsetY - nameHeight, offsetX + tooltipWidth, offsetY,
					new Color(0, 0, 0, 128).getRGB());
			Aoba.getInstance().render2D.drawStringWithScale(stack.getHoverName().getString(), offsetX + 5, offsetY - 10,
					Shader.solid(Colors.White), 1.0f, aobaFont);

		} catch (Exception ignore) {
			return false;
		}
		return true;
	}

	@Unique
	private void extractRenderState(GuiGraphicsExtractor context, List<ItemStack> itemStacks, int offsetX, int offsetY, int mouseX, int mouseY,
			float[] colors) {
		try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "Aoba Depth Clear",
				MC.gameRenderer.mainRenderTarget().getColorTextureView(),
				Optional.empty(),
				MC.gameRenderer.mainRenderTarget().hasDepth() ? MC.gameRenderer.mainRenderTarget().getDepthTextureView() : null,
				OptionalDouble.of(1.0))) {
		}

		offsetX += 8;
		offsetY -= 82;

		drawBackground(context, offsetX, offsetY, colors);

		int row = 0;
		int i = 0;
		for (ItemStack itemStack : itemStacks) {
			context.item(itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);

			if (mouseX > offsetX + 8 + i * 18 && mouseX < offsetX + 28 + i * 18 && mouseY > offsetY + 7 + row * 18
					&& mouseY < offsetY + 27 + row * 18)
				postRender = () -> context.setTooltipForNextFrame(font, getTooltipFromItem(MC, itemStack),
						itemStack.getTooltipImage(), mouseX, mouseY);

			i++;
			if (i >= 9) {
				i = 0;
				row++;
			}
		}
	}

	private void drawBackground(GuiGraphicsExtractor context, int x, int y, float[] colors) {
	}

	private void drawMapPreview(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
		int y1 = y - 12;
		int x1 = x + 8;

		MapItemSavedData mapState = MapItem.getSavedData(stack, minecraft.level);

		if (mapState != null) {
			mapState.getHoldingPlayer(minecraft.player);

			x1 += 8;
			y1 += 8;
			double scale = (double) (100 - 16) / 128.0D;

			MapId mapId = stack.get(DataComponents.MAP_ID);
			if (mapId != null) {
				context.pose().pushMatrix();
				context.pose().translate(x1, y1);
				context.pose().scale((float) scale, (float) scale);

				MapRenderer mapRenderer = minecraft.getMapRenderer();
				mapRenderer.extractRenderState(mapId, mapState, mapRenderState);
				context.map(mapRenderState);
				context.pose().popMatrix();
			}
		}
	}

	@Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
	private void onDrawMouseoverTooltip(GuiGraphicsExtractor context, int x, int y, CallbackInfo ci) {
		Tooltips tooltips = Aoba.getInstance().moduleManager.tooltips;

		if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty()
				&& minecraft.player.inventoryMenu.getCarried().isEmpty()) {
			if (hoveredSlot.getItem().getItem() == Items.FILLED_MAP && tooltips.state.getValue() && tooltips.getMap())
				ci.cancel();
			else if (hoveredSlot.getItem().getItem() instanceof BlockItem
					&& ((BlockItem) hoveredSlot.getItem().getItem()).getBlock() instanceof ShulkerBoxBlock
					&& tooltips.state.getValue() && tooltips.getStorage())
				ci.cancel();
		}
	}
}