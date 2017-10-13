package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketChunkCompressedData extends PacketWorldStreaming
{
	public PacketChunkCompressedData()
	{
		
	}
	
	public PacketChunkCompressedData(CubicChunk c, CompressedData data)
	{
		super((WorldMaster)c.getWorld());
		
		this.x = c.getChunkX();
		this.y = c.getChunkY();
		this.z = c.getChunkZ();
		
		this.data = data;
	}

	public int x, y, z;
	public CompressedData data = null;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		super.send(destinator, out);
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
	
		out.writeInt(data.voxelCompressedData.length);
		out.write(data.voxelCompressedData);
		
		out.writeInt(data.voxelComponentsCompressedData.length);
		out.write(data.voxelComponentsCompressedData);
		
		out.writeInt(data.entitiesCompressedData.length);
		out.write(data.entitiesCompressedData);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		super.process(sender, in, processor);
		
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		
		int voxelCompressedDataLength = in.readInt();
		byte[] voxelCompressedData;
		assert (voxelCompressedDataLength > 0);
		voxelCompressedData = new byte[voxelCompressedDataLength];
		in.readFully(voxelCompressedData, 0, voxelCompressedDataLength);
		
		int voxelComponentsCompressedDataLength = in.readInt();
		byte[] voxelComponentsCompressedData;
		assert (voxelComponentsCompressedDataLength > 0);
		voxelComponentsCompressedData = new byte[voxelComponentsCompressedDataLength];
		in.readFully(voxelComponentsCompressedData, 0, voxelComponentsCompressedDataLength);
		
		int entitiesCompressedDataLength = in.readInt();
		byte[] entitiesCompressedData;
		assert (entitiesCompressedDataLength > 0);
		entitiesCompressedData = new byte[entitiesCompressedDataLength];
		in.readFully(entitiesCompressedData, 0, entitiesCompressedDataLength);
		
		
		data = new CompressedData(voxelCompressedData, voxelComponentsCompressedData, entitiesCompressedData);
		//No fancy processing here, these packets are handled automatically by the IO controller on the client due to their 
		//PacketWorldStreaming nature
	}
}
