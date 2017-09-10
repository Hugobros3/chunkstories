package io.xol.chunkstories.world.chunk;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.io.IOTasks.IOTask;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.engine.concurrency.SafeWriteLock;
import io.xol.engine.concurrency.TrivialFence;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.renderer.chunks.RenderableChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ChunkHolderImplementation implements ChunkHolder
{
	private RegionImplementation region;
	Collection<CubicChunk> regionLoadedChunks;
	private int x, y, z;
	private int uuid;

	public ChunkHolderImplementation(RegionImplementation region, Collection<CubicChunk> loadedChunks, int x, int y, int z)
	{
		this.region = region;
		this.regionLoadedChunks = loadedChunks;
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		//int filteredChunkX = chunkX & (size.maskForChunksCoordinates);
		//int filteredChunkY = Math2.clampi(chunkY, 0, 31);
		//int filteredChunkZ = chunkZ & (size.maskForChunksCoordinates);
		
		uuid = ((x << region.getWorld().getWorldInfo().getSize().bitlengthOfVerticalChunksCoordinates) | y ) << region.getWorld().getWorldInfo().getSize().bitlengthOfHorizontalChunksCoordinates | z;
	}

	private Set<WeakReference<WorldUser>> users = ConcurrentHashMap.newKeySet();//<WeakReference<WorldUser>>();
	
	private SafeWriteLock compressedDataLock = new SafeWriteLock();
	private byte[] compressedData;
	
	private IOTask loadChunkTask;
	//private WeakReference<IOTask> loadChunkTask;
	private CubicChunk chunk;

	// LZ4 compressors & decompressors stuff
	private static LZ4Factory factory = LZ4Factory.fastestInstance();
	private static LZ4Compressor compressor = factory.fastCompressor();
	private static ThreadLocal<byte[]> compressedDataBuffer = new ThreadLocal<byte[]>()
	{
		@Override
		protected byte[] initialValue()
		{
			return new byte[32 * 32 * 32 * 4];
		}
	};
	
	@Override
	public void compressChunkData()
	{
		if(chunk == null)
			return;
		
		final CubicChunk chunk = this.chunk;
		final int changesTakenIntoAccount = chunk.unsavedBlockModifications.get();
		
		if (!chunk.isAirChunk())
		{

			byte[] toCompressData = new byte[32 * 32 * 32 * 4];

			int[] data = chunk.chunkVoxelData;
			int z = 0;
			for (int i : data)
			{
				toCompressData[z] = (byte) ((i >>> 24) & 0xFF);
				toCompressData[z + 1] = (byte) ((i >>> 16) & 0xFF);
				toCompressData[z + 2] = (byte) ((i >>> 8) & 0xFF);
				toCompressData[z + 3] = (byte) ((i) & 0xFF);
				z += 4;
			}
			int compressedDataLength = compressor.compress(toCompressData, compressedDataBuffer.get());

			// assert decompressor.decompress(compressedData.get(), 32 * 32 * 32 * 4).length == 32 * 32 * 32 * 4;

			// Locks the compressedChunks to prevent any concurrent execution
			compressedDataLock.beginWrite();
			
			// Copy the buffer's content to a byte array of the good size
			byte[] copyBuffer = new byte[compressedDataLength];
			System.arraycopy(compressedDataBuffer.get(), 0, copyBuffer, 0, compressedDataLength);
			
			// Changes atomically the compressedData reference
			compressedData = copyBuffer;
			
			compressedDataLock.endWrite();
		}
		else
		{
			compressedDataLock.beginWrite();
			
			//Nulls out the chunk's content
			compressedData = null;
			compressedDataLock.endWrite();

		}
		
		//Remove whatever modifications existed when the method started, this is for avoiding concurrent modifications not being taken into account
		chunk.unsavedBlockModifications.addAndGet(-changesTakenIntoAccount);
		//chunk.lastModificationSaved.set(System.currentTimeMillis());
	}
	
	public byte[] getCompressedData()
	{
		return compressedData;
	}

	public void setCompressedData(byte[] compressedData)
	{
		if(this.compressedData == null && this.chunk == null)
			this.compressedData = compressedData;
		else
		{
			System.out.println("Setting a chunk's compressed data but ");
			if(compressedData != null)
				System.out.println("it already has got that");
			if(chunk != null)
				System.out.println("it already has got a chunk");
		}
	}
	
	/*private void loadChunk()
	{
		if(loadChunkTask == null)// || loadChunkTask.get() == null)
			loadChunkTask = getRegion().getWorld().ioHandler.requestChunkLoad(this);
			//loadChunkTask = new WeakReference<IOTask>(getRegion().getWorld().ioHandler.requestChunkLoad(this));
	}*/

	private void unloadChunk()
	{
		//Kill any load chunk operation that is still scheduled
		if(loadChunkTask != null)
		{
			IOTask task = loadChunkTask;//.get();
			if(task != null)
				task.cancel();
			
			loadChunkTask = null;
			//Thread.dumpStack();
		}
		
		//Compress chunk if it changed
		if(chunk != null && region.getWorld() instanceof WorldMaster && chunk.unsavedBlockModifications.get() > 0)//chunk.lastModification.get() > chunk.lastModificationSaved.get())
			compressChunkData();

		//Unlist it and destroy it
		if(chunk != null) {
			regionLoadedChunks.remove(chunk);
			chunk.destroy();
		}
		
		//Null-out reference
		chunk = null;
	}

	@Override
	public Iterator<WorldUser> getChunkUsers()
	{
		return new Iterator<WorldUser>()
		{
			Iterator<WeakReference<WorldUser>> i = users.iterator();
			WorldUser user;

			@Override
			public boolean hasNext()
			{
				while(user == null && i.hasNext())
				{
					user = i.next().get();
				}
				return user != null;
			}

			@Override
			public WorldUser next()
			{
				hasNext();
				WorldUser u = user;
				user = null;
				return u;
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				return false;
		}
		
		//if(users.size() == 1)
		//	System.out.println("red flag");
		
		users.add(new WeakReference<WorldUser>(user));
		
		//This runs under a lock so we can afford to be lazy about thread safety
		if(chunk == null && loadChunkTask == null) {
			//We create a task only if one isn't already ongoing.
			loadChunkTask = getRegion().getWorld().ioHandler.requestChunkLoad(this);
		}
		
		return true;
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				i.remove();
		}
		
		if(users.isEmpty())
		{
			unloadChunk();
			return true;
		}
		
		return false;
	}

	/**
	 * Iterates over users references, cleans null ones and if the result is an empty list it promptly unloads the chunk.
	 */
	public boolean unloadsIfUnused()
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			
			//System.out.println("chunk used by "+u);
		}
		
		if(users.isEmpty())
		{
			unloadChunk();
			return true;
		}
		
		return false;
	}

	public int countUsers()
	{
		int c = 0;
		
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else
				c++;
		}
		
		return c;
	}

	@Override
	public CubicChunk getChunk()
	{
		return chunk;
	}

	@Override
	public RegionImplementation getRegion()
	{
		return region;
	}

	@Override
	public int getInRegionX()
	{
		return x & 0x7;
	}

	@Override
	public int getInRegionY()
	{
		return y & 0x7;
	}

	@Override
	public int getInRegionZ()
	{
		return z & 0x7;
	}

	public CubicChunk createChunk()
	{
		return this.createChunk(null);
	}
	
	public CubicChunk createChunk(int data[])
	{
		CubicChunk chunk = data == null ? new RenderableChunk(this, x, y, z) : new RenderableChunk(this, x, y, z, data);
		
		setChunk(chunk);
		
		return chunk;
	}
	
	public void setChunk(CubicChunk chunk)
	{
		if(this.chunk == null && chunk != null)
			regionLoadedChunks.add(chunk);
		this.chunk = chunk;
		
		if(region.getWorld() instanceof WorldClient)
			((WorldClient)region.getWorld()).getWorldRenderer().flagChunksModified();
	}

	@Override
	public int getChunkCoordinateX()
	{
		return getInRegionX() + region.getRegionX() * 8;
	}

	@Override
	public int getChunkCoordinateY()
	{
		return getInRegionY() + region.getRegionY() * 8;
	}

	@Override
	public int getChunkCoordinateZ()
	{
		return getInRegionZ() + region.getRegionZ() * 8;
	}
	
	/*@Override
	public int hashCode()
	{
		return uuid;
	}*/
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof ChunkHolderImplementation)
		{
			ChunkHolderImplementation ch = ((ChunkHolderImplementation)o);
			//boolean thoroughTest = ch.x == x && ch.y == y && ch.z == z;
			boolean fastTest = ch.uuid == uuid;

			/*if(fastTest != thoroughTest)
			{
				System.out.println("Grosse merde !"+thoroughTest+" != "+fastTest);
				System.out.println(x+" "+y+" "+z + " " + ch.uuid);
			}*/
			
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
		if(f != null)
			return f;
		
		//Return a trvial fence if the chunk is not currently loading anything
		return new TrivialFence();
	}
}
