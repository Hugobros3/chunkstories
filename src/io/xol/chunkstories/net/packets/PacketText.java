package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.server.Server;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketText extends Packet
{
	public PacketText(boolean client)
	{
		super(client);
	}

	public String text;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//out.writeByte(0x00);
		out.writeUTF(text);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		text = in.readUTF();
	}

	public void process(PacketsProcessor processor)
	{
		if(processor.isClient)
			processor.getClientToServerConnection().handleTextPacket(text);
		else
			Server.getInstance().getHandler().handle(processor.getServerClient(), text);
	}
}
