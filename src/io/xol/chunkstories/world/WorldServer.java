package io.xol.chunkstories.world;

import java.io.File;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.core.PlayerSpawnEvent;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.net.packets.PacketTime;
import io.xol.chunkstories.net.packets.PacketVoxelUpdate;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasksMultiplayerServer;
import io.xol.engine.math.LoopingMathHelper;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldServer extends World implements WorldMaster
{
	public WorldServer(String worldDir)
	{
		super(new WorldInfo(new File(worldDir + "/info.txt"), new File(worldDir).getName()));
		client = false;

		ioHandler = new IOTasksMultiplayerServer(this);
		ioHandler.start();
	}

	@Override
	public void tick()
	{
		this.trimRemovableChunks();
		//Update client tracking
		for (ServerClient client : Server.getInstance().handler.getAuthentificatedClients())
		{
			if (client.getProfile().hasSpawned())
			{
				//Load 8x4x8 chunks arround player
				Location loc = client.getProfile().getLocation();
				int chunkX = (int) (loc.getX() / 32f);
				int chunkY = (int) (loc.getY() / 32f);
				int chunkZ = (int) (loc.getZ() / 32f);
				for(int cx = chunkX - 4 ; cx < chunkX + 4; cx ++)
					for(int cy = chunkY - 2 ; cy < chunkY + 2; cy ++)
						for(int cz = chunkZ - 4 ; cz < chunkZ + 4; cz ++)
							this.getChunk(chunkX, chunkY, chunkZ, true);
				
				//System.out.println("chunk:"+this.getChunk(chunkX, chunkY, chunkZ, true));
				//System.out.println("holder:"+client.getProfile().getControlledEntity().getChunkHolder());
				//Update whatever he controls
				client.getProfile().updateTrackedEntities();
			}
			PacketTime packetTime = new PacketTime(false);
			packetTime.time = this.worldTime;
			client.sendPacket(packetTime);
		}
		super.tick();
	}

	public void handleWorldMessage(ServerClient sender, String message)
	{
		if (message.equals("info"))
		{
			//Sends the construction info for the world, and then the player entity
			worldInfo.sendInfo(sender);

			PlayerSpawnEvent playerSpawnEvent = new PlayerSpawnEvent(sender.getProfile(), sender.getProfile().getLastPosition());
			Server.getInstance().getPluginsManager().fireEvent(playerSpawnEvent);

		}
		else if(message.equals("respawn"))
		{
			//TODO respawn request
		}
		if (message.startsWith("getChunkCompressed"))
		{
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int y = Integer.parseInt(split[2]);
			int z = Integer.parseInt(split[3]);
			((IOTasksMultiplayerServer) ioHandler).requestCompressedChunkSend(x, y, z, sender);
		}
		if (message.startsWith("getChunkSummary"))
		{
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int z = Integer.parseInt(split[2]);
			((IOTasksMultiplayerServer) ioHandler).requestChunkSummary(x, z, sender);
		}
	}

	@Override
	public void trimRemovableChunks()
	{
		int chunksViewDistance = 256 / 32;
		int sizeInChunks = getWorldInfo().getSize().sizeInChunks;

		//Chunks pruner
		ChunksIterator i = Server.getInstance().world.iterator();
		CubicChunk c;
		while (i.hasNext())
		{
			c = i.next();
			boolean neededBySomeone = false;
			for (ServerClient client : Server.getInstance().handler.clients)
			{
				if (client.isAuthentificated())
				{
					Entity clientEntity = client.getProfile().getControlledEntity();
					if (clientEntity == null)
						continue;
					Location loc = clientEntity.getLocation();
					int pCX = (int) loc.x / 32;
					int pCY = (int) loc.y / 32;
					int pCZ = (int) loc.z / 32;
					//TODO use proper configurable values for this
					if (!((LoopingMathHelper.moduloDistance(c.chunkX, pCX, sizeInChunks) > chunksViewDistance + 2) || (LoopingMathHelper.moduloDistance(c.chunkZ, pCZ, sizeInChunks) > chunksViewDistance + 2) || (Math.abs(c.chunkY - pCY) > 4)))
					{
						neededBySomeone = true;
					}
				}
			}

			if (!neededBySomeone)
			{
				//TODO
				//System.out.println("Removed");
				removeChunk(c, true);
			}
		}
		//if(removedChunks > 0)
		//	System.out.println("Removed "+removedChunks+" chunks.");
	}

	@Override
	public void setDataAt(int x, int y, int z, int i, boolean load)
	{
		int blocksViewDistance = 256;
		int sizeInBlocks = getWorldInfo().getSize().sizeInChunks * 32;
		super.setDataAt(x, y, z, i, load);
		PacketVoxelUpdate packet = new PacketVoxelUpdate(false);
		packet.x = x;
		packet.y = y;
		packet.z = z;
		packet.data = i;
		for (ServerClient client : Server.getInstance().handler.clients)
		{
			if (client.isAuthentificated())
			{
				Entity clientEntity = client.getProfile().getControlledEntity();
				if (clientEntity == null)
					continue;
				Location loc = clientEntity.getLocation();
				int plocx = (int) loc.x;
				int plocy = (int) loc.y;
				int plocz = (int) loc.z;
				//TODO use proper configurable values for this
				if (!((LoopingMathHelper.moduloDistance(x, plocx, sizeInBlocks) > blocksViewDistance + 2) || (LoopingMathHelper.moduloDistance(z, plocz, sizeInBlocks) > blocksViewDistance + 2) || (y - plocy) > 4 * 32))
				{
					client.sendPacket(packet);
				}
			}
		}
	}
}
