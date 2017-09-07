package io.xol.chunkstories.renderer.terrain;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.WorldRenderer.SummariesTexturesHolder;
import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.voxel.VoxelTextureAtlased;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.graphics.textures.ArrayTextureGL;

public class SummariesArrayTexture implements SummariesTexturesHolder {
	final ArrayTextureGL heights;
	final ArrayTextureGL topVoxels;
	final ClientInterface client;
	
	public SummariesArrayTexture(ClientInterface client) {
		this.client = client;
		
		this.heights = new ArrayTextureGL(TextureFormat.RED_16I, 256, 9 * 9);
		this.topVoxels = new ArrayTextureGL(TextureFormat.RED_16I, 256, 9 * 9);
	}
	
	int lastRegionX = -1;
	int lastRegionZ = -1;
	
	ArrayTextureSlot arrayTextureContents[] = new ArrayTextureSlot[9 * 9]; 
	int arrayTextureReference[][] = new int[9][9];
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	ReadLock readLock = lock.readLock();
	
	public void update() {
		Player player = client.getPlayer();
		
		Location playerPosition = player.getLocation();
		if(playerPosition == null)
			return; //We won't do shit with that going on
		
		World world = playerPosition.getWorld();
		
		int chunkX = (int) Math.floor(playerPosition.x / 32.0);
		int chunkZ = (int) Math.floor(playerPosition.z / 32.0);
		
		int regionX = chunkX / 8;
		int regionZ = chunkZ / 8;
		
		//Remap the array
		if(lastRegionX != regionX || lastRegionZ != regionZ || redo.compareAndSet(true, false)) {
			WriteLock writeLock = lock.writeLock();
			writeLock.lock();
			//We may need this
			ByteBuffer bb = MemoryUtil.memAlloc(4 * 256 * 256);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			
			//Clear unused slots
			for(int i = 0; i < 81; i++) {
				ArrayTextureSlot slot = arrayTextureContents[i];
				if(slot == null)
					continue;
				
				//Frees slots immediately once out of the area we care about
				if(Math.abs(slot.regionX - regionX) >= 5 || Math.abs(slot.regionZ - regionZ) >= 5)
					arrayTextureContents[i] = null;
			}
			
			for(int i = -4; i <= 4; i++)
				for(int j = -4; j <= 4; j++) {
					int regionI = regionX + i;
					int regionJ = regionZ + j;
					
					//Wrap arround the world!
					if(regionI < 0) regionI += world.getSizeInChunks() / 256;
					if(regionJ < 0) regionJ += world.getSizeInChunks() / 256;
					
					//Look for a slot already containing our wanted textures
					int free = -1;
					int good = -1;
					for(int k = 0; k < 81; k++) {
						ArrayTextureSlot slot = arrayTextureContents[k];
						
						if(slot == null) {
							if(free == -1) free = k;
						}
						else {
							if(slot.regionX == regionI && slot.regionZ == regionJ) {
								good = k;
								break;
							}
						}
					}
					
					int slot;
					//If no good slot was found :(
					if(good == -1) {
						arrayTextureContents[free] = new ArrayTextureSlot();
						arrayTextureContents[free].regionX = regionI;
						arrayTextureContents[free].regionZ = regionJ;
						
						slot = free;
					}
					else {
						slot = good;
					}
					
					//If data is not yet in the slot, check if the world has data for it
					if(!arrayTextureContents[slot].hasData) {
						RegionSummary sum = world.getRegionsSummariesHolder().getRegionSummary(arrayTextureContents[slot].regionX, arrayTextureContents[slot].regionZ);
						if(sum != null && sum.isLoaded()) {
							
							loadHeights((RegionSummaryImplementation)sum, bb);
							/*glBindTexture(GL_TEXTURE_2D_ARRAY, heightsArrayTextureId);
							glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, slot, 256, 256, 1, GL_R16UI, GL_INT, bb);*/
							heights.uploadTextureData(slot, 0, bb);

							loadTopVoxels((RegionSummaryImplementation)sum, bb);
							/*glBindTexture(GL_TEXTURE_2D_ARRAY, topVoxelsArrayTextureId);
							glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, slot, 256, 256, 1, GL_R16UI, GL_INT, bb);*/
							topVoxels.uploadTextureData(slot, 0, bb);
							
							arrayTextureContents[slot].hasData = true;
						}
					}
					
					arrayTextureReference[i + 4][j + 4] = slot;
				}
			
			MemoryUtil.memFree(bb);
			
			lastRegionX = regionX;
			lastRegionZ = regionZ;
			
			writeLock.unlock();
		}
	}
	
	private void loadHeights(RegionSummaryImplementation sum, ByteBuffer bb) {
		int heights[] = sum.getHeightData();
		for (int i = 0; i < 256 * 256; i++)
		{
			bb.putInt(heights[i]);
		}
		bb.flip();
	}
	
	private void loadTopVoxels(RegionSummaryImplementation sum, ByteBuffer bb) {
		int ids[] = sum.getVoxelData();
		for (int i = 0; i < 256 * 256; i++)
		{
			int id = ids[i];
			Voxel v = VoxelsStore.get().getVoxelById(id);
			if (v.getType().isLiquid())
				bb.putInt(512);
			else
				bb.putInt(((VoxelTextureAtlased)v.getVoxelTexture(id, VoxelSides.TOP, null)).positionInColorIndex);
		}
		bb.flip();
	}

	class ArrayTextureSlot {
		int regionX;
		int regionZ;
		
		boolean hasData = false;
	}
	
	public void destroy() {
		heights.destroy();
		topVoxels.destroy();
		/*glDeleteTextures(heightsArrayTextureId);
		glDeleteTextures(topVoxelsArrayTextureId);*/
	}

	@Override
	public int getSummaryIndex(int regionX, int regionZ) {
		//Check inboundness
		if(Math.abs(regionX - lastRegionX) >= 5 || Math.abs(regionZ - lastRegionZ) >= 5)
			return -1;
		
		//Index within the 9x9 zone centered arround the player
		int i = regionX - lastRegionX;
		int j = regionZ - lastRegionZ;
		
		int arraySlotIndex = arrayTextureReference[i + 4][j + 4];
		
		//The zone arround the player hasn't had yet time to initialize to anything!
		if(arraySlotIndex == -1)
			return -1;
		
		//This can't be null in theory
		ArrayTextureSlot slot = arrayTextureContents[arraySlotIndex];
		
		//The data hasn't arrived yet
		if(!slot.hasData)
			return -1;
		
		return arraySlotIndex;
	}

	//ConcurrentLinkedDeque queue;
	AtomicBoolean redo = new AtomicBoolean();
	
	@Override
	public void warnDataHasArrived(int regionX, int regionZ) {
		//readLock.lock();
		
		if(Math.abs(regionX - lastRegionX) >= 5 || Math.abs(regionZ - lastRegionZ) >= 5)
			return;
		
		redo.set(true);
		
		//readLock.unlock();
	}

	@Override
	public ArrayTexture getHeightsArrayTexture() {
		// TODO Auto-generated method stub
		return heights;
	}

	@Override
	public ArrayTexture getTopVoxelsArrayTexture() {
		// TODO Auto-generated method stub
		return topVoxels;
	}
}
