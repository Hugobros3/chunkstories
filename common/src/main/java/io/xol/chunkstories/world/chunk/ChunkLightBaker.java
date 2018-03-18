//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.cell.ScratchCell;
import io.xol.engine.concurrency.SimpleLock;
import io.xol.engine.concurrency.TrivialFence;

//TODO use custom propagation for ALL propagation functions & cleanup this whole darn mess
/** Responsible for propagating voxel volumetric light */
public class ChunkLightBaker implements ChunkLightUpdater {
	final WorldImplementation world;
	final int chunkX, chunkY, chunkZ;
	final CubicChunk chunk;
	//final CubicChunk leftChunk, rightChunk, topChunk, bottomChunk, frontChunk, backChunk;

	final AtomicInteger unbakedUpdates = new AtomicInteger(0);
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
		unbakedUpdates.incrementAndGet();

		if(world instanceof WorldTool) {
			WorldTool tool = (WorldTool)world;
			if(!tool.isLightningEnabled()) {
				//System.out.println("too soon");
				return new TrivialFence();
			}
		}
		
		Task fence;

		taskLock.lock();

		if (task == null || task.isDone() || task.isCancelled()) {
			task = new TaskLightChunk(chunk, true);
			chunk.getWorld().getGameContext().tasks().scheduleTask(task);
		}

		fence = task;

		taskLock.unlock();

