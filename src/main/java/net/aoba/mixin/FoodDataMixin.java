package net.aoba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.aoba.Aoba;
import net.aoba.event.events.FoodLevelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;

@Mixin(FoodData.class)
public class FoodDataMixin {
	@Shadow
	private int foodLevel;

	@Shadow
	private float saturationLevel;

	@Shadow
	private float exhaustionLevel;

	@Inject(at = { @At("HEAD") }, method = {
			"tick(Lnet/minecraft/server/level/ServerPlayer;)V" }, cancellable = true)
	private void onHungerUpdate(ServerPlayer player, CallbackInfo ci) {
		// TODO: Right now we're only handling client player. Does any other player ever
		// get used???
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;

		if (player.getUUID().equals(mc.player.getUUID())) {
			Difficulty difficulty = player.level().getDifficulty();
			if (exhaustionLevel > 4.0f) {
				if (saturationLevel <= 0.0f && difficulty != Difficulty.PEACEFUL) {
					// Fire Event
					int newFoodLevel = Math.max(foodLevel - 1, 0);
					FoodLevelEvent event = new FoodLevelEvent(newFoodLevel);
					Aoba.getInstance().eventManager.Fire(event);
					if (event.isCancelled()) {
						ci.cancel();
					}
				}
			}
		}
	}

	@Inject(at = { @At("HEAD") }, method = { "setFoodLevel(I)V" }, cancellable = true)
	private void onSetHunger(int foodLevel, CallbackInfo ci) {
		FoodLevelEvent event = new FoodLevelEvent(foodLevel);
		Aoba.getInstance().eventManager.Fire(event);
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
