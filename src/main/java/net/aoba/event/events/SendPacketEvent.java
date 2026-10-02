/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.event.events;

import java.util.ArrayList;

import net.aoba.event.listeners.AbstractListener;
import net.aoba.event.listeners.SendPacketListener;
import net.minecraft.network.protocol.Packet;

public class SendPacketEvent extends AbstractEvent {

	private Packet<?> packet;

	public SendPacketEvent(Packet<?> packet) {
		this.packet = packet;
	}

	public Packet<?> GetPacket() {
		return packet;
	}

	public void SetPacket(Packet<?> packet) {
		this.packet = packet;
	}

	@Override
	public void Fire(ArrayList<? extends AbstractListener> listeners) {
		for (AbstractListener listener : listeners) {
			SendPacketListener sendPacketListener = (SendPacketListener) listener;
			sendPacketListener.onSendPacket(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<SendPacketListener> GetListenerClassType() {
		return SendPacketListener.class;
	}
}
