package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IOTasksMultiplayerClient extends IOTasks
{
	Client client;
	ServerConnection connection;
	
	public IOTasksMultiplayerClient(WorldClientRemote world)
	{
		super(world);
		this.connection = world.getConnection();
		this.client = world.getClient();
		
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}

	MessageDigest md = null;

	static ThreadLocal<byte[]> unCompressedSummariesData = new ThreadLocal<byte[]>()
	{
		@Override
		protected byte[] initialValue()
		{
			//Buffer for summaries
			return new byte[256 * 256 * 4 * 2];
		}
	};
	
	@Override
	public IOTask requestChunkLoad(ChunkHolderImplementation slot)
	{
		//connection.sendTextMessage("world/getChunkCompressed:" + slot.getChunkCoordinateX() + ":" + slot.getChunkCoordinateY() + ":" + slot.getChunkCoordinateZ());
		return null;
	}
	
	@Override
	public void requestRegionLoad(RegionImplementation holder)
	{
		holder.setDiskDataLoaded(true);
	}

	public class IOTaskProcessCompressedRegionSummaryArrival extends IOTask
	{
		PacketRegionSummary packet;

		public IOTaskProcessCompressedRegionSummaryArrival(PacketRegionSummary packet)
		{
			this.packet = packet;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			RegionSummaryImplementation summary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(packet.rx * 256, packet.rz * 256);
			if(summary == null)
			{
				logger().error("Summary data arrived for "+packet.rx+ ": "+packet.rz + "but there was no region summary waiting for it ?");
				return true;
			}
			
			int[] heights = new int[256*256];
			int[] ids = new int[256*256];
			
			byte[] unCompressedSummaries = unCompressedSummariesData.get();
			unCompressedSummaries = RegionSummaryImplementation.decompressor.decompress(packet.compressedData, 256 * 256 * 4 * 2);
			IntBuffer ib = ByteBuffer.wrap(unCompressedSummaries).asIntBuffer();
			ib.get(heights, 0, 256 * 256);
			ib.get(ids, 0, 256 * 256);
			
			summary.setSummaryData(heights, ids);
			
			return true;
		}

		/*@Override
		public boolean equals(Object o)
		{
			//All packets are unique
			return false;
		}*/

		@Override
		public int hashCode()
		{
			return 8792;
		}
	}

	public void requestRegionSummaryProcess(PacketRegionSummary packet)
	{
		IOTaskProcessCompressedRegionSummaryArrival task = new IOTaskProcessCompressedRegionSummaryArrival(packet);
		scheduleTask(task);
	}

	@Override
	public Fence requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		return null;
	}

	public void handlePacketWorldStreaming(PacketWorldStreaming packet) throws IllegalPacketException {
		
		//Region summaries
		if(packet instanceof PacketRegionSummary) {
			this.requestRegionSummaryProcess((PacketRegionSummary) packet);
		//Chunk data
		} else if(packet instanceof PacketChunkCompressedData) {
			RegionImplementation region = world.getRegionChunkCoordinates(((PacketChunkCompressedData) packet).x, ((PacketChunkCompressedData) packet).y, ((PacketChunkCompressedData) packet).z);
			
			//This *can* happen, ie if the player flies fucking fast and the server sends the chunk but he's already fucking gone
			if(region == null)
				return;
			region.getChunkHolder(((PacketChunkCompressedData) packet).x, ((PacketChunkCompressedData) packet).y, ((PacketChunkCompressedData) packet).z).
				createChunk(((PacketChunkCompressedData) packet).data);
		}
		
		//Else
		else throw new IllegalPacketException(packet) {
			private static final long serialVersionUID = 7843266994553911002L;

			@Override
			public String getMessage()
			{
				return "Illegal packet received : This type of World streaming packet isn't recognized ( "+packet.getClass().getName()+" )";
			}
		};
	}
}
