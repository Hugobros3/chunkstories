package io.xol.chunkstories.server.player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.net.packets.PacketWorldUser;
import io.xol.chunkstories.api.net.packets.PacketWorldUser.Type;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.region.RegionImplementation;

/** 
 * Receives the requests from the remote player & decides wether to accept them or not
 * Remembers which chunks and region summaries the player keeps in memory
 * Frees them once he disconnects
 */
public class RemotePlayerLoadingAgent {
	final ServerPlayer player;
	
	public RemotePlayerLoadingAgent(ServerPlayer player) {
		this.player = player;
	}

	Set<Integer> usedChunksHandles = new HashSet<Integer>();
	Set<Integer> usedRegionHandles = new HashSet<Integer>();
	
	//Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();
	
	Lock lock = new ReentrantLock();
	boolean destroyed = false;

	private int chunkHandle(int chunkX, int chunkY, int chunkZ) {
		WorldInfo.WorldSize size = player.getWorld().getWorldInfo().getSize();
		
		int filteredChunkX = chunkX & (size.maskForChunksCoordinates);
		int filteredChunkY = Math2.clampi(chunkY, 0, 31); //TODO don't assume 1024 height
		int filteredChunkZ = chunkZ & (size.maskForChunksCoordinates);
		
		return ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
	}
	
	private int[] chunk(int handle) {
		WorldInfo.WorldSize size = player.getWorld().getWorldInfo().getSize();
		
		int chunkX = (handle >> (size.bitlengthOfHorizontalChunksCoordinates + size.bitlengthOfVerticalChunksCoordinates)) & (size.maskForChunksCoordinates);
		int chunkY = ((handle >> size.bitlengthOfHorizontalChunksCoordinates) % 32); //TODO don't assume 1024 height
		int chunkZ = (handle) & (size.maskForChunksCoordinates);
		
		return new int[] {chunkX, chunkY, chunkZ};
	}
	
	private int summaryHandle(int regionX, int regionZ) {
		return regionX * (player.getWorld().getWorldInfo().getSize().sizeInChunks / 8) + regionZ;
	}
	
	private int[] summary(int handle) {
		int regionX = handle / (player.getWorld().getWorldInfo().getSize().sizeInChunks / 8);
		int regionZ = handle % (player.getWorld().getWorldInfo().getSize().sizeInChunks / 8);
		
		return new int[] {regionX, regionZ};
	}
	
	public void handleClientRequest(PacketWorldUser packet) {
		try {
			lock.lock();
			
			if(packet.getType() == Type.REGISTER_CHUNK) {
				int handle = chunkHandle(packet.getX(), packet.getY(), packet.getZ());
				if(usedChunksHandles.add(handle)) {
					ChunkHolder holder = player.getWorld().aquireChunkHolder(player, packet.getX(), packet.getY(), packet.getZ());
					assert holder != null; // assume it not being null because it's the supposed behaviour
				}
				else {
					System.out.println("Received twin request for chunk handle " + handle);
				}
			}
			else if(packet.getType() == Type.UNREGISTER_CHUNK) {
				int handle = chunkHandle(packet.getX(), packet.getY(), packet.getZ());
				
				//If we actually owned this handle
				if(usedChunksHandles.remove(handle)) {
					ChunkHolder holder = player.getWorld().getRegionChunkCoordinates(packet.getX(), packet.getY(), packet.getZ()).getChunkHolder(packet.getX(), packet.getY(), packet.getZ());
					assert holder != null; // We can assert the chunk holder exists because at this point it MUST be held by this very loading agent !
					holder.unregisterUser(player);
				} else {
					System.out.println("Client requested to unregister something he never registered in the first place.");
				}
			}
			else if(packet.getType() == Type.REGISTER_SUMMARY) {
				int handle = summaryHandle(packet.getX(), packet.getZ());
				
				int[] check = summary(handle);
				if(check[0] != packet.getX() || check[1] != packet.getZ()) {
					System.out.println("major fuck up with handle "+ handle);
					System.out.println("should have been ("+packet.getX()+", "+packet.getZ()+")");
					System.out.println("kys");
					System.exit(-1);
				}
				
				if(usedRegionHandles.add(handle)) {
					RegionSummary regionSummary = player.getWorld().getRegionsSummariesHolder().aquireRegionSummary(player, packet.getX(), packet.getZ());
					assert regionSummary != null; // assume it not being null because it's the supposed behaviour
				}
				else {
					System.out.println("Received twin request for region summary ("+packet.getX()+", "+packet.getZ()+")");
				}
			}
			else if(packet.getType() == Type.UNREGISTER_SUMMARY) {
				int handle = summaryHandle(packet.getX(), packet.getZ());
				
				//If we actually owned this handle
				if(usedRegionHandles.remove(handle)) {
					RegionSummary regionSummary = player.getWorld().getRegionsSummariesHolder().getRegionSummary(packet.getX(), packet.getZ());
					assert regionSummary != null; // We can assert the region summary exists because at this point it MUST be held by this very loading agent !
					regionSummary.unregisterUser(player);
				} else {
					System.out.println("Client requested to unregister summary ("+packet.getX()+", "+packet.getZ()+") that he never registered in the first place.");
				}
			}
			else {
				//TODO hurt him like he hurted you
				//throw new IllegalPacketException(packet);
			}
		}
		finally {
			lock.unlock();
		}
	}
	
	public void destroy() {
		try {
			lock.lock();
			
			for(int handle : this.usedChunksHandles) {
				int[] pos = chunk(handle);
				
				RegionImplementation region = player.getWorld().getRegionChunkCoordinates(pos[0], pos[1], pos[2]);
				//assert region != null;
				if(region != null) {
					ChunkHolder holder = region.getChunkHolder(pos[0], pos[1], pos[2]);
					//assert holder != null; // We can assert the chunk holder exists because at this point it MUST be held by this very loading agent !
					if(holder != null) {
						holder.unregisterUser(player);
						continue;
					}
				}
				
				player.getContext().logger().error("Error while disconnecting player: "+player+", chunkholder at [" + pos[0] + ":" + pos[1] + ":" + pos[2] +
						"] wasn't loaded even thought it was part of that player's subscriptions list");
			}
			this.usedChunksHandles.clear();

			for(int handle : this.usedRegionHandles) {
				int pos[] = summary(handle);
				RegionSummary regionSummary = player.getWorld().getRegionsSummariesHolder().getRegionSummary(pos[0], pos[1]);
				//assert regionSummary != null; // We can assert the region summary exists because at this point it MUST be held by this very loading agent !
				if(regionSummary != null)
					regionSummary.unregisterUser(player);
				else {
					player.getContext().logger().error("Error while disconnecting player: "+player+", region at [" + pos[0] + " :" + pos[1]+
							"] wasn't loaded even thought it was part of that player's subscriptions list");
				}
			}
			this.usedRegionHandles.clear();
			
			destroyed = true;
		}
		catch(Exception e) {
			player.getContext().logger().error("Error while disconnecting player: "+player+", exception thrown while freeing his held world data.");
			player.getContext().logger().error(e.getMessage());
		}
		finally {
			lock.unlock();
		}
	}
}
