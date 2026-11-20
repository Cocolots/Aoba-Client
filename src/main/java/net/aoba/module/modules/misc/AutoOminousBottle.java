/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2025 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */
package net.aoba.module.modules.misc;

import net.aoba.Aoba;
import net.aoba.event.events.ItemUsedEvent;
import net.aoba.event.events.ReceivePacketEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.ItemUsedListener;
import net.aoba.event.listeners.ReceivePacketListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.mixin.interfaces.IClientboundBossEventPacket;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.utils.FindItemResult;
import net.aoba.utils.player.ConsumableSession;
import net.aoba.utils.player.InteractionUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoOminousBottle extends Module implements ReceivePacketListener, ItemUsedListener, TickListener {
	private static final long USE_TIMEOUT_MS = 10_000;

	private final ConsumableSession session = new ConsumableSession();
	private ItemStack lastUsedItemStack;

	private final BooleanSetting swapBack = BooleanSetting.builder().id("auto_ominous_bottle_swap_back")
			.displayName("Swap Back").description("Restores the selected slot after drinking the bottle")
			.defaultValue(true).build();

	public AutoOminousBottle() {
		super("AutoOminous");
		setCategory(Category.of("Misc"));
		setDescription("Automatically drinks an ominous bottle when a boss bar is removed.");
		addSetting(swapBack);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(ReceivePacketListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(ItemUsedListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		stopSession(false);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(ReceivePacketListener.class, this);
		Aoba.getInstance().eventManager.AddListener(ItemUsedListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onReceivePacket(ReceivePacketEvent event) {
		Packet<?> packet = event.GetPacket();
		if (!(packet instanceof ClientboundBossEventPacket bossPacket) || MC.player == null || session.isActive()) {
			return;
		}

		IClientboundBossEventPacket accessor = (IClientboundBossEventPacket) bossPacket;
		if (accessor.getAction().getType() != ClientboundBossEventPacket.OperationType.REMOVE) {
			return;
		}

		FindItemResult result = findInHotbar(Items.OMINOUS_BOTTLE);
		if (!result.found()) {
			return;
		}

		int selectedSlot = MC.player.getInventory().getSelectedSlot();
		if (!session.start(selectedSlot, result.slot(), System.currentTimeMillis())) {
			return;
		}
		if (!result.isOffhand() && !result.isMainHand() && !swap(result.slot(), false)) {
			session.clear();
			return;
		}

		lastUsedItemStack = (result.isOffhand() ? MC.player.getOffhandItem()
				: MC.player.getInventory().getItem(result.slot())).copy();
		MC.options.keyUse.setDown(true);
	}

	@Override
	public void onItemUsed(ItemUsedEvent.Pre event) {
	}

	@Override
	public void onItemUsed(ItemUsedEvent.Post event) {
		if (lastUsedItemStack != null && ItemStack.isSameItem(event.getItemStack(), lastUsedItemStack)) {
			stopSession(true);
		}
	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null || session.isExpired(System.currentTimeMillis(), USE_TIMEOUT_MS)) {
			stopSession(MC.player != null);
		}
	}

	@Override
	public void onTick(Post event) {
	}

	private void stopSession(boolean restoreSlot) {
		MC.options.keyUse.setDown(false);
		lastUsedItemStack = null;
		if (!session.isActive()) {
			return;
		}

		int itemSlot = session.getItemSlot();
		int previousSlot = session.getPreviousSlot();
		session.stop();
		if (restoreSlot && swapBack.getValue() && previousSlot >= 0 && itemSlot != InteractionUtils.OFFHAND
				&& previousSlot != itemSlot && MC.player != null) {
			swap(previousSlot, false);
		}
	}
}
