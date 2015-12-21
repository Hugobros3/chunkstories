package io.xol.chunkstories.world;

import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.world.io.IOTasksImmediate;

import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
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
	
	public ChunkHolder(World world, int regionX, int regionY, int regionZ)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;

		handler = new File(world.getFolderPath() + "/regions/" + regionX + "." + regionY + "." + regionZ + ".csf");

		world.ioHandler.requestChunkHolderLoad(this);
	}

	/*public void addEntity(Entity entity)
	{
		//entity.parentHolder = this;
		synchronized(entities)
		{
			entities.add(entity);
		}
	}*/

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

	/*public void removeEntity(Entity e)
	{
		synchronized(entities)
		{
			Iterator<Entity> iterator = entities.iterator();
			Entity entity;
			while (iterator.hasNext())
			{
				entity = iterator.next();
				if(entity.equals(e))
					iterator.remove();
			}
		}
	}*/
	
	public void tick()
	{
		try{
			synchronized(world.entities)
			{
				/*Iterator<Entity> iterator = entities.iterator();
				Entity entity;
				while (iterator.hasNext())
				{
					entity = iterator.next();
					if (entity == null)
					{
						System.out.println("le remove");
						iterator.remove();
					}
					else
					{
						//TODO check chunk loaded
						entity.update();
					}
				}*/
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

	public void compressChunkData(CubicChunk chunk)
	{
		if (chunk.dataPointer >= 0)
		{
			byte[] toCompressData = new byte[32 * 32 * 32 * 4];
			
			int chunkX = chunk.chunkX;
			int chunkY = chunk.chunkY;
			int chunkZ = chunk.chunkZ;
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
			// System.out.println("Generated compressed data for chunk "+chunkX+"."+chunkY+"."+chunkZ+" size="+compressedDataLength);
		}
	}

	public void compressChunkData(int chunkX, int chunkY, int chunkZ)
	{
		CubicChunk chunk = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (chunk != null)
		{
			compressChunkData(chunk);
		}
	}

	public byte[] getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		byte[] cd = compressedChunks[chunkX % 8][chunkY % 8][chunkZ % 8];
		return cd;
	}

	public CubicChunk get(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		CubicChunk rslt = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (load && rslt == null)
		{
			// world.ioHandler.addTaskTodo(new IOTaskLoadChunk(chunkX, chunkY,
			// chunkZ, true));
			if(!requested[chunkX % 8][chunkY % 8][chunkZ % 8])
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
		synchronized (data)
		{
			if (data[chunkX % 8][chunkY % 8][chunkZ % 8] == null && c != null)
				loadedChunks.incrementAndGet();
			data[chunkX % 8][chunkY % 8][chunkZ % 8] = c;
			requested[chunkX % 8][chunkY % 8][chunkZ % 8] = false;
			// System.out.println("did set chunk lol");
			c.holder = this;
		}
		return c;
	}

	public boolean free(int chunkX, int chunkY, int chunkZ)
	{
		// System.out.println("freeee");
		CubicChunk c = data[chunkX % 8][chunkY % 8][chunkZ % 8];
		if (c != null)
		{
			synchronized (data[chunkX % 8][chunkY % 8][chunkZ % 8])
			{
				// save(chunkX,chunkY,chunkZ);
				compressChunkData(c);
				c.destroy();
				data[chunkX % 8][chunkY % 8][chunkZ % 8] = null;
				loadedChunks.decrementAndGet();
			}
		}
		return loadedChunks.get() == 0;
	}

	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		return data[chunkX % 8][chunkY % 8][chunkZ % 8] != null;
	}

	public List<CubicChunk> getLoadedChunks()
	{
		List<CubicChunk> chunks = new ArrayList<CubicChunk>();
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
					{
						chunks.add(data[a][b][c]);
					}
				}
		return chunks;
	}

	public void freeAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (data[a][b][c] != null)
						free(a, b, c);
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
		for(int a = 0; a <8; a++)
			for(int b = 0; b <8; b++)
				for(int c = 0; c <8; c++)
				{
					//CubicChunk chunk = data[a][b][c];
					int cx = this.regionX * 8 + a;
					int cy = this.regionY * 8 + b;
					int cz = this.regionZ * 8 + c;
					data[a][b][c] =	world.accessor.loadChunk(cx, cy, cz);
					compressChunkData(data[a][b][c]);
				}
	}
}
