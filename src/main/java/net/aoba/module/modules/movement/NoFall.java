/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.movement;

import net.aoba.Aoba;
import net.aoba.event.events.SendPacketEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.SendPacketListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.managers.rotation.Rotation;
import net.aoba.mixin.interfaces.IServerboundMovePlayerPacket;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.aoba.settings.types.EnumSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class NoFall extends Module implements TickListener, SendPacketListener {
	public enum Mode {
		Packet, Bucket
	}

	private final EnumSetting<Mode> mode = EnumSetting.<Mode>builder().id("nofall_mode").displayName("Mode")
			.description("Controls how NoFall will react to fall damage.").defaultValue(Mode.Packet).build();

	private final BooleanSetting ignoreElytra = BooleanSetting.builder().id("nofall_ignore_elytra").displayName("Ignore Elytra")
			.description("Does not trigger NoFall when the player is flying with an elytra.").defaultValue(false)
			.build();

	private final BooleanSetting ignoreMace = BooleanSetting.builder().id("nofall_ignore_mace")
			.displayName("Ignore Mace").description("Does not trigger NoFall when a mace is in the players hand.")
			.defaultValue(true).build();

	public NoFall() {
		super("No-Fall");
		setCategory(Category.of("Movement"));
		setDescription("Prevents fall damage.");

		addSetting(mode);
		addSetting(ignoreElytra);
		addSetting(ignoreMace);
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
		Aoba.getInstance().eventManager.RemoveListener(SendPacketListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
		Aoba.getInstance().eventManager.AddListener(SendPacketListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onSendPacket(SendPacketEvent event) {
		// Only trigger if the current mode is packet mode.
		if (mode.getValue() != Mode.Packet)
			return;

		Packet<?> packet = event.GetPacket();
		if (packet instanceof ServerboundMovePlayerPacket) {
			// Ignore creative mode
			if (MC.player.getAbilities().instabuild)
				return;

			// Ignore Mace
			if (ignoreMace.getValue() && MC.player.getMainHandItem().is(Items.MACE))
				return;

			// Ignore Elytras
			if (ignoreElytra.getValue() && MC.player.isFallFlying())
				return;

			// Set packet to onGround
			IServerboundMovePlayerPacket iPacket = (IServerboundMovePlayerPacket) packet;
			iPacket.setOnGround(true);
		}
	}

	@Override
	public void onTick(Pre event) {
		if (mode.getValue() != Mode.Bucket)
			return;

		if (MC.player.onGround())
			return;

		if (!willPlayerLandInWater()) {

			ClipContext context = new ClipContext(MC.player.position(), MC.player.position().subtract(0, 5, 0),
					ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, MC.player);
			BlockHitResult result = MC.level.clip(context);

			if (result != null && result.getType() == HitResult.Type.BLOCK) {
				BlockPos targetPos = result.getBlockPos().above();

				float rotationDegreesPerTick = 30f;
				Rotation rotation = Rotation.getPlayerRotationDeltaFromPosition(Vec3.atCenterOf(targetPos));

				float maxYawRotationDelta = Math.clamp((float) -rotation.yaw(), -rotationDegreesPerTick,
						rotationDegreesPerTick);
				float maxPitchRotation = Math.clamp((float) -rotation.pitch(), -rotationDegreesPerTick,
						rotationDegreesPerTick);

				Rotation newRotation = new Rotation(MC.player.getYRot() + maxYawRotationDelta,
						MC.player.getXRot() + maxPitchRotation);
				MC.player.setYRot((float) newRotation.yaw());
				MC.player.setXRot((float) newRotation.pitch());

				SwingAnimation swingAnimation = MC.player.getMainHandItem().getInteractAnimation();
				InteractionResult actionResult = MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, result);

				if (actionResult.consumesAction()) {
					if (actionResult instanceof InteractionResult.Success success
							&& success.swingSource() == InteractionResult.SwingSource.PREDICTED)
						MC.player.swing(InteractionHand.MAIN_HAND, swingAnimation, false);
					MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
				}
			}
		}
	}

	@Override
	public void onTick(Post event) {

	}

	private boolean willPlayerLandInWater() {
		for (int i = 0; i < 64; i++) {
			BlockPos blockPos = MC.player.blockPosition().offset(0, -i, 0);
			BlockState state = MC.level.getBlockState(blockPos);

			if (state.isSolid())
				break;

			Fluid fluid = state.getFluidState().getType();
			if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
				return true;
			}
		}

		return false;
	}
}
