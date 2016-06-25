package io.xol.chunkstories.world.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketChunkSummary;
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
				
				ChunkHolder holder = world.getChunksHolder().getChunkHolder(chunkX, chunkY, chunkZ, true);
				if(holder.isLoaded())
				{
					//System.out.println("snding actly: "+chunkX+":"+chunkY+":"+chunkZ);
					//CubicChunk c = world.getChunk(chunkX, chunkY, chunkZ, true);
					//ChunkHolder holder = c.holder;
					
					PacketChunkCompressedData packet = new PacketChunkCompressedData(false);
					packet.setPosition(chunkX, chunkY, chunkZ);
					packet.data = holder.getCompressedData(chunkX, chunkY, chunkZ);
					client.pushPacket(packet);
					//System.out.println("Replying with chunk ");
					
					return true;
				}
				else
				{
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
	
	class IOTaskSendChunkSummary extends IOTask
	{
		ServerClient client;
		int rx, rz;
		
		public IOTaskSendChunkSummary(int x, int z, ServerClient client)
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
				RegionSummary summary = world.getRegionSummaries().get(rx * 256, rz * 256);
				//System.out.println("Asking for summary at : "+rx+":"+rz);
				if(summary.loaded.get())
				{
					PacketChunkSummary packet = new PacketChunkSummary(false);
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

	public void requestChunkSummary(int x, int z, ServerClient sender)
	{
		IOTaskSendChunkSummary task = new IOTaskSendChunkSummary(x, z, sender);
		addTask(task);
	}
}
