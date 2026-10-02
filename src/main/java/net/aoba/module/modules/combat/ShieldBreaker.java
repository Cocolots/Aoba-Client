package net.aoba.module.modules.combat;

import net.aoba.Aoba;
import net.aoba.event.events.StartAttackEvent;
import net.aoba.event.listeners.StartAttackListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.utils.FindItemResult;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ShieldBreaker extends Module implements StartAttackListener {
	public ShieldBreaker() {
		super("ShieldBreaker");
		
		setCategory(Category.of("Combat"));
		setDescription("Renders an opponents shield useless. Requires an axe to be in your hotbar.");
	}

	@Override
	public void onDisable() {
		Aoba.getInstance().eventManager.RemoveListener(StartAttackListener.class, this);
	}

	@Override
	public void onEnable() {
		Aoba.getInstance().eventManager.AddListener(StartAttackListener.class, this);
	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onStartAttack(StartAttackEvent event) {

		Entity target = event.getTarget();
		if (target == null)
			return;
		
		// Ensure that target HAS a shield and is currently blocking.
		if(target instanceof LivingEntity livingTarget) {
			if(livingTarget.isBlocking()) {
				FindItemResult findItemResult = findInHotbar(s -> s.is(ItemTags.AXES));
				if (findItemResult.found() && swap(findItemResult.slot(), true)) {
					event.cancel();

					// Note: we do not want to use InteractionUtils.attack() here
					// Doing so will infinitely bubble the onStartAttack event.
					MC.gameMode.attack(MC.player, target);
					MC.player.swing(InteractionHand.MAIN_HAND, MC.player.getMainHandItem().getAttackAnimation(), false);
					MC.player.connection.send(ServerboundPunchPacket.INSTANCE);

					swapBack();
				}
			}
		}
	}
}