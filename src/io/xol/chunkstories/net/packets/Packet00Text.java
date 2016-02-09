package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Packet00Text extends Packet
{
	public Packet00Text(boolean client)
	{
		super(client);
	}

	public String text;

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		out.writeByte(0x00);
		out.writeUTF(text);
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		text = in.readUTF();
	}

	@Override
	public void process(PacketsProcessor processor)
	{
		// TODO Auto-generated method stub
		
	}

}
