//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.world.WorldInfoUtilKt;

public class PacketSendWorldInfo extends Packet {
	public WorldInfo worldInfo;

	public PacketSendWorldInfo() {

	}

	public PacketSendWorldInfo(WorldInfo info) {
		this.worldInfo = info;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeUTF(WorldInfoUtilKt.serializeWorldInfo(worldInfo, false));
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		throw new UnsupportedOperationException();
	}
}
