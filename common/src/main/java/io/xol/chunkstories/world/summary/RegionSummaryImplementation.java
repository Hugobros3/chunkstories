//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.summary;

import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.World.WorldCell;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.Cell;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.net.packets.PacketRegionSummary;
import io.xol.chunkstories.util.concurrency.SimpleFence;
import io.xol.chunkstories.util.concurrency.TrivialFence;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.io.IOTasks.IOTask;

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
	private final AtomicBoolean summaryUnloaded = new AtomicBoolean(false);

	private int[] heights = null;
	private int[] ids = null;
	
	public int[][] min, max;

	//Textures (client renderer)
	public final AtomicBoolean texturesUpToDate = new AtomicBoolean(false);

	//public final Texture2D heightsTexture;
	//public final Texture2D voxelTypesTexture;
	
	protected final Fence loadFence;

	/** The offsets in an array containing sequentially each mipmaps of a square texture of base size 256 */
	public final static int[] mainMimpmapOffsets = { 0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381 };
	
	/** The offsets in an array containing sequentially each mipmaps of a square texture of base size 128 */
	public final static int[] minHeightMipmapOffsets = {0, 16384, 20480, 21504, 21760, 21824, 21840, 21844, 21845};

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

	@SuppressWarnings("deprecation")
	public void updateOnBlockModification(int worldX, int height, int worldZ, FutureCell cell)
	{
		if(!this.isLoaded())
			return;
		
		worldX &= 0xFF;
		worldZ &= 0xFF;

		int h = getHeight(worldX, worldZ);
		
		//If we place something solid over the last solid thing
		if ((cell.getVoxel().getDefinition().isSolid() || cell.getVoxel().getDefinition().isLiquid()))
		{
			if (height >= h || h == RegionSummary.NO_DATA)
			{
				heights[index(worldX, worldZ)] = height;
				ids[index(worldX, worldZ)] = cell.getData();
			}
		}
		else
		{
			// If removing the top block, start a loop to find bottom.
			if (height == h)
			{
				int raw_data = cell.getData();
				
				boolean loaded = false;
				boolean solid = false;
				boolean liquid = false;
				do
				{
					height--;
					loaded = world.isChunkLoaded(worldX / 32, height / 32, worldZ / 32);

					WorldCell celli = world.peekSafely(worldX, height, worldZ);
					solid = celli.getVoxel().getDefinition().isSolid();
					liquid = celli.getVoxel().getDefinition().isLiquid();
					
					raw_data = world.peekRaw(worldX, height, worldZ);
				}
				while (height >= 0 && loaded && !solid && !liquid);

				if(loaded) {
					heights[index(worldX, worldZ)] = height;
					ids[index(worldX, worldZ)] = raw_data;
				}
			}
		}
	}

	@Override
	public void setTopCell(CellData cell)
	{
		if(!this.isLoaded())
			return;
		
		int worldX = cell.getX();
		int worldZ = cell.getZ();
		int height = cell.getY();
		
		worldX &= 0xFF;
		worldZ &= 0xFF;
		heights[index(worldX, worldZ)] = height;
		ids[index(worldX, worldZ)] = world.getContentTranslator().getIdForVoxel(cell.getVoxel());
	}

	@Override
	public int getHeight(int x, int z)
	{
		if(!this.isLoaded())
			return RegionSummary.NO_DATA;
		
		x &= 0xFF;
		z &= 0xFF;
		return heights[index(x, z)];
	}

	public int getRawVoxelData(int x, int z)
	{
		x &= 0xFF;
		z &= 0xFF;
		return ids[index(x, z)];
	}

	@Override
	public CellData getTopCell(int x, int z) {
		int raw_data = getRawVoxelData(x, z);
		return new SummaryCell(x, getHeight(x, z), z, world.getContentTranslator().getVoxelForId(VoxelFormat.id(raw_data)), VoxelFormat.sunlight(raw_data), VoxelFormat.blocklight(raw_data), VoxelFormat.meta(raw_data));
	}
	
	class SummaryCell extends Cell {

		public SummaryCell(int x, int y, int z, Voxel voxel, int meta, int blocklight, int sunlight) {
			super(x, y, z, voxel, meta, blocklight, sunlight);
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public CellData getNeightbor(int side_int) {
			VoxelSides side = VoxelSides.values()[side_int];
			return getTopCell(x + side.dx, z + side.dz);
		}
		
	}
	
	void unloadSummary()
	{
		if (summaryUnloaded.compareAndSet(false, true))
		{
			//Signal the loading fence if it's haven't been already
			if(loadFence instanceof SimpleFence)
				((SimpleFence) loadFence).signal();

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
			return RegionSummary.NO_DATA;
		if (level > 8)
			return RegionSummary.NO_DATA;
		int resolution = 256 >> level;
		x >>= level;
		z >>= level;
		int offset = mainMimpmapOffsets[level];
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
		int offset = mainMimpmapOffsets[level];
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
