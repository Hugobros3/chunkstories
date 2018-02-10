package io.xol.chunkstories.world.chunk;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.engine.concurrency.SimpleLock;

public class ChunkLightBaker implements ChunkLightUpdater {
	final CubicChunk chunk;
	final World world;
	final int chunkX, chunkY, chunkZ;
	
	final AtomicInteger unbakedUpdates = new AtomicInteger(1);
	public final SimpleLock onlyOneUpdateAtATime = new SimpleLock();
	
	protected TaskLightChunk task = null;
	final Lock taskLock = new ReentrantLock();
	
	public ChunkLightBaker(CubicChunk chunk) {
		this.chunk = chunk;
		this.world = chunk.world;
		
		this.chunkX = chunk.chunkX;
		this.chunkY = chunk.chunkY;
		this.chunkZ = chunk.chunkZ;
	}

	@Override
	public Fence requestLightningUpdate() {
		//Thread.dumpStack();
		
		unbakedUpdates.incrementAndGet();
		
		Task fence;
		
		taskLock.lock();
		
		if(task == null || task.isDone() || task.isCancelled()) {
			task = new TaskLightChunk(chunk, true);
			chunk.getWorld().getGameContext().tasks().scheduleTask(task);
		}

		fence = task;
		
		taskLock.unlock();
		
		return fence;
	}

	@Override
	public void spawnUpdateTaskIfNeeded() {
		if(unbakedUpdates.get() > 0) {
			taskLock.lock();
			
			if(task == null || task.isDone() || task.isCancelled()) {
				task = new TaskLightChunk(chunk, true);
				chunk.getWorld().getGameContext().tasks().scheduleTask(task);
			}
			
			taskLock.unlock();
		}
	}
	
	@Override
	public int pendingUpdates() {
		return this.unbakedUpdates.get();
	}

	/* Ressources for actual computations */
	static final int sunlightMask = 0x000F0000;
	static final int blocklightMask = 0x00F00000;
	static final int sunAntiMask = 0xFFF0FFFF;
	static final int blockAntiMask = 0xFF0FFFFF;
	static final int sunBitshift = 0x10;
	static final int blockBitshift = 0x14;
	
