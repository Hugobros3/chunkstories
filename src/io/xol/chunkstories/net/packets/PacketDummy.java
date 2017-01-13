package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A dummy packet has no header, nor any particular meaning. It is expected to contain some header information in the data so the other end
 * knows what it is supposed to be
 */
public class PacketDummy extends Packet
{
	public byte[] data;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.write(data);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		throw new UnsupportedOperationException();
	}

}
