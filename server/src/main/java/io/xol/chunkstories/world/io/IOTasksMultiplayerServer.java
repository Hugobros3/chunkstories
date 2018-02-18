package io.xol.chunkstories.world.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.server.net.ClientConnection;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class IOTasksMultiplayerServer extends IOTasks
{
	public IOTasksMultiplayerServer(WorldImplementation world)
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
	
	/*class IOTaskSendCompressedChunk extends IOTask
	{
		UserConnection client;
		int chunkX, chunkY, chunkZ;
		
		public IOTaskSendCompressedChunk(int x, int y, int z, UserConnection client)
		{
			this.client = client;
			this.chunkX = x;
			this.chunkY = y;
			this.chunkZ = z;
		}
		
		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			try
			{
				//Cancel the task if the client disconnected
				if(!client.isAlive())
					return true;
				
				RegionImplementation holder = world.getRegionsHolder().getRegionChunkCoordinates(chunkX, chunkY, chunkZ);
				if(holder == null)
					return false;

				//Wait until the disk data loads
				if(!holder.isDiskDataLoaded())
					return false;
				else
				{
					PacketChunkCompressedData packet = new PacketChunkCompressedData();
					packet.setPosition(chunkX, chunkY, chunkZ);
					
					//We make SURE not to push the fake placeholder byte[] tag AIR_CHUNK_NO_DATA_SAVED is.
					byte[] compressedData = holder.getCompressedData(chunkX, chunkY, chunkZ);
					if(compressedData != ChunkHolderImplementation.AIR_CHUNK_NO_DATA_SAVED)
						packet.data = compressedData;
					
					client.pushPacket(packet);
					
					return true;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o != null && o instanceof IOTaskSendCompressedChunk)
			{
				IOTaskSendCompressedChunk comp = ((IOTaskSendCompressedChunk)o);
				if(comp.client.equals(this.client) && comp.chunkX == this.chunkX && comp.chunkY == this.chunkY && comp.chunkZ == this.chunkZ)
					return true;
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return (-65536 + 7777 * chunkX + 256 * chunkY + chunkZ) % 2147483647;
		}
	}

	public void requestCompressedChunkSend_(int x, int y, int z, UserConnection sender)
	{
		IOTaskSendCompressedChunk task = new IOTaskSendCompressedChunk(x, y, z, sender);
		scheduleTask(task);
	}*/
	
	/*class IOTaskSendRegionSummary extends IOTask
	{
		ClientConnection client;
		int rx, rz;
		
		public IOTaskSendRegionSummary(int x, int z, ClientConnection client)
		{
			this.client = client;
			this.rx = x;
			this.rz = z;
		}
		
		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			try
			{
				//Cancel the task if the client disconnected
				if(!client.isOpen())
					return true;
				
				RegionSummaryImplementation summary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(rx * 256, rz * 256);
				
				//Don't send the data until we have the summary loaded in
				if(summary == null || !summary.isLoaded())
					return false;
				
				PacketRegionSummary packet = new PacketRegionSummary(world);
				packet.summary = summary;
				client.pushPacket(packet);
				return true;
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
	}

	public void requestRegionSummary(int x, int z, ClientConnection sender)
	{
		IOTaskSendRegionSummary task = new IOTaskSendRegionSummary(x, z, sender);
		scheduleTask(task);
	}*/
}
