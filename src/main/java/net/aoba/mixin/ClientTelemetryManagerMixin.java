/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.telemetry.ClientTelemetryManager")
public class ClientTelemetryManagerMixin {
    @Redirect(method = "createEventSender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;allowsTelemetry()Z"))
    private boolean onDisableTelemetrySession(Minecraft minecraftClient) {
        if (ProtocolManager.BLOCK_TELEMETRY) {
            return false;
        }
        return minecraftClient.allowsTelemetry();
    }
}
