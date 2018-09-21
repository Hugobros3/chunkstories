//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.packets.PacketWorldUser;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.ingame.LocalPlayerImplementation;
import io.xol.chunkstories.client.net.ClientPacketsContext;

public class ClientPacketWorldUser extends PacketWorldUser {

	public ClientPacketWorldUser(World world) {
		super(world);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext context) throws IOException {
		super.process(sender, in, context);

		System.out.println("weeiiird");
		if (context instanceof ClientPacketsContext) {
			try {
				((LocalPlayerImplementation) ((ClientPacketsContext) context).getContext().getPlayer()).loadingAgent
						.handleServerResponse(this);
			} catch (IllegalPacketException e) {
				e.printStackTrace();
			}
		}
	}
}
