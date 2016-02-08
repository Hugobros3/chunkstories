package io.xol.chunkstories.world;

import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.world.io.IOTasksImmediate;
import io.xol.chunkstories.world.iterators.ChunkHolderIterator;
import io.xol.engine.concurrency.SimpleLock;

import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
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
	private boolean[][][] requested = new boolean[8][8][8];
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
	public int compressedDataLength = 0;

	//byte[] unCompressedData = new byte[32 * 32 * 32 * 4];

	//public List<Entity> entities = new ArrayList<Entity>();
	
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
	
	public List<Entity> getAllLoadedEntities()
	{
		List<Entity> localEntities  = new ArrayList<Entity>();
		synchronized(world.entities)
		{
			Iterator<Entity> iterator = world.entities.iterator();
			Entity entity;
			while (iterator.hasNext())
			{
				entity = iterator.next();
				if (entity != null && entity.parentHolder != null && entity.parentHolder.equals(this))
				{
					localEntities.add(entity);
				}
			}
		}
		return localEntities;
	}
	
	public void tick()
	{
		try{
			synchronized(world.entities)
			{
				for(Entity entity : world.entities)
				{
					if(entity != null)
						entity.update();
				}
			}
		}
		catch(ConcurrentModificationException e)
		{
			//e.printStackTrace();
			System.out.println("bou bouh :'( ");
		}
	
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
			//
			compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = new byte[compressedDataLength];
			System.arraycopy(compressedData, 0, compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8], 0, compressedDataLength);
			//System.out.println("Generated compressed data for chunk "+chunkX+"."+chunkY+"."+chunkZ+" size="+compressedDataLength);
		}
		else
			compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
	}

	public byte[] getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		//byte[] cd = compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8];
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
				requested[chunkX % 8][chunkY % 8][chunkZ % 8] = true;
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

	public CubicChunk set(int chunkX, int chunkY, int chunkZ, CubicChunk c)
	{
		lock.lock();
		if (data[chunkX % 8][chunkY % 8][chunkZ % 8] == null && c != null)
			loadedChunks.incrementAndGet();
		data[chunkX % 8][chunkY % 8][chunkZ % 8] = c;
		requested[chunkX % 8][chunkY % 8][chunkZ % 8] = false;
		// System.out.println("did set chunk lol");
		c.holder = this;
		lock.unlock();
		return c;
	}
	
	public SimpleLock lock = new SimpleLock();

	public boolean removeChunk(int chunkX, int chunkY, int chunkZ)
	{
		lock.lock();
		CubicChunk c = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (c != null)
		{
			// System.out.println("freed"+c);
			// save(chunkX,chunkY,chunkZ);
			compressChunkData(c);
			c.destroy();
			data[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
			//compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
			loadedChunks.decrementAndGet();
		}
		lock.unlock();
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
		//System.out.println("Unloaded chunk holder with "+entities.size()+" entities remaining in it.");
	}

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
					chunk =	world.generator.generateChunk(cx, cy, cz);
					if(chunk == null)
						System.out.println("hmmmmm");
					chunk.holder = this;
					data[a][b][c] = chunk;
					compressChunkData(data[a][b][c]);
				}
	}
	
	long uuid;
	
	public String toString()
	{
		return "[ChunkHolder rx:"+regionX+" ry:"+regionY+" rz:"+regionZ+" uuid: "+uuid+"loaded:"+isLoaded.get()+"]";
	}

	public void compressAll()
	{
		lock.lock();
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
						compressChunkData(data[a][b][c]);
					else
						compressedChunks[a][b][c] = null;
				}
		lock.unlock();
	}
}
