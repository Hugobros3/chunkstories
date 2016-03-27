package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.WorldRemoteClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketWorldInfo extends Packet
{
	public PacketWorldInfo(boolean client)
	{
		super(client);
	}

	public WorldInfo info;
	
	@Override
	public void send(DataOutputStream out) throws IOException
	{
		String tg = "";
		for (String line : info.saveText())
			tg += line + "\n";
		char[] data = tg.toCharArray();
		//Wow such computations
		short length = (short) (data.length * 2);
		byte[] bytes = new byte[length + 2];
		//Write length in first 2 bytes
		bytes[0] = (byte) (length >> 8);
		bytes[1] = (byte) length;
		//Then the data
		for (int i = 0; i < data.length; i++)
		{
			bytes[i * 2 + 2] = (byte) (data[i] >> 8);
			bytes[i * 2 + 3] = (byte) data[i];
		}
		out.write(bytes);
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		short length = in.readShort();

		byte[] bytes = new byte[length];

		in.read(bytes, 0, length);

		char[] chars2 = new char[length / 2];
		for (int i = 0; i < chars2.length; i++)
			chars2[i] = (char) ((bytes[i * 2] << 8) + (bytes[i * 2 + 1] & 0xFF));
		
		info = new WorldInfo(new String(chars2), "");
	}

	@Override
	public void process(PacketsProcessor processor)
	{
		if(processor.isClient)
		{
			Client.world = new WorldRemoteClient(info);
			Client.world.startLogic();
		}
		
	}

}
