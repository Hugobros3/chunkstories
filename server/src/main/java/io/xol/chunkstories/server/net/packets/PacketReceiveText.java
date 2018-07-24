//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.packets.PacketText;



public class PacketReceiveText extends PacketText
{
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException
	{
		super.process(sender, in, processor);
		//((ClientPacketsContext)processor).getConnection().handle(text);
		//((ClientPacketsProcessorImplementation)processor).getConnection().handleTextPacket(text);
	}
}
