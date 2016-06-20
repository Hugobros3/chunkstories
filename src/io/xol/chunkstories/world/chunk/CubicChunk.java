package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class CubicChunk implements Chunk
{
	public WorldImplementation world;
	public Region holder;
	public int chunkX, chunkY, chunkZ;

	public int dataPointer = -1; // -1 means empty chunk (air)

	// Used in client rendering
	public ChunkRenderData chunkRenderData;
	public AtomicLong lastModification = new AtomicLong();
	public AtomicLong lastModificationSaved = new AtomicLong();
	
	public AtomicBoolean need_render = new AtomicBoolean(true);
	public AtomicBoolean need_render_fast = new AtomicBoolean(false);
	public AtomicBoolean requestable = new AtomicBoolean(true);

	public AtomicBoolean needRelightning = new AtomicBoolean(true);

	// Terrain Generation
	// public List<GenerableStructure> structures = new ArrayList<GenerableStructure>();

	// Occlusion
	boolean occludedTop = false;
	boolean occludedBot = false;

	boolean occludedNorth = false;
	boolean occludedSouth = false;

	boolean occludedLeft = false;
	boolean occludedRight = false;

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
	
	public CubicChunk(WorldImplementation world, int chunkX, int chunkY, int chunkZ)
	{
		this.world = world;
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Chunk#getDataAt(int, int, int)
	 */
	@Override
	public int getDataAt(int x, int y, int z)
	{
		if (dataPointer == -1)
		{
			return 0;
		}
		else
		{
			x %= 32;
			y %= 32;
			z %= 32;
			if (x < 0)
				x += 32;
			if (y < 0)
				y += 32;
			if (z < 0)
				z += 32;
			return world.chunksData.grab(dataPointer)[x * 32 * 32 + y * 32 + z];
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Chunk#setDataAt(int, int, int, int)
	 */
	@Override
	public void setDataAtWithUpdates(int x, int y, int z, int data)
	{
		x %= 32;
		y %= 32;
		z %= 32;
		if (x < 0)
			x += 32;
		if (y < 0)
			y += 32;
		if (z < 0)
			z += 32;
		//Allocate if it makes sense
		if (dataPointer < 0 && data != 0)
			dataPointer = world.chunksData.malloc(this);
		if (dataPointer >= 0)
		{
			int dataBefore = world.chunksData.grab(dataPointer)[x * 32 * 32 + y * 32 + z];
			world.chunksData.grab(dataPointer)[x * 32 * 32 + y * 32 + z] = data;
			computeLightSpread(x, y, z, dataBefore, data);
			lastModification.set(System.currentTimeMillis());
		}
	}
	
	public void setDataAtWithoutUpdates(int x, int y, int z, int data)
	{
		x %= 32;
		y %= 32;
		z %= 32;
		if (x < 0)
			x += 32;
		if (y < 0)
			y += 32;
		if (z < 0)
			z += 32;
		//Allocate if it makes sense
		if (dataPointer < 0 && data != 0)
			dataPointer = world.chunksData.malloc(this);
		if (dataPointer >= 0)
		{
			world.chunksData.grab(dataPointer)[x * 32 * 32 + y * 32 + z] = data;
			//lastModification.set(System.currentTimeMillis());
		}
	}

	@Override
	public String toString()
	{
		return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " NR:"+need_render.get()+" "+"]";
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Chunk#markDirty(boolean)
	 */
	@Override
	public void markDirty(boolean fast)
	{
		need_render.set(true);
		requestable.set(true);
		need_render_fast.set(fast);
	}

	public void destroy()
	{
		if (dataPointer >= 0)
			world.chunksData.free(dataPointer);
	}

	@Override
	public void bakeVoxelLightning(boolean adjacent)
	{
		// Checks first if chunk contains blocks
		if (dataPointer < 0)
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
		if(adjacent)
			addAdjacentChunksLightSources(blockSources, sunSources);
		
		//Propagates the light
		int c = propagateLightning(blockSources, sunSources);
		if(c > 0)
			this.need_render.set(true);
		
		//Return the queues after that
		//world.dequesPool.back(blockSources);
		//world.dequesPool.back(sunSources);
		//Not really jk
	}
	
	// Now entering lightning code part, brace yourselves
	private int propagateLightning(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		int data[] = world.chunksData.grab(dataPointer);
		int modifiedBlocks = 0;
		
		//Checks if the adjacent chunks are done loading
		CubicChunk adjacentChunkTop = world.getChunk(chunkX, chunkY + 1, chunkZ, false);
		CubicChunk adjacentChunkBottom = world.getChunk(chunkX, chunkY - 1, chunkZ, false);
		CubicChunk adjacentChunkFront = world.getChunk(chunkX, chunkY, chunkZ + 1, false);
		CubicChunk adjacentChunkBack = world.getChunk(chunkX, chunkY, chunkZ - 1, false);
		CubicChunk adjacentChunkLeft = world.getChunk(chunkX - 1, chunkY, chunkZ, false);
		CubicChunk adjacentChunkRight = world.getChunk(chunkX + 1, chunkY, chunkZ, false);
		//Don't spam the requeue requests
		boolean checkTopBleeding = (adjacentChunkTop != null) && !adjacentChunkTop.needRelightning.get();
		boolean checkBottomBleeding = (adjacentChunkBottom != null) && !adjacentChunkBottom.needRelightning.get();
		boolean checkFrontBleeding = (adjacentChunkFront != null) && !adjacentChunkFront.needRelightning.get();
		boolean checkBackBleeding = (adjacentChunkBack != null) && !adjacentChunkBack.needRelightning.get();
		boolean checkLeftBleeding = (adjacentChunkLeft != null) && !adjacentChunkLeft.needRelightning.get();
		boolean checkRightBleeding = (adjacentChunkRight != null) && !adjacentChunkRight.needRelightning.get();
		Voxel in;
		while (blockSources.size() > 0)
		{
			int y = blockSources.pop();
			int z = blockSources.pop();
			int x = blockSources.pop();
			int voxelData = data[x * 1024 + y * 32 + z];
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
					int adj = data[(x + 1) * 1024 + y * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[(x + 1) * 1024 + y * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x + 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				else if (checkRightBleeding)
				{
					int adjacentBlocklight = (adjacentChunkRight.getDataAt(0, y, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkRight.needRelightning.set(true);
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = data[(x - 1) * 1024 + y * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[(x - 1) * 1024 + y * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x - 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adjacentBlocklight = (adjacentChunkLeft.getDataAt(31, y, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkLeft.needRelightning.set(true);
						checkLeftBleeding = false;
					}
				}
				// Z-propagation
				if (z < 31)
				{
					int adj = data[x * 1024 + y * 32 + z + 1];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[x * 1024 + y * 32 + z + 1] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z + 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adjacentBlocklight = (adjacentChunkFront.getDataAt(x, y, 0) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkFront.needRelightning.set(true);
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = data[x * 1024 + y * 32 + z - 1];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[x * 1024 + y * 32 + z - 1] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z - 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBack.getDataAt(x, y, 31) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkBack.needRelightning.set(true);
						checkBackBleeding = false;
					}
				}
				// Y-propagation
				if (y < 31) // y = 254+1
				{
					int adj = data[x * 1024 + (y + 1) * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[x * 1024 + (y + 1) * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y + 1);
						//blockSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adjacentBlocklight = (adjacentChunkTop.getDataAt(x, 0, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkTop.needRelightning.set(true);
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = data[x * 1024 + (y - 1) * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[x * 1024 + (y - 1) * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y - 1);
						//blockSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adjacentBlocklight = (adjacentChunkBottom.getDataAt(x, 31, z) & 0xF0FFFFFF) << 0x18;
					if (ll > adjacentBlocklight + 1)
					{
						adjacentChunkBottom.needRelightning.set(true);
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

			int voxelData = data[x * 1024 + y * 32 + z];
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
					int adj = data[(x + 1) * 1024 + y * 32 + z];
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, 2);
					
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llRight - 1)
					{
						data[(x + 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (llRight - 1) << 0x14;
						modifiedBlocks++;
						sunSources.push(x + 1);
						sunSources.push(z);
						sunSources.push(y);
					}
				}
				else if (checkRightBleeding)
				{
					int adj = adjacentChunkRight.getDataAt(0, y, z);
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, 2);
					
					//int adjacentSunlight = (adjacentChunkRight.getDataAt(0, y, z) & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llRight - 1)
					{
						adjacentChunkRight.needRelightning.set(true);
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = data[(x - 1) * 1024 + y * 32 + z];
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, 0);
					//int id = (adj & 0xFFFF);
					//if(id == 25)
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
						data[(x - 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (llLeft - 1) << 0x14;
						modifiedBlocks++;
						sunSources.push(x - 1);
						sunSources.push(z);
						sunSources.push(y);
						//sunSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adj = adjacentChunkLeft.getDataAt(31, y, z);
					//int adjacentSunlight = (adjacentChunkLeft.getDataAt(31, y, z) & 0xFF0FFFFF) << 0x14;
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, 0);
					if (((adj & 0x00F00000) >> 0x14) < llLeft - 1)
					{
						adjacentChunkLeft.needRelightning.set(true);
						checkLeftBleeding = false;
					}
				}
				// Z-propagation
				if (z < 31)
				{
					int adj = data[x * 1024 + y * 32 + z + 1];
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, 1);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						data[x * 1024 + y * 32 + z + 1] = adj & 0xFF0FFFFF | (llFront - 1) << 0x14;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z + 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adj = adjacentChunkFront.getDataAt(x, y, 0);
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, 1);
					//int adjacentSunlight = (adjacentChunkFront.getDataAt(x, y, 0) & 0xFF0FFFFF) << 0x14;
					if (((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						adjacentChunkFront.needRelightning.set(true);
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = data[x * 1024 + y * 32 + z - 1];
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, 3);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBack - 1)
					{
						data[x * 1024 + y * 32 + z - 1] = adj & 0xFF0FFFFF | (llBack - 1) << 0x14;
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
					int adj = adjacentChunkBack.getDataAt(x, y, 31);
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, 3);
					if(((adj & 0x00F00000) >> 0x14) < llBack - 1)
					{
						adjacentChunkBack.needRelightning.set(true);
						checkBackBleeding = false;
					}
				}
				// Y-propagation
				if (y < 31) // y = 254+1
				{
					int adj = data[x * 1024 + (y + 1) * 32 + z];
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, 4);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						data[x * 1024 + (y + 1) * 32 + z] = adj & 0xFF0FFFFF | (llTop - 1) << 0x14;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y + 1);
						//sunSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adj = adjacentChunkTop.getDataAt(x, 0, z);
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, 4);
					//int adjacentSunlight = (adj & 0xFF0FFFFF) << 0x14;
					if(((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						adjacentChunkTop.needRelightning.set(true);
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = data[x * 1024 + (y - 1) * 32 + z];
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, 5);
					if (!VoxelTypes.get(adj).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						data[x * 1024 + (y - 1) * 32 + z] = adj & 0xFF0FFFFF | (llBottm /* - removed */) << 0x14;
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y - 1);
						//sunSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adj = adjacentChunkBottom.getDataAt(x, 31, z);
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, 5);
					//int adjacentSunlight = (adj & 0xFF0FFFFF) << 0x14;
					if(((adj & 0x00F00000) >> 0x14) < llBottm - 1)
					{
						adjacentChunkBottom.needRelightning.set(true);
						checkBottomBleeding = false;
					}
				}
			}
		}
		
		return modifiedBlocks;
	}
	
	private void addChunkLightSources(Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		int data[] = world.chunksData.grab(dataPointer);
		
		for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++)
			{
				int z = 31; // This is basically wrong since we work with cubic chunks
				boolean hit = false;
				int csh = world.getRegionSummaries().getHeightAt(chunkX * 32 + a, chunkZ * 32 + b) + 1;
				while (z >= 0)
				{
					int block = data[a * 1024 + z * 32 + b];
					int id = VoxelFormat.id(block);
					short ll = VoxelTypes.get(id).getLightLevel(block);
					if (ll > 0)
					{
						data[a * 1024 + z * 32 + b] = data[a * 1024 + z * 32 + b] & 0xF0FFFFFF | ((ll & 0xF) << 0x18);
						//blockSources.push(a << 16 | b << 8 | z);
						blockSources.push(a);
						blockSources.push(b);
						blockSources.push(z);
					}
					if (!hit)
					{
						if (chunkY * 32 + z >= csh)
						{
							data[a * 1024 + (z) * 32 + b] = data[a * 1024 + (z) * 32 + b] & 0xFF0FFFFF | (15 << 0x14);
							//sunSources.push(a << 16 | b << 8 | z);
							sunSources.push(a);
							sunSources.push(b);
							sunSources.push(z);
							if (chunkY * 32 + z < csh || VoxelTypes.get(VoxelFormat.id(data[a * 1024 + (z) * 32 + b])).getId() != 0)
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
			CubicChunk cc;
			cc = world.getChunk(chunkX + 1, chunkY, chunkZ, false);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(0, c, b);
						int current_data = getDataAt(31, c, b);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(31, c, b, ndata);
							blockSources.push(31);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(31 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAtWithoutUpdates(31, c, b, ndata);

							sunSources.push(31);
							sunSources.push(b);
							sunSources.push(c);
							//sunSources.push(31 << 16 | b << 8 | c);
						}
					}
			}
			cc = world.getChunk(chunkX - 1, chunkY, chunkZ, false);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(31, c, b);
						int current_data = getDataAt(0, c, b);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(0, c, b, ndata);

							blockSources.push(0);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(0 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAtWithoutUpdates(0, c, b, ndata);

							sunSources.push(0);
							sunSources.push(b);
							sunSources.push(c);
							//sunSources.push(0 << 16 | b << 8 | c);
						}
					}
			}
			// Top chunk
			cc = world.getChunk(chunkX, chunkY + 1, chunkZ, false);
			if (cc != null && cc.dataPointer != -1)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(c, 0, b);
						int current_data = getDataAt(c, 31, b);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(c, 31, b, ndata);
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
							setDataAtWithoutUpdates(c, 31, b, ndata);
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
						int heightInSummary = world.getRegionSummaries().getHeightAt(chunkX * 32 + b, chunkZ * 32 + c);
						// System.out.println("compute "+heightInSummary+" <= ? "+chunkY*32);
						if (heightInSummary <= chunkY * 32)
						{
							int sourceAt = chunkY * 32 - heightInSummary;
							sourceAt = Math.min(31, sourceAt);
							int current_data = getDataAt(b, sourceAt, c);

							int ndata = current_data & 0xFF0FFFFF | (15) << 0x14;
							setDataAtWithoutUpdates(b, sourceAt, c, ndata);

							sunSources.push(b);
							sunSources.push(c);
							sunSources.push(sourceAt);
							//sunSources.push(b << 16 | c << 8 | sourceAt);
							// System.out.println("Added sunsource cause summary etc");
						}
					}
			}
			// Bottom chunk
			cc = world.getChunk(chunkX, chunkY - 1, chunkZ, false);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(c, 31, b);
						int current_data = getDataAt(c, 0, b);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(c, 0, b, ndata);
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
							setDataAtWithoutUpdates(c, 0, b, ndata);
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
			cc = world.getChunk(chunkX, chunkY, chunkZ + 1, false);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(c, b, 0);
						int current_data = getDataAt(c, b, 31);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(c, b, 31, ndata);
							blockSources.push(c);
							blockSources.push(31);
							blockSources.push(b);
							//blockSources.push(c << 16 | 31 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAtWithoutUpdates(c, b, 31, ndata);
							sunSources.push(c);
							sunSources.push(31);
							sunSources.push(b);
							//sunSources.push(c << 16 | 31 << 8 | b);
						}
					}
			}
			cc = world.getChunk(chunkX, chunkY, chunkZ - 1, false);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.getDataAt(c, b, 31);
						int current_data = getDataAt(c, b, 0);

						int adjacent_blo = ((adjacent_data & 0x0F000000) >>> 0x18);
						int current_blo = ((current_data & 0x0F000000) >>> 0x18);
						int adjacent_sun = ((adjacent_data & 0x00F00000) >>> 0x14);
						int current_sun = ((current_data & 0x00F00000) >>> 0x14);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & 0xF0FFFFFF | (adjacent_blo - 1) << 0x18;
							setDataAtWithoutUpdates(c, b, 0, ndata);
							blockSources.push(c);
							blockSources.push(0);
							blockSources.push(b);
							//blockSources.push(c << 16 | 0 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAtWithoutUpdates(c, b, 0, ndata);
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

		int csh = world.getRegionSummaries().getHeightAt(bx + chunkX * 32, bz + chunkZ * 32);
		int block_height = by + chunkY * 32;
		
		//If the block is at or above (never) the topmost tile it's sunlit
		if(block_height >= csh)
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
		if(sunLightAfter > 0)
		{
			sunSources.push(bx);
			sunSources.push(bz);
			sunSources.push(by);
		}
		if(blockLightAfter > 0)
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
		while(sunSourcesRemoval.size() > 0)
		{
			int sunLightLevel = sunSourcesRemoval.pop();
			int z = sunSourcesRemoval.pop();
			int y = sunSourcesRemoval.pop();
			int x = sunSourcesRemoval.pop();
			
			int neighborSunLightLevel;
			
			// X Axis
			if(x > 0)
			{
				neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.push(x - 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x - 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			if(x < 31)
			{
				neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.push(x + 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x + 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			// Y axis
			if(y > 0)
			{
				neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel)
				{
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y - 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y - 1);
				}
			}
			if(y < 31)
			{
				neighborSunLightLevel = this.getSunLight(x, y + 1, z);
				
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y + 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y + 1);
				}
			}
			// Z Axis
			if(z > 0)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z - 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z - 1);
					sunSources.push(y);
				}
			}
			if(z < 31)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z + 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z + 1);
					sunSources.push(y);
				}
			}
		}
		
		
		while(blockSourcesRemoval.size() > 0)
		{
			int blockLightLevel = blockSourcesRemoval.pop();
			int z = blockSourcesRemoval.pop();
			int y = blockSourcesRemoval.pop();
			int x = blockSourcesRemoval.pop();
			
			int neighborBlockLightLevel;
			
			// X Axis
			if(x > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				//System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.push(x - 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x - 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			if(x < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.push(x + 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x + 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			// Y axis
			if(y > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y - 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y - 1);
				}
			}
			if(y < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y + 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y + 1);
				}
			}
			// Z Axis
			if(z > 0)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z - 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z - 1);
					blockSources.push(y);
				}
			}
			if(z < 31)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z + 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
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
		while(sunSourcesRemoval.size() > 0)
		{
			int sunLightLevel = sunSourcesRemoval.pop();
			int z = sunSourcesRemoval.pop();
			int y = sunSourcesRemoval.pop();
			int x = sunSourcesRemoval.pop();
			
			int neighborSunLightLevel;
			
			// X Axis
			if(x > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.push(x - 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x - 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			if(x < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.push(x + 1);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x + 1);
					sunSources.push(z);
					sunSources.push(y);
				}
			}
			// Y axis
			if(y > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel)
				{
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y - 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y - 1);
				}
			}
			if(y < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y + 1, z);
				
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y + 1);
					sunSourcesRemoval.push(z);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z);
					sunSources.push(y + 1);
				}
			}
			// Z Axis
			if(z > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z - 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z - 1);
					sunSources.push(y);
				}
			}
			if(z < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if(neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.push(x);
					sunSourcesRemoval.push(y);
					sunSourcesRemoval.push(z + 1);
					sunSourcesRemoval.push(neighborSunLightLevel);
				}
				else if(neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.push(x);
					sunSources.push(z + 1);
					sunSources.push(y);
				}
			}
		}
		
		
		while(blockSourcesRemoval.size() > 0)
		{
			int blockLightLevel = blockSourcesRemoval.pop();
			int z = blockSourcesRemoval.pop();
			int y = blockSourcesRemoval.pop();
			int x = blockSourcesRemoval.pop();
			
			int neighborBlockLightLevel;
			
			// X Axis
			if(x > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				//System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.push(x - 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x - 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			if(x < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.push(x + 1);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x + 1);
					blockSources.push(z);
					blockSources.push(y);
				}
			}
			// Y axis
			if(y > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y - 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y - 1);
				}
			}
			if(y < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y + 1);
					blockSourcesRemoval.push(z);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z);
					blockSources.push(y + 1);
				}
			}
			// Z Axis
			if(z > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z - 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.push(x);
					blockSources.push(z - 1);
					blockSources.push(y);
				}
			}
			if(z < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if(neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.push(x);
					blockSourcesRemoval.push(y);
					blockSourcesRemoval.push(z + 1);
					blockSourcesRemoval.push(neighborBlockLightLevel);
				}
				else if(neighborBlockLightLevel >= blockLightLevel)
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
			int voxelData = getWorldData(x, y, z);
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
					int adj = this.getWorldData(x + 1, y, z);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x + 1, y, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
						modifiedBlocks++;
						blockSources.push(x + 1);
						blockSources.push(z);
						blockSources.push(y);
						//blockSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldData(x - 1, y, z);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x - 1, y, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					int adj = this.getWorldData(x, y, z + 1);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x, y, z + 1, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z + 1);
						blockSources.push(y);
						//blockSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldData(x, y, z - 1);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x, y, z - 1, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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
					int adj = this.getWorldData(x, y + 1, z);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x, y + 1, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
						modifiedBlocks++;
						blockSources.push(x);
						blockSources.push(z);
						blockSources.push(y + 1);
						//blockSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldData(x, y - 1, z);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						this.setWorldData(x, y - 1, z, adj & 0xF0FFFFFF | (ll - 1) << 0x18);
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

			int voxelData = this.getWorldData(x, y, z);
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
					int adj = this.getWorldData(x + 1, y, z);
					int llRight = ll - in.getLightLevelModifier(voxelData, adj, 2);
					
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llRight - 1)
					{
						this.setWorldData(x + 1, y, z, adj & 0xFF0FFFFF | (llRight - 1) << 0x14);
						modifiedBlocks++;
						sunSources.push(x + 1);
						sunSources.push(z);
						sunSources.push(y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldData(x - 1, y, z);
					int llLeft = ll - in.getLightLevelModifier(voxelData, adj, 0);
					//int id = (adj & 0xFFFF);
					//if(id == 25)
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llLeft - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
						this.setWorldData(x - 1, y, z, adj & 0xFF0FFFFF | (llLeft - 1) << 0x14);
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
					int adj = this.getWorldData(x, y, z + 1);
					int llFront = ll - in.getLightLevelModifier(voxelData, adj, 1);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llFront - 1)
					{
						this.setWorldData(x, y, z + 1, adj & 0xFF0FFFFF | (llFront - 1) << 0x14);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z + 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldData(x, y, z - 1);
					int llBack = ll - in.getLightLevelModifier(voxelData, adj, 3);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBack - 1)
					{
						this.setWorldData(x, y, z - 1, adj & 0xFF0FFFFF | (llBack - 1) << 0x14);
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
					int adj = this.getWorldData(x, y + 1, z);
					int llTop = ll - in.getLightLevelModifier(voxelData, adj, 4);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llTop - 1)
					{
						this.setWorldData(x, y + 1, z, adj & 0xFF0FFFFF | (llTop - 1) << 0x14);
						modifiedBlocks++;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y + 1);
						//sunSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldData(x, y - 1, z);
					int llBottm = ll - in.getLightLevelModifier(voxelData, adj, 5);
					if (!VoxelTypes.get(adj).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						this.setWorldData(x, y - 1, z, adj & 0xFF0FFFFF | (llBottm /* - removed */) << 0x14);
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

	private int getWorldData(int x, int y, int z)
	{
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
					this.getDataAt(x, y, z);
		return world.getDataAt(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, false);
	}
	
	private void setWorldData(int x, int y, int z, int data)
	{
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
				{
					this.setDataAtWithoutUpdates(x, y, z, data);
					return;
				}
		world.setDataAtWithoutUpdates(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, data, false);
		CubicChunk c = world.getChunk((x + chunkX * 32) / 32, (y + chunkY * 32) / 32, (z + chunkZ * 32) / 32, false);
		if(c != null)
		{
			//c.needRelightning.set(true);
			c.need_render.set(true);
			c.markDirty(true);
		}
	}

	@Override
	public int getSunLight(int x, int y, int z)
	{
		//if(this.dataPointer == -1)
		//	return y <= world.getRegionSummaries().getHeightAt(chunkX * 32 + x, chunkZ * 32 + z) ? 0 : 15;
		
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
					return VoxelFormat.sunlight(this.getDataAt(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.sunlight(this.getWorldData(x, y, z));
	}

	@Override
	public int getBlockLight(int x, int y, int z)
	{
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
					return VoxelFormat.blocklight(this.getDataAt(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.blocklight(this.getWorldData(x, y, z));
	}

	public boolean isAirChunk()
	{
		return dataPointer == -1;
	}
	
	@Override
	public void setSunLight(int x, int y, int z, int level)
	{
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
				{
					this.setDataAtWithoutUpdates(x, y, z, VoxelFormat.changeSunlight(this.getDataAt(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldData(x, y, z, VoxelFormat.changeSunlight(this.getWorldData(x, y, z), level));CubicChunk c = world.getChunk((x + chunkX * 32) / 32, (y + chunkY * 32) / 32, (z + chunkZ * 32) / 32, false);
		if(c != null)
		{
			//c.needRelightning.set(true);
			c.need_render.set(true);
			c.markDirty(true);
		}
	}

	@Override
	public void setBlockLight(int x, int y, int z, int level)
	{
		if(x > 0 && x < 31)
			if(y > 0 && y < 31)
				if(z > 0 && z < 31)
				{
					this.setDataAtWithoutUpdates(x, y, z, VoxelFormat.changeBlocklight(this.getDataAt(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldData(x, y, z, VoxelFormat.changeBlocklight(this.getWorldData(x, y, z), level));CubicChunk c = world.getChunk((x + chunkX * 32) / 32, (y + chunkY * 32) / 32, (z + chunkZ * 32) / 32, false);
		if(c != null)
		{
			//c.needRelightning.set(true);
			c.need_render.set(true);
			c.markDirty(true);
		}
	}
}
