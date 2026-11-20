/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.misc;

import net.aoba.Aoba;
import net.aoba.event.events.ReceivePacketEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.ReceivePacketListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.aoba.utils.FindItemResult;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AutoFish extends Module implements ReceivePacketListener, TickListener {
	private final AutoFishPlanner planner = new AutoFishPlanner();

	private final BooleanSetting autoSwitch = BooleanSetting.builder().id("autofish_autoswitch").displayName("Auto Switch")
			.description("Automatically switch to fishing rod before casting.").defaultValue(true).build();

	private final BooleanSetting autoToggle = BooleanSetting.builder().id("autofish_autotoggle").displayName("Auto Toggle")
			.description("Automatically toggles off if no fishing rod is available.").defaultValue(false).build();

	private final BooleanSetting autoCast = BooleanSetting.builder().id("autofish_autocast").displayName("Auto Cast")
			.description("Casts once when the module is enabled.").defaultValue(true).build();

	private final BooleanSetting ownSplashOnly = BooleanSetting.builder().id("autofish_own_splash")
			.displayName("Own Splash Only").description("Reels only for splashes near your active fishing hook")
			.defaultValue(true).build();

	private final FloatSetting splashRadius = FloatSetting.builder().id("autofish_splash_radius")
			.displayName("Splash Radius").description("Maximum distance between your hook and a matching splash")
			.defaultValue(2f).minValue(0.5f).maxValue(8f).step(0.5f).build();

	private final IntegerSetting recastDelay = IntegerSetting.builder().id("autofish_recast_delay")
			.displayName("Recast Delay").description("Delay between reeling and casting again in milliseconds")
			.defaultValue(300).minValue(100).maxValue(2000).step(50).build();

	public AutoFish() {
		super("AutoFish");
		setCategory(Category.of("Misc"));
		setDescription("Automatically fishes using a delayed reel and recast state machine.");

		addSetting(autoSwitch);
		addSetting(autoToggle);
		addSetting(autoCast);
		addSetting(ownSplashOnly);
		addSetting(splashRadius);
		addSetting(recastDelay);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(ReceivePacketListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		planner.reset();
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(ReceivePacketListener.class, this);
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		planner.reset();
		if (autoCast.getValue()) {
			planner.scheduleRecast(System.currentTimeMillis(), recastDelay.getValue());
		}
	}

	@Override
	public void onToggle() {
	}

	@Override
	public void onTick(Pre event) {
		if (MC.player == null || MC.gameMode == null) {
			planner.reset();
			return;
		}
		if (planner.onTick(System.currentTimeMillis()) == AutoFishPlanner.Action.RECAST) {
			useRod();
		}
	}

	@Override
	public void onTick(Post event) {
	}

	@Override
	public void onReceivePacket(ReceivePacketEvent event) {
		Packet<?> packet = event.GetPacket();
		if (!(packet instanceof ClientboundSoundPacket soundPacket)
				|| !soundPacket.getSound().value().equals(SoundEvents.FISHING_BOBBER_SPLASH)) {
			return;
		}
		if (ownSplashOnly.getValue() && !isOwnSplash(soundPacket)) {
			return;
		}
		if (planner.onSplash(System.currentTimeMillis(), recastDelay.getValue()) == AutoFishPlanner.Action.REEL) {
			useRod();
		}
	}

	private boolean isOwnSplash(ClientboundSoundPacket packet) {
		if (MC.player == null || MC.player.fishing == null || MC.player.fishing.isRemoved()) {
			return false;
		}
		Vec3 hookPosition = MC.player.fishing.position();
		double dx = hookPosition.x - packet.getX();
		double dy = hookPosition.y - packet.getY();
		double dz = hookPosition.z - packet.getZ();
		return dx * dx + dy * dy + dz * dz <= splashRadius.getValueSqr();
	}

	private boolean useRod() {
		if (MC.player == null || MC.gameMode == null) {
			return false;
		}

		FindItemResult rod = findInHotbar(Items.FISHING_ROD);
		if (!rod.found()) {
			if (autoToggle.getValue()) {
				state.setValue(false);
			}
			return false;
		}

		InteractionHand hand;
		if (rod.isOffhand()) {
			hand = InteractionHand.OFF_HAND;
		} else if (rod.isMainHand()) {
			hand = InteractionHand.MAIN_HAND;
		} else if (autoSwitch.getValue() && swap(rod.slot(), false)) {
			hand = InteractionHand.MAIN_HAND;
		} else {
			if (autoToggle.getValue()) {
				state.setValue(false);
			}
			return false;
		}

		MC.gameMode.useItem(MC.player, hand);
		return true;
	}
}
