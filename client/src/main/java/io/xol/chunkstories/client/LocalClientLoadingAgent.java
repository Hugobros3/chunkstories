package io.xol.chunkstories.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.net.packets.PacketWorldUser;
import io.xol.chunkstories.api.net.packets.PacketWorldUser.Type;
import io.xol.chunkstories.api.player.LocalPlayer;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LocalClientLoadingAgent {
	final ClientInterface client;
	final LocalPlayer player;
	final WorldClient world;
	
	Set<Integer> fastChunksMask = new HashSet<Integer>();
	Set<ChunkHolder> usedChunks = new HashSet<ChunkHolder>();
	Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();
	
	Lock lock = new ReentrantLock();
	
	public LocalClientLoadingAgent(ClientInterface client, LocalPlayer player, WorldClient world) {
		this.client = client;
		this.player = player;
		this.world = world;
	}
	
	public void updateUsedWorldBits()
	{
		Entity controlledEntity = player.getControlledEntity();
		
		if(controlledEntity == null)
			return;
		
		try {
			lock.lock();
			
			//Subscribe to nearby wanted chunks
			int cameraChunkX = Math2.floor((controlledEntity.getLocation().x()) / 32);
			int cameraChunkY = Math2.floor((controlledEntity.getLocation().y()) / 32);
			int cameraChunkZ = Math2.floor((controlledEntity.getLocation().z()) / 32);
			int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);
			
			for (int chunkX = (cameraChunkX - chunksViewDistance - 1); chunkX <= cameraChunkX + chunksViewDistance + 1; chunkX++)
			{
				for (int chunkZ = (cameraChunkZ - chunksViewDistance - 1); chunkZ <= cameraChunkZ + chunksViewDistance + 1; chunkZ++)
					for (int chunkY = cameraChunkY - 3; chunkY <= cameraChunkY + 3; chunkY++)
					{
						WorldInfo worldInfo = world.getWorldInfo();
						WorldInfo.WorldSize size = worldInfo.getSize();
						
						int filteredChunkX = chunkX & (size.maskForChunksCoordinates);
						int filteredChunkY = Math2.clampi(chunkY, 0, 31);
						int filteredChunkZ = chunkZ & (size.maskForChunksCoordinates);
						
						int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
						
						if(fastChunksMask.contains(summed))
							continue;
						
						ChunkHolder holder = world.aquireChunkHolder(player, chunkX, chunkY, chunkZ);
						
						assert holder != null;
						
						if(holder == null)
							continue;
						
						usedChunks.add(holder);
						fastChunksMask.add(summed);
						
						if(world instanceof WorldClientRemote) {
							WorldClientRemote remote = (WorldClientRemote)world;
							remote.getConnection().pushPacket(PacketWorldUser.registerChunkPacket(filteredChunkX, filteredChunkY, filteredChunkZ));
						}
					}
			}
			
			//Unsubscribe for far ones
			Iterator<ChunkHolder> i = usedChunks.iterator();
			while(i.hasNext())
			{
				ChunkHolder holder = i.next();
				if (		(LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateX(), cameraChunkX, world.getSizeInChunks()) > chunksViewDistance + 1) 
						|| 	(LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateZ(), cameraChunkZ, world.getSizeInChunks()) > chunksViewDistance + 1)
						|| 	(Math.abs(							holder.getChunkCoordinateY() - cameraChunkY) > 4))
				{
					WorldInfo worldInfo = world.getWorldInfo();
					WorldInfo.WorldSize size = worldInfo.getSize();
					
					int filteredChunkX = holder.getChunkCoordinateX() & (size.maskForChunksCoordinates);
					int filteredChunkY = Math2.clampi(holder.getChunkCoordinateY(), 0, 31);
					int filteredChunkZ = holder.getChunkCoordinateZ() & (size.maskForChunksCoordinates);
					
					int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
	
					fastChunksMask.remove(summed);
					
					i.remove();
					holder.unregisterUser(player);
					
					if(world instanceof WorldClientRemote) {
						WorldClientRemote remote = (WorldClientRemote)world;
						remote.getConnection().pushPacket(PacketWorldUser.unregisterChunkPacket(filteredChunkX, filteredChunkY, filteredChunkZ));
					}
				}
			}
	
			//We load the region summaries we fancy
			int summaryDistance = 32;
			for (int chunkX = (cameraChunkX - summaryDistance); chunkX < cameraChunkX + summaryDistance; chunkX++)
				for (int chunkZ = (cameraChunkZ - summaryDistance); chunkZ < cameraChunkZ + summaryDistance; chunkZ++)
				{
					if(chunkX % 8 == 0 && chunkZ % 8 == 0)
					{
						int regionX = chunkX / 8;
						int regionZ = chunkZ / 8;
						
						//TODO bad to aquire each time!!!
						RegionSummary regionSummary = world.getRegionsSummariesHolder().aquireRegionSummary(player, regionX, regionZ);
					
						if(regionSummary != null) {
							if(usedRegionSummaries.add(regionSummary)) {
								
								if(world instanceof WorldClientRemote) {
									WorldClientRemote remote = (WorldClientRemote)world;
									
									remote.getConnection().pushPacket(PacketWorldUser.registerSummary(regionX, regionZ));
								}
							}
						}
					}
				}
			
			int cameraRegionX = cameraChunkX / 8;
			int cameraRegionZ = cameraChunkZ / 8;
	
			int distInRegions = summaryDistance / 8;
			int sizeInRegions = world.getSizeInChunks() / 8;
			
			//And we unload the ones we no longer need
			Iterator<RegionSummary> iterator = usedRegionSummaries.iterator();
			while (iterator.hasNext())
			{
				RegionSummary entry = iterator.next();
				int regionX = entry.getRegionX();
				int regionZ = entry.getRegionZ();
	
				int dx = LoopingMathHelper.moduloDistance(cameraRegionX, regionX, sizeInRegions);
				int dz = LoopingMathHelper.moduloDistance(cameraRegionZ, regionZ, sizeInRegions);
				if (dx > distInRegions || dz > distInRegions)
				{
					entry.unregisterUser(player);
					iterator.remove();
					
					if(world instanceof WorldClientRemote) {
						WorldClientRemote remote = (WorldClientRemote)world;
						remote.getConnection().pushPacket(PacketWorldUser.unregisterSummary(regionX, regionZ));
					}
				}
			}
			
		}
		finally {
			lock.unlock();
		}
	}
	
	public void handleServerResponse(PacketWorldUser packet) throws IllegalPacketException {
		
		try {
			lock.lock();
			//The server refused to register us to this chunk. We gracefully accept.
			if(packet.getType() == Type.UNREGISTER_CHUNK) {
				ChunkHolder holder = world.getRegionChunkCoordinates(packet.getX(), packet.getY(), packet.getZ()).getChunkHolder(packet.getX(), packet.getY(), packet.getZ());
				
				//Apparently we already figured we didn't need this anyway
				if(holder == null)
					return;
				
				WorldInfo worldInfo = world.getWorldInfo();
				WorldInfo.WorldSize size = worldInfo.getSize();
				
				int filteredChunkX = holder.getChunkCoordinateX() & (size.maskForChunksCoordinates);
				int filteredChunkY = Math2.clampi(holder.getChunkCoordinateY(), 0, 31);
				int filteredChunkZ = holder.getChunkCoordinateZ() & (size.maskForChunksCoordinates);
				
				int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;

				//We remove it from our list
				fastChunksMask.remove(summed);
				usedChunks.remove(holder);
				
				//And we unsub.
				holder.unregisterUser(player);
						
			//This is the same but for region summaries
			} else if(packet.getType() == Type.UNREGISTER_SUMMARY) {
				RegionSummary regionSummary = world.getRegionsSummariesHolder().getRegionSummary(packet.getX(), packet.getZ());
				
				if(regionSummary == null)
					return;

				usedRegionSummaries.remove(regionSummary);
				regionSummary.unregisterUser(player);
				
			} else
				//We only expect UNREGISTER packets from the server !
				throw new IllegalPacketException(packet);
		}
		finally{
			lock.unlock();
		}
	}
}
