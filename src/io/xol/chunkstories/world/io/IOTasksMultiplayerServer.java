package io.xol.chunkstories.world.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.summary.RegionSummary;

//(c) 2015-2016 XolioWare Interactive
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
	
	class IOTaskSendCompressedChunk extends IOTask
	{
		ServerClient client;
		int chunkX, chunkY, chunkZ;
		
		public IOTaskSendCompressedChunk(int x, int y, int z, ServerClient client)
		{
			this.client = client;
			this.chunkX = x;
			this.chunkY = y;
			this.chunkZ = z;
		}
		
		@Override
		public boolean run()
		{
			try
			{
				//Don't bother if the client died.
				if(!client.isAlive())
					return true;
				
				ChunkHolder holder = world.getChunksHolder().getChunkHolderChunkCoordinates(chunkX, chunkY, chunkZ, true);
				if(holder.isDiskDataLoaded())
				{
					//System.out.println("snding actly: "+chunkX+":"+chunkY+":"+chunkZ);
					//CubicChunk c = world.getChunk(chunkX, chunkY, chunkZ, true);
					//ChunkHolder holder = c.holder;
					
					PacketChunkCompressedData packet = new PacketChunkCompressedData(false);
					packet.setPosition(chunkX, chunkY, chunkZ);
					packet.data = holder.getCompressedData(chunkX, chunkY, chunkZ);
					client.pushPacket(packet);
					
					return true;
				}
				else
				{
					//System.out.println("holder not loaded yet "+holder);
					
					/*for(IOTask task : tasks)
					{
						if(task instanceof IOTaskLoadChunkHolder)
							System.out.println(task);
					}
					
					
					IOTaskLoadChunkHolder saveChunkHolder = new IOTaskLoadChunkHolder(holder);
					if (tasks != null && tasks.contains(saveChunkHolder))
					{
						System.out.println("A load operation is still running on " + holder + ", waiting for it to complete.");
						return false;
					}*/
					
					//world.ioHandler.requestChunkLoad(chunkX, chunkY, chunkZ, false);
					return false;
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

	public void requestCompressedChunkSend(int x, int y, int z, ServerClient sender)
	{
		IOTaskSendCompressedChunk task = new IOTaskSendCompressedChunk(x, y, z, sender);
		addTask(task);
	}
	
	class IOTaskSendRegionSummary extends IOTask
	{
		ServerClient client;
		int rx, rz;
		
		public IOTaskSendRegionSummary(int x, int z, ServerClient client)
		{
			this.client = client;
			this.rx = x;
			this.rz = z;
		}
		
		@Override
		public boolean run()
		{
			try
			{
				//Don't bother if the client died.
				if(!client.isAlive())
					return true;
				
				RegionSummary summary = world.getRegionSummaries().getRegionSummaryWorldCoordinates(rx * 256, rz * 256);
				//System.out.println("Asking for summary at : "+rx+":"+rz);
				if(summary.summaryLoaded.get())
				{
					PacketRegionSummary packet = new PacketRegionSummary(false);
					packet.summary = summary;
					client.pushPacket(packet);
					return true;
				}
				else
				{
					return false;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
	}

	public void requestRegionSummary(int x, int z, ServerClient sender)
	{
		IOTaskSendRegionSummary task = new IOTaskSendRegionSummary(x, z, sender);
		addTask(task);
	}
}
