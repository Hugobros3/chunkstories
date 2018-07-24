//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer.SummariesTexturesHolder;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.renderer.opengl.texture.ArrayTextureGL;
import io.xol.chunkstories.voxel.VoxelTextureAtlased;
import io.xol.chunkstories.world.cell.ScratchCell;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;

public class HeightmapArrayTexture implements SummariesTexturesHolder {
	final ArrayTextureGL heights;
	final ArrayTextureGL topVoxels;
	final ClientInterface client;
	final World world;

	public HeightmapArrayTexture(ClientInterface client, World world) {
		this.client = client;
		this.world = world;

		this.heights = new ArrayTextureGL(TextureFormat.RED_16I, 256, 9 * 9);
		this.topVoxels = new ArrayTextureGL(TextureFormat.RED_16I, 256, 9 * 9);
	}

	int lastRegionX = -1;
	int lastRegionZ = -1;

	ArrayTextureSlot arrayTextureContents[] = new ArrayTextureSlot[9 * 9];
	int arrayTextureReference[][] = new int[9][9];

	ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	// ReadLock readLock = lock.readLock();

	// ConcurrentLinkedDeque queue;
	AtomicInteger pending = new AtomicInteger(0);
	// AtomicBoolean redo = new AtomicBoolean();

	public void update() {
		Player player = client.getPlayer();

		Location playerPosition = player.getLocation();
		if (playerPosition == null)
			return; // We won't do shit with that going on

		World world = playerPosition.getWorld();

		int chunkX = (int) Math.floor(playerPosition.x / 32.0);
		int chunkZ = (int) Math.floor(playerPosition.z / 32.0);

		int regionX = chunkX / 8;
		int regionZ = chunkZ / 8;

		int todo = pending.get();

		// Remap the array
		try {
			lock.writeLock().lock();

			if (lastRegionX != regionX || lastRegionZ != regionZ || todo > 0) {
				// We may need this
				ByteBuffer bb = MemoryUtil.memAlloc(4 * 256 * 256);
				bb.order(ByteOrder.LITTLE_ENDIAN);

				// Clear unused slots
				for (int i = 0; i < 81; i++) {
					ArrayTextureSlot slot = arrayTextureContents[i];
					if (slot == null)
						continue;

					// Frees slots immediately once out of the area we care about
					if (Math.abs(slot.regionX - regionX) >= 5 || Math.abs(slot.regionZ - regionZ) >= 5)
						arrayTextureContents[i] = null;
				}

				for (int i = -4; i <= 4; i++)
					for (int j = -4; j <= 4; j++) {
						int regionI = regionX + i;
						int regionJ = regionZ + j;

						// Wrap arround the world!
						if (regionI < 0)
							regionI += world.getSizeInChunks() / 256;
						if (regionJ < 0)
							regionJ += world.getSizeInChunks() / 256;

						// Look for a slot already containing our wanted textures
						int free = -1;
						int good = -1;
						for (int k = 0; k < 81; k++) {
							ArrayTextureSlot slot = arrayTextureContents[k];

							if (slot == null) {
								if (free == -1)
									free = k;
							} else {
								if (slot.regionX == regionI && slot.regionZ == regionJ) {
									good = k;
									break;
								}
							}
						}

						int slot;
						// If no good slot was found :(
						if (good == -1) {
							arrayTextureContents[free] = new ArrayTextureSlot();
							arrayTextureContents[free].regionX = regionI;
							arrayTextureContents[free].regionZ = regionJ;

							slot = free;
						} else {
							slot = good;
						}

						// If data is not yet in the slot, check if the world has data for it
						if (!arrayTextureContents[slot].hasData) {
							Heightmap sum = world.getRegionsSummariesHolder().getHeightmap(
									arrayTextureContents[slot].regionX, arrayTextureContents[slot].regionZ);
							if (sum != null && sum.isLoaded()) {

								loadHeights((HeightmapImplementation) sum, bb, 0);
								heights.uploadTextureData(slot, 0, bb);
								// heights.computeMipmaps();

								for (int lod = 1; lod <= 8; lod++) {
									loadHeights((HeightmapImplementation) sum, bb, lod);
									heights.uploadTextureData(slot, lod, bb);
								}
								heights.setMipMapping(true);
								heights.setMipmapLevelsRange(0, 8);

								loadTopVoxels((HeightmapImplementation) sum, bb, 0);
								topVoxels.uploadTextureData(slot, 0, bb);

								for (int lod = 1; lod <= 8; lod++) {
									loadTopVoxels((HeightmapImplementation) sum, bb, lod);
									topVoxels.uploadTextureData(slot, lod, bb);
								}
								topVoxels.setMipMapping(true);
								topVoxels.setMipmapLevelsRange(0, 8);

								arrayTextureContents[slot].hasData = true;
							}
						}

						arrayTextureReference[i + 4][j + 4] = slot;
					}

				MemoryUtil.memFree(bb);

				lastRegionX = regionX;
				lastRegionZ = regionZ;

			}
		} finally {
			lock.writeLock().unlock();
			pending.addAndGet(-todo);
		}
	}

