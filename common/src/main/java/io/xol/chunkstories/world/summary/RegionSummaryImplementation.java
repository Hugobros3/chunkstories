package io.xol.chunkstories.world.summary;

import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.io.IOTasks.IOTask;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.concurrency.TrivialFence;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * A region summary contains metadata about an 8x8 chunks ( or 256x256 blocks ) vertical slice of the world
 */
public class RegionSummaryImplementation implements RegionSummary
{
	final WorldRegionSummariesHolder worldSummariesHolder;
	public final WorldImplementation world;
	private final int regionX;
	private final int regionZ;

	private final Set<WorldUser> users = ConcurrentHashMap.newKeySet();//new HashSet<WorldUser>();
	private final Set<RemotePlayer> usersWaitingForIntialData = new HashSet<RemotePlayer>();
	private final Lock usersLock = new ReentrantLock();

	// LZ4 compressors & decompressors
	static LZ4Factory factory = LZ4Factory.fastestInstance();
	public static LZ4Compressor compressor = factory.highCompressor(10);
	public static LZ4FastDecompressor decompressor = factory.fastDecompressor();

	//Public so IOTasks can access it
	//TODO a cleaner way
	public final File handler;
	private final AtomicBoolean summaryLoaded = new AtomicBoolean(false);
	private AtomicBoolean summaryUnloaded = new AtomicBoolean(false);

	private int[] heights = null;
	private int[] ids = null;
	
	public int[][] min, max;

	//Textures (client renderer)
	public final AtomicBoolean texturesUpToDate = new AtomicBoolean(false);

	//public final Texture2D heightsTexture;
	//public final Texture2D voxelTypesTexture;
	
	protected final Fence loadFence;

