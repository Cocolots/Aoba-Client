/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

@Mixin(ClientboundResourcePackPushPacket.class)
public class ClientboundResourcePackPushPacketMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPacketInit(UUID id, String url, String hash, boolean required, Optional<Component> prompt, CallbackInfo ci) {
        if (ProtocolManager.BLOCK_LOCAL_SCAN && url != null) {
            String cleanUrl = url.toLowerCase(Locale.ROOT).trim();
            if (cleanUrl.contains("127.0.0.1") || cleanUrl.contains("localhost") || cleanUrl.contains("0.0.0.0") || cleanUrl.startsWith("file://")) {
                try {
                    java.lang.reflect.Field urlField = this.getClass().getDeclaredField("url");
                    urlField.setAccessible(true);
                    urlField.set(this, "http://localhost:9999/blocked_by_aoba");
                } catch (Exception e) {
                    try {
                        for (java.lang.reflect.Field field : this.getClass().getDeclaredFields()) {
                            if (field.getType() == String.class) {
                                field.setAccessible(true);
                                String val = (String) field.get(this);
                                if (val != null && (val.contains("127.0.0.1") || val.contains("localhost") || val.contains("0.0.0.0"))) {
                                    field.set(this, "http://127.0.0.1:9999/blocked_by_aoba");
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
