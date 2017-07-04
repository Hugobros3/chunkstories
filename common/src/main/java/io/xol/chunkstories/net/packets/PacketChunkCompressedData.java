package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketChunkCompressedData extends PacketWorldStreaming
{
	public PacketChunkCompressedData()
	{
		
	}
	
	public PacketChunkCompressedData(CubicChunk c)
	{
		super((WorldMaster)c.getWorld());
		
		this.x = c.getChunkX();
		this.y = c.getChunkY();
		this.z = c.getChunkZ();
		
		this.data = c.holder().getCompressedData();
	}

	public int x, y, z;
	public byte[] data = null;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		super.send(destinator, out);
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
		super.process(sender, in, processor);
		
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		//Thread.dumpStack();
		
		int length = in.readInt();
		
		if(length > 0)
		{
			data = new byte[length];
			in.readFully(data, 0, length);
		}
		else
			data = null;
		
		//No fancy processing here, these packets are handled automatically by the IO controller on the client due to their 
		//PacketWorldStreaming nature
	}

	public void setPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