	int size[] = { 256, 128, 64, 32, 16, 8, 4, 2, 1 };

	private void loadHeights(HeightmapImplementation sum, ByteBuffer bb, int lod) {
		bb.clear();
		int heights[] = sum.getHeightData();
		for (int i = 0; i < size[lod] * size[lod]; i++) {
			int j = HeightmapImplementation.mainMimpmapOffsets[lod] + i;
			bb.putInt(heights[j]);
		}
		bb.flip();
	}

	private void loadTopVoxels(HeightmapImplementation sum, ByteBuffer bb, int lod) {
		bb.clear();
		int data[] = sum.getVoxelData();
		ScratchCell cell = new ScratchCell(world);
		cell.sunlight = 15;
		for (int i = 0; i < size[lod] * size[lod]; i++) {
			int j = HeightmapImplementation.mainMimpmapOffsets[lod] + i;

			int raw_data = data[j];
			Voxel v = world.getContentTranslator().getVoxelForId(VoxelFormat.id(raw_data));

			cell.voxel = v;
			if (v.getDefinition().isLiquid())
				bb.putInt(512);
			else
				bb.putInt(((VoxelTextureAtlased) v.getVoxelTexture(VoxelSide.TOP, cell)).positionInColorIndex);
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
	}

	@Override
	public int getSummaryIndex(int regionX, int regionZ) {
		// Check inboundness
		if (Math.abs(regionX - lastRegionX) >= 5 || Math.abs(regionZ - lastRegionZ) >= 5)
			return -1;

		// Index within the 9x9 zone centered arround the player
		int i = regionX - lastRegionX;
		int j = regionZ - lastRegionZ;

		int arraySlotIndex = arrayTextureReference[i + 4][j + 4];

		// The zone arround the player hasn't had yet time to initialize to anything!
		if (arraySlotIndex == -1)
			return -1;

		// This can't be null in theory
		ArrayTextureSlot slot = arrayTextureContents[arraySlotIndex];

		// The data hasn't arrived yet
		if (!slot.hasData)
			return -1;

		return arraySlotIndex;
	}

	@Override
	public void warnDataHasArrived(int regionX, int regionZ) {

		// System.out.println("data_arrived: "+Math.abs(regionX - lastRegionX) + ":" +
		// Math.abs(regionZ - lastRegionZ));
		if (Math.abs(regionX - lastRegionX) >= 5 || Math.abs(regionZ - lastRegionZ) >= 5)
			return;

		lock.writeLock().lock();
		int index = getSummaryIndex(regionX, regionZ);
		if (index != -1) {
			arrayTextureContents[index].hasData = false;
			// System.out.println("has data = false");
		}
		pending.incrementAndGet();

		lock.writeLock().unlock();
	}

	@Override
	public ArrayTexture getHeightsArrayTexture() {
		return heights;
	}

	@Override
	public ArrayTexture getTopVoxelsArrayTexture() {
		return topVoxels;
	}
}
