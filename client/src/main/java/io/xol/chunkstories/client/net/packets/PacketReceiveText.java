package io.xol.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.client.net.ClientPacketsProcessorImplementation;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketReceiveText extends PacketText
{
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		super.process(sender, in, processor);
		((ClientPacketsProcessorImplementation)processor).getConnection().handleTextPacket(text);
	}
}
