//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.api.net.packets.PacketText;

public class PacketReceiveText extends PacketText {
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		super.process(sender, in, processor);
		// ((ClientPacketsEncoderDecoder)processor).getConnection().handleTextPacket(text);
	}
}
