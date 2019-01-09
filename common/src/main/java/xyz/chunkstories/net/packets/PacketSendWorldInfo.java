//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.PacketDestinator;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.api.net.PacketSendingContext;
import xyz.chunkstories.api.world.WorldInfo;
import xyz.chunkstories.world.WorldInfoUtilKt;

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
