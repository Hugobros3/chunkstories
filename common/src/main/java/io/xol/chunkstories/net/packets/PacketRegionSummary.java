package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketRegionSummary extends PacketWorldStreaming
{
	// Server-side
	public RegionSummaryImplementation summary;
	
	// Client-side
	public int rx, rz;
	public byte[] compressedData;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		super.send(destinator, out);
		
		out.writeInt(summary.getRegionX());
		out.writeInt(summary.getRegionZ());
		
		int[] heights = summary.getHeightData();
		int[] ids = summary.getVoxelData();
		
		ByteBuffer compressMe = ByteBuffer.allocateDirect(256 * 256 * 4 * 2);
			for(int i = 0; i < 256 * 256; i++)
				compressMe.putInt(heights[i]);
			for(int i = 0; i < 256 * 256; i++)
				compressMe.putInt(ids[i]);
			
		compressMe.flip();
		byte[] unCompressed = new byte[compressMe.remaining()];
		compressMe.get(unCompressed);
		byte[] compressedData = RegionSummaryImplementation.compressor.compress(unCompressed);
		out.writeInt(compressedData.length);
		out.write(compressedData);
	}
	
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		super.process(sender, in, processor);
		
		rx = in.readInt();
		rz = in.readInt();
		//System.out.println("read "+rx+":"+rz);
		int dataLength = in.readInt();
		compressedData = new byte[dataLength];
		in.readFully(compressedData);
		
		//No fancy processing here, these packets are handled automatically by the IO controller on the client due to their 
		//PacketWorldStreaming nature
	}
}
