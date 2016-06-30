package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketChunkSummary;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.summary.RegionSummary;
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

	Set<ChunkLocation> chunksAlreadyAsked = ConcurrentHashMap.newKeySet();
	List<int[]> summariesAlreadyAsked = new ArrayList<int[]>();

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
			
			CubicChunk c = new CubicChunk(region, chunkX, chunkY, chunkZ);
			
			//In any client scenario we don't need to check for a chunk holder to be already present neither do we need
			//to let it load.

			if (data != null)
			{
				try
				{
					decompressor.decompress(data, unCompressedDataBuffer.get());
					for (int i = 0; i < 32 * 32 * 32; i++)
					{
						int data = ((unCompressedDataBuffer.get()[i * 4] & 0xFF) << 24) | ((unCompressedDataBuffer.get()[i * 4 + 1] & 0xFF) << 16) | ((unCompressedDataBuffer.get()[i * 4 + 2] & 0xFF) << 8) | (unCompressedDataBuffer.get()[i * 4 + 3] & 0xFF);
						c.setDataAtWithoutUpdates(i / 32 / 32, (i / 32) % 32, i % 32, data);
					}
				}
				catch (LZ4Exception exception)
				{
					ChunkStoriesLogger.getInstance().warning("Invalid chunk data received for : " + c);
				}
			}

			c.bakeVoxelLightning(true);

			//Remove any object preventing us from asking it again
			ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);
			chunksAlreadyAsked.remove(loc);

			world.setChunk(c);
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
		addTask(task);
	}

	public void requestChunkCompressedDataProcess(int x, int y, int z, byte[] data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(x, y, z, data);
		addTask(task);
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
	public void requestChunkLoad(ChunkHolder holder, int chunkX, int chunkY, int chunkZ, boolean overwrite)
	{
		ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);
		//Only asks server once about the load request
		if (!this.chunksAlreadyAsked.contains(loc))
		{
			chunksAlreadyAsked.add(loc);
			//Thread.currentThread().dumpStack();
			Client.connection.sendTextMessage("world/getChunkCompressed:" + chunkX + ":" + chunkY + ":" + chunkZ);
		}
	}

	@Override
	public void notifyChunkUnload(int chunkX, int chunkY, int chunkZ)
	{
		ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);
		chunksAlreadyAsked.remove(loc);
	}

	@Override
	public void requestChunkHolderLoad(ChunkHolder holder)
	{
		holder.setLoaded(true);
	}

	public class IOTaskProcessCompressedChunkSummaryArrival extends IOTask
	{
		PacketChunkSummary packet;

		public IOTaskProcessCompressedChunkSummaryArrival(PacketChunkSummary packet)
		{
			this.packet = packet;
		}

		@Override
		public boolean run()
		{
			synchronized (Client.world.getRegionSummaries())
			{
				RegionSummary summary = Client.world.getRegionSummaries().getRegionSummaryWorldCoordinates(packet.rx * 256, packet.rz * 256);
				byte[] unCompressedSummaries = unCompressedSummariesData.get();
				unCompressedSummaries = RegionSummary.decompressor.decompress(packet.compressedData, 256 * 256 * 4 * 2);
				IntBuffer ib = ByteBuffer.wrap(unCompressedSummaries).asIntBuffer();
				ib.get(summary.heights, 0, 256 * 256);
				ib.get(summary.ids, 0, 256 * 256);
				// System.arraycopy(uncompressed, 0, summary.heights, 0, 256 *
				// 256 * 4);
				summary.uploadUpToDate.set(false);
				summary.loaded.set(true);

				summary.computeHeightMetadata();
			}
			synchronized (summariesAlreadyAsked)
			{
				Iterator<int[]> i = summariesAlreadyAsked.iterator();
				while (i.hasNext())
				{
					int[] coordinates = i.next();
					if (coordinates[0] == packet.rx && coordinates[1] == packet.rz)
						i.remove();
				}
			}
			// System.out.println("Successfully loaded chunk summary from remote server. rx:"+summary.rx+"rz:"+summary.rz);
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

	public void requestChunkSummaryProcess(PacketChunkSummary packet)
	{
		IOTaskProcessCompressedChunkSummaryArrival task = new IOTaskProcessCompressedChunkSummaryArrival(packet);
		addTask(task);
	}

	@Override
	public void requestChunkSummaryLoad(RegionSummary summary)
	{
		// don't spam packets !
		int rx = summary.rx;
		int rz = summary.rz;
		boolean alreadyAsked = false;
		synchronized (summariesAlreadyAsked)
		{
			for (int[] coordinates : summariesAlreadyAsked)
			{
				if (coordinates[0] == rx && coordinates[1] == rz)
					alreadyAsked = true;
			}
		}
		if (!alreadyAsked)
		{
			synchronized (summariesAlreadyAsked)
			{
				summariesAlreadyAsked.add(new int[] { rx, rz });
			}
			Client.connection.sendTextMessage("world/getChunkSummary:" + rx + ":" + rz);
		}
	}
}
