//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitDontSave;
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable;
import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.entity.EntitySerializer;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.util.concurrency.SafeWriteLock;
import io.xol.chunkstories.util.concurrency.TrivialFence;
import io.xol.chunkstories.voxel.components.CellComponentsHolder;
import io.xol.chunkstories.world.io.IOTask;
import io.xol.chunkstories.world.region.RegionImplementation;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkHolderImplementation implements ChunkHolder {
	// Position stuff
	private final RegionImplementation region;
	private final int x, y, z;
	private final int uuid;

	// To update the parent object's collection (used initerator)
	private final Collection<CubicChunk> regionLoadedChunks;

	protected final Set<WorldUser> users = new HashSet<>(); // Keep tracks of who needs this data loaded
	private final Set<RemotePlayer> usersWaitingForIntialData = new HashSet<RemotePlayer>();
	// protected final Lock usersLock;
	// protected final Lock usersLock = new ReentrantLock();

	// The compressed version of the chunk data
	private SafeWriteLock compressedDataLock = new SafeWriteLock();
	private CompressedData compressedData;

	/** Symbolic reference indicating there is othing worth saving in this chunk, but data was generated */
	public final static byte[] AIR_CHUNK_NO_DATA_SAVED = new byte[] {};

	public IOTask loadChunkTask;

	private ReadWriteLock chunkLock = new ReentrantReadWriteLock();
	private CubicChunk chunk;

	public final static AtomicInteger globalRegisteredUsers = new AtomicInteger(0);

	public ChunkHolderImplementation(RegionImplementation region, Collection<CubicChunk> loadedChunks, int x, int y,
			int z) {
		this.region = region;
		this.regionLoadedChunks = loadedChunks;

		this.x = x;
		this.y = y;
		this.z = z;

		uuid = ((x << region.getWorld().getWorldInfo().getSize().bitlengthOfVerticalChunksCoordinates) | y) << region.getWorld().getWorldInfo().getSize().bitlengthOfHorizontalChunksCoordinates | z;
	}

	// LZ4 compressors & decompressors stuff
	private static LZ4Factory factory = LZ4Factory.fastestInstance();

	@Override
	/** Publically exposed compressChunkData method */
	public void compressChunkData() {
		CubicChunk chunk = this.chunk;
		if (chunk == null)
			return;

		chunk.entitiesLock.lock();
		CompressedData compressedData = compressChunkData(chunk);
		chunk.entitiesLock.unlock();

		this.setCompressedData(compressedData);
	}

	/** This method is called assumming the chunk is well-locked */
	private CompressedData compressChunkData(final CubicChunk chunk) {
		final int changesTakenIntoAccount = chunk.compr_uncomittedBlockModifications.get();

		// Stage 1: Compress the actual voxel data
		byte[] voxelCompressedData;
		if (!chunk.isAirChunk()) {
			// Heuristic value for the size of the buffer: fixed voxel size + factor of
			// components & entities
			int uncompressedStuffBufferSize = 32 * 32 * 32 * 4;// + chunk.voxelComponents.size() * 1024 +
																// chunk.localEntities.size() * 2048;
			ByteBuffer uncompressedStuff = MemoryUtil.memAlloc(uncompressedStuffBufferSize);

			uncompressedStuff.asIntBuffer().put(chunk.chunkVoxelData);
			// uncompressedStuff.flip();

			ByteBuffer compressedStuff = MemoryUtil.memAlloc(uncompressedStuffBufferSize + 2048);

			LZ4Compressor compressor = factory.fastCompressor();
			compressor.compress(uncompressedStuff, compressedStuff);

			// No longer need that buffer
			MemoryUtil.memFree(uncompressedStuff);

			// Make a Java byte[] array to put the final stuff in
			voxelCompressedData = new byte[compressedStuff.position()];
			compressedStuff.flip();

			compressedStuff.get(voxelCompressedData);

			// No longer need that buffer either
			MemoryUtil.memFree(compressedStuff);
		} else {
			// Just use a symbolic null here
			voxelCompressedData = null;
		}

		// Stage 2: Take care of the voxel components

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);

		// ByteBuffer smallBuffer = MemoryUtil.memAlloc(4096);
		// byte[] smallArray = new byte[4096];

		// ByteBufferOutputStream bbos = new ByteBufferOutputStream(smallBuffer);
		ByteArrayOutputStream bbos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bbos);

		try {
			// For all cells that have components
			for (CellComponentsHolder voxelComponents : chunk.allCellComponents.values()) {

				// Write a 1 then their in-chunk index
				daos.writeByte((byte) 0x01);
				daos.writeInt(voxelComponents.getIndex());

				// For all components in this getCell
				for (Entry<String, VoxelComponent> entry : voxelComponents.getAllVoxelComponents()) {
					daos.writeUTF(entry.getKey()); // Write component name

					// Push the component in the temporary buffer
					entry.getValue().push(region.handler, dos);
					// smallBuffer.flip();

					byte[] bytesPushed = bbos.toByteArray();
					bbos.reset();

					// Write how many bytes the temporary buffer now contains
					// int bytesPushed = smallBuffer.limit();
					daos.writeShort(bytesPushed.length);

					// Get those bytes as an array then write it in the compressed stuff
					// smallBuffer.getVoxelComponent(smallArray);
					daos.write(bytesPushed, 0, bytesPushed.length);

					// Reset the temporary buffer
					// smallBuffer.clear();
				}

				daos.writeUTF("\n");
			}

			// Write the final 00, so to be clear we are done with voxel components
			daos.writeByte((byte) 0x00);

			// Since we output to a local buffer, any failure is viewed as catastrophic
		} catch (IOException e) {
			assert false;
		}

		// Extract the byte array from the baos
		byte[] voxelComponentsData = baos.toByteArray();

		// MemoryUtil.memFree(smallBuffer);

		// Stage 3: Compress entities
		baos.reset();

		for (Entity entity : chunk.localEntities) {

			// Don't save controllable entities
			if (!entity.traitLocation.wasRemoved() && !(entity.traits.has(TraitDontSave.class))) {
				EntitySerializer.writeEntityToStream(daos, region.handler, entity);
			}
		}
		EntitySerializer.writeEntityToStream(daos, region.handler, null);

		byte[] entityData = baos.toByteArray();

		// Remove whatever modifications existed when the method started, this is for
		// avoiding concurrent modifications not being taken into account
		chunk.compr_uncomittedBlockModifications.addAndGet(-changesTakenIntoAccount);

		return new CompressedData(voxelCompressedData, voxelComponentsData, entityData);
	}

	public CompressedData getCompressedData() {
		return compressedData;
	}

	/** Used by IO operations only */
	public void setCompressedData(CompressedData compressedData) {
		compressedDataLock.beginWrite();
		this.compressedData = compressedData;
		compressedDataLock.endWrite();
	}

	private void unloadChunk() {
		chunkLock.writeLock().lock();

		try {
			if (chunk == null) {
				if (loadChunkTask == null)
					logger.info("Unloading holder but there was no chunk loaded, nor a task to load one");
				//else
				//	logger.info("Unloading holder but there was no chunk loaded, but a task was in progress");
				return;
			}

			// Unlist it immediately
			regionLoadedChunks.remove(chunk);

			// Remove the entities from this chunk from the world
			region.world.getEntitiesLock().writeLock().lock();
			for (Entity entity : chunk.localEntities) {
				if (entity.traits.tryWith(TraitControllable.class, TraitControllable::getController) == null) {
					region.world.removeEntityFromList(entity);
				}  // TODO this is sloppy!
			}
			region.world.getEntitiesLock().writeLock().unlock();

			// Lock it down
			chunk.entitiesLock.lock();

			// Compress chunk one last time before it has to go
			setCompressedData(compressChunkData(chunk));

			// destroy it (returns any internal data using up ressources)
			chunk.destroy();
			CubicChunk.chunksCounter.decrementAndGet();

			// unlock it (whoever messes with it now, his problem)
			chunk.entitiesLock.unlock();

			this.chunk = null;
		} finally {
			// Kill any load chunk operation that might want to set the chunk later on
			if (loadChunkTask != null) {
				loadChunkTask.cancel();
				loadChunkTask = null;
			}

			chunkLock.writeLock().unlock();
		}
	}

	@Override
	public IterableIterator<WorldUser> getChunkUsers() {
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

			// Let remove() throw UnsupportedOperationException; we don't want to enable
			// this iterator to mutate shit
		};
	}

	@Override
	public boolean registerUser(WorldUser user) {
		try {
			region.usersLock.lock();

			if (users.add(user)) {
				region.usersCount++;
				globalRegisteredUsers.incrementAndGet();
			}

			CubicChunk chunk = this.chunk;

			// If the user registering is remote, we also need to send him the data
			if (user instanceof RemotePlayer) {
				RemotePlayer player = (RemotePlayer) user;
				if (chunk != null) {
					// Chunk already loaded ? Compress and send it immediately
					// TODO recompress chunk data each tick it's needed

					player.pushPacket(new PacketChunkCompressedData(chunk, this.getCompressedData()));
				} else {
					// Add him to the wait list else
					usersWaitingForIntialData.add(player);
				}
			}

			// This runs under a lock already, so we can spawn that task without worrying
			// too much
			if (chunk == null && loadChunkTask == null) {
				// We create a task only if one isn't already ongoing.
				loadChunkTask = getRegion().getWorld().getIoHandler().requestChunkLoad(this);
			}

			return true;
		} finally {
			region.usersLock.unlock();
		}
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk, or
	 * even region.
	 */
	public boolean unregisterUser(WorldUser user) {
		try {
			region.usersLock.lock();

			if (users.remove(user)) {
				globalRegisteredUsers.decrementAndGet();
				region.usersCount--;
			}

			if (users.isEmpty()) {
				unloadChunk(); // Unload the chunk as soon as nobody holds on to it

				if (region.usersCount == 0)
					region.internalUnload();
				return true;
			}

			return false;
		} finally {
			region.usersLock.unlock();
		}
	}

	public int countUsers() {
		return users.size();
	}

	@Override
	public CubicChunk getChunk() {
		return chunk;
	}

	@Override
	public RegionImplementation getRegion() {
		return region;
	}

	@Override
	public int getInRegionX() {
		return x & 0x7;
	}

	@Override
	public int getInRegionY() {
		return y & 0x7;
	}

	@Override
	public int getInRegionZ() {
		return z & 0x7;
	}

	static Logger logger = LoggerFactory.getLogger("world.chunkHolder");

	//TODO check the data we are receiving is from the task we wanted
	public void receiveDataAndCreate(CompressedData data) {
		this.chunkLock.writeLock().lock();

		if (this.chunk != null) {
			logger.error("Creating an already existing chunk!");
			throw new RuntimeException("Boo !");
			//System.out.println("Warning: creating a chunk but the chunkholder already had one, ignoring");
			//this.chunkLock.writeLock().unlock();
			//return this.chunk;
		}

		if(this.loadChunkTask == null) {
			logger.error("No load chunk task was waiting...");
			return;
		} else {
			// This task is now done
			this.loadChunkTask = null;
		}

		// Create the actual chunk object
		this.chunk = new CubicChunk(this, x, y, z, data);

		regionLoadedChunks.add(chunk);

		//TODO maybe a callback here ?
		//if (region.getWorld() instanceof WorldClient)
		//	((WorldClient) region.getWorld()).getWorldRenderer().flagChunksModified();

		this.chunkLock.writeLock().unlock();

		try {
			region.usersLock.lock();
			// Already have clients waiting for the chunk data ? Satisfy these messieurs
			for (RemotePlayer user : usersWaitingForIntialData) {
				user.pushPacket(new PacketChunkCompressedData(chunk, data));
			}
			usersWaitingForIntialData.clear();
		} finally {
			region.usersLock.unlock();
		}
	}

	@Override
	public int getChunkCoordinateX() {
		return getInRegionX() + region.getRegionX() * 8;
	}

	@Override
	public int getChunkCoordinateY() {
		return getInRegionY() + region.getRegionY() * 8;
	}

	@Override
	public int getChunkCoordinateZ() {
		return getInRegionZ() + region.getRegionZ() * 8;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChunkHolderImplementation) {
			ChunkHolderImplementation ch = ((ChunkHolderImplementation) o);
			return ch.uuid == uuid;
		}

		return false;
	}

	@Override
	public boolean isChunkLoaded() {
		return chunk != null;
	}

	@Override
	public Fence waitForLoading() {
		Fence f = this.loadChunkTask;
		if (f != null)
			return f;

		// Return a trvial fence if the chunk is not currently loading anything
		return new TrivialFence();
	}
}
