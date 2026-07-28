package net.aoba.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.aoba.Aoba;
import net.aoba.event.events.BreedEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

@Mixin(Animal.class)
public abstract class AnimalMixin {
	@Inject(at = { @At("HEAD") }, method = "handleEntityEvent(B)V")
	public void onEntityEvent(final byte id, CallbackInfo ci) {
		if (id == 18) {
			Aoba.getInstance().eventManager.Fire(new BreedEvent((LivingEntity) (Object) this, id));
		}
	}
}
