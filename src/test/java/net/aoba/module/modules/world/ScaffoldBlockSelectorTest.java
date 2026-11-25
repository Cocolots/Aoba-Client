package net.aoba.module.modules.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.aoba.module.modules.world.ScaffoldBlockSelector.Selection;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

class ScaffoldBlockSelectorTest {
	private Minecraft minecraft;
	private Inventory inventory;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void setUp() {
		minecraft = mock(Minecraft.class);
		LocalPlayer player = mock(LocalPlayer.class);
		ClientLevel level = mock(ClientLevel.class);
		inventory = mock(Inventory.class);

		minecraft.player = player;
		minecraft.level = level;
		when(player.getInventory()).thenReturn(inventory);
		when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);
		when(inventory.getSelectedSlot()).thenReturn(0);
		when(inventory.getItem(anyInt())).thenReturn(ItemStack.EMPTY);
	}

	@Test
	void avoidsEntityBlocksOutsideBaseEntityBlockHierarchy() {
		ItemStack stack = directEntityBlockStack();
		when(inventory.getItem(0)).thenReturn(stack);

		Selection selection = ScaffoldBlockSelector.select(minecraft, Set.of(), true, false, false, true);

		assertThat(selection).isNull();
	}

	@Test
	void allowsDirectEntityBlocksWhenEntityBlocksAreNotAvoided() {
		ItemStack stack = directEntityBlockStack();
		when(inventory.getItem(0)).thenReturn(stack);

		Selection selection = ScaffoldBlockSelector.select(minecraft, Set.of(), true, false, false, false);

		assertThat(selection).isNotNull();
		assertThat(selection.slot()).isZero();
	}

	private static ItemStack directEntityBlockStack() {
		ItemStack stack = mock(ItemStack.class);
		when(stack.isEmpty()).thenReturn(false);
		when(stack.getItem()).thenReturn(Blocks.JIGSAW.asItem());
		when(stack.getCount()).thenReturn(1);
		return stack;
	}
}
