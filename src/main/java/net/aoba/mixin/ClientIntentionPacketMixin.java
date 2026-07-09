/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */
package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static String injectVanillaHostName(String hostName) {
        if (ProtocolManager.STRIP_KNOWN_PACKS) {
            if (hostName.contains("\0")) {
                return hostName.split("\0")[0];
            }
        }
        return hostName;
    }
}
