package io.xol.chunkstories.world.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.net.packets.Packet02ChunkCompressedData;
import io.xol.chunkstories.net.packets.Packet03ChunkSummary;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.ChunkHolder;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.summary.ChunkSummary;

public class IOTasksMultiplayerServer extends IOTasks
{
	public IOTasksMultiplayerServer(World world)
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
		
		public boolean run()
		{
			try
			{
				ChunkHolder holder = world.chunksHolder.getChunkHolder(chunkX, chunkY, chunkZ, true);
				if(holder.isLoaded())
				{
					//CubicChunk c = world.getChunk(chunkX, chunkY, chunkZ, true);
					//ChunkHolder holder = c.holder;
					
					Packet02ChunkCompressedData packet = new Packet02ChunkCompressedData(false);
					packet.setPosition(chunkX, chunkY, chunkZ);
					packet.data = holder.getCompressedData(chunkX, chunkY, chunkZ);
					client.sendPacket(packet);
					//System.out.println("Replying with chunk "+c);
					
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

		/*private String toStr(byte[] digest)
		{
			BigInteger bigInt = new BigInteger(1,digest);
			return bigInt.toString(16);
		}*/
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
		
		public boolean run()
		{
			try
			{
				ChunkSummary summary = world.chunkSummaries.get(rx * 256, rz * 256);
				//System.out.println("Asking for summary at : "+rx+":"+rz);
				if(summary.loaded.get())
				{
					Packet03ChunkSummary packet = new Packet03ChunkSummary(false);
					packet.summary = summary;
					client.sendPacket(packet);
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
