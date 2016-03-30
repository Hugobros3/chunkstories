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

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketChunkSummary;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.summary.ChunkSummary;
import net.jpountz.lz4.LZ4Exception;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IOTasksMultiplayerClient extends IOTasks
{
	public IOTasksMultiplayerClient(World world)
	{
		super(world);
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	MessageDigest md = null;

	public class IOTaskProcessCompressedChunkArrival extends IOTask
	{
		int x, y, z;
		byte[] data;

		public IOTaskProcessCompressedChunkArrival(int x, int y, int z, byte[] data)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.data = data;
		}

		@Override
		public boolean run()
		{
			CubicChunk c = new CubicChunk(world, x, y, z);
			ChunkHolder holder = world.chunksHolder.getChunkHolder(x, y, z, true);
			// If for some reasons the chunks holder's are still not loaded, we
			// requeue the job for later.
			if (holder == null)
				return false;
			if (!holder.isLoaded())
				return false;

			// if (holder.isChunkLoaded(x, y, z) && !overwrite)
			// return true;

			if (data != null)
			{
				// System.out.println("Running task x:" + x + "y:" + y + "z:" +
				// z + " data.length=" + data.length + " md5:" +
				// toStr(md.digest(data)));
				try
				{
					decompressor.decompress(data, unCompressedData);
					for (int i = 0; i < 32 * 32 * 32; i++)
					{
						int data = ((unCompressedData[i * 4] & 0xFF) << 24) | ((unCompressedData[i * 4 + 1] & 0xFF) << 16) | ((unCompressedData[i * 4 + 2] & 0xFF) << 8) | (unCompressedData[i * 4 + 3] & 0xFF);
						c.setDataAt(i / 32 / 32, (i / 32) % 32, i % 32, data);
					}
				}
				catch (LZ4Exception exception)
				{
					ChunkStoriesLogger.getInstance().warning("Invalid chunk data received for : " + c);
				}
			}

			c.bakeVoxelLightning(false);

			//Remove any object preventing us from asking it again
			ChunkLocation loc = new ChunkLocation(x, y, z);
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
			return 6792;
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

	Set<ChunkLocation> chunksAlreadyAsked = ConcurrentHashMap.newKeySet();

	class ChunkLocation{
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
			if(o instanceof ChunkLocation)
			{
				ChunkLocation loc = ((ChunkLocation)o);
				if(loc.chunkX == chunkX && loc.chunkY == chunkY && loc.chunkZ == chunkZ)
					return true;
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return (chunkX * 65536 * 256 + chunkY * 65536 + chunkZ)%21000000;
		}
	}
	
	@Override
	public void requestChunkLoad(int chunkX, int chunkY, int chunkZ, boolean overwrite)
	{
		ChunkLocation loc = new ChunkLocation(chunkX, chunkY, chunkZ);
		if (!this.chunksAlreadyAsked.contains(loc))
		{
			chunksAlreadyAsked.add(loc);
			Client.connection.sendTextMessage("world/getChunkCompressed:" + chunkX + ":" + chunkY + ":" + chunkZ);
			//System.out.println("K x" + chunkX + "y:" + chunkY + "z:" + chunkZ + "alreadyAsked" + chunksAlreadyAsked.size());
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

	static byte[] uncompressed = new byte[256 * 256 * 4 * 2];

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
			synchronized (Client.world.chunkSummaries)
			{
				ChunkSummary summary = Client.world.chunkSummaries.get(packet.rx * 256, packet.rz * 256);
				uncompressed = ChunkSummary.decompressor.decompress(packet.compressedData, 256 * 256 * 4 * 2);
				IntBuffer ib = ByteBuffer.wrap(uncompressed).asIntBuffer();
				ib.get(summary.heights, 0, 256 * 256);
				ib.get(summary.ids, 0, 256 * 256);
				// System.arraycopy(uncompressed, 0, summary.heights, 0, 256 *
				// 256 * 4);
				summary.uploadUpToDate.set(false);
				summary.loaded.set(true);
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

	List<int[]> summariesAlreadyAsked = new ArrayList<int[]>();

	@Override
	public void requestChunkSummaryLoad(ChunkSummary summary)
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
			// System.out.println("K x" + chunkX + "y:" + chunkY + "z:" + chunkZ
			// + "alreadyAsked" + chunksAlreadyAsked.size());
		}
	}
}
