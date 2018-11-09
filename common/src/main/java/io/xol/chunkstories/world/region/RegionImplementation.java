//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.region;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.util.CompoundIterator;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.util.concurrency.SafeWriteLock;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTask;
import io.xol.chunkstories.world.region.format.CSFRegionFile;

public class RegionImplementation implements Region {
	public final WorldImplementation world;
	public final int regionX, regionY, regionZ;
	public final long uuid;

	protected Collection<CubicChunk> loadedChunks = ConcurrentHashMap.newKeySet();

	/**
	 * Contains the users that registered to this region using world.aquireRegion or
	 * region.registerUser
	 */
	public final Set<WorldUser> users = new HashSet<>();

	/**
	 * Keeps track of the number of users of this region. Warning: this includes
	 * users of not only the region itself, but ALSO the chunks that make up this
	 * region. An user registered in 3 chunks for instance, will be counted three
	 * times. When this counter reaches zero, the region is unloaded.
	 */
	public int usersCount = 0;

	/**
	 * Lock shared with the chunk holders to ensure consistency for the above fields
	 */
	public final Lock usersLock = new ReentrantLock();

	// Only relevant on Master worlds
	// public final CSFRegionFile handler;
	public final File file;
	public CSFRegionFile handler;

	private AtomicLong unloadCooldown = new AtomicLong();
	private boolean unloadedFlag = false;

	// TODO find a clean way to let IOTaks fiddle with this
	public SafeWriteLock chunksArrayLock = new SafeWriteLock();

	// Holds 8x8x8 CubicChunks
	private ChunkHolderImplementation[] chunkHolders;
	private AtomicBoolean isDiskDataLoaded = new AtomicBoolean(false);

	private static Random random = new Random();

	public RegionImplementation(WorldImplementation world, int regionX, int regionY, int regionZ) {
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;

		if (regionX < 0 || regionY < 0 || regionZ < 0)
			throw new RuntimeException("Regions aren't allowed negative coordinates.");

		// Initialize slots
		chunkHolders = new ChunkHolderImplementation[512];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				for (int k = 0; k < 8; k++)
					chunkHolders[i * 64 + j * 8 + k] = new ChunkHolderImplementation(this, loadedChunks,
							regionX * 8 + i, regionY * 8 + j, regionZ * 8 + k);

		// Unique UUID
		uuid = random.nextLong();

		// Set the initial cooldown delay
		unloadCooldown.set(System.currentTimeMillis());

		// Only the WorldMaster has a concept of files
		if (world instanceof WorldMaster) {
			file = new File(world.getFolderPath() + "/regions/" + regionX + "." + regionY + "." + regionZ + ".csf");
			// CSFRegionFile.determineVersionAndCreate(this);
			world.getIoHandler().requestRegionLoad(this);
		} else {
			file = null;
			isDiskDataLoaded.set(true);
		}
	}

