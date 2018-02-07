package io.xol.chunkstories.net;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.packets.PacketDummy;

public class PacketIngoingBuffered extends PacketDummy {

	private PacketSender sender;
	
	public PacketIngoingBuffered(PacketSender sender) {
		this.sender = sender;
	}
	
	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException, PacketProcessingException
	{
		int payload_size = in.readInt();
		data = new byte[payload_size];
		in.readFully(data);
	}
}
