/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.movement;

import net.aoba.Aoba;
import net.aoba.event.events.ReceivePacketEvent;
import net.aoba.event.events.TickEvent;
import net.aoba.event.listeners.ReceivePacketListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.CommandManager;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.aoba.settings.types.FloatSetting;
import net.aoba.settings.types.IntegerSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Elytra fly
 *
 * Bounce  - Highway travel. Sprint-jumps and re-opens the elytra every time you
 *           touch the ground with a locked pitch. Needs a highway or flat floor.
 * Control - Direct velocity control while gliding. Fastest and least legit;
 *           keep Speed low on servers with movement checks.
 *
 * Both modes back off for a few ticks whenever the server rubberbands you.
 */
public class ElytraFly extends Module implements TickListener, ReceivePacketListener {

	public enum ElytraFlyMode {
		Bounce, Control
	}

	private final EnumSetting<ElytraFlyMode> mode = EnumSetting.<ElytraFlyMode>builder().id("elytrafly_mode")
			.displayName("Mode").description("Bounce for highways, Control for direct flight.")
			.defaultValue(ElytraFlyMode.Bounce).build();

	// Bounce
	private final FloatSetting bouncePitch = FloatSetting.builder().id("elytrafly_bounce_pitch").displayName("Bounce Pitch")
			.description("Pitch locked while bouncing. Higher keeps you lower to the ground.").defaultValue(85f)
			.minValue(0f).maxValue(90f).step(0.5f).build();

	private final BooleanSetting autoJump = BooleanSetting.builder().id("elytrafly_auto_jump").displayName("Auto Jump")
			.description("Bounce: jump automatically when touching the ground.").defaultValue(true).build();

	// Control
	private final FloatSetting speed = FloatSetting.builder().id("elytrafly_speed").displayName("Speed")
			.description("Control: horizontal speed in blocks per tick. Lower is safer against anticheat.")
			.defaultValue(1.0f).minValue(0.1f).maxValue(5f).step(0.05f).build();

	private final FloatSetting verticalSpeed = FloatSetting.builder().id("elytrafly_vertical_speed")
			.displayName("Vertical Speed").description("Control: up/down speed with jump/sneak.").defaultValue(0.5f)
			.minValue(0.05f).maxValue(3f).step(0.05f).build();

	private final FloatSetting fallSpeed = FloatSetting.builder().id("elytrafly_fall_speed").displayName("Fall Speed")
			.description("Control: constant sink when idle. A small sink looks less like hovering.").defaultValue(0.01f)
			.minValue(0f).maxValue(0.5f).step(0.005f).build();

	// Shared
	private final BooleanSetting autoOpen = BooleanSetting.builder().id("elytrafly_auto_open").displayName("Auto Open")
			.description("Control: open the elytra automatically while falling.").defaultValue(true).build();

	private final IntegerSetting rubberbandPause = IntegerSetting.builder().id("elytrafly_rubberband_pause")
			.displayName("Rubberband Pause").description("Ticks to stop controlling after the server sets you back.")
			.defaultValue(10).minValue(0).maxValue(60).step(1).build();

	private final BooleanSetting rubberbandNotify = BooleanSetting.builder().id("elytrafly_rubberband_notify")
			.displayName("Notify Setbacks").description("Print a chat message when you get rubberbanded.")
			.defaultValue(false).build();

	private int pauseTicks = 0;
	private int openCooldown = 0;

	public ElytraFly() {
		super("ElytraFly");
		setCategory(Category.of("Movement"));
		setDescription("Elytra flight for anarchy servers. Bounce and Control modes.");

		addSetting(mode);
		addSetting(bouncePitch);
		addSetting(autoJump);
		addSetting(speed);
		addSetting(verticalSpeed);
		addSetting(fallSpeed);
		addSetting(autoOpen);
		addSetting(rubberbandPause);
		addSetting(rubberbandNotify);
	}

	@Override
	public void onEnable() {
		resetState();
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(ReceivePacketListener.class, this);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(ReceivePacketListener.class, this);
		resetState();
	}

	@Override
	public void onToggle() {
	}

	private void resetState() {
		pauseTicks = 0;
		openCooldown = 0;
	}

	private boolean hasElytra(LocalPlayer player) {
		return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
	}

	/** Asks the server to open the elytra, the same way vanilla does when you press jump mid-air. */
	private void openElytra(LocalPlayer player) {
		if (openCooldown > 0)
			return;
		player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
		openCooldown = 5;
	}

	@Override
	public void onTick(TickEvent.Pre event) {
		LocalPlayer player = MC.player;
		if (player == null || player.connection == null)
			return;

		if (openCooldown > 0)
			openCooldown--;

		if (!hasElytra(player))
			return;

		if (pauseTicks > 0) {
			pauseTicks--;
			return;
		}

		switch (mode.getValue()) {
		case Bounce -> tickBounce(player);
		case Control -> {
			if (autoOpen.getValue() && !player.isFallFlying() && !player.onGround() && player.getDeltaMovement().y < 0)
				openElytra(player);
		}
		}
	}

	@Override
	public void onTick(TickEvent.Post event) {
		LocalPlayer player = MC.player;
		if (player == null || pauseTicks > 0 || !hasElytra(player))
			return;

		if (mode.getValue() == ElytraFlyMode.Control && player.isFallFlying())
			tickControl(player);
	}

	private void tickBounce(LocalPlayer player) {
		player.setSprinting(true);
		player.setXRot(bouncePitch.getValue());

		if (player.onGround()) {
			if (autoJump.getValue())
				player.jumpFromGround();
		} else if (!player.isFallFlying()) {
			// Re-open every time we leave the ground; this is what keeps the bounce going.
			openElytra(player);
		}
	}

	private void tickControl(LocalPlayer player) {
		double yawRad = Math.toRadians(player.getYRot());
		Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
		Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));

		Vec3 dir = Vec3.ZERO;
		if (MC.options.keyUp.isDown())
			dir = dir.add(forward);
		if (MC.options.keyDown.isDown())
			dir = dir.subtract(forward);
		if (MC.options.keyRight.isDown())
			dir = dir.add(right);
		if (MC.options.keyLeft.isDown())
			dir = dir.subtract(right);

		Vec3 horizontal = dir.lengthSqr() > 0 ? dir.normalize().scale(speed.getValue()) : Vec3.ZERO;

		double y = -fallSpeed.getValue();
		if (MC.options.keyJump.isDown())
			y = verticalSpeed.getValue();
		else if (MC.options.keyShift.isDown())
			y = -verticalSpeed.getValue();

		player.setDeltaMovement(horizontal.x, y, horizontal.z);
	}

	@Override
	public void onReceivePacket(ReceivePacketEvent event) {
		if (!(event.GetPacket() instanceof ClientboundPlayerPositionPacket))
			return;
		LocalPlayer player = MC.player;
		if (player == null || !player.isFallFlying())
			return;

		// Server set us back: stop pushing so we don't chain setbacks into a kick.
		pauseTicks = rubberbandPause.getValue();
		if (rubberbandNotify.getValue())
			CommandManager.sendChatMessage("ElytraFly: rubberbanded, pausing " + pauseTicks + " ticks.");
	}
}