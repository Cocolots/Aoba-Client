/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import net.aoba.managers.ProtocolManager;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.concurrent.Executor;

@Mixin(YggdrasilUserApiService.class)
public class YggdrasilTelemetryMixin {
    @Overwrite(remap = false)
    public TelemetrySession newTelemetrySession(final Executor executor) {
        if (ProtocolManager.BLOCK_TELEMETRY) {
            return TelemetrySession.DISABLED;
        }
        return TelemetrySession.DISABLED;
    }
}
