package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketChunkCompressedData extends Packet
{

	public PacketChunkCompressedData(boolean client)
	{
		super(client);
	}

	public void setPosition(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int x, y, z;
	public byte[] data = null;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		if (data == null || data.length == 0)
			out.writeInt(0);
		else
		{
			out.writeInt(data.length);
			out.write(data);
		}
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		
		int length = in.readInt();

		//System.out.println(length+"b packet received x:"+x+"y:"+y+"z:"+z);
		
		if(length > 0)
		{
			data = new byte[length];
			in.readFully(data, 0, length);
		}
	}

	public void process(PacketsProcessor processor)
	{
		if(processor.isClient)		
			((IOTasksMultiplayerClient) Client.world.ioHandler).requestChunkCompressedDataProcess(this);
	}
}
