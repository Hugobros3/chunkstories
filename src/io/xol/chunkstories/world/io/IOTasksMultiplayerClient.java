package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import net.jpountz.lz4.LZ4Exception;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IOTasksMultiplayerClient extends IOTasks
{
	public IOTasksMultiplayerClient(WorldImplementation world)
	{
		super(world);
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
		public boolean run()
		{
			Region region = world.getRegionChunkCoordinates(chunkX, chunkY, chunkZ);
			
			CubicChunk c = null;// new CubicChunk(region, chunkX, chunkY, chunkZ);
			
			//In any client scenario we don't need to check for a chunk holder to be already present neither do we need
			//to let it load.

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
					c = new CubicChunk(region, chunkX, chunkY, chunkZ, data);
				}
				catch (LZ4Exception exception)
				{
					c = new CubicChunk(region, chunkX, chunkY, chunkZ);
					ChunkStoriesLogger.getInstance().warning("Invalid chunk data received for : " + c);
				}
			}
			else
				c = new CubicChunk(region, chunkX, chunkY, chunkZ);

			c.bakeVoxelLightning(true);

			//Remove any object preventing us from asking it again
			//ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);

			world.getRegionsHolder().getRegionChunkCoordinates(chunkX, chunkY, chunkZ).getChunkHolder(chunkX, chunkY, chunkZ).setChunk(c);
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			//All packets are unique
			return false;
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

	public void requestChunkCompressedDataProcess(int x, int y, int z, byte[] data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(x, y, z, data);
		scheduleTask(task);
	}

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
		Client.connection.sendTextMessage("world/getChunkCompressed:" + slot.getChunkCoordinateX() + ":" + slot.getChunkCoordinateY() + ":" + slot.getChunkCoordinateZ());
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
		public boolean run()
		{
				RegionSummaryImplementation summary = Client.world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(packet.rx * 256, packet.rz * 256);
				
				if(summary == null)
				{
					ChunkStoriesLogger.getInstance().error("Summary data arrived for "+packet.rx+ ": "+packet.rz + "but there was no region summary waiting for it ?");
					return true;
				}
				
				byte[] unCompressedSummaries = unCompressedSummariesData.get();
				unCompressedSummaries = RegionSummaryImplementation.decompressor.decompress(packet.compressedData, 256 * 256 * 4 * 2);
				IntBuffer ib = ByteBuffer.wrap(unCompressedSummaries).asIntBuffer();
				ib.get(summary.heights, 0, 256 * 256);
				ib.get(summary.ids, 0, 256 * 256);
				
				summary.texturesUpToDate.set(false);
				summary.summaryLoaded.set(true);

				summary.computeHeightMetadata();
			
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			//All packets are unique
			return false;
		}

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
	public void requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		// don't spam packets !
		int rx = summary.getRegionX();
		int rz = summary.getRegionZ();
		
		Client.connection.sendTextMessage("world/getChunkSummary:" + rx + ":" + rz);
	}
}
