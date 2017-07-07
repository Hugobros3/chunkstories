package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.net.ClientConnection;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.workers.TaskExecutor;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.concurrency.SimpleFence;
import net.jpountz.lz4.LZ4Exception;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IOTasksMultiplayerClient extends IOTasks
{
	ClientConnection connection;
	
	public IOTasksMultiplayerClient(WorldImplementation world, ClientConnection connection)
	{
		super(world);
		this.connection = connection;
		
		
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

	public class IOTaskProcessCompressedChunkArrival extends IOTask
	{
		int chunkX, chunkY, chunkZ;
		byte[] data;

		public IOTaskProcessCompressedChunkArrival(int x, int y, int z, byte[] packetData)
		{
			this.chunkX = x;
			this.chunkY = y;
			this.chunkZ = z;
			this.data = packetData;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			RegionImplementation region = world.getRegionChunkCoordinates(chunkX, chunkY, chunkZ);

			if(region == null)
			{
				System.out.println("Notice: received chunk data for a chunk within an unloaded region ("+chunkX+","+chunkY+","+chunkZ+"). Ignoring.");
				return true;
			}
			
			ChunkHolderImplementation holder = region.getChunkHolder(chunkX, chunkY, chunkZ);
			//CubicChunk chunk = holder.getChunk();
			
			
			//Irrelevant because we made the IO handler create the chunks
			
			/*
			//Should never happen but sanity check doesn't hurt
			if(chunk == null)
			{
				System.out.println("Notice: received chunk data for an unloaded/unaquired chunk within a loaded region ("+chunkX+","+chunkY+","+chunkZ+"). Ignoring.");
				return true;
			}*/
			
			
			if (data != null)
			{
				try
				{
					decompressor.decompress(data, unCompressedDataBuffer.get());
					int[] data = new int[32 * 32 * 32];
					for (int i = 0; i < 32 * 32 * 32; i++)
					{
						data[i] = ((unCompressedDataBuffer.get()[i * 4] & 0xFF) << 24) | ((unCompressedDataBuffer.get()[i * 4 + 1] & 0xFF) << 16) | ((unCompressedDataBuffer.get()[i * 4 + 2] & 0xFF) << 8) | (unCompressedDataBuffer.get()[i * 4 + 3] & 0xFF);
					}
					
					holder.createChunk(data);
					//chunk.setChunkData(data);
					//chunk = new CubicChunk(region, chunkX, chunkY, chunkZ, data);
				}
				catch (LZ4Exception exception)
				{
					//chunk = new CubicChunk(region, chunkX, chunkY, chunkZ);
					ChunkStoriesLoggerImplementation.getInstance().warning("Invalid chunk data received for Chunk ("+chunkX+","+chunkY+","+chunkZ+")");
				}
			}
			else
				holder.createChunk();
				//chunk.setChunkData(null);

			//TODO make that a task ?
			//chunk.computeVoxelLightning(true);

			//Remove any object preventing us from asking it again
			//ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);

			//world.getRegionsHolder().getRegionChunkCoordinates(chunkX, chunkY, chunkZ).getChunkHolder(chunkX, chunkY, chunkZ).setChunk(chunk);
			return true;
		}

		@Override
		public int hashCode()
		{
			return 324028;
		}
	}

	public void requestChunkCompressedDataProcess(PacketChunkCompressedData data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(data.x, data.y, data.z, data.data);
		scheduleTask(task);
	}

	/*public void requestChunkCompressedDataProcess(int x, int y, int z, byte[] data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(x, y, z, data);
		scheduleTask(task);
	}*/

	class ChunkLocation
	{
		int chunkX, chunkY, chunkZ;

		public ChunkLocation(int x, int y, int z)
		{
			chunkX = x;
			chunkY = y;
			chunkZ = z;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof ChunkLocation)
			{
				ChunkLocation loc = ((ChunkLocation) o);
				if (loc.chunkX == chunkX && loc.chunkY == chunkY && loc.chunkZ == chunkZ)
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return (chunkX * 65536 * 256 + chunkY * 65536 + chunkZ) % 21000000;
		}
	}

	@Override
	public IOTask requestChunkLoad(ChunkHolderImplementation slot)
	{
		connection.sendTextMessage("world/getChunkCompressed:" + slot.getChunkCoordinateX() + ":" + slot.getChunkCoordinateY() + ":" + slot.getChunkCoordinateZ());
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
				ChunkStoriesLoggerImplementation.getInstance().error("Summary data arrived for "+packet.rx+ ": "+packet.rz + "but there was no region summary waiting for it ?");
				return true;
			}
			
			int[] heights = new int[256*256];
			int[] ids = new int[256*256];
			
			byte[] unCompressedSummaries = unCompressedSummariesData.get();
			unCompressedSummaries = RegionSummaryImplementation.decompressor.decompress(packet.compressedData, 256 * 256 * 4 * 2);
			IntBuffer ib = ByteBuffer.wrap(unCompressedSummaries).asIntBuffer();
			ib.get(heights, 0, 256 * 256);
			ib.get(ids, 0, 256 * 256);
			
			summary.setData(heights, ids);
			
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
		// don't spam packets !
		int rx = summary.getRegionX();
		int rz = summary.getRegionZ();
		
		connection.sendTextMessage("world/getChunkSummary:" + rx + ":" + rz);
		
		return new SimpleFence();
	}

	public void handlePacketWorldStreaming(PacketWorldStreaming packet) throws IllegalPacketException {
		
		//Region summaries
		if(packet instanceof PacketRegionSummary)
			this.requestRegionSummaryProcess((PacketRegionSummary) packet);
		
		//Chunk data
		else if(packet instanceof PacketChunkCompressedData)
			this.requestChunkCompressedDataProcess((PacketChunkCompressedData) packet);
		
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