		return fence;
	}

	@Override
	public void spawnUpdateTaskIfNeeded() {
		if (unbakedUpdates.get() > 0) {
			taskLock.lock();

			if (task == null || task.isDone() || task.isCancelled()) {
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

	static ThreadLocal<IntDeque> tl_blockSources = new ThreadLocal<IntDeque>() {
		@Override
		protected IntDeque initialValue() {
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_sunSources = new ThreadLocal<IntDeque>() {
		@Override
		protected IntDeque initialValue() {
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_blockSourcesRemoval = new ThreadLocal<IntDeque>() {
		@Override
		protected IntDeque initialValue() {
			return new IntArrayDeque();
		}
	};
	static ThreadLocal<IntDeque> tl_sunSourcesRemoval = new ThreadLocal<IntDeque>() {
		@Override
		protected IntDeque initialValue() {
			return new IntArrayDeque();
		}
	};

	int computeVoxelLightningInternal(boolean adjacent) {
		// Checks first if chunk contains blocks
		if (chunk.chunkVoxelData == null)
			return 0; // Nothing to do

		// Lock the chunk & grab 2 queues
		IntDeque blockSources = tl_blockSources.get();
		IntDeque sunSources = tl_sunSources.get();

		// Reset any remnant data
		blockSources.clear();
		sunSources.clear();

		// Find our own light sources, add them
		this.addChunkLightSources(blockSources, sunSources);

		int mods = 0;

		// Load nearby chunks and check if they contain bright spots we haven't
		// accounted for yet
		if (adjacent)
			mods += addAdjacentChunksLightSources(blockSources, sunSources);

		// Propagates the light
		mods += propagateLightning(blockSources, sunSources, adjacent);

		return mods;
	}

	// Now entering lightning code part, brace yourselves
	private int propagateLightning(IntDeque blockSources, IntDeque sunSources, boolean adjacent) {
		int modifiedBlocks = 0;

		CubicChunk leftChunk, rightChunk, topChunk, bottomChunk, frontChunk, backChunk;
		
		topChunk = world.getChunk(chunkX, chunkY + 1, chunkZ);
		bottomChunk = world.getChunk(chunkX, chunkY - 1, chunkZ);
		frontChunk = world.getChunk(chunkX, chunkY, chunkZ + 1);
		backChunk = world.getChunk(chunkX, chunkY, chunkZ - 1);
		leftChunk = world.getChunk(chunkX - 1, chunkY, chunkZ);
		rightChunk = world.getChunk(chunkX + 1, chunkY, chunkZ);
		
		// Don't spam the requeue requests
		boolean checkTopBleeding = adjacent && (topChunk != null);
		boolean checkBottomBleeding = adjacent && (bottomChunk != null);
		boolean checkFrontBleeding = adjacent && (frontChunk != null);
		boolean checkBackBleeding = adjacent && (backChunk != null);
		boolean checkLeftBleeding = adjacent && (leftChunk != null);
		boolean checkRightBleeding = adjacent && (rightChunk != null);

		boolean requestTop = false;
		boolean requestBot = false;
		boolean requestFront = false;
		boolean requestBack = false;
		boolean requestLeft = false;
		boolean requestRight = false;

		ScratchCell cell = new ScratchCell(world);
		ScratchCell adj = new ScratchCell(world);
		while (blockSources.size() > 0) {
			int z = blockSources.removeLast();
			int y = blockSources.removeLast();
			int x = blockSources.removeLast();

			peek(x, y, z, cell);
			int cellLightLevel = cell.blocklight;

			if (cell.voxel.getDefinition().isOpaque())
				cellLightLevel = cell.voxel.getEmittedLightLevel(cell);

			if (cellLightLevel > 1) {
				if (x < 31) {
					peek(x + 1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.LEFT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x + 1);
						blockSources.addLast(y);
						blockSources.addLast(z);
					}
				} else if (checkRightBleeding) {
					peek(32, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.LEFT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestRight = true;
						checkRightBleeding = false;
					}
				}
				if (x > 0) {
					peek(x - 1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.RIGHT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x - 1);
						blockSources.addLast(y);
						blockSources.addLast(z);
					}
				} else if (checkLeftBleeding) {
					peek(-1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.RIGHT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestLeft = true;
						checkLeftBleeding = false;
					}
				}

				if (z < 31) {
					peek(x, y, z + 1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BACK) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y);
						blockSources.addLast(z + 1);
					}
				} else if (checkFrontBleeding) {
					peek(x, y, 32, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BACK) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestFront = true;
						checkFrontBleeding = false;
					}
				}
				if (z > 0) {
					peek(x, y, z - 1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.FRONT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y);
						blockSources.addLast(z - 1);
					}
				} else if (checkBackBleeding) {
					peek(x, y, -1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.FRONT) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestBack = true;
						checkBackBleeding = false;
					}
				}

				if (y < 31) {
					peek(x, y + 1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BOTTOM) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y + 1);
						blockSources.addLast(z);
					}
				} else if (checkTopBleeding) {
					peek(x, 32, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BOTTOM) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestTop = true;
						checkTopBleeding = false;
					}
				}

				if (y > 0) {
					peek(x, y - 1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.TOP) + 1);
					if (adj.blocklight < fadedLightLevel) {
						adj.blocklight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y - 1);
						blockSources.addLast(z);
					}
				} else if (checkBottomBleeding) {
					peek(x, -1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.TOP) + 1);
					if (adj.blocklight < fadedLightLevel) {
						requestBot = true;
						checkBottomBleeding = false;
					}
				}
			}
		}

		while (sunSources.size() > 0) {
			int z = sunSources.removeLast();
			int y = sunSources.removeLast();
			int x = sunSources.removeLast();

			peek(x, y, z, cell);
			int cellLightLevel = cell.sunlight;

			if (cell.voxel.getDefinition().isOpaque())
				cellLightLevel = cell.voxel.getEmittedLightLevel(cell);

			if (cellLightLevel > 1) {
				if (x < 31) {
					peek(x + 1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.LEFT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x + 1);
						sunSources.addLast(y);
						sunSources.addLast(z);
					}
				} else if (checkRightBleeding) {
					peek(32, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.LEFT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						requestRight = true;
						checkRightBleeding = false;
					}
				}
				if (x > 0) {
					peek(x - 1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.RIGHT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x - 1);
						sunSources.addLast(y);
						sunSources.addLast(z);
					}
				} else if (checkLeftBleeding) {
					peek(-1, y, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.RIGHT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						requestLeft = true;
						checkLeftBleeding = false;
					}
				}

				if (z < 31) {
					peek(x, y, z + 1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BACK) + 1);
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y);
						sunSources.addLast(z + 1);
					}
				} else if (checkFrontBleeding) {
					peek(x, y, 32, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BACK) + 1);
					if (adj.sunlight < fadedLightLevel) {
						requestFront = true;
						checkFrontBleeding = false;
					}
				}
				if (z > 0) {
					peek(x, y, z - 1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.FRONT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y);
						sunSources.addLast(z - 1);
					}
				} else if (checkBackBleeding) {
					peek(x, y, -1, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.FRONT) + 1);
					if (adj.sunlight < fadedLightLevel) {
						requestBack = true;
						checkBackBleeding = false;
					}
				}

				if (y < 31) {
					peek(x, y + 1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BOTTOM) + 1);
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y + 1);
						sunSources.addLast(z);
					}
				} else if (checkTopBleeding) {
					peek(x, 32, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.BOTTOM) + 1);
					if (adj.sunlight < fadedLightLevel) {
						requestTop = true;
						checkTopBleeding = false;
					}
				}

				// Special case! This is the bottom computation for light spread, light doesn't
				// fade when traveling backwards so we do not decrement fadedLightLevel !
				if (y > 0) {
					peek(x, y - 1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.TOP));
					if (adj.sunlight < fadedLightLevel) {
						adj.sunlight = fadedLightLevel;
						poke(adj);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y - 1);
						sunSources.addLast(z);
					}
				} else if (checkBottomBleeding) {
					peek(x, -1, z, adj);
					int fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSides.TOP));
					if (adj.sunlight < fadedLightLevel) {
						requestBot = true;
						checkBottomBleeding = false;
					}
				}
			}
		}

		if (requestTop)
			topChunk.lightBaker().requestLightningUpdate();
		if (requestBot)
			bottomChunk.lightBaker().requestLightningUpdate();
		if (requestLeft)
			leftChunk.lightBaker().requestLightningUpdate();
		if (requestRight)
			rightChunk.lightBaker().requestLightningUpdate();
		if (requestBack)
			backChunk.lightBaker().requestLightningUpdate();
		if (requestFront)
			frontChunk.lightBaker().requestLightningUpdate();

		return modifiedBlocks;
	}

	private void addChunkLightSources(IntDeque blockSources, IntDeque sunSources) {
		ScratchCell cell = new ScratchCell(world);
		for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++) {
				int y = 31; // This is basically wrong since we work with cubic chunks
				boolean hitGroundYet = false;
				int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + a, chunkZ * 32 + b);
				while (y >= 0) {
					peek(a, y, b, cell);
					int ll = cell.voxel.getEmittedLightLevel(cell);

					if (ll > 0) {
						cell.blocklight = ll;
						//chunk.chunkVoxelData[a * 1024 + y * 32 + b] = chunk.chunkVoxelData[a * 1024 + y * 32 + b] & blockAntiMask
						//		| ((ll & 0xF) << blockBitshift);
						blockSources.addLast(a);
						blockSources.addLast(y);
						blockSources.addLast(b);
					}
					
					if (!hitGroundYet && csh != RegionSummary.NO_DATA) {
						if (chunkY * 32 + y >= csh) {
							if (chunkY * 32 + y <= csh || !world.getContentTranslator().getVoxelForId(VoxelFormat.id(chunk.chunkVoxelData[a * 1024 + (y) * 32 + b])).isAir())
								hitGroundYet = true;
							else {
								cell.sunlight = 15;
								//chunk.chunkVoxelData[a * 1024 + (y) * 32 + b] = chunk.chunkVoxelData[a * 1024 + (y) * 32 + b] & sunAntiMask | (15 << sunBitshift);
								sunSources.addLast(a);
								sunSources.addLast(y);
								sunSources.addLast(b);
							}
						}
					}
					
					poke(cell);
					y--;
				}
			}
	}

	private int addAdjacentChunksLightSources(IntDeque blockSources, IntDeque sunSources) {
		ScratchCell cell = new ScratchCell(world);
		ScratchCell adj = new ScratchCell(world);

		CubicChunk leftChunk, rightChunk, topChunk, bottomChunk, frontChunk, backChunk;
		
		topChunk = world.getChunk(chunkX, chunkY + 1, chunkZ);
		bottomChunk = world.getChunk(chunkX, chunkY - 1, chunkZ);
		frontChunk = world.getChunk(chunkX, chunkY, chunkZ + 1);
		backChunk = world.getChunk(chunkX, chunkY, chunkZ - 1);
		leftChunk = world.getChunk(chunkX - 1, chunkY, chunkZ);
		rightChunk = world.getChunk(chunkX + 1, chunkY, chunkZ);
		
		int mods = 0;
		if (world != null) {
			if (rightChunk != null) {
				for (int z = 0; z < 32; z++)
					for (int y = 0; y < 32; y++) {
						peek(32, y, z, adj);
						peek(31, y, z, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.RIGHT) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
			if (leftChunk != null) {
				for (int z = 0; z < 32; z++)
					for (int y = 0; y < 32; y++) {
						peek(-1, y, z, adj);
						peek(0, y, z, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.LEFT) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
			if (topChunk != null && !topChunk.isAirChunk()) {
				for (int z = 0; z < 32; z++)
					for (int x = 0; x < 32; x++) {
						peek(x, 32, z, adj);
						peek(x, 31, z, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.TOP) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						modifier -= 1; // sunlight doesn't dim travelling downwards
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			} else {
				for (int x = 0; x < 32; x++)
					for (int z = 0; z < 32; z++) {
						peek(x, 32, z, adj);
						peek(x, 31, z, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.TOP);
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							poke(cell);
							mods++;
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
			if (bottomChunk != null) {
				for (int z = 0; z < 32; z++)
					for (int x = 0; x < 32; x++) {
						peek(x, -1, z, adj);
						peek(x, 0, z, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.BOTTOM) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
			// cc = world.getChunk(chunkX, chunkY, chunkZ + 1);
			if (frontChunk != null) {
				for (int y = 0; y < 32; y++)
					for (int x = 0; x < 32; x++) {
						peek(x, y, 32, adj);
						peek(x, y, 31, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.FRONT) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
			// cc = world.getChunk(chunkX, chunkY, chunkZ - 1);
			if (backChunk != null) {
				for (int y = 0; y < 32; y++)
					for (int x = 0; x < 32; x++) {
						peek(x, y, -1, adj);
						peek(x, y, 0, cell);

						int modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSides.BACK) + 1;
						if (adj.blocklight - modifier > cell.blocklight) {
							cell.blocklight = adj.blocklight - modifier;
							poke(cell);
							mods++;
							blockSources.addLast(cell.x & 0x1f);
							blockSources.addLast(cell.y & 0x1f);
							blockSources.addLast(cell.z & 0x1f);
						}
						if (adj.sunlight - modifier > cell.sunlight) {
							cell.sunlight = adj.sunlight - modifier;
							mods++;
							poke(cell);
							sunSources.addLast(cell.x & 0x1f);
							sunSources.addLast(cell.y & 0x1f);
							sunSources.addLast(cell.z & 0x1f);
						}
					}
			}
		}

		return mods;
	}

	/** Called when a voxel is changed */
	public void computeLightSpread(int bx, int by, int bz, int dataBefore, int data) {
		int sunLightBefore = VoxelFormat.sunlight(dataBefore);
		int blockLightBefore = VoxelFormat.blocklight(dataBefore);

		int sunLightAfter = VoxelFormat.sunlight(data);
		int blockLightAfter = VoxelFormat.blocklight(data);

		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx + chunkX * 32, bz + chunkZ * 32);
		int block_height = by + chunkY * 32;

		// If the block is at or above (never) the topmost tile it's sunlit
		if (block_height >= csh)
			sunLightAfter = 15;

		IntDeque blockSourcesRemoval = tl_blockSourcesRemoval.get();
		IntDeque sunSourcesRemoval = tl_sunSourcesRemoval.get();
		IntDeque blockSources = tl_blockSources.get();
		IntDeque sunSources = tl_sunSources.get();
		
		blockSourcesRemoval.clear();
		sunSourcesRemoval.clear();
		blockSources.clear();
		sunSources.clear();

		blockSourcesRemoval.addLast(bx);
		blockSourcesRemoval.addLast(by);
		blockSourcesRemoval.addLast(bz);
		blockSourcesRemoval.addLast(blockLightBefore);

		sunSourcesRemoval.addLast(bx);
		sunSourcesRemoval.addLast(by);
		sunSourcesRemoval.addLast(bz);
		sunSourcesRemoval.addLast(sunLightBefore);

		propagateLightRemovalBeyondChunks(blockSources, sunSources, blockSourcesRemoval, sunSourcesRemoval);

		// Add light sources if relevant
		if (sunLightAfter > 0) {
			sunSources.addLast(bx);
			sunSources.addLast(by);
			sunSources.addLast(bz);
		}
		if (blockLightAfter > 0) {
			blockSources.addLast(bx);
			blockSources.addLast(by);
			blockSources.addLast(bz);
		}

		// Propagate remaining light
		this.propagateLightningBeyondChunk(blockSources, sunSources);
	}

	//TODO use getLightLevelModifier
	private void propagateLightRemovalBeyondChunks(IntDeque blockSources, IntDeque sunSources, IntDeque blockSourcesRemoval, IntDeque sunSourcesRemoval) {
		
		int bounds = 64;
		while (sunSourcesRemoval.size() > 0) {
			int sunLightLevel = sunSourcesRemoval.removeLast();
			int z = sunSourcesRemoval.removeLast();
			int y = sunSourcesRemoval.removeLast();
			int x = sunSourcesRemoval.removeLast();

			// X Axis
			if (x > -bounds) {
				int neighborSunLightLevel = this.getSunLight(x - 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel) {
					this.setSunLight(x - 1, y, z, 0);
					sunSourcesRemoval.addLast(x - 1);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x - 1);
					sunSources.addLast(y);
					sunSources.addLast(z);
				}
			}
			if (x < bounds) {
				int neighborSunLightLevel = this.getSunLight(x + 1, y, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel) {
					this.setSunLight(x + 1, y, z, 0);
					sunSourcesRemoval.addLast(x + 1);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x + 1);
					sunSources.addLast(y);
					sunSources.addLast(z);
				}
			}
			// Y axis
			if (y > -bounds) {
				int neighborSunLightLevel = this.getSunLight(x, y - 1, z);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel <= sunLightLevel) {
					this.setSunLight(x, y - 1, z, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y - 1);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x);
					sunSources.addLast(y - 1);
					sunSources.addLast(z);
				}
			}
			if (y < bounds) {
				int neighborSunLightLevel = this.getSunLight(x, y + 1, z);

				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel) {
					this.setSunLight(x, y + 1, z, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y + 1);
					sunSourcesRemoval.addLast(z);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x);
					sunSources.addLast(y + 1);
					sunSources.addLast(z);
				}
			}
			// Z Axis
			if (z > -bounds) {
				int neighborSunLightLevel = this.getSunLight(x, y, z - 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel) {
					this.setSunLight(x, y, z - 1, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z - 1);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x);
					sunSources.addLast(y);
					sunSources.addLast(z - 1);
				}
			}
			if (z < bounds) {
				int neighborSunLightLevel = this.getSunLight(x, y, z + 1);
				if (neighborSunLightLevel > 0 && neighborSunLightLevel < sunLightLevel) // TODO wrong!
				{
					this.setSunLight(x, y, z + 1, 0);
					sunSourcesRemoval.addLast(x);
					sunSourcesRemoval.addLast(y);
					sunSourcesRemoval.addLast(z + 1);
					sunSourcesRemoval.addLast(neighborSunLightLevel);
				} else if (neighborSunLightLevel >= sunLightLevel) {
					sunSources.addLast(x);
					sunSources.addLast(y);
					sunSources.addLast(z + 1);
				}
			}
		}

		while (blockSourcesRemoval.size() > 0) {
			int blockLightLevel = blockSourcesRemoval.removeLast();
			int z = blockSourcesRemoval.removeLast();
			int y = blockSourcesRemoval.removeLast();
			int x = blockSourcesRemoval.removeLast();

			// X Axis
			if (x > -bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x - 1, y, z);
				// System.out.println(neighborBlockLightLevel + "|" + blockLightLevel);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x - 1, y, z, 0);
					blockSourcesRemoval.addLast(x - 1);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x - 1);
					blockSources.addLast(y);
					blockSources.addLast(z);
				}
			}
			if (x < bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x + 1, y, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x + 1, y, z, 0);
					blockSourcesRemoval.addLast(x + 1);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x + 1);
					blockSources.addLast(y);
					blockSources.addLast(z);
				}
			}
			// Y axis
			if (y > -bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x, y - 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x, y - 1, z, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y - 1);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x);
					blockSources.addLast(y - 1);
					blockSources.addLast(z);
				}
			}
			if (y < bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x, y + 1, z);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x, y + 1, z, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y + 1);
					blockSourcesRemoval.addLast(z);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x);
					blockSources.addLast(y + 1);
					blockSources.addLast(z);
				}
			}
			// Z Axis
			if (z > -bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x, y, z - 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x, y, z - 1, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z - 1);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x);
					blockSources.addLast(y);
					blockSources.addLast(z - 1);
				}
			}
			if (z < bounds) {
				int neighborBlockLightLevel = this.getBlockLight(x, y, z + 1);
				if (neighborBlockLightLevel > 0 && neighborBlockLightLevel < blockLightLevel) {
					this.setBlockLight(x, y, z + 1, 0);
					blockSourcesRemoval.addLast(x);
					blockSourcesRemoval.addLast(y);
					blockSourcesRemoval.addLast(z + 1);
					blockSourcesRemoval.addLast(neighborBlockLightLevel);
				} else if (neighborBlockLightLevel >= blockLightLevel) {
					blockSources.addLast(x);
					blockSources.addLast(y);
					blockSources.addLast(z + 1);
				}
			}
		}
	}

	//TODO use getLightLevelModifier
	private int propagateLightningBeyondChunk(IntDeque blockSources, IntDeque sunSources) {
		int modifiedBlocks = 0;
		int bounds = 64;

		ScratchCell cell = new ScratchCell(world);
		ScratchCell sideCell = new ScratchCell(world);
		while (blockSources.size() > 0) {
			int z = blockSources.removeLast();
			int y = blockSources.removeLast();
			int x = blockSources.removeLast();
			peek(x, y, z, cell);
			int ll = cell.getBlocklight();

			if (cell.getVoxel().getDefinition().isOpaque())
				ll = cell.getVoxel().getEmittedLightLevel(cell);

			if (ll > 1) {
				// X-propagation
				if (x < bounds) {
					int adj = this.peekRawFast(x + 1, y, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x + 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x + 1);
						blockSources.addLast(y);
						blockSources.addLast(z);
					}
				}
				if (x > -bounds) {
					int adj = this.peekRawFast(x - 1, y, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x - 1, y, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x - 1);
						blockSources.addLast(y);
						blockSources.addLast(z);
					}
				}
				// Z-propagation
				if (z < bounds) {
					int adj = this.peekRawFast(x, y, z + 1);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x, y, z + 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y);
						blockSources.addLast(z + 1);
					}
				}
				if (z > -bounds) {
					int adj = this.peekRawFast(x, y, z - 1);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x, y, z - 1, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y);
						blockSources.addLast(z - 1);
					}
				}
				// Y-propagation
				if (y < bounds) // y = 254+1
				{
					int adj = this.peekRawFast(x, y + 1, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x, y + 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y + 1);
						blockSources.addLast(z);
					}
				}
				if (y > -bounds) {
					int adj = this.peekRawFast(x, y - 1, z);
					if (!world.getContentTranslator().getVoxelForId((adj & 0xFFFF)).getDefinition().isOpaque()
							&& ((adj & blocklightMask) >> blockBitshift) < ll - 1) {
						this.pokeRawFast(x, y - 1, z, adj & blockAntiMask | (ll - 1) << blockBitshift);
						modifiedBlocks++;
						blockSources.addLast(x);
						blockSources.addLast(y - 1);
						blockSources.addLast(z);
					}
				}
			}
		}
		// Sunlight propagation
		while (sunSources.size() > 0) {
			int z = sunSources.removeLast();
			int y = sunSources.removeLast();
			int x = sunSources.removeLast();
			peek(x, y, z, cell);
			int ll = cell.sunlight;

			if (cell.getVoxel().getDefinition().isOpaque())
				ll = 0;

			if (ll > 1) {
				// X-propagation
				if (x < bounds) {
					peek(x + 1, y, z, sideCell);
					int llRight = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.LEFT);
					if (!sideCell.getVoxel().getDefinition().isOpaque() && sideCell.sunlight < llRight - 1) {
						sideCell.sunlight = llRight - 1;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x + 1);
						sunSources.addLast(y);
						sunSources.addLast(z);
					}
				}
				if (x > -bounds) {
					peek(x - 1, y, z, sideCell);
					int llLeft = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.RIGHT);
					if (!sideCell.voxel.getDefinition().isOpaque() && sideCell.sunlight < llLeft - 1) {
						sideCell.sunlight = llLeft - 1;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x - 1);
						sunSources.addLast(y);
						sunSources.addLast(z);
					}
				}
				// Z-propagation
				if (z < bounds) {
					peek(x, y, z + 1, sideCell);
					int llFront = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.BACK);
					if (!sideCell.voxel.getDefinition().isOpaque() && sideCell.sunlight < llFront - 1) {
						sideCell.sunlight = llFront - 1;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y);
						sunSources.addLast(z + 1);
					}
				}
				if (z > -bounds) {
					peek(x, y, z - 1, sideCell);
					int llBack = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.FRONT);
					if (!sideCell.voxel.getDefinition().isOpaque() && sideCell.sunlight < llBack - 1) {
						sideCell.sunlight = llBack - 1;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y);
						sunSources.addLast(z - 1);
					}
				}
				// Y-propagation
				if (y < bounds) {
					peek(x, y + 1, z, sideCell);
					int llTop = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.BOTTOM);
					if (!sideCell.voxel.getDefinition().isOpaque() && sideCell.sunlight < llTop - 1) {
						sideCell.sunlight = llTop - 1;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y + 1);
						sunSources.addLast(z);
					}
				}
				if (y > -bounds) {
					peek(x, y - 1, z, sideCell);
					int llBottom = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSides.TOP);
					if (!sideCell.voxel.getDefinition().isOpaque() && sideCell.sunlight < llBottom) {
						sideCell.sunlight = llBottom;
						poke(sideCell);
						modifiedBlocks++;
						sunSources.addLast(x);
						sunSources.addLast(y - 1);
						sunSources.addLast(z);
					}
				}
			}
		}
		return modifiedBlocks;
	}

	private CubicChunk findRelevantChunk(int x, int y, int z) {
		if (x >= 0 && x < 32)
			if (y >= 0 && y < 32)
				if (z >= 0 && z < 32)
					return chunk;
					
		/*
		if (x >= 0 && x < 32) {
			if (y >= 0 && y < 32) {
				if (z >= 0 && z < 32) {
					return chunk;
				} else if (z >= 32 && z < 64) {
					return frontChunk;
				} else if (z >= -32 && z < 0) {
					return backChunk;
				}
			} else if (z >= 0 && z < 32) {
				if (y >= 32 && y < 64) {
					return topChunk;
				} else if (y >= -32 && y < 0)
					return bottomChunk;
			}
		} else if (z >= 0 && z < 32 && y >= 0 && y < 32) {
			if (x >= 32 && x < 64) {
				return rightChunk;
			} else if (x >= -32 && x < 0)
				return leftChunk;
		}*/

		return null;
	}
	
	private int peekRawFast(int x, int y, int z) {
		CubicChunk relevantChunk = findRelevantChunk(x, y, z);
		if (relevantChunk != null) {
			return relevantChunk.peekRaw(x, y, z);
		}
		
		return world.peekRaw(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32);
	}

	private void peek(int x, int y, int z, ScratchCell cell) {
		cell.x = x;
		cell.y = y;
		cell.z = z;
		int rawData = peekRawFast(x, y, z);
		cell.voxel = world.getContentTranslator().getVoxelForId(VoxelFormat.id(rawData));
		cell.sunlight = VoxelFormat.sunlight(rawData);
		cell.blocklight = VoxelFormat.blocklight(rawData);
		cell.metadata = VoxelFormat.meta(rawData);
	}

	private void pokeRawFast(int x, int y, int z, int data) {
		// Still within bounds !
		CubicChunk relevantChunk = findRelevantChunk(x, y, z);
		if (relevantChunk != null) {
			chunk.pokeRawSilently(x, y, z, data);
			return;
		}
		
		int oldData = world.peekRaw(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32);
		world.pokeRawSilently(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, data);

		Chunk updateme = world.getChunkWorldCoordinates(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32);
		if (updateme != null && oldData != data)
			updateme.lightBaker().requestLightningUpdate();
	}

	private void poke(ScratchCell cell) {
		int data = VoxelFormat.format(world.getContentTranslator().getIdForVoxel(cell.voxel), cell.metadata, cell.sunlight, cell.blocklight);
		pokeRawFast(cell.x, cell.y, cell.z, data);
	}

	private int getSunLight(int x, int y, int z) {
		if (x >= 0 && x < 32)
			if (y >= 0 && y < 32)
				if (z >= 0 && z < 32)
					return VoxelFormat.sunlight(chunk.peekRaw(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.sunlight(this.peekRawFast(x, y, z));
	}

	private int getBlockLight(int x, int y, int z) {
		if (x >= 0 && x < 32)
			if (y >= 0 && y < 32)
				if (z >= 0 && z < 32)
					return VoxelFormat.blocklight(chunk.peekRaw(x, y, z));
		// Stronger implementation for unbound spread functions
		return VoxelFormat.blocklight(this.peekRawFast(x, y, z));
	}

	private void setSunLight(int x, int y, int z, int level) {
		if (x >= 0 && x < 32)
			if (y >= 0 && y < 32)
				if (z >= 0 && z < 32) {
					chunk.pokeRawSilently(x, y, z, VoxelFormat.changeSunlight(chunk.peekRaw(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.pokeRawFast(x, y, z, VoxelFormat.changeSunlight(this.peekRawFast(x, y, z), level));
	}

	private void setBlockLight(int x, int y, int z, int level) {
		if (x >= 0 && x < 32)
			if (y >= 0 && y < 32)
				if (z >= 0 && z < 32) {
					chunk.pokeRawSilently(x, y, z, VoxelFormat.changeBlocklight(chunk.peekRaw(x, y, z), level));
					return;
				}
		// Stronger implementation for unbound spread functions
		this.pokeRawFast(x, y, z, VoxelFormat.changeBlocklight(this.peekRawFast(x, y, z), level));
	}

	/** cleanup */
	public void destroy() {
		Task task = this.task;
		if (task != null)
			task.cancel();
	}
}
