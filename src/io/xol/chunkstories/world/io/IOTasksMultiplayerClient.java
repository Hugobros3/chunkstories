package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.Packet02ChunkCompressedData;
import io.xol.chunkstories.net.packets.Packet03ChunkSummary;
import io.xol.chunkstories.world.ChunkHolder;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.summary.ChunkSummary;

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
				decompressor.decompress(data, unCompressedData);
				for (int i = 0; i < 32 * 32 * 32; i++)
				{
					int data = ((unCompressedData[i * 4] & 0xFF) << 24) | ((unCompressedData[i * 4 + 1] & 0xFF) << 16) | ((unCompressedData[i * 4 + 2] & 0xFF) << 8) | (unCompressedData[i * 4 + 3] & 0xFF);
					c.setDataAt(i / 32 / 32, (i / 32) % 32, i % 32, data);
				}
			}

			c.doLightning(false, blockSources, sunSources);

			// synchronized (chunksAlreadyAsked)
			{
				Iterator<int[]> i = chunksAlreadyAsked.iterator();
				while (i.hasNext())
				{
					int[] coordinates = i.next();
					if (coordinates[0] == x && coordinates[1] == y && coordinates[2] == z)
						i.remove();
				}
				// System.out.println(chunksAlreadyAsked.size());
			}

			world.setChunk(c);
			return true;
		}
		
		@Override
		public boolean equals(Object o)
		{
			/*if(o instanceof IOTaskProcessCompressedChunkArrival)
			{
				IOTaskProcessCompressedChunkArrival comp = ((IOTaskProcessCompressedChunkArrival)o);
				if(comp.x == this.x && comp.y == this.y && comp.z == this.z)
					return true;
			}*/
			//All packets are unique
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return 6792;
			//return (int) ((65536 + 65536L * x + 256 * y + z) % 2147483647);
		}
	}

	public void requestChunkCompressedDataProcess(Packet02ChunkCompressedData data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(data.x, data.y, data.z, data.data);
		addTask(task);
	}

	public void requestChunkCompressedDataProcess(int x, int y, int z, byte[] data)
	{
		IOTaskProcessCompressedChunkArrival task = new IOTaskProcessCompressedChunkArrival(x, y, z, data);
		addTask(task);
	}

	Queue<int[]> chunksAlreadyAsked = new ConcurrentLinkedQueue<int[]>();

	public void requestChunkLoad(int chunkX, int chunkY, int chunkZ, boolean overwrite)
	{
		// don't spam packets !
		boolean alreadyAsked = false;
		// synchronized (chunksAlreadyAsked)
		// {
		for (int[] coordinates : chunksAlreadyAsked)
		{
			if (coordinates[0] == chunkX && coordinates[1] == chunkY && coordinates[2] == chunkZ)
				alreadyAsked = true;
		}
		// }
		if (!alreadyAsked)
		{
			// synchronized (chunksAlreadyAsked)
			{
				chunksAlreadyAsked.add(new int[] { chunkX, chunkY, chunkZ });
				// chunksAlreadyAsked.clear();
			}
			Client.connection.sendTextMessage("world/getChunkCompressed:" + chunkX + ":" + chunkY + ":" + chunkZ);
			// System.out.println("K x" + chunkX + "y:" + chunkY + "z:" + chunkZ
			// + "alreadyAsked" + chunksAlreadyAsked.size());
		}
		else
		{
			// chunksAlreadyAsked.clear();
			// System.out.println("K x" + chunkX + "y:" + chunkY + "z:" + chunkZ
			// + "alreadyAsked" + chunksAlreadyAsked.size());
		}
	}

	public void notifyChunkUnload(int chunkX, int chunkY, int chunkZ)
	{
		// synchronized (chunksAlreadyAsked)
		{
			Iterator<int[]> i = chunksAlreadyAsked.iterator();
			while (i.hasNext())
			{
				int[] coordinates = i.next();
				if (coordinates[0] == chunkX && coordinates[1] == chunkY && coordinates[2] == chunkZ)
					i.remove();
			}
		}
	}

	public void requestChunkHolderLoad(ChunkHolder holder)
	{
		holder.setLoaded(true);
		// IOTask task = new IOTaskLoadChunkHolder(holder);
		// addTask(task);
	}

	static byte[] uncompressed = new byte[256 * 256 * 4 * 2];

	public class IOTaskProcessCompressedChunkSummaryArrival extends IOTask
	{
		Packet03ChunkSummary packet;

		public IOTaskProcessCompressedChunkSummaryArrival(Packet03ChunkSummary packet)
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
			/*if(o instanceof IOTaskProcessCompressedChunkSummaryArrival)
			{
				IOTaskProcessCompressedChunkSummaryArrival comp = ((IOTaskProcessCompressedChunkSummaryArrival)o);
				if(comp.packet.rx == this.packet.rx && comp.packet.rz == this.packet.rz)
					return true;
			}*/
			
			//All packets are unique
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return (int) 8792;
		}
	}

	public void requestChunkSummaryProcess(Packet03ChunkSummary packet)
	{
		IOTaskProcessCompressedChunkSummaryArrival task = new IOTaskProcessCompressedChunkSummaryArrival(packet);
		addTask(task);
	}

	List<int[]> summariesAlreadyAsked = new ArrayList<int[]>();

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