	@Override
	public IterableIterator<WorldUser> getUsers() {
		return new IterableIterator<WorldUser>() {
			Iterator<WorldUser> i = users.iterator();

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public WorldUser next() {
				return i.next();
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user) {
		try {
			usersLock.lock();
			boolean notRedundant = users.add(user);

			if (notRedundant)
				usersCount++;

			return notRedundant;
		} finally {
			usersLock.unlock();
		}
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user) {
		try {
			usersLock.lock();
			if (users.remove(user))
				usersCount--;

			if (usersCount == 0) {
				internalUnload();
				return true;
			}

			return false;
		} finally {
			usersLock.unlock();
		}

	}

	public int countUsers() {
		return usersCount;
	}

	public CompressedData getCompressedData(int chunkX, int chunkY, int chunkZ) {
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].getCompressedData();
	}

	@Override
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ) {
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].getChunk();
	}

	@Override
	public ChunkHolderImplementation getChunkHolder(int chunkX, int chunkY, int chunkZ) {
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)];
	}

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ) {
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].isChunkLoaded();
	}

	public ChunksIterator iterator() {
		return new ChunksIterator() {

			Iterator<CubicChunk> i = loadedChunks.iterator();

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public Chunk next() {
				return i.next();
			}

		};
	}

	@Override
	public boolean isDiskDataLoaded() {
		return isDiskDataLoaded.get();
	}

	public boolean isUnloaded() {
		return unloadedFlag;
	}

	public void setDiskDataLoaded(boolean b) {
		isDiskDataLoaded.set(b);
	}

	public void internalUnload() {
		if (/* should save */ world instanceof WorldMaster) {
			this.save();
		}

		// Before unloading the holder we want to make sure we finish all saving
		// operations
		// if (handler != null)
		// handler.finishSavingOperations();

		// Set unloaded flag to true so we are not using again an unloaded holder
		unloadedFlag = true;

		// Remove the reference in the world to this
		this.getWorld().getRegionsHolder().removeRegion(this);
	}

	@Override
	public IOTask save() {
		return world.getIoHandler().requestRegionSave(this);
	}

	/*
	 * @Override public IOTask unloadAndSave() { unload(); return
	 * world.ioHandler.requestRegionSave(this); }
	 */

	@Override
	public String toString() {
		return "[Region rx:" + regionX + " ry:" + regionY + " rz:" + regionZ + " uuid: " + uuid + "loaded?:"
				+ isDiskDataLoaded.get() + " u:" + unloadedFlag + " chunks: " + "NULL" + " entities:" + "tbf" + "]";
	}

	public void compressAll() {
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					chunkHolders[a * 64 + b * 8 + c].compressChunkData();
	}

	public void compressChangedChunks() {
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++) {
					if (chunkHolders[a * 64 + b * 8 + c].getChunk() != null) {
						CubicChunk chunk = chunkHolders[a * 64 + b * 8 + c].getChunk();

						if (chunk.compr_uncomittedBlockModifications.get() > 0)
							// if (chunk.lastModification.getVoxelComponent() > chunk.lastModificationSaved.getVoxelComponent())
							chunkHolders[a * 64 + b * 8 + c].compressChunkData();
					}
				}
	}

	@Override
	public int getNumberOfLoadedChunks() {
		int count = 0;

		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					if (chunkHolders[a * 64 + b * 8 + c].isChunkLoaded())
						count++;

		return count;
	}

	@Override
	public int getRegionX() {
		return regionX;
	}

	@Override
	public int getRegionY() {
		return regionY;
	}

	@Override
	public int getRegionZ() {
		return regionZ;
	}

	@Override
	public IterableIterator<Entity> getEntitiesWithinRegion() {
		List<Iterator<Entity>> listOfIterators = new ArrayList<Iterator<Entity>>();
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++) {
					Chunk chunk = this.getChunk(a, b, c);
					if (chunk != null)
						listOfIterators.add(chunk.getEntitiesWithinChunk());
				}

		return new CompoundIterator<Entity>(listOfIterators);
	}

	public void resetUnloadCooldown() {
		unloadCooldown.set(System.currentTimeMillis());
	}

	public boolean canBeUnloaded() {
		// Don't unload it until it has been loaded for 10s
		return this.isDiskDataLoaded() && (System.currentTimeMillis() - this.unloadCooldown.get() > 10 * 1000L);
	}

	public WorldImplementation getWorld() {
		return world;
	}

	/**
	 * Unloads unused chunks, returns true if all chunks were unloaded
	 */
	/*
	 * public boolean unloadsUnusedChunks() { int loadedChunks = 0;
	 * 
	 * for (int a = 0; a < 8; a++) for (int b = 0; b < 8; b++) for (int c = 0; c <
	 * 8; c++) { chunkHolders[a * 64 + b * 8 + c].unloadsIfUnused();
	 * if(chunkHolders[a * 64 + b * 8 + c].isChunkLoaded()) loadedChunks++; }
	 * 
	 * return loadedChunks == 0; }
	 */

	// @Override
	/**
	 * Returns true if no one uses the region or one of it's chunk holders
	 */
	/*
	 * public boolean isUnused() { int usedChunks = 0;
	 * 
	 * for (int a = 0; a < 8; a++) for (int b = 0; b < 8; b++) for (int c = 0; c <
	 * 8; c++) { chunkHolders[a * 64 + b * 8 + c].unloadsIfUnused();
	 * if(chunkHolders[a * 64 + b * 8 + c].isChunkLoaded() || chunkHolders[a * 64 +
	 * b * 8 + c].countUsers() > 0) usedChunks++; }
	 * 
	 * //if(this.regionY == 0) // System.out.println(usedChunks + " vs " +
	 * this.countUsers());
	 * 
	 * return usedChunks == 0 && this.countUsers() == 0; }
	 */
}
