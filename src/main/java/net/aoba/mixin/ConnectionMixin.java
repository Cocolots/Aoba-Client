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

import java.net.InetSocketAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.aoba.Aoba;
import net.aoba.event.events.ReceivePacketEvent;
import net.aoba.event.events.SendPacketEvent;
import net.aoba.managers.proxymanager.Socks5Proxy;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

	private boolean hasTickSentPosition;
	
	@Shadow
	public abstract void send(final Packet<?> packet);
	
	@Inject(at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", ordinal = 0) }, method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", cancellable = true)
	protected void onChannelRead(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
		ReceivePacketEvent event = new ReceivePacketEvent(packet);
		Aoba.getInstance().eventManager.Fire(event);
	}

	@Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;)V", cancellable = true)
	private void onSend(Packet<?> packet, CallbackInfo ci) {
		SendPacketEvent event = new SendPacketEvent(packet);
		Aoba.getInstance().eventManager.Fire(event);

		// We need to send a matching TickEnd packet with a move packet
		// or else we will get kicked from the game.
		if(packet instanceof ServerboundClientTickEndPacket)
            hasTickSentPosition = false;
		else if(packet instanceof ServerboundMovePlayerPacket move){
			if(!move.hasPosition()) {
				return;
			}

			if(hasTickSentPosition) {
				send(ServerboundClientTickEndPacket.INSTANCE);
			}

			hasTickSentPosition = true;
		}
		
		if (event.isCancelled()) {
			ci.cancel();
		} else if (event.GetPacket() != packet) {
			ci.cancel();
			send(event.GetPacket());
		}
	}

	@Inject(method = "configureSerialization", at = @At("RETURN"))
	private static void addHandlersHook(ChannelPipeline pipeline, PacketFlow side, boolean local,
			BandwidthDebugMonitor packetSizeLogger, CallbackInfo ci) {
		Socks5Proxy proxy = Aoba.getInstance().proxyManager.getActiveProxy();

		if (proxy != null && side == PacketFlow.CLIENTBOUND && !local) {
			InetSocketAddress proxyAddress = new InetSocketAddress(proxy.getIp(), proxy.getPort());
			if (proxy.isAnonymous()) {
				pipeline.addFirst(new Socks5ProxyHandler(proxyAddress));
			} else {
				pipeline.addFirst(new Socks5ProxyHandler(proxyAddress, proxy.getUsername(), proxy.getPassword()));
			}
		}
	}
}
