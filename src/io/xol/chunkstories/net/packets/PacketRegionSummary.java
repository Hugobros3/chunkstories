package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketRegionSummary extends Packet
{
	// Server-side
	public RegionSummaryImplementation summary;
	
	// Client-side
	public int rx, rz;
	public byte[] compressedData;
	
	public PacketRegionSummary(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeInt(summary.getRegionX());
		out.writeInt(summary.getRegionZ());
		ByteBuffer compressMe = ByteBuffer.allocateDirect(256 * 256 * 4 * 2);
			for(int i = 0; i < 256 * 256; i++)
				compressMe.putInt(summary.heights[i]);
			for(int i = 0; i < 256 * 256; i++)
				compressMe.putInt(summary.ids[i]);
			
		compressMe.flip();
		byte[] unCompressed = new byte[compressMe.remaining()];
		compressMe.get(unCompressed);
		byte[] compressedData = RegionSummaryImplementation.compressor.compress(unCompressed);
		out.writeInt(compressedData.length);
		out.write(compressedData);
	}
	
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		rx = in.readInt();
		rz = in.readInt();
		//System.out.println("read "+rx+":"+rz);
		int dataLength = in.readInt();
		compressedData = new byte[dataLength];
		in.readFully(compressedData);
	}

	public void process(PacketsProcessor processor)
	{
		if(processor.isClient)
			((IOTasksMultiplayerClient) Client.world.ioHandler).requestRegionSummaryProcess(this);
	}

}
