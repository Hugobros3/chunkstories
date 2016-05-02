package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.io.IOTasksImmediate;
import io.xol.chunkstories.world.iterators.ChunkHolderIterator;
import io.xol.engine.concurrency.SafeWriteLock;
import io.xol.engine.concurrency.SimpleLock;

import java.io.File;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkHolder
{

	// Holds 8x8x8 CubicChunks
	private CubicChunk[][][] data = new CubicChunk[8][8][8];
	//private boolean[][][] requested = new boolean[8][8][8];
	public byte[][][][] compressedChunks = new byte[8][8][8][];

	AtomicInteger loadedChunks = new AtomicInteger();
	AtomicBoolean isLoaded = new AtomicBoolean(false);

	public World world;
	// World coordinates / 256
	public int regionX, regionY, regionZ;

	// LZ4 compressors & decompressors
	static LZ4Factory factory = LZ4Factory.fastestInstance();
	public static LZ4Compressor compressor = factory.fastCompressor();
	public static LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public File handler;

	// public static byte[] compressedData = new byte[32*32*32*4];
	byte[] compressedData = new byte[32 * 32 * 32 * 4];
	private int compressedDataLength = 0;
	
	public static Random random = new Random();
	
	public ChunkHolder(World world, int regionX, int regionY, int regionZ, boolean dontLoad)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;

		handler = new File(world.getFolderPath() + "/regions/" + regionX + "." + regionY + "." + regionZ + ".csf");

		uuid = random.nextLong();
		
		if(!dontLoad)
			world.ioHandler.requestChunkHolderLoad(this);
	}
	
	public void save()
	{
		world.ioHandler.requestChunkHolderSave(this);
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
			compressedDataLength = compressor.compress(toCompressData, compressedData);
			
			assert decompressor.decompress(compressedData, 32 * 32 * 32 * 4).length == 32 * 32 * 32 * 4;
			
			// Locks the compressedChunks array so nothing freakes out
			compressedChunksLock.beginWrite();
			compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = new byte[compressedDataLength];
			System.arraycopy(compressedData, 0, compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8], 0, compressedDataLength);
			compressedChunksLock.endWrite();
			
			//System.out.println("Generated compressed data for chunk "+chunkX+"."+chunkY+"."+chunkZ+" size="+compressedDataLength);
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

	public CubicChunk get(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		CubicChunk rslt = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (load && rslt == null)
		{
			// world.ioHandler.addTaskTodo(new IOTaskLoadChunk(chunkX, chunkY,
			// chunkZ, true));
			//if(!requested[chunkX % 8][chunkY % 8][chunkZ % 8])
			{
				//requested[chunkX % 8][chunkY % 8][chunkZ % 8] = true;
				//System.out.println("IO REQUEST");
				world.ioHandler.requestChunkLoad(chunkX, chunkY, chunkZ, false);
				if(world.ioHandler instanceof IOTasksImmediate)
				{
					return get(chunkX, chunkY, chunkZ, false);
				}
			}
			return null;
			// check for compressed version avaible
		}
		return rslt;
	}

	public Chunk set(int chunkX, int chunkY, int chunkZ, CubicChunk c)
	{
		chunksArrayLock.lock();
		
		if (data[chunkX % 8][chunkY % 8][chunkZ % 8] == null && c != null)
			loadedChunks.incrementAndGet();
		
		//Remove any form of cuck
		if(data[chunkX % 8][chunkY % 8][chunkZ % 8] != null && data[chunkX % 8][chunkY % 8][chunkZ % 8].dataPointer != c.dataPointer)
		{
			System.out.println("Overriding existing chunk, deleting old one");
			data[chunkX % 8][chunkY % 8][chunkZ % 8].destroy();
		}
		
		data[chunkX % 8][chunkY % 8][chunkZ % 8] = c;
		// requested[chunkX % 8][chunkY % 8][chunkZ % 8] = false;
		// System.out.println("did set chunk lol");
		c.holder = this;
		chunksArrayLock.unlock();
		return c;
	}
	
	public SimpleLock chunksArrayLock = new SimpleLock();
	public SafeWriteLock compressedChunksLock = new SafeWriteLock();

	public boolean removeChunk(int chunkX, int chunkY, int chunkZ)
	{
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

	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		return data[chunkX % 8][chunkY % 8][chunkZ % 8] != null;
	}
	
	public ChunksIterator iterator()
	{
		return new ChunkHolderIterator(this);
	}

	public void freeAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
						removeChunk(a, b, c);
				}
	}

	public boolean isLoaded()
	{
		return isLoaded.get();
	}

	public void setLoaded(boolean b)
	{
		isLoaded.set(b);
	}

	public void destroy()
	{
		freeAll();
		System.out.println("Unloaded chunk holder with "+" 0 entities remaining in it.");
	}

	/**
	 * Generates all chunks in the holder (8x8x8)
	 * Will lock loaded chunks array
	 */
	public void generateAll()
	{
		// Generate terrain for the chunk holder !
		CubicChunk chunk;
		for(int a = 0; a <8; a++)
			for(int b = 0; b <8; b++)
				for(int c = 0; c <8; c++)
				{
					//CubicChunk chunk = data[a][b][c];
					int cx = this.regionX * 8 + a;
					int cy = this.regionY * 8 + b;
					int cz = this.regionZ * 8 + c;
					chunk =	world.getGenerator().generateChunk(cx, cy, cz);
					if(chunk == null)
						System.out.println("hmmmmm");
					chunk.holder = this;
					this.set(cx, cy, cz, chunk);
					//compressChunkData(data[a][b][c]);
				}
		
		compressAll();
	}
	
	long uuid;
	
	@Override
	public String toString()
	{
		return "[ChunkHolder rx:"+regionX+" ry:"+regionY+" rz:"+regionZ+" uuid: "+uuid+"loaded:"+isLoaded.get()+"]";
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
}
