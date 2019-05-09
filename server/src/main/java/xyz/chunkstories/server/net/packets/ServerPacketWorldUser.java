//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.api.net.packets.PacketWorldUser;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.server.player.ServerPlayer;

public class ServerPacketWorldUser extends PacketWorldUser {

	public ServerPacketWorldUser(World world) {
		super(world);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		super.process(sender, in, processor);

		// that was a fun debugging session >:(
		/*if (type == Type.REGISTER_SUMMARY || type == Type.UNREGISTER_SUMMARY) {

			int sizeInRegions = processor.getWorld().getSizeInChunks() / 8;
			int filteredX = x % sizeInRegions;
			if (filteredX < 0)
				filteredX += sizeInRegions;

			int filteredZ = z % sizeInRegions;
			if (filteredZ < 0)
				filteredZ += sizeInRegions;

			if (x != filteredX || z != filteredZ) {
				// System.out.println("warning: someone forgot to sanitize their region coordinates!");
				x = filteredX;
				z = filteredZ;

				// System.out.println("og: "+x+": "+z);
				// System.out.println(filteredX+":"+filteredZ);
			}
		}*/

		if (sender instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) sender;
			player.getLoadingAgent().handleClientRequest(this);
		}
	}
}
