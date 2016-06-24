package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.io.IOTasksImmediate;
import io.xol.chunkstories.world.iterators.ChunkHolderIterator;
import io.xol.engine.concurrency.SafeWriteLock;
import io.xol.engine.concurrency.SimpleLock;

import java.io.File;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkHolder implements Region
{
	public final WorldImplementation world;
	public final int regionX, regionY, regionZ;
	public final long uuid;

	//Only relevant on Master worlds
	public final File handler;

	// Holds 8x8x8 CubicChunks
	private CubicChunk[][][] data = new CubicChunk[8][8][8];

	private AtomicInteger loadedChunks = new AtomicInteger();

	//TODO find a clean way to let IOTaks fiddle with this
	public SimpleLock chunksArrayLock = new SimpleLock();
	public SafeWriteLock compressedChunksLock = new SafeWriteLock();

	public byte[][][][] compressedChunks = new byte[8][8][8][];
	private AtomicBoolean isDiskDataLoaded = new AtomicBoolean(false);
	
	//Local entities
	private BlockingQueue<Entity> localEntities = new LinkedBlockingQueue<Entity>();

	// LZ4 compressors & decompressors stuff
	private static LZ4Factory factory = LZ4Factory.fastestInstance();
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

	private static Random random = new Random();

	public ChunkHolder(WorldImplementation world, int regionX, int regionY, int regionZ)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;

		//Unique UUID
		uuid = random.nextLong();

		//Only the WorldMaster has a concept of files
		if (world instanceof WorldMaster)
		{
			handler = new File(world.getFolderPath() + "/regions/" + regionX + "." + regionY + "." + regionZ + ".csf");
			world.ioHandler.requestChunkHolderLoad(this);
		}
		else
			handler = null;
	}

	private void compressChunkData(CubicChunk chunk)
	{
		int chunkX = chunk.chunkX;
		int chunkY = chunk.chunkY;
		int chunkZ = chunk.chunkZ;
		if (chunk.dataPointer >= 0)
		{
			byte[] toCompressData = new byte[32 * 32 * 32 * 4];

			int[] data = world.chunksData.grab(chunk.dataPointer);
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
			compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = new byte[compressedDataLength];
			System.arraycopy(compressedData.get(), 0, compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8], 0, compressedDataLength);
			compressedChunksLock.endWrite();
		}
		else
		{
			compressedChunksLock.beginWrite();
			compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
			compressedChunksLock.endWrite();
		}
		
		chunk.lastModificationSaved.set(System.currentTimeMillis());
	}

	public byte[] getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		return compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8];
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#get(int, int, int, boolean)
	 */
	@Override
	public CubicChunk get(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		CubicChunk rslt = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (load && rslt == null)
		{
			world.ioHandler.requestChunkLoad(this, chunkX, chunkY, chunkZ, false);
			if (world.ioHandler instanceof IOTasksImmediate)
			{
				return get(chunkX, chunkY, chunkZ, false);
			}
			return null;
		}
		return rslt;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#set(int, int, int, io.xol.chunkstories.world.chunk.CubicChunk)
	 */
	public Chunk set(int chunkX, int chunkY, int chunkZ, CubicChunk chunk)
	{
		chunksArrayLock.lock();

		if (data[chunkX % 8][chunkY % 8][chunkZ % 8] == null && chunk != null)
			loadedChunks.incrementAndGet();

		//Remove any form of cuck
		if (data[chunkX % 8][chunkY % 8][chunkZ % 8] != null && data[chunkX % 8][chunkY % 8][chunkZ % 8].dataPointer != chunk.dataPointer)
		{
			System.out.println("Overriding existing chunk, deleting old one");
			data[chunkX % 8][chunkY % 8][chunkZ % 8].destroy();
		}


		//System.out.println("set chunk"+chunk);
		data[chunkX % 8][chunkY % 8][chunkZ % 8] = chunk;
		
		//Change chunk holder to this
		chunk.holder = this;
		chunksArrayLock.unlock();
		return chunk;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#removeChunk(int, int, int)
	 */
	@Override
	public boolean removeChunk(int chunkX, int chunkY, int chunkZ)
	{
		//Thread.currentThread().dumpStack();
		//System.out.println("remove chunk "+chunkX+":"+chunkY+":"+chunkZ);
		
		CubicChunk c = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (c != null)
		{
			compressChunkData(c);

			chunksArrayLock.lock();
			c.destroy();
			data[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
			//compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
			loadedChunks.decrementAndGet();
			chunksArrayLock.unlock();
		}
		return loadedChunks.get() == 0;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#isChunkLoaded(int, int, int)
	 */
	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		return data[chunkX % 8][chunkY % 8][chunkZ % 8] != null;
	}

	public ChunksIterator iterator()
	{
		return new ChunkHolderIterator(this);
	}

	public void unloadAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
						removeChunk(a, b, c);
				}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#isLoaded()
	 */
	@Override
	public boolean isLoaded()
	{
		return isDiskDataLoaded.get();
	}

	public void setLoaded(boolean b)
	{
		isDiskDataLoaded.set(b);
	}

	public void unload()
	{
		unloadAll();
		System.out.println("Unloaded chunk holder "+this+" with " + " 0 entities remaining in it.");
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#generateAll()
	 */
	public void generateAll()
	{
		// Generate terrain for the chunk holder !
		CubicChunk chunk;
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					//CubicChunk chunk = data[a][b][c];
					int cx = this.regionX * 8 + a;
					int cy = this.regionY * 8 + b;
					int cz = this.regionZ * 8 + c;
					chunk = world.getGenerator().generateChunk(cx, cy, cz);
					if (chunk == null)
						System.out.println("hmmmmm");
					chunk.holder = this;
					this.set(cx, cy, cz, chunk);
					//compressChunkData(data[a][b][c]);
				}

		compressAll();
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#save()
	 */
	@Override
	public void save()
	{
		world.ioHandler.requestChunkHolderSave(this);
	}

	@Override
	public String toString()
	{
		return "[ChunkHolder rx:" + regionX + " ry:" + regionY + " rz:" + regionZ + " uuid: " + uuid + "loaded:" + isDiskDataLoaded.get() + "]";
	}

	public void compressAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
						compressChunkData(data[a][b][c]);
					//else
					//	compressedChunks[a][b][c] = null;
				}
	}

	@Override
	public int getNumberOfLoadedChunks()
	{
		return loadedChunks.get();
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
	public boolean removeEntity(Entity entity)
	{
		return localEntities.remove(entity);
	}

	@Override
	public boolean addEntity(Entity entity)
	{
		return localEntities.add(entity);
	}
}
