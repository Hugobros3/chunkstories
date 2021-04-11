//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.net.packets.PacketWorldUser;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.client.ingame.LocalPlayerImplementation;
import xyz.chunkstories.client.net.ClientPacketsEncoderDecoder;

public class ClientPacketWorldUser extends PacketWorldUser {

	public ClientPacketWorldUser(World world) {
		super(world);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext context) throws IOException {
		super.process(sender, in, context);

		System.out.println("weeiiird");
		if (context instanceof ClientPacketsEncoderDecoder) {
			try {
				((LocalPlayerImplementation) ((ClientPacketsEncoderDecoder) context).getContext().getPlayer()).getLoadingAgent()
						.handleServerResponse(this);
			} catch (IllegalPacketException e) {
				e.printStackTrace();
			}
		}
	}
}
