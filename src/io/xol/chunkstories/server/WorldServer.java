package io.xol.chunkstories.server;

import java.io.File;
import java.util.List;

import io.xol.chunkstories.net.packets.Packet04Entity;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksMultiplayerServer;
import io.xol.engine.math.LoopingMathHelper;

public class WorldServer extends World
{
	public WorldServer(String worldDir)
	{
		super(new WorldInfo(new File(worldDir+"/info.txt"), new File(worldDir).getName()));
		client = false;
		
		ioHandler = new IOTasksMultiplayerServer(this);
		ioHandler.start();
	}

	@Override
	public void tick()
	{
		List<CubicChunk> allChunks = getAllLoadedChunks();
		
		int chunksViewDistance = 256/32;
		int sizeInChunks = size.sizeInChunks;
		
		int removedChunks = 0;
		//Chunks pruner
		for (CubicChunk c : allChunks)
		{
			boolean neededBySomeone = false;
			//TODO clean
			for(ServerClient client : Server.getInstance().handler.clients)
			{
				if(client.authentificated && client.profile != null)
				{
					int pCX = (int)client.profile.entity.posX/32;
					int pCY = (int)client.profile.entity.posY/32;
					int pCZ = (int)client.profile.entity.posZ/32;
					
					if ( !((LoopingMathHelper.moduloDistance(c.chunkX, pCX, sizeInChunks) > chunksViewDistance + 2)
							|| (LoopingMathHelper.moduloDistance(c.chunkZ, pCZ, sizeInChunks) > chunksViewDistance + 2) || (Math.abs(c.chunkY - pCY) > 4)) )
					{
						 neededBySomeone = true;
					}
				}
			}
			
			if(!neededBySomeone)
			{
				//System.out.println("Removed");
				removeChunk(c, false);
				removedChunks++;
			}
		}
		if(removedChunks > 0)
			System.out.println("Removed "+removedChunks+" chunks.");
		
		//Update client tracking
		for(ServerClient client : Server.getInstance().handler.getAuthentificatedClients())
		{
			//System.out.println("Tast");
			if(client.profile != null)
				if(client.profile.hasSpawned)
					client.profile.updateTrackedEntities();
				//else
				//	System.out.println("not spawned :'(");
		}
		
		//System.out.println("Test");
		super.tick();
	}
	
	public void handleWorldMessage(ServerClient sender, String message)
	{
		if(message.equals("info"))
		{
			//Sends the construction info for the world, and then the player entity
			worldInfo.sendInfo(sender);
			if(sender.profile.entity != null)
			{
				Packet04Entity packet = new Packet04Entity(false, this);
				packet.entity = sender.profile.entity;
				packet.defineControl = true;
				sender.sendPacket(packet);
				sender.profile.hasSpawned = true;
				System.out.println("hasSpawned = true");
			}
		}
		if(message.startsWith("getChunkCompressed"))
		{
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int y = Integer.parseInt(split[2]);
			int z = Integer.parseInt(split[3]);
			((IOTasksMultiplayerServer) ioHandler).requestCompressedChunkSend(x, y, z, sender);
		}
		if(message.startsWith("getChunkSummary"))
		{
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int z = Integer.parseInt(split[2]);
			((IOTasksMultiplayerServer) ioHandler).requestChunkSummary(x, z, sender);
		}
	}
}
