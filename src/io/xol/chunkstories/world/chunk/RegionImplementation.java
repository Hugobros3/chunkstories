package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.entity.interfaces.EntityUnsaveable;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.io.CSFRegionFile;
import io.xol.chunkstories.world.iterators.RegionIterator;
import io.xol.engine.concurrency.SafeWriteLock;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RegionImplementation implements Region
{
	public final WorldImplementation world;
	public final int regionX, regionY, regionZ;
	public final long uuid;
	private final WorldRegionsHolder worldChunksHolder;

	//Only relevant on Master worlds
	public final CSFRegionFile handler;

	// Holds 8x8x8 CubicChunks

	//private CubicChunk[][][] data = new CubicChunk[8][8][8];
	//private AtomicInteger loadedChunks = new AtomicInteger();

	private AtomicLong unloadCooldown = new AtomicLong();
	private boolean unloadedFlag = false;

	//TODO find a clean way to let IOTaks fiddle with this
	public SafeWriteLock chunksArrayLock = new SafeWriteLock();

	private ChunkHolderImplementation[][][] chunkHolders;
	//public SafeWriteLock compressedChunksLock = new SafeWriteLock();
	//public byte[][][][] compressedChunks = new byte[8][8][8][];
	private AtomicBoolean isDiskDataLoaded = new AtomicBoolean(false);

	//Local entities
	private Set<Entity> localEntities = ConcurrentHashMap.newKeySet();

	// LZ4 compressors & decompressors stuff
	/*private static LZ4Factory factory = LZ4Factory.fastestInstance();
	private static LZ4Compressor compressor = factory.fastCompressor();
	private static LZ4FastDecompressor decompressor = factory.fastDecompressor();
	
	private static ThreadLocal<byte[]> compressedData = new ThreadLocal<byte[]>()
	{
		@Override
		protected byte[] initialValue()
		{
			return new byte[32 * 32 * 32 * 4];
		}
	};
	 */

	private static Random random = new Random();

	public RegionImplementation(WorldImplementation world, int regionX, int regionY, int regionZ, WorldRegionsHolder worldChunksHolder)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;
		this.worldChunksHolder = worldChunksHolder;

		//Initialize slots
		chunkHolders = new ChunkHolderImplementation[8][8][8];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				for (int k = 0; k < 8; k++)
					chunkHolders[i][j][k] = new ChunkHolderImplementation(this, i, j, k);

		//Unique UUID
		uuid = random.nextLong();

		worldChunksHolder.regionConstructorCallBack(this);

		//Set the initial cooldown delay
		unloadCooldown.set(System.currentTimeMillis());

		//Only the WorldMaster has a concept of files
		if (world instanceof WorldMaster)
		{
			handler = new CSFRegionFile(this);
			world.ioHandler.requestRegionLoad(this);
		}
		else
		{
			handler = null;
			isDiskDataLoaded.set(true);
		}
	}

	/*private void compressChunkData(Chunk chunk)
	{
		int chunkX = chunk.getChunkX();
		int chunkY = chunk.getChunkY();
		int chunkZ = chunk.getChunkZ();
		if (chunk instanceof CubicChunk)
		{
			CubicChunk cubic = (CubicChunk) chunk;
			if (!chunk.isAirChunk())
			{
	
				byte[] toCompressData = new byte[32 * 32 * 32 * 4];
	
				int[] data = cubic.chunkVoxelData;
				int z = 0;
				for (int i : data)
				{
					toCompressData[z] = (byte) ((i >>> 24) & 0xFF);
					toCompressData[z + 1] = (byte) ((i >>> 16) & 0xFF);
					toCompressData[z + 2] = (byte) ((i >>> 8) & 0xFF);
					toCompressData[z + 3] = (byte) ((i) & 0xFF);
					z += 4;
				}
				int compressedDataLength = compressor.compress(toCompressData, compressedData.get());
	
				assert decompressor.decompress(compressedData.get(), 32 * 32 * 32 * 4).length == 32 * 32 * 32 * 4;
	
				// Locks the compressedChunks array so nothing freakes out
				compressedChunksLock.beginWrite();
				compressedChunks[chunkX & 7][chunkY & 7][chunkZ & 7] = new byte[compressedDataLength];
				System.arraycopy(compressedData.get(), 0, compressedChunks[chunkX & 7][chunkY & 7][chunkZ & 7], 0, compressedDataLength);
				compressedChunksLock.endWrite();
			}
			else
			{
				compressedChunksLock.beginWrite();
				compressedChunks[chunkX & 7][chunkY & 7][chunkZ & 7] = null;
				compressedChunksLock.endWrite();
	
			}
			cubic.lastModificationSaved.set(System.currentTimeMillis());
		}
	}*/

	public byte[] getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[chunkX & 7][chunkY & 7][chunkZ & 7].getCompressedData();
	}

	@Override
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[chunkX & 7][chunkY & 7][chunkZ & 7].getChunk();
	}

	@Override
	public ChunkHolderImplementation getChunkHolder(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[chunkX & 7][chunkY & 7][chunkZ & 7];
	}

	/*public Chunk setChunk(int chunkX, int chunkY, int chunkZ, Chunk chunk)
	{
		chunksArrayLock.beginWrite();
	
		if (data[chunkX & 7][chunkY & 7][chunkZ & 7] == null && chunk != null)
			loadedChunks.incrementAndGet();
	
		//Remove any form of cuck
		if (data[chunkX & 7][chunkY & 7][chunkZ & 7] != null)// && data[chunkX & 7][chunkY & 7][chunkZ & 7].dataPointer != chunk.dataPointer)
		{
			System.out.println(chunk);
			//System.out.println(this + "chunkX"+chunkX+":"+chunkY+":"+chunkZ);
			//Thread.currentThread().dumpStack();
			//System.out.println("Overriding existing chunk, deleting old one");
			data[chunkX & 7][chunkY & 7][chunkZ & 7].destroy();
		}
	
		//System.out.println("set chunk"+chunk);
		data[chunkX & 7][chunkY & 7][chunkZ & 7] = (CubicChunk) chunk;
	
		//Change chunk holder to this
		assert chunk.getRegion().equals(this);
	
		chunksArrayLock.endWrite();
		return chunk;
	}*/

	/*@Override
	public boolean removeChunk(int chunkX, int chunkY, int chunkZ)
	{
		Chunk c = data[chunkX & 7][chunkY & 7][chunkZ & 7];
		if (c != null)
		{
			//Save back whatever the chunk
			compressChunkData(c);
	
			//Locks the chunks array
			chunksArrayLock.beginWrite();
			//Destroys the chunk
			data[chunkX & 7][chunkY & 7][chunkZ & 7] = null;
			c.destroy();
			//Update counter
			loadedChunks.decrementAndGet();
			//Unlocks
			chunksArrayLock.endWrite();
		}
	
		//True -> holder is now empty.
		return loadedChunks.get() == 0;
	}*/

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#isChunkLoaded(int, int, int)
	 */

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[chunkX & 7][chunkY & 7][chunkZ & 7].isChunkLoaded();
	}

	public ChunksIterator iterator()
	{
		return new RegionIterator(this);
	}

	@Override
	public boolean isDiskDataLoaded()
	{
		return isDiskDataLoaded.get();
	}

	public boolean isUnloaded()
	{
		return unloadedFlag;
	}

	public void setDiskDataLoaded(boolean b)
	{
		isDiskDataLoaded.set(b);
	}

	public void unload()
	{
		//Before unloading the holder we want to make sure we finish all saving operations
		if (handler != null)
			handler.finishSavingOperations();

		//Set unloaded flag to true so we are not using again an unloaded holder
		unloadedFlag = true;

		//No need to unload chunks, this is assumed when we unload the holder
		//unloadAllChunks();

		world.entitiesLock.lock();

		//int countRemovedEntities = 0;

		Iterator<Entity> i = this.getEntitiesWithinRegion();
		while (i.hasNext())
		{
			Entity entity = i.next();
			//i.remove();

			//Skip entities that shouldn't be saved
			if ((entity instanceof EntityUnsaveable && !((EntityUnsaveable) entity).shouldSaveIntoRegion()))
				continue;

			//System.out.println("Unloading entity"+entity+" currently in chunk holder "+this);

			//We keep the inner reference so serialization can still write entities contained within
			world.removeEntityFromList(entity);
			//countRemovedEntities++;
		}

		world.entitiesLock.unlock();

		//Remove the reference in the world to this
		this.getWorld().getRegionsHolder().removeRegion(this);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#generateAll()
	 */
	public void generateAll()
	{
		// Generate terrain for the chunk holder !
		Chunk chunk;
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					//CubicChunk chunk = data[a][b][c];
					int cx = this.regionX * 8 + a;
					int cy = this.regionY * 8 + b;
					int cz = this.regionZ * 8 + c;
					chunk = world.getGenerator().generateChunk(this, cx, cy, cz);

					if (chunk == null)
						System.out.println("Notice : generator " + world.getGenerator() + " produced a null chunk.");

					this.chunkHolders[a][b][c].setChunk((CubicChunk) chunk);
					//compressChunkData(data[a][b][c]);
				}

		compressAll();
	}

	@Override
	public void save()
	{
		world.ioHandler.requestRegionSave(this);
	}

	@Override
	public void unloadAndSave()
	{
		unload();
		world.ioHandler.requestRegionSave(this);
	}

	@Override
	public String toString()
	{
		return "[Region rx:" + regionX + " ry:" + regionY + " rz:" + regionZ + " uuid: " + uuid + "loaded?:" + isDiskDataLoaded.get() + " u:" + unloadedFlag + " chunks: " + "NULL" + " entities:" + this.localEntities.size() + "]";
	}

	public void compressAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					chunkHolders[a][b][c].compressChunkData();
	}

	public void compressChangedChunks()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (chunkHolders[a][b][c].getChunk() != null)
					{
						CubicChunk chunk = chunkHolders[a][b][c].getChunk();
						if (chunk.lastModification.get() > chunk.lastModificationSaved.get())
							chunkHolders[a][b][c].compressChunkData();
					}
				}
	}

	@Override
	public int getNumberOfLoadedChunks()
	{
		int count = 0;

		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					if (chunkHolders[a][b][c].isChunkLoaded())
						count++;

		return count;
	}

	@Override
	public int getRegionX()
	{
		return regionX;
	}

	@Override
	public int getRegionY()
	{
		return regionY;
	}

	@Override
	public int getRegionZ()
	{
		return regionZ;
	}

	@Override
	public Iterator<Entity> getEntitiesWithinRegion()
	{
		return localEntities.iterator();
	}

	@Override
	public boolean removeEntityFromRegion(Entity entity)
	{
		return localEntities.remove(entity);
	}

	@Override
	public boolean addEntityToRegion(Entity entity)
	{
		return localEntities.add(entity);
	}

	public void resetUnloadCooldown()
	{
		unloadCooldown.set(System.currentTimeMillis());
	}

	public boolean canBeUnloaded()
	{
		//Don't unload it until it has been loaded for 10s
		return this.isDiskDataLoaded() && (System.currentTimeMillis() - this.unloadCooldown.get() > 10 * 1000L);
	}

	public WorldImplementation getWorld()
	{
		return world;
	}

	@Override
	public EntityVoxel getEntityVoxelAt(int worldX, int worldY, int worldZ)
	{
		Iterator<Entity> entities = this.getEntitiesWithinRegion();
		while (entities.hasNext())
		{
			Entity entity = entities.next();
			if (entity != null && entity instanceof EntityVoxel && ((int) entity.getLocation().getX() == worldX) && ((int) entity.getLocation().getY() == worldY) && ((int) entity.getLocation().getZ() == worldZ))
				return (EntityVoxel) entity;
		}
		return null;
	}

	/**
	 * Unloads unused chunks, returns true if all chunks were unloaded
	 */
	public boolean unloadsUnusedChunks()
	{
		int loadedChunks = 0;
		
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					chunkHolders[a][b][c].unloadsIfUnused();
					if(chunkHolders[a][b][c].isChunkLoaded())
						loadedChunks++;
				}
		
		return loadedChunks == 0;
	}

	@Override
	public boolean isUnused()
	{
		int usedChunks = 0;
		
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					chunkHolders[a][b][c].unloadsIfUnused();
					if(chunkHolders[a][b][c].isChunkLoaded() || chunkHolders[a][b][c].countUsers() > 0)
						usedChunks++;
				}
		
		return usedChunks == 0;
	}
}
