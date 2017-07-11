package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class CubicChunk implements Chunk
{
	final protected WorldImplementation world;
	final protected ChunkHolderImplementation chunkHolder;
	
	final protected RegionImplementation holdingRegion;
	
	final protected int chunkX;
	final protected int chunkY;
	final protected int chunkZ;
	final protected int uuid;

	//Actual data holding here
	public int[] chunkVoxelData = null;
	
	//Count unsaved edits atomically, fancy :]
	public AtomicInteger unsavedBlockModifications = new AtomicInteger();
	
	public AtomicBoolean needRelightning = new AtomicBoolean(true);

	// Terrain Generation
	// public List<GenerableStructure> structures = new ArrayList<GenerableStructure>();

	// Occlusion lookup, there are 6 sides you can enter a chunk by and 5 sides you can exit it by. we use 6 coz it's easier and who the fuck cares about a six-heights of a byte
	public boolean occlusionSides[][] = new boolean[6][6];

	static final int sunlightMask = 0x000F0000;
	static final int blocklightMask = 0x00F00000;
	
	static final int sunAntiMask = 0xFFF0FFFF;
	static final int blockAntiMask = 0xFF0FFFFF;

	static final int sunBitshift = 0x10;
	static final int blockBitshift = 0x14;
	
	private Semaphore chunkDataArrayCreation = new Semaphore(1);
	
	//These wonderfull things does magic for us, they are unique per-thread so they won't ever clog memory neither will they have contigency issues
	//Seriously awesome
	static ThreadLocal<Deque<Integer>> blockSources = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};
	static ThreadLocal<Deque<Integer>> sunSources = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};
	static ThreadLocal<Deque<Integer>> blockSourcesRemoval = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};
	static ThreadLocal<Deque<Integer>> sunSourcesRemoval = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};

	public CubicChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ)
	{
		this.chunkHolder = holder;
		
		this.holdingRegion = holder.getRegion();
		this.world = holdingRegion.getWorld();
		
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;
		
		this.uuid = ((chunkX << world.getWorldInfo().getSize().bitlengthOfVerticalChunksCoordinates) | chunkY ) << world.getWorldInfo().getSize().bitlengthOfHorizontalChunksCoordinates | chunkZ;
	}

	public CubicChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ, int[] data)
	{
		this(holder, chunkX, chunkY, chunkZ);

		assert data.length == 32 * 32 * 32;

		this.chunkVoxelData = data;
		computeOcclusionTable();
	}

	static ThreadLocal<Deque<Integer>> occlusionFaces = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};

	private void computeOcclusionTable()
	{
		//System.out.println("Computing occlusion table ...");
		occlusionSides = new boolean[6][6];

		Deque<Integer> deque = occlusionFaces.get();
		deque.clear();
		boolean[] mask = new boolean[32768];
		int x = 0, y = 0, z = 0;
		int completion = 0;
		int p = 0;
		
		@SuppressWarnings("unused")
		int bits = 0;
		//Until all 32768 blocks have been processed
		while (completion < 32768)
		{
			//If this face was already done, we find one that wasn't
			while (mask[x * 1024 + y * 32 + z])
			{
				p++;
				p %= 32768;

				x = p / 1024;
				y = (p / 32) % 32;
				z = p % 32;
			}

			bits++;
			
			//We put this face on the deque
			deque.push(x * 1024 + y * 32 + z);

			/**
			 * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3 BACK z- 4 y+ top X 5 y- bottom
			 */
			Set<Integer> touchingSides = new HashSet<Integer>();
			while (!deque.isEmpty())
			{
				//Pop the topmost element
				int d = deque.pop();

				//Don't iterate twice over one element
				if(mask[d])
					continue;
				
				//Separate coordinates
				x = d / 1024;
				y = (d / 32) % 32;
				z = d % 32;
				
				//Mark the case as done
				mask[x * 1024 + y * 32 + z] = true;
				completion++;
				
				if (!VoxelsStore.get().getVoxelById(this.getVoxelData(x, y, z)).getType().isOpaque())
				{
					//Adds touched sides to set
					
					if (x == 0)
						touchingSides.add(0);
					else if (x == 31)
						touchingSides.add(2);

					if (y == 0)
						touchingSides.add(5);
					else if (y == 31)
						touchingSides.add(4);

					if (z == 0)
						touchingSides.add(3);
					else if (z == 31)
						touchingSides.add(1);
					
					//Flood fill
					
					if(x > 0)
						deque.push((x - 1) * 1024 + (y) * 32 + (z));
					if(y > 0)
						deque.push((x) * 1024 + (y - 1) * 32 + (z));
					if(z > 0)
						deque.push((x) * 1024 + (y) * 32 + (z - 1));
					
					if(x < 31)
						deque.push((x + 1) * 1024 + (y) * 32 + (z));
					if(y < 31)
						deque.push((x) * 1024 + (y + 1) * 32 + (z));
					if(z < 31)
						deque.push((x) * 1024 + (y) * 32 + (z + 1));
				}
			}
			
			for(int i : touchingSides)
			{
				for(int j : touchingSides)
					occlusionSides[i][j] = true;
			}
		}
		
		//System.out.println("chunk "+this+" is made of "+bits+" bits");
	}

	public int getChunkX()
	{
		return chunkX;
	}

	public int getChunkY()
	{
		return chunkY;
	}

	public int getChunkZ()
	{
		return chunkZ;
	}

	private int sanitizeCoordinate(int a)
	{
		return a & 0x1F;
	}

	@Override
	public int getVoxelData(int x, int y, int z)
	{
		if (chunkVoxelData == null)
		{
			return 0;
		}
		else
		{
			x = sanitizeCoordinate(x);
			y = sanitizeCoordinate(y);
			z = sanitizeCoordinate(z);
			return chunkVoxelData[x * 32 * 32 + y * 32 + z];
		}
	}
	
	@Override
	public void setVoxelDataWithUpdates(int x, int y, int z, int data)
	{
		x = sanitizeCoordinate(x);
		y = sanitizeCoordinate(y);
		z = sanitizeCoordinate(z);
		//Allocate if it makes sense
		if (chunkVoxelData == null)
			chunkVoxelData = atomicalyCreateInternalData();

		int dataBefore = chunkVoxelData[x * 32 * 32 + y * 32 + z];
		chunkVoxelData[x * 32 * 32 + y * 32 + z] = data;
		computeLightSpread(x, y, z, dataBefore, data);
		
		unsavedBlockModifications.incrementAndGet();
		//lastModification.set(System.currentTimeMillis());

		if (dataBefore != data && this instanceof ChunkRenderable)
			((ChunkRenderable)this).markForReRender();
	}

	private int[] atomicalyCreateInternalData() {
		chunkDataArrayCreation.acquireUninterruptibly();

		//If it's STILL null
		if (chunkVoxelData == null)
			chunkVoxelData = new int[32 * 32 * 32];
		
		chunkDataArrayCreation.release();
		
		return chunkVoxelData;
	}
	
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data)
	{
		x = sanitizeCoordinate(x);
		y = sanitizeCoordinate(y);
		z = sanitizeCoordinate(z);
		//Allocate if it makes sense
		if (chunkVoxelData == null)
			chunkVoxelData = atomicalyCreateInternalData();

		chunkVoxelData[x * 32 * 32 + y * 32 + z] = data;
		
		unsavedBlockModifications.incrementAndGet();
	}

	public void setChunkData(int[] data) {
		this.chunkVoxelData = data;
		
		unsavedBlockModifications.incrementAndGet();
		
		if (this instanceof ChunkRenderable)
			((ChunkRenderable)this).markForReRender();
		
		this.markInNeedForLightningUpdate();
	}

	@Override
	public String toString()
	{
		return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " nl:" + this.needRelightning + "]";
	}

	@Override
	public void computeVoxelLightning(boolean adjacent)
	{
		// Checks first if chunk contains blocks
		if (chunkVoxelData == null)
			return; // Nothing to do

		//Lock the chunk & grab 2 queues
		Deque<Integer> blockSources = CubicChunk.blockSources.get();
		Deque<Integer> sunSources = CubicChunk.sunSources.get();

		// Reset any prospective residual data
		blockSources.clear();
		sunSources.clear();

		// Find our own light sources, add them
		this.addChunkLightSources(blockSources, sunSources);

		// Load nearby chunks and check if they contain bright spots
		if (adjacent)
			addAdjacentChunksLightSources(blockSources, sunSources);

		//Propagates the light
		int c = propagateLightning(blockSources, sunSources);

		if (c > 0 && this instanceof ChunkRenderable)
			((ChunkRenderable)this).markForReRender();

		needRelightning.set(false);
		//Return the queues after that
		//world.dequesPool.back(blockSources);
		//world.dequesPool.back(sunSources);
		//Not really jk
	}

	// Now entering lightning code part, brace yourselves
	private int propagateLightning(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		int modifiedBlocks = 0;

		//Checks if the adjacent chunks are done loading
		Chunk adjacentChunkTop = world.getChunk(chunkX, chunkY + 1, chunkZ);
		Chunk adjacentChunkBottom = world.getChunk(chunkX, chunkY - 1, chunkZ);
		Chunk adjacentChunkFront = world.getChunk(chunkX, chunkY, chunkZ + 1);
		Chunk adjacentChunkBack = world.getChunk(chunkX, chunkY, chunkZ - 1);
		Chunk adjacentChunkLeft = world.getChunk(chunkX - 1, chunkY, chunkZ);
		Chunk adjacentChunkRight = world.getChunk(chunkX + 1, chunkY, chunkZ);
		//Don't spam the requeue requests
		boolean checkTopBleeding = (adjacentChunkTop != null) && !adjacentChunkTop.needsLightningUpdates();
		boolean checkBottomBleeding = (adjacentChunkBottom != null) && !adjacentChunkBottom.needsLightningUpdates();
		boolean checkFrontBleeding = (adjacentChunkFront != null) && !adjacentChunkFront.needsLightningUpdates();
		boolean checkBackBleeding = (adjacentChunkBack != null) && !adjacentChunkBack.needsLightningUpdates();
		boolean checkLeftBleeding = (adjacentChunkLeft != null) && !adjacentChunkLeft.needsLightningUpdates();
		boolean checkRightBleeding = (adjacentChunkRight != null) && !adjacentChunkRight.needsLightningUpdates();
		Voxel in;
		while (blockSources.size() > 0)
		{
			int y = blockSources.pop();
			int z = blockSources.pop();
			int x = blockSources.pop();
			int voxelData = chunkVoxelData[x * 1024 + y * 32 + z];
			int ll = (voxelData & blocklightMask) >> blockBitshift;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelsStore.get().getVoxelById(cId);

			if (VoxelsStore.get().getVoxelById(cId).getType().isOpaque())
				ll = in.getLightLevel(voxelData);

			if (ll > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int adj = chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x + 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				else if (checkRightBleeding)
				{
					int adjacentBlocklight = (adjacentChunkRight.getVoxelData(0, y, z) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkRight.markInNeedForLightningUpdate();
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = chunkVoxelData[(x - 1) * 1024 + y * 32 + z];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x - 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adjacentBlocklight = (adjacentChunkLeft.getVoxelData(31, y, z) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkLeft.markInNeedForLightningUpdate();
						checkLeftBleeding = false;
					}
				}
				// Z-propagation
				if (z < 31)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z + 1];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z + 1] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z + 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adjacentBlocklight = (adjacentChunkFront.getVoxelData(x, y, 0) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkFront.markInNeedForLightningUpdate();
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z - 1];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z - 1] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z - 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBack.getVoxelData(x, y, 31) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkBack.markInNeedForLightningUpdate();
						checkBackBleeding = false;
					}
				}
				// Y-propagation
				if (y < 31) // y = 254+1
				{
					int adj = chunkVoxelData[x * 1024 + (y + 1) * 32 + z];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y + 1);
						//blockSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adjacentBlocklight = (adjacentChunkTop.getVoxelData(x, 0, z) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkTop.markInNeedForLightningUpdate();
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = adj & blockAntiMask | (ll - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y - 1);
						//blockSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBottom.getVoxelData(x, 31, z) & blockAntiMask) << blockBitshift;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkBottom.markInNeedForLightningUpdate();
						checkBottomBleeding = false;
					}
				}
			}
		}
		// Sunlight propagation
		while (sunSources.size() > 0)
		{
			int y = sunSources.pop();
			int z = sunSources.pop();
			int x = sunSources.pop();

			int voxelData = chunkVoxelData[x * 1024 + y * 32 + z];
			int ll = (voxelData & sunlightMask) >> sunBitshift;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelsStore.get().getVoxelById(cId);

			if (in.getType().isOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int adj = chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = adj & sunAntiMask | (llRight - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x + 1);
						sunSources.push(z);
						sunSources.push(y);
					}
				}
				else if (checkRightBleeding)
				{
					int adj = adjacentChunkRight.getVoxelData(0, y, z);
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					//int adjacentSunlight = (adjacentChunkRight.getDataAt(0, y, z) & sunAntiMask) << sunBitshift;
					if (((adj & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						adjacentChunkRight.markInNeedForLightningUpdate();
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = chunkVoxelData[(x - 1) * 1024 + y * 32 + z];
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.LEFT);
					//int id = (adj & 0xFFFF);
					//if(id == 25)
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).getType().isOpaque() + " -> " +((adj & sunlightMask) >> sunBitshift));
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).getType().isOpaque() + " -> " +((adj & sunlightMask) >> sunBitshift));
						chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = adj & sunAntiMask | (llLeft - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x - 1);
						sunSources.push(z);
						sunSources.push(y);
						//sunSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adj = adjacentChunkLeft.getVoxelData(31, y, z);
					//int adjacentSunlight = (adjacentChunkLeft.getDataAt(31, y, z) & sunAntiMask) << sunBitshift;
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.LEFT);
					if (((adj & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						adjacentChunkLeft.markInNeedForLightningUpdate();
						checkLeftBleeding = false;
					}
				}
				// Z-propagation
				if (z < 31)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z + 1];
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.FRONT);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z + 1] = adj & sunAntiMask | (llFront - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z + 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adj = adjacentChunkFront.getVoxelData(x, y, 0);
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.FRONT);
					//int adjacentSunlight = (adjacentChunkFront.getDataAt(x, y, 0) & sunAntiMask) << sunBitshift;
					if (((adj & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						adjacentChunkFront.markInNeedForLightningUpdate();
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z - 1];
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z - 1] = adj & sunAntiMask | (llBack - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z - 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					//int adjacentSunlight = (adjacentChunkBack.getDataAt(x, y, 31) & sunAntiMask) << sunBitshift;
					int adj = adjacentChunkBack.getVoxelData(x, y, 31);
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (((adj & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						adjacentChunkBack.markInNeedForLightningUpdate();
						checkBackBleeding = false;
					}
				}
				// Y-propagation
				if (y < 31) // y = 254+1
				{
					int adj = chunkVoxelData[x * 1024 + (y + 1) * 32 + z];
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.TOP);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = adj & sunAntiMask | (llTop - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y + 1);
						//sunSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adj = adjacentChunkTop.getVoxelData(x, 0, z);
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.TOP);
					//int adjacentSunlight = (adj & sunAntiMask) << sunBitshift;
					if (((adj & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						adjacentChunkTop.markInNeedForLightningUpdate();
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BOTTOM);
					if (!VoxelsStore.get().getVoxelById(adj).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = adj & sunAntiMask | (llBottm /* - removed */) << sunBitshift;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y - 1);
						//sunSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adj = adjacentChunkBottom.getVoxelData(x, 31, z);
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BOTTOM);
					//int adjacentSunlight = (adj & sunAntiMask) << sunBitshift;
					if (((adj & sunlightMask) >> sunBitshift) < llBottm - 1)
					{
						adjacentChunkBottom.markInNeedForLightningUpdate();
						checkBottomBleeding = false;
					}
				}
			}
		}

		return modifiedBlocks;
	}

	private void addChunkLightSources(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++)
			{
				int z = 31; // This is basically wrong since we work with cubic chunks
				boolean hit = false;
				int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + a, chunkZ * 32 + b) + 1;
				while (z >= 0)
				{
					int block = chunkVoxelData[a * 1024 + z * 32 + b];
					int id = VoxelFormat.id(block);
					short ll = VoxelsStore.get().getVoxelById(id).getLightLevel(block);
					if (ll > 0)
					{
						chunkVoxelData[a * 1024 + z * 32 + b] = chunkVoxelData[a * 1024 + z * 32 + b] & blockAntiMask | ((ll & 0xF) << blockBitshift);
						//blockSources.push(a << 16 | b << 8 | z);
						blockSources.push(a);
						blockSources.push(b);
						blockSources.push(z);
					}
					if (!hit)
					{
						if (chunkY * 32 + z >= csh)
						{
							chunkVoxelData[a * 1024 + (z) * 32 + b] = chunkVoxelData[a * 1024 + (z) * 32 + b] & sunAntiMask | (15 << sunBitshift);
							//sunSources.push(a << 16 | b << 8 | z);
							sunSources.push(a);
							sunSources.push(b);
							sunSources.push(z);
							if (chunkY * 32 + z < csh || VoxelsStore.get().getVoxelById(VoxelFormat.id(chunkVoxelData[a * 1024 + (z) * 32 + b])).getId() != 0)
							{
								hit = true;
							}
							//check_em++;
						}
					}
					z--;
				}
			}
	}

	private void addAdjacentChunksLightSources(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		if (world != null)
		{
			Chunk cc;
			cc = world.getChunk(chunkX + 1, chunkY, chunkZ);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(0, c, b);
						int current_data = getVoxelData(31, c, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(31, c, b, ndata);
							blockSources.push(31);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(31 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(31, c, b, ndata);

							sunSources.push(31);
							sunSources.push(b);
							sunSources.push(c);
							//sunSources.push(31 << 16 | b << 8 | c);
						}
					}
			}
			cc = world.getChunk(chunkX - 1, chunkY, chunkZ);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(31, c, b);
						int current_data = getVoxelData(0, c, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(0, c, b, ndata);

							blockSources.push(0);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(0 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(0, c, b, ndata);

							sunSources.push(0);
							sunSources.push(b);
							sunSources.push(c);
							//sunSources.push(0 << 16 | b << 8 | c);
						}
					}
			}
			// Top chunk
			cc = world.getChunk(chunkX, chunkY + 1, chunkZ);
			if (cc != null && !cc.isAirChunk())// && chunkVoxelData != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(c, 0, b);
						int current_data = getVoxelData(c, 31, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(c, 31, b, ndata);
							if (adjacent_blo > 2)
							{
								blockSources.push(c);
								blockSources.push(b);
								blockSources.push(31);
								//blockSources.push(c << 16 | b << 8 | 31);
							}
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(c, 31, b, ndata);
							//System.out.println(cc + " : "+adjacent_sun);
							if (adjacent_sun > 2)
							{
								sunSources.push(c);
								sunSources.push(b);
								sunSources.push(31);
								//sunSources.push(c << 16 | b << 8 | 31);
							}
						}
					}
			}
			else
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int heightInSummary = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + b, chunkZ * 32 + c);
						
						//If the top chunk is air
						if(heightInSummary <= this.chunkY * 32 + 32)
						{
							//int adjacent_data = cc.getVoxelData(c, 0, b);
							int current_data = getVoxelData(c, 31, b);
	
							int adjacent_blo = 0;//((adjacent_data & blocklightMask) >>> blockBitshift);
							int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
							int adjacent_sun = 15;//((adjacent_data & sunlightMask) >>> sunBitshift);
							int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
							if (adjacent_blo > 1 && adjacent_blo > current_blo)
							{
								int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
								setVoxelDataWithoutUpdates(c, 31, b, ndata);
								if (adjacent_blo > 2)
								{
									blockSources.push(c);
									blockSources.push(b);
									blockSources.push(31);
									//blockSources.push(c << 16 | b << 8 | 31);
								}
							}
							if (adjacent_sun > 1 && adjacent_sun > current_sun)
							{
								int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
								setVoxelDataWithoutUpdates(c, 31, b, ndata);
								//System.out.println(cc + " : "+adjacent_sun);
								if (adjacent_sun > 2)
								{
									sunSources.push(c);
									sunSources.push(b);
									sunSources.push(31);
									//sunSources.push(c << 16 | b << 8 | 31);
								}
							}
						}
					}
				
				/*for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int heightInSummary = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + b, chunkZ * 32 + c);
						// System.out.println("compute "+heightInSummary+" <= ? "+chunkY*32);
						if (heightInSummary <= chunkY * 32 + 32)
						{
							int sourceAt = chunkY * 32 - heightInSummary;
							sourceAt = Math.min(31, sourceAt);
							int current_data = getVoxelData(b, sourceAt, c);

							int ndata = current_data & sunAntiMask | (15) << sunBitshift;
							setVoxelDataWithoutUpdates(b, sourceAt, c, ndata);

							sunSources.push(b);
							sunSources.push(c);
							sunSources.push(sourceAt);
							//sunSources.push(b << 16 | c << 8 | sourceAt);
							// System.out.println("Added sunsource cause summary etc");
						}
					}*/
			}
			// Bottom chunk
			cc = world.getChunk(chunkX, chunkY - 1, chunkZ);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(c, 31, b);
						int current_data = getVoxelData(c, 0, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(c, 0, b, ndata);
							if (adjacent_blo > 2)
							{
								blockSources.push(c);
								blockSources.push(b);
								blockSources.push(0);
								//blockSources.push(c << 16 | b << 8 | 0);
							}
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(c, 0, b, ndata);
							if (adjacent_sun > 2)
							{
								sunSources.push(c);
								sunSources.push(b);
								sunSources.push(0);
								//sunSources.push(c << 16 | b << 8 | 0);
							}
						}
					}
			}
			// Z
			cc = world.getChunk(chunkX, chunkY, chunkZ + 1);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(c, b, 0);
						int current_data = getVoxelData(c, b, 31);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(c, b, 31, ndata);
							blockSources.push(c);
							blockSources.push(31);
							blockSources.push(b);
							//blockSources.push(c << 16 | 31 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(c, b, 31, ndata);
							sunSources.push(c);
							sunSources.push(31);
							sunSources.push(b);
							//sunSources.push(c << 16 | 31 << 8 | b);
						}
					}
			}
			cc = world.getChunk(chunkX, chunkY, chunkZ - 1);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(c, b, 31);
						int current_data = getVoxelData(c, b, 0);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							setVoxelDataWithoutUpdates(c, b, 0, ndata);
							blockSources.push(c);
							blockSources.push(0);
							blockSources.push(b);
							//blockSources.push(c << 16 | 0 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							setVoxelDataWithoutUpdates(c, b, 0, ndata);
							sunSources.push(c);
							sunSources.push(0);
							sunSources.push(b);
							//sunSources.push(c << 16 | 0 << 8 | b);
						}
					}
			}
		}
	}

	private void computeLightSpread(int bx, int by, int bz, int dataBefore, int data)
	{
		int sunLightBefore = VoxelFormat.sunlight(dataBefore);
		int blockLightBefore = VoxelFormat.blocklight(dataBefore);

		int sunLightAfter = VoxelFormat.sunlight(data);
		int blockLightAfter = VoxelFormat.blocklight(data);

		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx + chunkX * 32, bz + chunkZ * 32);
		int block_height = by + chunkY * 32;

		//If the block is at or above (never) the topmost tile it's sunlit
		if (block_height >= csh)
			sunLightAfter = 15;

		Deque<Integer> blockSourcesRemoval = CubicChunk.blockSourcesRemoval.get();
		Deque<Integer> sunSourcesRemoval = CubicChunk.sunSourcesRemoval.get();
		Deque<Integer> blockSources = CubicChunk.blockSources.get();
		Deque<Integer> sunSources = CubicChunk.sunSources.get();

		/*Deque<Integer> blockSourcesRemoval = world.dequesPool.grab();
		Deque<Integer> sunSourcesRemoval = world.dequesPool.grab();
		Deque<Integer> blockSources = world.dequesPool.grab();
		Deque<Integer> sunSources = world.dequesPool.grab();*/

		blockSourcesRemoval.push(bx);
		blockSourcesRemoval.push(by);
		blockSourcesRemoval.push(bz);
		blockSourcesRemoval.push(blockLightBefore);

		sunSourcesRemoval.push(bx);
		sunSourcesRemoval.push(by);
		sunSourcesRemoval.push(bz);
		sunSourcesRemoval.push(sunLightBefore);

		propagateLightRemovalBeyondChunks(blockSources, sunSources, blockSourcesRemoval, sunSourcesRemoval);

		//Add light sources if relevant
		if (sunLightAfter > 0)
		{
			sunSources.push(bx);
			sunSources.push(bz);
			sunSources.push(by);
		}
		if (blockLightAfter > 0)
		{
			blockSources.push(bx);
			blockSources.push(bz);
			blockSources.push(by);
		}

		//Propagate remaining light
		this.propagateLightningBeyondChunk(blockSources, sunSources);

		//Return the queues after that

		/*world.dequesPool.back(blockSourcesRemoval);
		world.dequesPool.back(sunSourcesRemoval);
		world.dequesPool.back(blockSources);
		world.dequesPool.back(sunSources);*/
	}

	@SuppressWarnings("unused")
	private void propagateLightRemovalLocal(Deque<Integer> blockSources, Deque<Integer> sunSources, Deque<Integer> blockSourcesRemoval, Deque<Integer> sunSourcesRemoval)
	{
		while (sunSourcesRemoval.size() > 0)
		{
			int sunLightLevel = sunSourcesRemoval.pop();
			int z = sunSourcesRemoval.pop();
			int y = sunSourcesRemoval.pop();
			int x = sunSourcesRemoval.pop();

			int neighborSunLightLevel;

			// X Axis
			if (x > 0)
			{
				neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.push(x - 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x - 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			if (x < 31)
			{
				neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.push(x + 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x + 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			// Y axis
			if (y > 0)
			{
				neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel)
				{
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y - 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y - 1);
				}
			}
			if (y < 31)
			{
				neighborSunLightLevel = this.getSunLight(x, y + 1, z);

				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y + 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y + 1);
				}
			}
			// Z Axis
			if (z > 0)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z - 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z - 1);
					sunSources.push(y);
				}
			}
			if (z < 31)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z + 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z + 1);
					sunSources.push(y);
				}
			}
		}

		while (blockSourcesRemoval.size() > 0)
		{
			int blockLightLevel = blockSourcesRemoval.pop();
			int z = blockSourcesRemoval.pop();
			int y = blockSourcesRemoval.pop();
			int x = blockSourcesRemoval.pop();

			int neighborBlockLightLevel;

			// X Axis
			if (x > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				//System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.push(x - 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x - 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			if (x < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.push(x + 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x + 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			// Y axis
			if (y > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y - 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y - 1);
				}
			}
			if (y < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y + 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y + 1);
				}
			}
			// Z Axis
			if (z > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z - 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z - 1);
					blockSources.push(y);
				}
			}
			if (z < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z + 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z + 1);
					blockSources.push(y);
				}
			}
		}
	}

	private void propagateLightRemovalBeyondChunks(Deque<Integer> blockSources, Deque<Integer> sunSources, Deque<Integer> blockSourcesRemoval, Deque<Integer> sunSourcesRemoval)
	{
		int bounds = 64;
		while (sunSourcesRemoval.size() > 0)
		{
			int sunLightLevel = sunSourcesRemoval.pop();
			int z = sunSourcesRemoval.pop();
			int y = sunSourcesRemoval.pop();
			int x = sunSourcesRemoval.pop();

			int neighborSunLightLevel;

			// X Axis
			if (x > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.push(x - 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x - 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			if (x < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.push(x + 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x + 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			// Y axis
			if (y > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel)
				{
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y - 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y - 1);
				}
			}
			if (y < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y + 1, z);

				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y + 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y + 1);
				}
			}
			// Z Axis
			if (z > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z - 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z - 1);
					sunSources.push(y);
				}
			}
			if (z < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z + 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z + 1);
					sunSources.push(y);
				}
			}
		}

		while (blockSourcesRemoval.size() > 0)
		{
			int blockLightLevel = blockSourcesRemoval.pop();
			int z = blockSourcesRemoval.pop();
			int y = blockSourcesRemoval.pop();
			int x = blockSourcesRemoval.pop();

			int neighborBlockLightLevel;

			// X Axis
			if (x > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				//System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.push(x - 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x - 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			if (x < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.push(x + 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x + 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			// Y axis
			if (y > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y - 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y - 1);
				}
			}
			if (y < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y + 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y + 1);
				}
			}
			// Z Axis
			if (z > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z - 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z - 1);
					blockSources.push(y);
				}
			}
			if (z < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z + 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z + 1);
					blockSources.push(y);
				}
			}
		}
	}

	private int propagateLightningBeyondChunk(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		//int data[] = world.chunksData.grab(dataPointer);
		int modifiedBlocks = 0;
		int bounds = 64;

		// The ints are composed as : 0x0BSMIIII
		// Second pass : loop fill bfs algo
		Voxel in;
		while (blockSources.size() > 0)
		{
			int y = blockSources.pop();
			int z = blockSources.pop();
			int x = blockSources.pop();
			int voxelData = getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z);
			int ll = (voxelData & blocklightMask) >> blockBitshift;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelsStore.get().getVoxelById(cId);

			if (VoxelsStore.get().getVoxelById(cId).getType().isOpaque())
				ll = in.getLightLevel(voxelData);

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x + 1, y, z);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x + 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x - 1, y, z);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x - 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				// Z-propagation
				if (z < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z + 1);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z + 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z - 1);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z - 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				// Y-propagation
				if (y < bounds) // y = 254+1
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y + 1, z);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y + 1);
						//blockSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y - 1, z);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y - 1);
						//blockSources.push(x << 16 | z << 8 | y - 1);
					}
				}
			}
		}
		// Sunlight propagation
		while (sunSources.size() > 0)
		{
			int y = sunSources.pop();
			int z = sunSources.pop();
			int x = sunSources.pop();

			int voxelData = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z);
			int ll = (voxelData & sunlightMask) >> sunBitshift;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelsStore.get().getVoxelById(cId);

			if (in.getType().isOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x + 1, y, z);
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & sunAntiMask | (llRight - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x + 1);
						sunSources.push(z);
						sunSources.push(y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x - 1, y, z);
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.LEFT);
					//int id = (adj & 0xFFFF);
					//if(id == 25)
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).getType().isOpaque() + " -> " +((adj & sunlightMask) >> sunBitshift));
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).getType().isOpaque() + " -> " +((adj & sunlightMask) >> sunBitshift));
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & sunAntiMask | (llLeft - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x - 1);
						sunSources.push(z);
						sunSources.push(y);
						//sunSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				// Z-propagation
				if (z < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z + 1);
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.FRONT);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & sunAntiMask | (llFront - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z + 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z - 1);
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & sunAntiMask | (llBack - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z - 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				// Y-propagation
				if (y < bounds) // y = 254+1
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y + 1, z);
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.TOP);
					if (!VoxelsStore.get().getVoxelById((adj & 0xFFFF)).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & sunAntiMask | (llTop - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y + 1);
						//sunSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y - 1, z);
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BOTTOM);
					if (!VoxelsStore.get().getVoxelById(adj).getType().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & sunAntiMask | (llBottm /* - removed */) << sunBitshift);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y - 1);
						//sunSources.push(x << 16 | z << 8 | y - 1);
					}
				}
			}
		}

		return modifiedBlocks;
	}

	private int getWorldDataOnlyForLightningUpdatesFuncitons(int x, int y, int z)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					this.getVoxelData(x, y, z);
		return world.getVoxelData(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32);
	}

	private void setWorldDataOnlyForLightningUpdatesFunctions(int x, int y, int z, int data)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					this.setVoxelDataWithoutUpdates(x, y, z, data);
					return;
				}

		int oldData = world.getVoxelData(x, y, z);
		world.setVoxelDataWithoutUpdates(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, data);
		
		Chunk c = world.getChunk((x + chunkX * 32) / 32, (y + chunkY * 32) / 32, (z + chunkZ * 32) / 32);
		if (c != null && oldData != data)
			c.markInNeedForLightningUpdate();
	}

	@Override
	public int getSunLight(int x, int y, int z)
	{
		//if(this.dataPointer == -1)
		//	return y <= world.getRegionSummaries().getHeightAt(chunkX * 32 + x, chunkZ * 32 + z) ? 0 : 15;

		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					return VoxelFormat.sunlight(this.getVoxelData(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.sunlight(this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z));
	}

	@Override
	public int getBlockLight(int x, int y, int z)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					return VoxelFormat.blocklight(this.getVoxelData(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.blocklight(this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z));
	}

	public boolean isAirChunk()
	{
		return chunkVoxelData == null;
	}

	@Override
	public void setSunLight(int x, int y, int z, int level)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					this.setVoxelDataWithoutUpdates(x, y, z, VoxelFormat.changeSunlight(this.getVoxelData(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z, VoxelFormat.changeSunlight(this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z), level));
	}

	@Override
	public void setBlockLight(int x, int y, int z, int level)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					this.setVoxelDataWithoutUpdates(x, y, z, VoxelFormat.changeBlocklight(this.getVoxelData(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z, VoxelFormat.changeBlocklight(this.getWorldDataOnlyForLightningUpdatesFuncitons(x, y, z), level));
	}

	@Override
	public World getWorld()
	{
		return world;
	}

	@Override
	public Region getRegion()
	{
		return holdingRegion;
	}
	
	public ChunkHolderImplementation holder()
	{
		return chunkHolder;
	}

	@Override
	public boolean needsLightningUpdates()
	{
		return needRelightning.get();
	}

	@Override
	public void markInNeedForLightningUpdate()
	{
		this.needRelightning.set(true);
	}
	
	@Override
	public IterableIterator<Entity> getEntitiesWithinChunk()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashCode()
	{
		return uuid;
	}

	@Override
	public ChunkVoxelContext peek(Vector3dm location)
	{
		return peek((int)(double)location.getX(), (int)(double)location.getY(), (int)(double)location.getZ());
	}

	@Override
	public ChunkVoxelContext peek(int x, int y, int z)
	{
		x &= 0xf;
		y &= 0xf;
		z &= 0xf;
		return new ActualChunkVoxelContext(x, y, z);
	}
	
	class ActualChunkVoxelContext implements ChunkVoxelContext {
		
		final int x, y, z;
		final int data;
		
		public ActualChunkVoxelContext(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			
			this.data = CubicChunk.this.getVoxelData(x, y, z);
		}

		@Override
		public World getWorld()
		{
			return world;
		}

		@Override
		public Location getLocation()
		{
			return new Location(world, getX(), getY(), getZ());
		}

		@Override
		public Voxel getVoxel()
		{
			return world.getGameContext().getContent().voxels().getVoxelById(data);
		}

		@Override
		public int getData()
		{
			return data;
		}

		@Override
		public int getX()
		{
			return CubicChunk.this.getChunkX() * 32 + x;
		}

		@Override
		public int getY()
		{
			return CubicChunk.this.getChunkX() * 32 + y;
		}

		@Override
		public int getZ()
		{
			return CubicChunk.this.getChunkX() * 32 + z;
		}

		@Override
		public int getNeightborData(int side)
		{
			switch (side)
			{
			case (0):
				return world.getVoxelData(getX() - 1, getY(), getZ());
			case (1):
				return world.getVoxelData(getX(), getY(), getZ() + 1);
			case (2):
				return world.getVoxelData(getX() + 1, getY(), getZ());
			case (3):
				return world.getVoxelData(getX(), getY(), getZ() - 1);
			case (4):
				return world.getVoxelData(getX(), getY() + 1, getZ());
			case (5):
				return world.getVoxelData(getX(), getY() - 1, getZ());
			}
			throw new RuntimeException("Fuck off");
		}

		@Override
		public Chunk getChunk()
		{
			return CubicChunk.this;
		}
	}

	@Override
	public void destroy() {
		//Nothing to do
	}
}
