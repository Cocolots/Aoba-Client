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

import net.aoba.gui.screens.ProtocolScreen;
import net.aoba.gui.screens.alts.AltScreen;
import net.aoba.gui.screens.proxy.ProxyScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {

    @Unique
    private Button altManagerButton;
    @Unique
    private Button proxyManagerButton;
    @Unique
    private Button protocolButton; //new protocol button

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = {@At("TAIL")}, method = {"init()V"})
    private void onInit(CallbackInfo ci) {
        altManagerButton = Button.builder(Component.nullToEmpty("Alt Manager"), b -> minecraft.gui.setScreen(new AltScreen((JoinMultiplayerScreen) (Object) this)))
                .bounds(width / 2 + 69, 5, 85, 20).build();
        addRenderableWidget(altManagerButton);

        proxyManagerButton = Button.builder(Component.nullToEmpty("Proxy Manager"), b -> minecraft.gui.setScreen(new ProxyScreen((JoinMultiplayerScreen) (Object) this)))
                .bounds(width / 2 - 154, 5, 85, 20).build();
        addRenderableWidget(proxyManagerButton);
        
        protocolButton = Button.builder(Component.nullToEmpty("Protocol"), b -> minecraft.setScreen(new ProtocolScreen(this)))
                .bounds(width / 2 + 154, height - 28, 74, 20).build();
        addRenderableWidget(protocolButton);
    }

    @Inject(at = {@At("TAIL")}, method = {"repositionElements()V"})
    private void onRepositionElements(CallbackInfo ci) {
        if (altManagerButton != null) {
            altManagerButton.setX(width / 2 + 69);
        }
        if (proxyManagerButton != null) {
            proxyManagerButton.setX(width / 2 - 154);
        }
        if (protocolButton != null) {
            protocolButton.setX(width / 2 + 154);
            protocolButton.setY(height - 28);
        }
    }
}
