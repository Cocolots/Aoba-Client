/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.movement;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.AntiCheat;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.FloatSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Noclip extends Module implements TickListener {
	private final FloatSetting flySpeed = FloatSetting.builder().id("noclip_fly_speed").displayName("Speed")
			.description("Fly speed.").defaultValue(2f).minValue(0.1f).maxValue(15f).step(0.5f).build();

	private final FloatSetting speedMultiplier = FloatSetting.builder().id("noclip_speedmultiplier")
			.displayName("Speed Multiplier").description("Noclip speed multiplier.").defaultValue(1.5f).minValue(0.1f)
			.maxValue(15f).step(0.1f).build();

	private final BooleanSetting onGround = BooleanSetting.builder().id("noclip_onground")
			.displayName("On Ground Packet").description("Whether to send the onground packet while moving.")
			.defaultValue(true).build();

	private final FloatSetting packetDistanceThreshold = FloatSetting.builder().id("noclip_packet_distance_threshold")
			.displayName("Packet Distance Threshold").description("Distance threshold for sending packets.")
			.defaultValue(10f).minValue(1f).maxValue(100f).step(1f).build();

	private final FloatSetting packetCountOffset = FloatSetting.builder().id("noclip_packet_count_offset")
			.displayName("Packet Count Offset").description("Offset for the number of packets required.")
			.defaultValue(1f).minValue(1f).maxValue(10f).step(1f).build();

	private final FloatSetting yawOffset = FloatSetting.builder().id("noclip_yaw_offset").displayName("Yaw Offset")
			.description("Angle offset for right direction.").defaultValue(90f).minValue(0f).maxValue(360f).step(1f)
			.build();

	private final FloatSetting maxPackets = FloatSetting.builder().id("noclip_max_packets")
			.displayName("Max Packets Per Update").description("The maximum amount of packets allowed every update.")
			.defaultValue(5f).minValue(1f).maxValue(40f).step(1f).build();

	public Noclip() {
		super("Noclip");
		setCategory(Category.of("Movement"));
		setDescription("Allows the player to clip through blocks (Only work clientside).");

		addSetting(flySpeed);
		addSetting(speedMultiplier);
		addSetting(onGround);
		addSetting(packetDistanceThreshold);
		addSetting(packetCountOffset);
		addSetting(yawOffset);
		addSetting(maxPackets);

		setDetectable(
		    AntiCheat.NoCheatPlus,
		    AntiCheat.Vulcan,
		    AntiCheat.AdvancedAntiCheat,
		    AntiCheat.Verus,
		    AntiCheat.Grim,
		    AntiCheat.Matrix,
		    AntiCheat.Negativity,
		    AntiCheat.Karhu
		);
	}

	public void setSpeed(float speed) {
		flySpeed.setValue(speed);
	}

	public float getSpeed() {
		return flySpeed.getValue();
	}

	@Override
	public void onDisable() {
		if (MC.player != null) {
			MC.player.noPhysics = false;
		}
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onTick(Pre event) {
		LocalPlayer player = MC.player;
		if (player == null) {
			return;
		}

		float speed = flySpeed.getValue();

		if (MC.options.keySprint.isDown()) {
			speed *= speedMultiplier.getValue();
		}

		player.setDeltaMovement(new Vec3(0, 0, 0));

		Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
		Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + yawOffset.getValue());

		Vec3 vec = new Vec3(0, 0, 0);

		if (MC.options.keyUp.isDown()) {
			vec = vec.add(forward.scale(speed));
		} else if (MC.options.keyDown.isDown()) {
			vec = vec.subtract(forward.scale(speed));
		}

		if (MC.options.keyRight.isDown()) {
			vec = vec.add(right.scale(speed));
		} else if (MC.options.keyLeft.isDown()) {
			vec = vec.subtract(right.scale(speed));
		}

		Vec3 newPos = player.position().add(vec);
		int packetsRequired = (int) ((int) Math
				.ceil(MC.player.position().distanceTo(newPos) / packetDistanceThreshold.getValue())
				- packetCountOffset.getValue());
		packetsRequired = Math.min(packetsRequired, maxPackets.getValue().intValue());

		for (int i = 0; i < packetsRequired; i++) {
			MC.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(onGround.getValue(), false));
		}

		MC.player.connection.send(
				new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, onGround.getValue(), false));
	}

	@Override
	public void onTick(Post event) {

	}
}
