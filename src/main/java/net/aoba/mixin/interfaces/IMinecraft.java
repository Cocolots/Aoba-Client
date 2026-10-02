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

package net.aoba.mixin.interfaces;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;

@Mixin(Minecraft.class)
public interface IMinecraft {
    @Accessor("rightClickDelay")
    int getItemUseCooldown();

    @Accessor("rightClickDelay")
    void setItemUseCooldown(int value);

    @Invoker("startUseItem")
    void useItem();

    @Mutable
    @Accessor("user")
    void setSession(User session);

    @Mutable
    @Accessor("playerSocialManager")
    void setSocialInteractionsManager(PlayerSocialManager socialInteractionsManager);
    
    @Mutable
    @Accessor("reportingContext")
    void setAbuseReportContext(ReportingContext abuseReportContext);
    
    @Mutable
    @Accessor("profileFuture")
    void setGameProfileFuture(CompletableFuture<ProfileResult> future);
    
    @Mutable
    @Accessor("profileKeyPairManager")
    void setProfileKeys(ProfileKeyPairManager keys);
    
    @Mutable
    @Accessor
    void setUserApiService(UserApiService apiService);
}
