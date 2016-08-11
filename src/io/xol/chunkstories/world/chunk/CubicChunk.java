package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.voxel.VoxelTypes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class CubicChunk implements Chunk, ChunkRenderable
{
	public World world;
	public Region holder;
	private int chunkX;
	private int chunkY;
	private int chunkZ;

	public int[] chunkVoxelData = null;

	// Used in client rendering
	private ChunkRenderData chunkRenderData;
	public AtomicLong lastModification = new AtomicLong();
	public AtomicLong lastModificationSaved = new AtomicLong();

	public AtomicBoolean need_render = new AtomicBoolean(true);
	public AtomicBoolean need_render_fast = new AtomicBoolean(false);
	public AtomicBoolean requestable = new AtomicBoolean(true);

	public AtomicBoolean needRelightning = new AtomicBoolean(true);

	// Terrain Generation
	// public List<GenerableStructure> structures = new ArrayList<GenerableStructure>();

	// Occlusion lookup, there are 6 sides you can enter a chunk by and 5 sides you can exit it by. we use 6 coz it's easier and who the fuck cares about a six-heights of a byte
	public boolean occlusionSides[][] = new boolean[6][6];

	/*boolean occludedTop = false;
	boolean occludedBot = false;
	
	boolean occludedNorth = false;
	boolean occludedSouth = false;
	
	boolean occludedLeft = false;
	boolean occludedRight = false;*/

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

	public CubicChunk(Region holder, int chunkX, int chunkY, int chunkZ)
	{
		this.holder = holder;
		this.world = holder.getWorld();
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;
	}

	public CubicChunk(Region holder, int chunkX, int chunkY, int chunkZ, int[] data)
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
				
				if (!VoxelTypes.get(this.getVoxelData(x, y, z)).isVoxelOpaque())
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

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Chunk#setDataAt(int, int, int, int)
	 */
	@Override
	public void setVoxelDataWithUpdates(int x, int y, int z, int data)
	{
		x = sanitizeCoordinate(x);
		y = sanitizeCoordinate(y);
		z = sanitizeCoordinate(z);
		//Allocate if it makes sense
		if (chunkVoxelData == null)
			chunkVoxelData = new int[32 * 32 * 32];

		int dataBefore = chunkVoxelData[x * 32 * 32 + y * 32 + z];
		chunkVoxelData[x * 32 * 32 + y * 32 + z] = data;
		computeLightSpread(x, y, z, dataBefore, data);
		lastModification.set(System.currentTimeMillis());

		if (dataBefore != data)
			this.markForReRender();
	}

	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data)
	{
		x = sanitizeCoordinate(x);
		y = sanitizeCoordinate(y);
		z = sanitizeCoordinate(z);
		//Allocate if it makes sense
		if (chunkVoxelData == null)
			chunkVoxelData = new int[32 * 32 * 32];

		chunkVoxelData[x * 32 * 32 + y * 32 + z] = data;
		lastModification.set(System.currentTimeMillis());
	}

	@Override
	public String toString()
	{
		return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " nl:" + this.needRelightning + " nr:" + need_render.get() + " ip: " + this.isRenderAleadyInProgress() + "]";
	}

	@Override
	public void bakeVoxelLightning(boolean adjacent)
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

		if (c > 0)
			this.need_render.set(true);

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
			int ll = (voxelData & 0x0F000000) >> 0x18;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelTypes.get(cId);

			if (VoxelTypes.get(cId).isVoxelOpaque())
				ll = in.getLightLevel(voxelData);

			if (ll > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int adj = chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x + 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				else if (checkRightBleeding)
				{
					int adjacentBlocklight = (adjacentChunkRight.getVoxelData(0, y, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkRight.markInNeedForLightningUpdate();
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = chunkVoxelData[(x - 1) * 1024 + y * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x - 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adjacentBlocklight = (adjacentChunkLeft.getVoxelData(31, y, z) & 0xF0FFFFFF) << 0x18;
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z + 1] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z + 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adjacentBlocklight = (adjacentChunkFront.getVoxelData(x, y, 0) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkFront.markInNeedForLightningUpdate();
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z - 1];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z - 1] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z - 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBack.getVoxelData(x, y, 31) & 0xF0FFFFFF) << 0x18;
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y + 1);
						//blockSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adjacentBlocklight = (adjacentChunkTop.getVoxelData(x, 0, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkTop.markInNeedForLightningUpdate();
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y - 1);
						//blockSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBottom.getVoxelData(x, 31, z) & 0xF0FFFFFF) << 0x18;
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
			int ll = (voxelData & 0x00F00000) >> 0x14;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelTypes.get(cId);

			if (in.isVoxelOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int adj = chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llRight - 1)
					{
						chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (llRight - 1) << 0x14;
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

					//int adjacentSunlight = (adjacentChunkRight.getDataAt(0, y, z) & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llRight - 1)
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
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
						chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (llLeft - 1) << 0x14;
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
					//int adjacentSunlight = (adjacentChunkLeft.getDataAt(31, y, z) & 0xFF0FFFFF) << 0x14;
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.LEFT);
					if (((adj & 0x00F00000) >> 0x14) < llLeft - 1)
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z + 1] = adj & 0xFF0FFFFF | (llFront - 1) << 0x14;
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
					//int adjacentSunlight = (adjacentChunkFront.getDataAt(x, y, 0) & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						adjacentChunkFront.markInNeedForLightningUpdate();
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = chunkVoxelData[x * 1024 + y * 32 + z - 1];
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBack - 1)
					{
						chunkVoxelData[x * 1024 + y * 32 + z - 1] = adj & 0xFF0FFFFF | (llBack - 1) << 0x14;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z - 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					//int adjacentSunlight = (adjacentChunkBack.getDataAt(x, y, 31) & 0xFF0FFFFF) << 0x14;
					int adj = adjacentChunkBack.getVoxelData(x, y, 31);
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (((adj & 0x00F00000) >> 0x14) < llBack - 1)
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = adj & 0xFF0FFFFF | (llTop - 1) << 0x14;
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
					//int adjacentSunlight = (adj & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						adjacentChunkTop.markInNeedForLightningUpdate();
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.BOTTOM);
					if (!VoxelTypes.get(adj).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = adj & 0xFF0FFFFF | (llBottm /* - removed */) << 0x14;
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
					//int adjacentSunlight = (adj & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llBottm - 1)
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
				int csh = world.getRegionSummaries().getHeightAtWorldCoordinates(chunkX * 32 + a, chunkZ * 32 + b) + 1;
				while (z >= 0)
				{
					int block = chunkVoxelData[a * 1024 + z * 32 + b];
					int id = VoxelFormat.id(block);
					short ll = VoxelTypes.get(id).getLightLevel(block);
					if (ll > 0)
					{
						chunkVoxelData[a * 1024 + z * 32 + b] = chunkVoxelData[a * 1024 + z * 32 + b] & 0xF0FFFFFF | ((ll & 0xF) << 0x18);
						//blockSources.push(a << 16 | b << 8 | z);
						blockSources.push(a);
						blockSources.push(b);
						blockSources.push(z);
					}
					if (!hit)
					{
						if (chunkY * 32 + z >= csh)
						{
							chunkVoxelData[a * 1024 + (z) * 32 + b] = chunkVoxelData[a * 1024 + (z) * 32 + b] & 0xFF0FFFFF | (15 << 0x14);
							//sunSources.push(a << 16 | b << 8 | z);
							sunSources.push(a);
							sunSources.push(b);
							sunSources.push(z);
							if (chunkY * 32 + z < csh || VoxelTypes.get(VoxelFormat.id(chunkVoxelData[a * 1024 + (z) * 32 + b])).getId() != 0)
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

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setVoxelDataWithoutUpdates(31, c, b, ndata);
							blockSources.push(31);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(31 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setVoxelDataWithoutUpdates(0, c, b, ndata);

							blockSources.push(0);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(0 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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
			if (cc != null && chunkVoxelData != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getVoxelData(c, 0, b);
						int current_data = getVoxelData(c, 31, b);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
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
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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
						int heightInSummary = world.getRegionSummaries().getHeightAtWorldCoordinates(chunkX * 32 + b, chunkZ * 32 + c);
						// System.out.println("compute "+heightInSummary+" <= ? "+chunkY*32);
						if (heightInSummary <= chunkY * 32)
						{
							int sourceAt = chunkY * 32 - heightInSummary;
							sourceAt = Math.min(31, sourceAt);
							int current_data = getVoxelData(b, sourceAt, c);

							int ndata = current_data & 0xFF0FFFFF | (15) << 0x14;
							setVoxelDataWithoutUpdates(b, sourceAt, c, ndata);

							sunSources.push(b);
							sunSources.push(c);
							sunSources.push(sourceAt);
							//sunSources.push(b << 16 | c << 8 | sourceAt);
							// System.out.println("Added sunsource cause summary etc");
						}
					}
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

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
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
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setVoxelDataWithoutUpdates(c, b, 31, ndata);
							blockSources.push(c);
							blockSources.push(31);
							blockSources.push(b);
							//blockSources.push(c << 16 | 31 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setVoxelDataWithoutUpdates(c, b, 0, ndata);
							blockSources.push(c);
							blockSources.push(0);
							blockSources.push(b);
							//blockSources.push(c << 16 | 0 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
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

		int csh = world.getRegionSummaries().getHeightAtWorldCoordinates(bx + chunkX * 32, bz + chunkZ * 32);
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
			int ll = (voxelData & 0x0F000000) >> 0x18;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelTypes.get(cId);

			if (VoxelTypes.get(cId).isVoxelOpaque())
				ll = in.getLightLevel(voxelData);

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x + 1, y, z);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
			int ll = (voxelData & 0x00F00000) >> 0x14;
			int cId = VoxelFormat.id(voxelData);

			in = VoxelTypes.get(cId);

			if (in.isVoxelOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFuncitons(x + 1, y, z);
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llRight - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & 0xFF0FFFFF | (llRight - 1) << 0x14);
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
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & 0xFF0FFFFF | (llLeft - 1) << 0x14);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & 0xFF0FFFFF | (llFront - 1) << 0x14);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBack - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & 0xFF0FFFFF | (llBack - 1) << 0x14);
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
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & 0xFF0FFFFF | (llTop - 1) << 0x14);
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
					if (!VoxelTypes.get(adj).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & 0xFF0FFFFF | (llBottm /* - removed */) << 0x14);
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
		return holder;
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
	public void markForReRender()
	{
		this.need_render.set(true);
	}

	@Override
	public boolean isMarkedForReRender()
	{
		return this.need_render.get();
	}

	@Override
	public void markRenderInProgress(boolean inProgress)
	{
		this.requestable.set(!inProgress);
	}

	@Override
	public boolean isRenderAleadyInProgress()
	{
		return !requestable.get();
	}

	@Override
	public void destroyRenderData()
	{
		//Add it to the deletion queue for ressources
		if (chunkRenderData != null)
			chunkRenderData.markForDeletion();
		//Delete the reference
		chunkRenderData = null;

		markForReRender();
	}

	public void destroy()
	{
		destroyRenderData();
	}

	@Override
	public void setChunkRenderData(ChunkRenderData chunkRenderData)
	{
		//Delete old one
		if (this.chunkRenderData != null && !this.chunkRenderData.equals(chunkRenderData))
			this.chunkRenderData.markForDeletion();
		//Replaces it
		this.chunkRenderData = chunkRenderData;
	}

	@Override
	public ChunkRenderData getChunkRenderData()
	{
		return chunkRenderData;
	}
}