	public final static int[] offsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };

	RegionSummaryImplementation(WorldRegionSummariesHolder worldSummariesHolder, int rx, int rz)
	{
		this.worldSummariesHolder = worldSummariesHolder;
		this.world = worldSummariesHolder.getWorld();
		this.regionX = rx;
		this.regionZ = rz;

		if (world instanceof WorldMaster) {
			handler = new File(world.getFolderPath() + "/summaries/" + rx + "." + rz + ".sum");
			loadFence = this.world.ioHandler.requestRegionSummaryLoad(this);
		}
		else {
			handler = null;
			loadFence = new TrivialFence();
		}
	}

	@Override
	public int getRegionX()
	{
		return regionX;
	}

	@Override
	public int getRegionZ()
	{
		return regionZ;
	}

	@Override
	public IterableIterator<WorldUser> getSummaryUsers()
	{
		return new IterableIterator<WorldUser>()
		{
			Iterator<WorldUser> i = users.iterator();

			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public WorldUser next()
			{
				return i.next();
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user)
	{
		try {
			usersLock.lock();
			if(users.add(user)) {
				
				if(user instanceof RemotePlayer) {
					RemotePlayer player = (RemotePlayer)user;
					if(this.isLoaded()) {
						player.pushPacket(new PacketRegionSummary(this));
					} else {
						this.usersWaitingForIntialData.add(player);
					}
						
				}
				
				return true;
			}
			
		} finally {
			usersLock.unlock();
		}
		
		return false;
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user)
	{
		try {
			usersLock.lock();
			users.remove(user);
	
			if (users.isEmpty()) {
				unloadSummary();
				return true;
			}
			return false;
		} finally {
			usersLock.unlock();
		}
	}

	/**
	 * Iterates over users references, cleans null ones and if the result is an empty list it promptly unloads the chunk.
	 */
	/*public boolean unloadsIfUnused()
	{
		try {
			usersLock.lock();
	
			if (users.isEmpty())
			{
				unloadSummary();
				return true;
			}
			return false;
		} finally {
			usersLock.unlock();
		}
	}*/

	public int countUsers()
	{
		return users.size();
	}

	public IOTask saveSummary()
	{
		return this.world.ioHandler.requestRegionSummarySave(this);
	}

	private int index(int x, int z)
	{
		return x * 256 + z;
	}

	@Override
	public void updateOnBlockModification(int worldX, int height, int worldZ, int voxelData)
	{
		if(!this.isLoaded())
			return;
		
		worldX &= 0xFF;
		worldZ &= 0xFF;

		Voxel voxel = VoxelsStore.get().getVoxelById(voxelData);
		int h = getHeight(worldX, worldZ);
		//If we place something solid over the last solid thing
		if ((voxel.getType().isSolid() || voxel.getType().isLiquid()) && height >= h)
		{
			if (height >= h)
			{
				heights[index(worldX, worldZ)] = height;
				ids[index(worldX, worldZ)] = voxelData;
			}
		}
		else
		{
			// If removing the top block, start a loop to find bottom.
			if (height == h)
			{
				boolean loaded = false;
				boolean solid = false;
				boolean liquid = false;
				do
				{
					height--;
					loaded = world.isChunkLoaded(worldX / 32, height / 32, worldZ / 32);

					voxelData = world.peekSimple(worldX, height, worldZ);
					solid = VoxelsStore.get().getVoxelById(voxelData).getType().isSolid();
					liquid = VoxelsStore.get().getVoxelById(voxelData).getType().isLiquid();
				}
				while (height >= 0 && loaded && !solid && !liquid);

				heights[index(worldX, worldZ)] = height;
				ids[index(worldX, worldZ)] = voxelData;
			}
		}
	}

	@Override
	public void setHeightAndId(int worldX, int height, int worldZ, int voxelData)
	{
		if(!this.isLoaded())
			return;
		
		worldX &= 0xFF;
		worldZ &= 0xFF;
		heights[index(worldX, worldZ)] = height;
		ids[index(worldX, worldZ)] = voxelData;
	}

	@Override
	public int getHeight(int x, int z)
	{
		if(!this.isLoaded())
			return 0;
		
		x &= 0xFF;
		z &= 0xFF;
		return heights[index(x, z)];
	}

	@Override
	public int getVoxelData(int x, int z)
	{
		x &= 0xFF;
		z &= 0xFF;
		return ids[index(x, z)];
	}
	
	/*private boolean uploadTextures_()
	{
		if (heights == null)
			return false;
		
		if (texturesUpToDate.get())
			return false;

		//Upload stuff
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			bb.putFloat(heights[i]);
		}

		bb.flip(); // 0x822e is GL_R32F, 0x8235 is GL_R32I

		heightsTexture.uploadTextureData(256, 256, bb);

		//Upload stuff
		bb = ByteBuffer.allocateDirect(4 * 256 * 256);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 256 * 256; i++)
		{
			int id = ids[i];
			Voxel v = VoxelsStore.get().getVoxelById(id);
			if (v.getType().isLiquid())
				bb.putFloat(512f);
			else
				bb.putFloat(((VoxelTextureAtlased)v.getVoxelTexture(id, VoxelSides.TOP, null)).positionInColorIndex);
		}
		bb.rewind();

		voxelTypesTexture.uploadTextureData(256, 256, bb);

		//Tell world renderer
		((WorldClient) this.world).getWorldRenderer().getFarTerrainRenderer().markFarTerrainMeshDirty();

		//Flag it
		texturesUpToDate.set(true);
		return true;
	}*/

	void unloadSummary()
	{
		if (summaryUnloaded.compareAndSet(false, true))
		{
			//Signal the loading fence if it's haven't been already
			if(loadFence instanceof SimpleFence)
				((SimpleFence) loadFence).signal();
			
			/*if (world instanceof WorldClient)
			{
				heightsTexture.destroy();
				voxelTypesTexture.destroy();
			}*/

			if (!worldSummariesHolder.removeSummary(this))
			{
				System.out.println(this+" failed to be removed from the holder "+worldSummariesHolder);
			}
		}
	}

	public boolean isLoaded()
	{
		return summaryLoaded.get();
	}

	public boolean isUnloaded()
	{
		return summaryUnloaded.get();
	}

	private void computeHeightMetadata()
	{
		if(heights == null)
			return;
		
		//Max mipmaps
		int resolution = 128;
		int offset = 0;
		while (resolution > 1)
		{
			for (int x = 0; x < resolution; x++)
				for (int z = 0; z < resolution; z++)
				{
					//Fetch from the current resolution
					//int v00 = heights[offset + (resolution * 2) * (x * 2) + (z * 2)];
					//int v01 = heights[offset + (resolution * 2) * (x * 2) + (z * 2 + 1)];
					//int v10 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2)];
					//int v11 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2) + 1];

					int maxIndex = 0;
					int maxHeight = 0;
					for (int i = 0; i <= 1; i++)
						for (int j = 0; j <= 1; j++)
						{
							int locationThere = offset + (resolution * 2) * (x * 2 + i) + (z * 2) + j;
							int heightThere = heights[locationThere];

							if (heightThere >= maxHeight)
							{
								maxIndex = locationThere;
								maxHeight = heightThere;
							}
						}

					//int maxHeight = max(max(v00, v01), max(v10, v11));

					//Skip the already passed steps and the current resolution being sampled data to go write the next one
					heights[offset + (resolution * 2) * (resolution * 2) + resolution * x + z] = maxHeight;
					ids[offset + (resolution * 2) * (resolution * 2) + resolution * x + z] = ids[maxIndex];
				}

			offset += resolution * 2 * resolution * 2;
			resolution /= 2;
		}
	}

	public int getHeightMipmapped(int x, int z, int level)
	{
		if(!this.isLoaded())
			return -1;
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		return heights[offset + resolution * x + z];
	}

	public int getDataMipmapped(int x, int z, int level)
	{
		if(!this.isLoaded())
			return -1;
		if (level > 8)
			return -1;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = offsets[level];
		return ids[offset + resolution * x + z];
	}

	public int[] getHeightData()
	{
		return heights;
	}

	public int[] getVoxelData()
	{
		return ids;
	}
	
	public void setSummaryData(int[] heightData, int[] voxelData)
	{
		//texturesUpToDate.set(false);
		
		// 512kb per summary, use of max mipmaps for heights
		heights = new int[(int) Math.ceil(256 * 256 * (1 + 1 / 3D))];
		ids = new int[(int) Math.ceil(256 * 256 * (1 + 1 / 3D))];
		
		System.arraycopy(heightData, 0, heights, 0, 256 * 256);
		System.arraycopy(voxelData, 0, ids, 0, 256 * 256);
		
		recomputeMetadata();
		
		summaryLoaded.set(true);
		
		if(world instanceof WorldClient) {
			((WorldClient)world).getWorldRenderer().getSummariesTexturesHolder().warnDataHasArrived(regionX, regionZ);
		}
		
		// Already have clients waiting for it ? Satisfy these messieurs
		usersLock.lock();
		for(RemotePlayer user : usersWaitingForIntialData) {
			user.pushPacket(new PacketRegionSummary(this));
		}
		usersWaitingForIntialData.clear();
		usersLock.unlock();
	}
	
	private void recomputeMetadata() {
		this.computeHeightMetadata();
		this.computeMinMax();
		
		//if(world instanceof WorldClient)
		//	uploadTextures();
	}

	private void computeMinMax()
	{
		min = new int[8][8];
		max = new int[8][8];
		
		for(int i = 0; i < 8; i++)
			for(int j = 0; j < 8; j++)
			{
				int minl = Integer.MAX_VALUE;
				int maxl = 0;
				for(int a = 0; a < 32; a++)
					for(int b = 0; b < 32; b++)
						{
							int h = heights[index(i * 32 + a, j * 32 + b)];
							if(h > maxl)
								maxl = h;
							if(h < minl)
								minl = h;
						}
				min[i][j] = minl;
				max[i][j] = maxl;
			}
		
	}

	@Override
	public Fence waitForLoading() {
		return this.loadFence;
	}

	@Override
	public String toString() {
		return "[RegionSummary x:"+regionX+" z:"+regionZ+" users: "+this.countUsers()+" loaded: "+this.isLoaded()+" zombie: "+this.isUnloaded()+"]";
	}
	
	
}
