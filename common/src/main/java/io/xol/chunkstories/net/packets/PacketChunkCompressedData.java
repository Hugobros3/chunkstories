//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class PacketChunkCompressedData extends PacketWorldStreaming
{
	public PacketChunkCompressedData(World world)
	{
		super(world);
	}
	
	public PacketChunkCompressedData(CubicChunk c, CompressedData data)
	{
		super(c.getWorld());
		
		this.x = c.getChunkX();
		this.y = c.getChunkY();
		this.z = c.getChunkZ();
		
		if(data == null) {
			data = new CompressedData(null, null, null);
		}
		
		this.data = data;
	}

	public int x, y, z;
	public CompressedData data = null;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException
	{
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
	
		if(data.voxelCompressedData != null) {
			out.writeInt(data.voxelCompressedData.length);
			out.write(data.voxelCompressedData);
		} else
			out.writeInt(0);

		if(data.voxelComponentsCompressedData != null) {
		out.writeInt(data.voxelComponentsCompressedData.length);
		out.write(data.voxelComponentsCompressedData);
		} else
			out.writeInt(0);
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException
	{
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		
		int voxelCompressedDataLength = in.readInt();
		byte[] voxelCompressedData = null;
		
		//assert (voxelCompressedDataLength > 0);
		if(voxelCompressedDataLength > 0) {
			voxelCompressedData = new byte[voxelCompressedDataLength];
			in.readFully(voxelCompressedData, 0, voxelCompressedDataLength);
		}
		
		int voxelComponentsCompressedDataLength = in.readInt();
		byte[] voxelComponentsCompressedData = null;
		
		//assert (voxelComponentsCompressedDataLength > 0);
		if(voxelComponentsCompressedDataLength > 0) {
			voxelComponentsCompressedData = new byte[voxelComponentsCompressedDataLength];
			in.readFully(voxelComponentsCompressedData, 0, voxelComponentsCompressedDataLength);
		}
		
		data = new CompressedData(voxelCompressedData, voxelComponentsCompressedData, null);
		//No fancy processing here, these packets are handled automatically by the IO controller on the client due to their 
		//PacketWorldStreaming nature
	}
}