	static ThreadLocal<IntDeque> tl_blockSources = new ThreadLocal<IntDeque>()
	{
		@Override
		protected IntDeque initialValue()
		{
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_sunSources = new ThreadLocal<IntDeque>()
	{
		@Override
		protected IntDeque initialValue()
		{
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_blockSourcesRemoval = new ThreadLocal<IntDeque>()
	{
		@Override
		protected IntDeque initialValue()
		{
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_sunSourcesRemoval = new ThreadLocal<IntDeque>()
	{
		@Override
		protected IntDeque initialValue()
		{
			return new IntArrayDeque();
		}
	};

	public int computeVoxelLightningInternal(boolean adjacent)
	{
		// Checks first if chunk contains blocks
		if (chunk.chunkVoxelData == null)
			return 0; // Nothing to do

		//Lock the chunk & grab 2 queues
		IntDeque blockSources = tl_blockSources.get();
		IntDeque sunSources = tl_sunSources.get();

		// Reset any remnant data
		blockSources.clear();
		sunSources.clear();

		// Find our own light sources, add them
		this.addChunkLightSources(blockSources, sunSources);

		int mods = 0;
		
		// Load nearby chunks and check if they contain bright spots we haven't accounted for yet
		if (adjacent)
			mods += addAdjacentChunksLightSources(blockSources, sunSources);

		//Propagates the light
		mods += propagateLightning(blockSources, sunSources);

		return mods;
	}
	

	// Now entering lightning code part, brace yourselves
	private int propagateLightning(IntDeque blockSources, IntDeque sunSources)
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
		boolean checkTopBleeding = (adjacentChunkTop != null);
		boolean checkBottomBleeding = (adjacentChunkBottom != null);
		boolean checkFrontBleeding = (adjacentChunkFront != null);
		boolean checkBackBleeding = (adjacentChunkBack != null);
		boolean checkLeftBleeding = (adjacentChunkLeft != null);
		boolean checkRightBleeding = (adjacentChunkRight != null);
		
		boolean requestTop = false;
		boolean requestBot = false;
		boolean requestFront = false;
		boolean requestBack = false;
		boolean requestLeft = false;
		boolean requestRight = false;
		
		Voxel voxel;
		while (blockSources.size() > 0)
		{
			int y = blockSources.removeLast();
			int z = blockSources.removeLast();
			int x = blockSources.removeLast();
			
			int voxelData = chunk.chunkVoxelData[x * 1024 + y * 32 + z];
			int cellLightLevel = (voxelData & blocklightMask) >> blockBitshift;
			int cId = VoxelFormat.id(voxelData);
			voxel = world.getContentTranslator().getVoxelForId(cId);

			if (world.getContentTranslator().getVoxelForId(cId).getDefinition().isOpaque())
				cellLightLevel = voxel.getLightLevel(voxelData);
			
			if (cellLightLevel > 1)
			{
				if (x < 31)
				{
					int rightData = chunk.chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					int llRight = cellLightLevel - voxel.getLightLevelModifier(voxelData, rightData, VoxelSides.RIGHT);
					if (!world.getContentTranslator().getVoxelForId((rightData & 0xFFFF)).getDefinition().isOpaque() && ((rightData & blocklightMask) >> blockBitshift) < llRight - 1)
					{
						chunk.chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = rightData & blockAntiMask | (llRight - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x + 1);
						blockSources.addLast(z);
						blockSources.addLast(y);
					}
				}
				else if (checkRightBleeding)
				{
					int rightData = adjacentChunkRight.peekRaw(0, y, z);
					int llRight = cellLightLevel - voxel.getLightLevelModifier(voxelData, rightData, VoxelSides.RIGHT);
					if (((rightData & blocklightMask) >> blockBitshift) < llRight - 1)
					{
						requestRight = true;
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int leftData = chunk.chunkVoxelData[(x - 1) * 1024 + y * 32 + z];
					int llLeft = cellLightLevel - voxel.getLightLevelModifier(voxelData, leftData, VoxelSides.LEFT);
					if (!world.getContentTranslator().getVoxelForId((leftData & 0xFFFF)).getDefinition().isOpaque() && ((leftData & blocklightMask) >> blockBitshift) < llLeft - 1)
					{
						chunk.chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = leftData & blockAntiMask | (llLeft - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x - 1);
						blockSources.addLast(z);
						blockSources.addLast(y);
					}
				}
				else if (checkLeftBleeding)
				{
					int leftData = adjacentChunkLeft.peekRaw(31, y, z);
					int llLeft = cellLightLevel - voxel.getLightLevelModifier(voxelData, leftData, VoxelSides.LEFT);
					if (((leftData & blocklightMask) >> blockBitshift) < llLeft - 1)
					{
						requestLeft = true;
						checkLeftBleeding = false;
					}
				}

				if (z < 31)
				{
					int frontData = chunk.chunkVoxelData[x * 1024 + y * 32 + z + 1];
					int llFront = cellLightLevel - voxel.getLightLevelModifier(voxelData, frontData, VoxelSides.FRONT);
					if (!world.getContentTranslator().getVoxelForId((frontData & 0xFFFF)).getDefinition().isOpaque() && ((frontData & blocklightMask) >> blockBitshift) < llFront - 1)
					{
						chunk.chunkVoxelData[x * 1024 + y * 32 + z + 1] = frontData & blockAntiMask | (llFront - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z + 1);
						blockSources.addLast(y);
					}
				}
				else if (checkFrontBleeding)
				{
					int frontData = adjacentChunkFront.peekRaw(x, y, 0);
					int llFront = cellLightLevel - voxel.getLightLevelModifier(voxelData, frontData, VoxelSides.FRONT);
					if (((frontData & blocklightMask) >> blockBitshift) < llFront - 1)
					{
						requestFront = true;
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int backData = chunk.chunkVoxelData[x * 1024 + y * 32 + z - 1];
					int llBack = cellLightLevel - voxel.getLightLevelModifier(voxelData, backData, VoxelSides.BACK);
					if (!world.getContentTranslator().getVoxelForId((backData & 0xFFFF)).getDefinition().isOpaque() && ((backData & blocklightMask) >> blockBitshift) < llBack - 1)
					{
						chunk.chunkVoxelData[x * 1024 + y * 32 + z - 1] = backData & blockAntiMask | (llBack - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z - 1);
						blockSources.addLast(y);
					}
				}
				else if (checkBackBleeding)
				{
					int backData = adjacentChunkBack.peekRaw(x, y, 31);
					int llBack = cellLightLevel - voxel.getLightLevelModifier(voxelData, backData, VoxelSides.BACK);
					if (((backData & blocklightMask) >> blockBitshift) < llBack - 1)
					{
						requestBack = true;
						checkBackBleeding = false;
					}
				}

				if (y < 31)
				{
					int topData = chunk.chunkVoxelData[x * 1024 + (y + 1) * 32 + z];
					int llTop = cellLightLevel - voxel.getLightLevelModifier(voxelData, topData, VoxelSides.TOP);
					if (!world.getContentTranslator().getVoxelForId((topData & 0xFFFF)).getDefinition().isOpaque() && ((topData & blocklightMask) >> blockBitshift) < llTop - 1)
					{
						chunk.chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = topData & blockAntiMask | (llTop - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z);
						blockSources.addLast(y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int topData = adjacentChunkTop.peekRaw(x, 0, z);
					int llTop = cellLightLevel - voxel.getLightLevelModifier(voxelData, topData, VoxelSides.TOP);
					if (((topData & blocklightMask) >> blockBitshift) < llTop - 1)
					{
						requestTop = true;
						checkTopBleeding = false;
					}
				}
				
				if (y > 0)
				{
					int belowData = chunk.chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					int llTop = cellLightLevel - voxel.getLightLevelModifier(voxelData, belowData, VoxelSides.BOTTOM);
					
					if (!world.getContentTranslator().getVoxelForId((belowData & 0xFFFF)).getDefinition().isOpaque() && ((belowData & blocklightMask) >> blockBitshift) < llTop - 1)
					{
						chunk.chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = belowData & blockAntiMask | (llTop - 1) << blockBitshift;
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z);
						blockSources.addLast(y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int belowData = adjacentChunkBottom.peekRaw(x, 31, z);
					int llBottom = cellLightLevel - voxel.getLightLevelModifier(voxelData, belowData, VoxelSides.BOTTOM);
					
					if(((belowData & blocklightMask) >> blockBitshift) < llBottom - 1)
					{
						requestBot = true;
						checkBottomBleeding = false;
					}
				}
			}
		}
		
		// Sunlight propagation
		while (sunSources.size() > 0)
		{
			int y = sunSources.removeLast();
			int z = sunSources.removeLast();
			int x = sunSources.removeLast();

			int voxelData = chunk.chunkVoxelData[x * 1024 + y * 32 + z];
			int cellLightLevel = (voxelData & sunlightMask) >> sunBitshift;
			int cId = VoxelFormat.id(voxelData);
			voxel = world.getContentTranslator().getVoxelForId(cId);

			if (voxel.getDefinition().isOpaque())
				cellLightLevel = 0;
			if (cellLightLevel > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int rightData = chunk.chunkVoxelData[(x + 1) * 1024 + y * 32 + z];
					int llRight = cellLightLevel - voxel.getLightLevelModifier(voxelData, rightData, VoxelSides.RIGHT);
					if (!world.getContentTranslator().getVoxelForId((rightData & 0xFFFF)).getDefinition().isOpaque() && ((rightData & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						chunk.chunkVoxelData[(x + 1) * 1024 + y * 32 + z] = rightData & sunAntiMask | (llRight - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x + 1);
						sunSources.addLast(z);
						sunSources.addLast(y);
					}
				}
				else if (checkRightBleeding)
				{
					int rightData = adjacentChunkRight.peekRaw(0, y, z);
					int llRight = cellLightLevel - voxel.getLightLevelModifier(voxelData, rightData, VoxelSides.RIGHT);
					if (((rightData & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						requestRight = true;
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int leftData = chunk.chunkVoxelData[(x - 1) * 1024 + y * 32 + z];
					int llLeft = cellLightLevel - voxel.getLightLevelModifier(voxelData, leftData, VoxelSides.LEFT);
					if (!world.getContentTranslator().getVoxelForId((leftData & 0xFFFF)).getDefinition().isOpaque() && ((leftData & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						chunk.chunkVoxelData[(x - 1) * 1024 + y * 32 + z] = leftData & sunAntiMask | (llLeft - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x - 1);
						sunSources.addLast(z);
						sunSources.addLast(y);
					}
				}
				else if (checkLeftBleeding)
				{
					int leftData = adjacentChunkLeft.peekRaw(31, y, z);
					int llLeft = cellLightLevel - voxel.getLightLevelModifier(voxelData, leftData, VoxelSides.LEFT);
					if (((leftData & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						requestLeft = true;
						checkLeftBleeding = false;
					}
				}
				
				if (z < 31)
				{
					int frontData = chunk.chunkVoxelData[x * 1024 + y * 32 + z + 1];
					int llFront = cellLightLevel - voxel.getLightLevelModifier(voxelData, frontData, VoxelSides.FRONT);
					if (!world.getContentTranslator().getVoxelForId((frontData & 0xFFFF)).getDefinition().isOpaque() && ((frontData & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						chunk.chunkVoxelData[x * 1024 + y * 32 + z + 1] = frontData & sunAntiMask | (llFront - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z + 1);
						sunSources.addLast(y);
					}
				}
				else if (checkFrontBleeding)
				{
					int frontData = adjacentChunkFront.peekRaw(x, y, 0);
					int llFront = cellLightLevel - voxel.getLightLevelModifier(voxelData, frontData, VoxelSides.FRONT);
					if (((frontData & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						requestFront = true;
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int backData = chunk.chunkVoxelData[x * 1024 + y * 32 + z - 1];
					int llBack = cellLightLevel - voxel.getLightLevelModifier(voxelData, backData, VoxelSides.BACK);
					if (!world.getContentTranslator().getVoxelForId((backData & 0xFFFF)).getDefinition().isOpaque() && ((backData & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						chunk.chunkVoxelData[x * 1024 + y * 32 + z - 1] = backData & sunAntiMask | (llBack - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z - 1);
						sunSources.addLast(y);
					}
				}
				else if (checkBackBleeding)
				{
					int backData = adjacentChunkBack.peekRaw(x, y, 31);
					int llBack = cellLightLevel - voxel.getLightLevelModifier(voxelData, backData, VoxelSides.BACK);
					if (((backData & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						requestBack = true;
						checkBackBleeding = false;
					}
				}
				
				if (y < 31)
				{
					int topData = chunk.chunkVoxelData[x * 1024 + (y + 1) * 32 + z];
					int llTop = cellLightLevel - voxel.getLightLevelModifier(voxelData, topData, VoxelSides.TOP);
					if (!world.getContentTranslator().getVoxelForId((topData & 0xFFFF)).getDefinition().isOpaque() && ((topData & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						chunk.chunkVoxelData[x * 1024 + (y + 1) * 32 + z] = topData & sunAntiMask | (llTop - 1) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z);
						sunSources.addLast(y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int topData = adjacentChunkTop.peekRaw(x, 0, z);
					int llTop = cellLightLevel - voxel.getLightLevelModifier(voxelData, topData, VoxelSides.TOP);
					if (((topData & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						requestTop = true;
						checkTopBleeding = false;
					}
				}
				
				//Special case! This is the bottom computation for light spread, and thus we don't decrement automatially the light by one for each step !
				if (y > 0)
				{
					int belowData = chunk.chunkVoxelData[x * 1024 + (y - 1) * 32 + z];
					int llBottom = cellLightLevel - voxel.getLightLevelModifier(voxelData, belowData, VoxelSides.BOTTOM);
					if (!world.getContentTranslator().getVoxelForId(belowData).getDefinition().isOpaque() && ((belowData & sunlightMask) >> sunBitshift) < llBottom)
					{
						chunk.chunkVoxelData[x * 1024 + (y - 1) * 32 + z] = belowData & sunAntiMask | (llBottom) << sunBitshift;
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z);
						sunSources.addLast(y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int belowData = adjacentChunkBottom.peekRaw(x, 31, z);
					int llBottom = cellLightLevel - voxel.getLightLevelModifier(voxelData, belowData, VoxelSides.BOTTOM);
					if (((belowData & sunlightMask) >> sunBitshift) < llBottom)
					{
						requestBot = true;
						checkBottomBleeding = false;
					}
				}
			}
		}
		
		if(requestTop)
			adjacentChunkTop.lightBaker().requestLightningUpdate();
		if(requestBot)
			adjacentChunkBottom.lightBaker().requestLightningUpdate();
		if(requestLeft)
			adjacentChunkLeft.lightBaker().requestLightningUpdate();
		if(requestRight)
			adjacentChunkRight.lightBaker().requestLightningUpdate();
		if(requestBack)
			adjacentChunkBack.lightBaker().requestLightningUpdate();
		if(requestFront)
			adjacentChunkFront.lightBaker().requestLightningUpdate();

		return modifiedBlocks;
	}

	private void addChunkLightSources(IntDeque blockSources, IntDeque sunSources)
	{
		for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++)
			{
				int z = 31; // This is basically wrong since we work with cubic chunks
				boolean hitGroundYet = false;
				int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + a, chunkZ * 32 + b) + 1;
				while (z >= 0)
				{
					int block = chunk.chunkVoxelData[a * 1024 + z * 32 + b];
					int id = VoxelFormat.id(block);
					short ll = world.getContentTranslator().getVoxelForId(id).getLightLevel(block);
					if (ll > 0)
					{
						chunk.chunkVoxelData[a * 1024 + z * 32 + b] = chunk.chunkVoxelData[a * 1024 + z * 32 + b] & blockAntiMask | ((ll & 0xF) << blockBitshift);
						blockSources.addLast(a);
						blockSources.addLast(b);
						blockSources.addLast(z);
					}
					if (!hitGroundYet)
					{
						if (chunkY * 32 + z >= csh)
						{
							chunk.chunkVoxelData[a * 1024 + (z) * 32 + b] = chunk.chunkVoxelData[a * 1024 + (z) * 32 + b] & sunAntiMask | (15 << sunBitshift);
							sunSources.addLast(a);
							sunSources.addLast(b);
							sunSources.addLast(z);
							if (chunkY * 32 + z < csh || !world.getContentTranslator().getVoxelForId(VoxelFormat.id(chunk.chunkVoxelData[a * 1024 + (z) * 32 + b])).isAir())
								hitGroundYet = true;
						}
					}
					z--;
				}
			}
	}

	private int addAdjacentChunksLightSources(IntDeque blockSources, IntDeque sunSources)
	{
		int mods = 0;
		if (world != null)
		{
			Chunk cc;
			cc = world.getChunk(chunkX + 1, chunkY, chunkZ);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.peekRaw(0, c, b);
						int current_data = chunk.peekRaw(31, c, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(31, c, b, ndata);
							mods++;
							blockSources.addLast(31);
							blockSources.addLast(b);
							blockSources.addLast(c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(31, c, b, ndata);
							mods++;
							sunSources.addLast(31);
							sunSources.addLast(b);
							sunSources.addLast(c);
						}
					}
			}
			cc = world.getChunk(chunkX - 1, chunkY, chunkZ);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.peekRaw(31, c, b);
						int current_data = chunk.peekRaw(0, c, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(0, c, b, ndata);
							mods++;
							blockSources.addLast(0);
							blockSources.addLast(b);
							blockSources.addLast(c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(0, c, b, ndata);
							mods++;
							sunSources.addLast(0);
							sunSources.addLast(b);
							sunSources.addLast(c);
						}
					}
			}
			// Top chunk
			cc = world.getChunk(chunkX, chunkY + 1, chunkZ);
			if (cc != null && !cc.isAirChunk())
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.peekRaw(c, 0, b);
						int current_data = chunk.peekRaw(c, 31, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(c, 31, b, ndata);
							mods++;
							if (adjacent_blo > 2)
							{
								blockSources.addLast(c);
								blockSources.addLast(b);
								blockSources.addLast(31);
							}
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(c, 31, b, ndata);
							mods++;
							if (adjacent_sun > 2)
							{
								sunSources.addLast(c);
								sunSources.addLast(b);
								sunSources.addLast(31);
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
							int current_data = chunk.peekRaw(c, 31, b);
	
							int adjacent_blo = 0;
							int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
							int adjacent_sun = 15;
							int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
							if (adjacent_blo > 1 && adjacent_blo > current_blo)
							{
								int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
								chunk.pokeRawSilently(c, 31, b, ndata);
								mods++;
								if (adjacent_blo > 2)
								{
									blockSources.addLast(c);
									blockSources.addLast(b);
									blockSources.addLast(31);
								}
							}
							if (adjacent_sun > 1 && adjacent_sun > current_sun)
							{
								int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
								chunk.pokeRawSilently(c, 31, b, ndata);
								mods++;
								if (adjacent_sun > 2)
								{
									sunSources.addLast(c);
									sunSources.addLast(b);
									sunSources.addLast(31);
								}
							}
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
						int adjacent_data = cc.peekRaw(c, 31, b);
						int current_data = chunk.peekRaw(c, 0, b);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(c, 0, b, ndata);
							mods++;
							if (adjacent_blo > 2)
							{
								blockSources.addLast(c);
								blockSources.addLast(b);
								blockSources.addLast(0);
							}
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(c, 0, b, ndata);
							mods++;
							if (adjacent_sun > 2)
							{
								sunSources.addLast(c);
								sunSources.addLast(b);
								sunSources.addLast(0);
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
						int adjacent_data = cc.peekRaw(c, b, 0);
						int current_data = chunk.peekRaw(c, b, 31);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(c, b, 31, ndata);
							mods++;
							blockSources.addLast(c);
							blockSources.addLast(31);
							blockSources.addLast(b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(c, b, 31, ndata);
							mods++;
							sunSources.addLast(c);
							sunSources.addLast(31);
							sunSources.addLast(b);
						}
					}
			}
			cc = world.getChunk(chunkX, chunkY, chunkZ - 1);
			if (cc != null)
			{
				for (int b = 0; b < 32; b++)
					for (int c = 0; c < 32; c++)
					{
						int adjacent_data = cc.peekRaw(c, b, 31);
						int current_data = chunk.peekRaw(c, b, 0);

						int adjacent_blo = ((adjacent_data & blocklightMask) >>> blockBitshift);
						int current_blo = ((current_data & blocklightMask) >>> blockBitshift);
						int adjacent_sun = ((adjacent_data & sunlightMask) >>> sunBitshift);
						int current_sun = ((current_data & sunlightMask) >>> sunBitshift);
						if (adjacent_blo > 1 && adjacent_blo > current_blo)
						{
							int ndata = current_data & blockAntiMask | (adjacent_blo - 1) << blockBitshift;
							chunk.pokeRawSilently(c, b, 0, ndata);
							mods++;
							blockSources.addLast(c);
							blockSources.addLast(0);
							blockSources.addLast(b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & sunAntiMask | (adjacent_sun - 1) << sunBitshift;
							chunk.pokeRawSilently(c, b, 0, ndata);
							mods++;
							sunSources.addLast(c);
							sunSources.addLast(0);
							sunSources.addLast(b);
						}
					}
			}
		}
		
		return mods;
	}

	public void computeLightSpread(int bx, int by, int bz, int dataBefore, int data)
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

		IntDeque blockSourcesRemoval = tl_blockSourcesRemoval.get();
		IntDeque sunSourcesRemoval = tl_sunSourcesRemoval.get();
		IntDeque blockSources = tl_blockSources.get();
		IntDeque sunSources = tl_sunSources.get();

		blockSourcesRemoval.addLast(bx);
		blockSourcesRemoval.addLast(by);
		blockSourcesRemoval.addLast(bz);
		blockSourcesRemoval.addLast(blockLightBefore);

		sunSourcesRemoval.addLast(bx);
		sunSourcesRemoval.addLast(by);
		sunSourcesRemoval.addLast(bz);
		sunSourcesRemoval.addLast(sunLightBefore);

		propagateLightRemovalBeyondChunks(blockSources, sunSources, blockSourcesRemoval, sunSourcesRemoval);

		//Add light sources if relevant
		if (sunLightAfter > 0)
		{
			sunSources.addLast(bx);
			sunSources.addLast(bz);
			sunSources.addLast(by);
		}
		if (blockLightAfter > 0)
		{
			blockSources.addLast(bx);
			blockSources.addLast(bz);
			blockSources.addLast(by);
		}

		//Propagate remaining light
		this.propagateLightningBeyondChunk(blockSources, sunSources);
	}

	private void propagateLightRemovalBeyondChunks(IntDeque blockSources, IntDeque sunSources, IntDeque blockSourcesRemoval, IntDeque sunSourcesRemoval)
	{
		int bounds = 64;
		while (sunSourcesRemoval.size() > 0)
		{
			int sunLightLevel = sunSourcesRemoval.removeLast();
			int z = sunSourcesRemoval.removeLast();
			int y = sunSourcesRemoval.removeLast();
			int x = sunSourcesRemoval.removeLast();

			int neighborSunLightLevel;

			// X Axis
			if (x > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.addLast(x - 1);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x - 1);
					sunSources.addLast(z);
					sunSources.addLast(y);
				}
			}
			if (x < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.addLast(x + 1);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x + 1);
					sunSources.addLast(z);
					sunSources.addLast(y);
				}
			}
			// Y axis
			if (y > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel)
				{
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y - 1);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x);
					sunSources.addLast(z);
					sunSources.addLast(y - 1);
				}
			}
			if (y < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y + 1, z);

				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y + 1);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x);
					sunSources.addLast(z);
					sunSources.addLast(y + 1);
				}
			}
			// Z Axis
			if (z > -bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z - 1);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x);
					sunSources.addLast(z - 1);
					sunSources.addLast(y);
				}
			}
			if (z < bounds)
			{
				neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel)
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z + 1);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				}
				else if (neighborSunLightLevel >= sunLightLevel)
				{
					sunSources.addLast(x);
					sunSources.addLast(z + 1);
					sunSources.addLast(y);
				}
			}
		}

		while (blockSourcesRemoval.size() > 0)
		{
			int blockLightLevel = blockSourcesRemoval.removeLast();
			int z = blockSourcesRemoval.removeLast();
			int y = blockSourcesRemoval.removeLast();
			int x = blockSourcesRemoval.removeLast();

			int neighborBlockLightLevel;

			// X Axis
			if (x > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				//System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.addLast(x - 1);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x - 1);
					blockSources.addLast(z);
					blockSources.addLast(y);
				}
			}
			if (x < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.addLast(x + 1);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x + 1);
					blockSources.addLast(z);
					blockSources.addLast(y);
				}
			}
			// Y axis
			if (y > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y - 1);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x);
					blockSources.addLast(z);
					blockSources.addLast(y - 1);
				}
			}
			if (y < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y + 1);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x);
					blockSources.addLast(z);
					blockSources.addLast(y + 1);
				}
			}
			// Z Axis
			if (z > -bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z - 1);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x);
					blockSources.addLast(z - 1);
					blockSources.addLast(y);
				}
			}
			if (z < bounds)
			{
				neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel)
				{
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z + 1);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				}
				else if (neighborBlockLightLevel >= blockLightLevel)
				{
					blockSources.addLast(x);
					blockSources.addLast(z + 1);
					blockSources.addLast(y);
				}
			}
		}
	}

	private int propagateLightningBeyondChunk(IntDeque blockSources, IntDeque sunSources)
	{
		//int data[] = world.chunksData.grab(dataPointer);
		int modifiedBlocks = 0;
		int bounds = 64;

		// The ints are composed as : 0x0BSMIIII
		// Second pass : loop fill bfs algo
		Voxel voxel;
		while (blockSources.size() > 0)
		{
			int y = blockSources.removeLast();
			int z = blockSources.removeLast();
			int x = blockSources.removeLast();
			int voxelData = getWorldDataOnlyForLightningUpdatesFunctions(x, y, z);
			int ll = (voxelData & blocklightMask) >> blockBitshift;
			int cId = VoxelFormat.id(voxelData);

			voxel = world.getContentTranslator().getVoxelForId(cId);

			if (world.getContentTranslator().getVoxelForId(cId).getDefinition().isOpaque())
				ll = voxel.getLightLevel(voxelData);

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x + 1);
						blockSources.addLast(z);
						blockSources.addLast(y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x - 1);
						blockSources.addLast(z);
						blockSources.addLast(y);
					}
				}
				// Z-propagation
				if (z < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z + 1);
						blockSources.addLast(y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z - 1);
						blockSources.addLast(y);
					}
				}
				// Y-propagation
				if (y < bounds) // y = 254+1
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z);
						blockSources.addLast(y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & blocklightMask) >> blockBitshift) < ll - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(z);
						blockSources.addLast(y - 1);
					}
				}
			}
		}
		// Sunlight propagation
		while (sunSources.size() > 0)
		{
			int y = sunSources.removeLast();
			int z = sunSources.removeLast();
			int x = sunSources.removeLast();

			int voxelData = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z);
			int ll = (voxelData & sunlightMask) >> sunBitshift;
			int cId = VoxelFormat.id(voxelData);

			voxel = world.getContentTranslator().getVoxelForId(cId);

			if (voxel.getDefinition().isOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z);
					int llRight = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.RIGHT);

					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llRight - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x + 1, y, z, adj & sunAntiMask | (llRight - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x + 1);
						sunSources.addLast(z);
						sunSources.addLast(y);
					}
				}
				if (x > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z);
					int llLeft = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.LEFT);
					
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llLeft - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x - 1, y, z, adj & sunAntiMask | (llLeft - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x - 1);
						sunSources.addLast(z);
						sunSources.addLast(y);
					}
				}
				// Z-propagation
				if (z < bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1);
					int llFront = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.FRONT);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llFront - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z + 1, adj & sunAntiMask | (llFront - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z + 1);
						sunSources.addLast(y);
					}
				}
				if (z > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1);
					int llBack = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.BACK);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBack - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z - 1, adj & sunAntiMask | (llBack - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z - 1);
						sunSources.addLast(y);
					}
				}
				// Y-propagation
				if (y < bounds) // y = 254+1
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z);
					int llTop = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.TOP);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llTop - 1)
					{
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y + 1, z, adj & sunAntiMask | (llTop - 1) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z);
						sunSources.addLast(y + 1);
					}
				}
				if (y > -bounds)
				{
					int adj = this.getWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z);
					int llBottm = ll - voxel.getLightLevelModifier(voxelData, adj, VoxelSides.BOTTOM);
					if (!world.getContentTranslator().getVoxelForId(adj).getDefinition().isOpaque() && ((adj & sunlightMask) >> sunBitshift) < llBottm)
					{
						//removed = ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)
						this.setWorldDataOnlyForLightningUpdatesFunctions(x, y - 1, z, adj & sunAntiMask | (llBottm /* - removed */) << sunBitshift);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(z);
						sunSources.addLast(y - 1);
					}
				}
			}
		}

		return modifiedBlocks;
	}

	private int getWorldDataOnlyForLightningUpdatesFunctions(int x, int y, int z)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					chunk.peekRaw(x, y, z);
		return world.peekRaw(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32);
	}

	private void setWorldDataOnlyForLightningUpdatesFunctions(int x, int y, int z, int data)
	{
		//Still within bounds !
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					chunk.pokeRawSilently(x, y, z, data);
					return;
				}

		int oldData = world.peekRaw(x, y, z);
		world.pokeRawSilently(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, data);
		
		Chunk c = world.getChunk((x + chunkX * 32) / 32, (y + chunkY * 32) / 32, (z + chunkZ * 32) / 32);
		if (c != null && oldData != data)
			c.lightBaker().requestLightningUpdate();
	}

	private int getSunLight(int x, int y, int z)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					return VoxelFormat.sunlight(chunk.peekRaw(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.sunlight(this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z));
	}

	private int getBlockLight(int x, int y, int z)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
					return VoxelFormat.blocklight(chunk.peekRaw(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.blocklight(this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z));
	}

	private void setSunLight(int x, int y, int z, int level)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					chunk.pokeRawSilently(x, y, z, VoxelFormat.changeSunlight(chunk.peekRaw(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z, VoxelFormat.changeSunlight(this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z), level));
	}
	
	private void setBlockLight(int x, int y, int z, int level)
	{
		if (x > 0 && x < 31)
			if (y > 0 && y < 31)
				if (z > 0 && z < 31)
				{
					chunk.pokeRawSilently(x, y, z, VoxelFormat.changeBlocklight(chunk.peekRaw(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.setWorldDataOnlyForLightningUpdatesFunctions(x, y, z, VoxelFormat.changeBlocklight(this.getWorldDataOnlyForLightningUpdatesFunctions(x, y, z), level));
	}

	
	/** cleanup */
	public void destroy() {
		Task task = this.task;
		if(task != null)
			task.cancel();
	}
}
