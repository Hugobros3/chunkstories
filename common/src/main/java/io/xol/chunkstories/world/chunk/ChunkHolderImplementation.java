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
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitDontSave;
import io.xol.chunkstories.api.entity.traits.serializable.TraitController;
import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.WorldClient;
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

public class ChunkHolderImplementation implements ChunkHolder {
	// Position stuff
	private final RegionImplementation region;
	private final int x, y, z;
	private final int uuid;

	private final Collection<CubicChunk> regionLoadedChunks; // To update the parent object's collection (used in
																// iterator)

	protected final Set<WorldUser> users = new HashSet<>(); // Keep tracks of who needs this data loaded
	private final Set<RemotePlayer> usersWaitingForIntialData = new HashSet<RemotePlayer>();
	// protected final Lock usersLock;
	// protected final Lock usersLock = new ReentrantLock();

	// The compressed version of the chunk data
	private SafeWriteLock compressedDataLock = new SafeWriteLock();
	private CompressedData compressedData;

	public final static byte[] AIR_CHUNK_NO_DATA_SAVED = new byte[] {}; // Symbolic reference indicating there is
																		// nothing worth saving in this chunk, but data
																		// was generated

	private IOTask loadChunkTask;

	private ReadWriteLock chunkLock = new ReentrantReadWriteLock();
	private CubicChunk chunk;

	public ChunkHolderImplementation(RegionImplementation region, Collection<CubicChunk> loadedChunks, int x, int y,
			int z) {
		this.region = region;
		this.regionLoadedChunks = loadedChunks;

		this.x = x;
		this.y = y;
		this.z = z;

		uuid = ((x << region.getWorld().getWorldInfo().getSize().bitlengthOfVerticalChunksCoordinates) | y) << region
				.getWorld().getWorldInfo().getSize().bitlengthOfHorizontalChunksCoordinates | z;
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

				// For all components in this cell
				for (Entry<String, VoxelComponent> entry : voxelComponents.all()) {
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
					// smallBuffer.get(smallArray);
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
		CubicChunk chunk = this.chunk;

		if (chunk == null) {
			chunkLock.writeLock().unlock();
			return;
		}

		// Unlist it immediately
		regionLoadedChunks.remove(chunk);
		this.chunk = null;

		// Remove the entities from this chunk from the world
		region.world.getEntitiesLock().writeLock().lock();
		Iterator<Entity> i = chunk.localEntities.iterator();
		while (i.hasNext()) {
			Entity entity = i.next();
			if (entity.traits.tryWith(TraitController.class, ec -> ec.getController()) != null) {
				continue; // give grace to controlled entities
				// TODO this is sloppy!
			} else {
				region.world.removeEntityFromList(entity);
			}
		}
		region.world.getEntitiesLock().writeLock().unlock();

		// Lock it down
		chunk.entitiesLock.lock();

		// Kill any load chunk operation that is still scheduled
		if (loadChunkTask != null) {
			IOTask task = loadChunkTask;
			if (task != null)
				task.cancel();

			loadChunkTask = null;
		}

		// Compress chunk one last time before it has to go
		setCompressedData(compressChunkData(chunk));

		// destroy it (returns any internal data using up ressources)
		chunk.destroy();

		// unlock it (whoever messes with it now, his problem)
		chunk.entitiesLock.unlock();
		chunkLock.writeLock().unlock();
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

			if (users.add(user))
				region.usersCount++;

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

			if (users.remove(user)) // Remove the user from the set and decrement the region users count
				region.usersCount--;

			if (users.isEmpty()) {
				unloadChunk(); // Unload the chunk as soon as nobody holds on to it

				if (region.usersCount == 0) // Nota bene: This can only happen if this chunk has no user, since the
											// chunk users ARE region users
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

	public CubicChunk createChunk() {
		return this.createChunk(null);
	}

	static Constructor<? extends CubicChunk> clientChunkConstructor;
	static Constructor<? extends CubicChunk> clientChunkConstructorNoData;

	public CubicChunk createChunk(CompressedData data) {
		this.chunkLock.writeLock().lock();

		if (this.chunk != null) {
			System.out.println("Warning: creating a chunk but the chunkholder already had one, ignoring");
			this.chunkLock.writeLock().unlock();
			return this.chunk;
		}

		CubicChunk chunk = data == null ? new CubicChunk(this, x, y, z) : new CubicChunk(this, x, y, z, data);

		if (this.chunk == null && chunk != null)
			regionLoadedChunks.add(chunk);
		this.chunk = chunk;

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

		return chunk;
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
			// boolean thoroughTest = ch.x == x && ch.y == y && ch.z == z;
			boolean fastTest = ch.uuid == uuid;

			/*
			 * if(fastTest != thoroughTest) {
			 * System.out.println("Grosse merde !"+thoroughTest+" != "+fastTest);
			 * System.out.println(x+" "+y+" "+z + " " + ch.uuid); }
			 */

			return fastTest;
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
