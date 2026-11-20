/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.aoba.Aoba;
import net.aoba.AobaClient;
import net.aoba.event.events.StartAttackEvent;
import net.aoba.event.events.SubtickEvent;
import net.aoba.event.events.TickEvent;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	private boolean aobaLoaded = false;
	
	@Shadow
	private int rightClickDelay;
	@Shadow
	@Final
	private User user;

	@Shadow
	@Final
	private MouseHandler mouseHandler;

	@Shadow
	public ClientLevel level;

	@Shadow
	public LocalPlayer player;

	private User aobaSession;

	private long aobaLastSubtickNanos;

	@Shadow
	public abstract void delayCrash(CrashReport crashReport);

	@Shadow
	@Nullable
	public HitResult hitResult;
	
	@Shadow
	@Final
	public Options options;

	@Inject(at = @At("HEAD"), method = "onResourceLoadFinished(Lnet/minecraft/client/GameLoadCookie;)V")
	private void onfinishedloading(CallbackInfo info) {
		if(aobaLoaded)
			return;

		aobaLoaded = true;
		try {
			Aoba.getInstance().loadAssets();
		} catch (Throwable t) {
			delayCrash(CrashReport.forThrowable(t, "Initializing Aoba Client"));
		}
	}

	@Inject(at = @At("HEAD"), method = "tick()V")
	public void onPreTick(CallbackInfo info) {
		if (level != null && player != null) {
			TickEvent.Pre updateEvent = new TickEvent.Pre();
			Aoba.getInstance().eventManager.Fire(updateEvent);
		}
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void onPostTick(CallbackInfo info) {
		if (level != null && player != null) {
			TickEvent.Post updateEvent = new TickEvent.Post();
			Aoba.getInstance().eventManager.Fire(updateEvent);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;handleAccumulatedMovement()V", shift = At.Shift.AFTER), method = "runTick(Z)V")
	public void onSubtick(boolean advanceGameTime, CallbackInfo info) {
		long now = System.nanoTime();
		if (aobaLastSubtickNanos == 0L)
			aobaLastSubtickNanos = now;
		float delta = (now - aobaLastSubtickNanos) / 1_000_000.0f;
		aobaLastSubtickNanos = now;

		if (level != null && player != null) {
			SubtickEvent updateEvent = new SubtickEvent(delta);
			Aoba.getInstance().eventManager.Fire(updateEvent);
		}
	}

	@Inject(at = { @At("HEAD") }, method = { "getUser()Lnet/minecraft/client/User;" }, cancellable = true)
	private void onGetSession(CallbackInfoReturnable<User> cir) {
		if (aobaSession == null)
			return;
		cir.setReturnValue(aobaSession);
	}

	@Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;user:Lnet/minecraft/client/User;", opcode = Opcodes.GETFIELD, ordinal = 0), method = {
			"getUser()Lnet/minecraft/client/User;" })
	private User getSessionForSessionProperties(Minecraft mc) {
		if (aobaSession != null)
			return aobaSession;
		return user;
	}

	@Inject(at = { @At(value = "HEAD") }, method = { "close()V" })
	private void onClose(CallbackInfo ci) {
		try {
			Aoba.getInstance().endClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = { @At(value = "HEAD") }, method = { "pauseGame(Z)V" })
	private void onOpenPauseMenu(boolean pause, CallbackInfo ci) {
		AobaClient aoba = Aoba.getInstance();

		if (aoba.guiManager != null) {
			aoba.guiManager.setClickGuiOpen(false);
		}
	}

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> cir) {
    	Entity target = null;
    	if(hitResult.getType() == HitResult.Type.ENTITY) {
    		target = ((EntityHitResult)hitResult).getEntity();
    	}
    	
    	StartAttackEvent event = new StartAttackEvent(target);
    	Aoba.getInstance().eventManager.Fire(event);
    	
    	if(event.isCancelled()) {
            cir.setReturnValue(false);
    	}
    }

}
