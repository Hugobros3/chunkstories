package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.world.WorldInfoImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketSendWorldInfo extends Packet {
	public WorldInfoImplementation info;

	public PacketSendWorldInfo() {

	}

	public PacketSendWorldInfo(WorldInfoImplementation info) {
		this.info = info;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeUTF(info.saveAsString());
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		throw new UnsupportedOperationException();
	}
}
