package io.xol.chunkstories.world;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.generator.structures.GenerableStructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class CubicChunk
{
	public World world;
	public ChunkHolder holder;
	public int chunkX, chunkY, chunkZ;

	public int dataPointer = -1; // -1 means empty chunk (air)

	// Used in client rendering
	public AtomicBoolean need_render = new AtomicBoolean(true);
	public AtomicBoolean need_render_fast = new AtomicBoolean(false);
	public AtomicBoolean requestable = new AtomicBoolean(true);

	public AtomicBoolean needRelightning = new AtomicBoolean(true);

	public int vbo_id = -1;
	public int vbo_size_normal;
	public int vbo_size_complex;
	public int vbo_size_water;

	// Terrain Generation
	public List<GenerableStructure> structures = new ArrayList<GenerableStructure>();

	// Occlusion
	boolean occludedTop = false;
	boolean occludedBot = false;

	boolean occludedNorth = false;
	boolean occludedSouth = false;

	boolean occludedLeft = false;
	boolean occludedRight = false;

	public CubicChunk(World world, int chunkX, int chunkY, int chunkZ)
	{
		this.world = world;
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.chunkZ = chunkZ;
	}

	public int getDataAt(int x, int y, int z)
	{
		if (dataPointer == -1)
		{
			// System.out.println("lol null datapointer");
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

	public void setDataAbsolute(int x, int y, int z, int data)
	{
		setDataAt(x - chunkX * 32, y - chunkY * 32, z - chunkZ * 32, data);
	}

	public void setDataAt(int x, int y, int z, int data)
	{
		if (x < 0 || y < 0 || z < 0 || x >= 32 || y >= 32 || z >= 32)
			return;
		/*
		 * if(dataPointer < 0 && data == 0) return;
		 */
		if (dataPointer < 0)
		{
			// System.out.println("malloc for "+toString());
			dataPointer = world.chunksData.malloc();
		}
		if (dataPointer >= 0)
		{
			world.chunksData.grab(dataPointer)[x * 32 * 32 + y * 32 + z] = data;
			// need_render = true;
		}
	}

	public String toString()
	{
		return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + "]";
	}

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

	// Now entering lightning code part, brace yourselves

	//List<Integer> blockSources = new ArrayList<Integer>();
	//List<Integer> sunSources = new ArrayList<Integer>();

	public static long totalLightTimings = 0L;
	public static int totalLightRuns = 0;

	public void doLightning(boolean adjacent, Deque<Integer> blockSources, Deque<Integer> sunSources)
	{
		//if(true)
		//	return;

		long lightInitTime = System.nanoTime();

		// Whole chunk pass
		blockSources.clear();
		sunSources.clear();
		// Checks first if chunk contains blocks
		if (dataPointer < 0)
			return; // Nothing to do

		int data[] = world.chunksData.grab(dataPointer);

		// Reset chunk lightdata

		/*for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++)
				for (int c = 0; c < 32; c++)
					data[a * 1024 + b * 32 + c] = data[a * 1024 + b * 32 + c] & 0xF00FFFFF;
		*/

		// [a][b][c] => a*1024+b*32+c

		//int check_em = 0;

		// The ints are composed as : 0x0BSMIIII
		// First pass : find all light sources and mark'em
		// ChunkSummary cs = world.chunkSummaries.get(chunkX*32, chunkZ*32);
		for (int a = 0; a < 32; a++)
			for (int b = 0; b < 32; b++)
			{
				int z = 31; // This is basically wrong since we work with cubic chunks
				boolean hit = false;
				int csh = world.chunkSummaries.getHeightAt(chunkX * 32 + a, chunkZ * 32 + b) + 1;
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
						if (chunkY * 32 + z >= csh - 1)
						{
							data[a * 1024 + (z) * 32 + b] = data[a * 1024 + (z) * 32 + b] & 0xFF0FFFFF | (15 << 0x14);
							//sunSources.push(a << 16 | b << 8 | z);
							sunSources.push(a);
							sunSources.push(b);
							sunSources.push(z);

							if (chunkY * 32 + z < csh - 1 || VoxelTypes.get(VoxelFormat.id(data[a * 1024 + (z) * 32 + b])).isVoxelOpaque())
							{
								hit = true;
							}
							//check_em++;
						}
					}
					z--;
				}
			}

		//System.out.println("tg"+check_em);
		// Load nearby chunks and check for lightning
		if (world != null && adjacent)
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
							setDataAt(31, c, b, ndata);
							blockSources.push(31);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(31 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAt(31, c, b, ndata);

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
							setDataAt(0, c, b, ndata);

							blockSources.push(0);
							blockSources.push(b);
							blockSources.push(c);
							//blockSources.push(0 << 16 | b << 8 | c);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAt(0, c, b, ndata);

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
							setDataAt(c, 31, b, ndata);
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
							setDataAt(c, 31, b, ndata);
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
						int heightInSummary = world.chunkSummaries.getHeightAt(chunkX * 32 + b, chunkZ * 32 + c);
						// System.out.println("compute "+heightInSummary+" <= ? "+chunkY*32);
						if (heightInSummary <= chunkY * 32)
						{
							int sourceAt = chunkY * 32 - heightInSummary;
							sourceAt = Math.min(31, sourceAt);
							int current_data = getDataAt(b, sourceAt, c);

							int ndata = current_data & 0xFF0FFFFF | (15) << 0x14;
							setDataAt(b, sourceAt, c, ndata);

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
							setDataAt(c, 0, b, ndata);
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
							setDataAt(c, 0, b, ndata);
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
							setDataAt(c, b, 31, ndata);
							blockSources.push(c);
							blockSources.push(31);
							blockSources.push(b);
							//blockSources.push(c << 16 | 31 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAt(c, b, 31, ndata);
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
							setDataAt(c, b, 0, ndata);
							blockSources.push(c);
							blockSources.push(0);
							blockSources.push(b);
							//blockSources.push(c << 16 | 0 << 8 | b);
						}
						if (adjacent_sun > 1 && adjacent_sun > current_sun)
						{
							int ndata = current_data & 0xFF0FFFFF | (adjacent_sun - 1) << 0x14;
							setDataAt(c, b, 0, ndata);
							sunSources.push(c);
							sunSources.push(0);
							sunSources.push(b);
							//sunSources.push(c << 16 | 0 << 8 | b);
						}
					}
			}
		}

		// The ints are composed as : 0x0BSMIIII
		// Second pass : loop fill bfs algo
		CubicChunk adjacentChunkTop = world.getChunk(chunkX, chunkY + 1, chunkZ, false);
		CubicChunk adjacentChunkBottom = world.getChunk(chunkX, chunkY - 1, chunkZ, false);
		CubicChunk adjacentChunkFront = world.getChunk(chunkX, chunkY, chunkZ + 1, false);
		CubicChunk adjacentChunkBack = world.getChunk(chunkX, chunkY, chunkZ - 1, false);
		CubicChunk adjacentChunkLeft = world.getChunk(chunkX - 1, chunkY, chunkZ, false);
		CubicChunk adjacentChunkRight = world.getChunk(chunkX + 1, chunkY, chunkZ, false);
		boolean checkTopBleeding = (adjacentChunkTop != null) && !adjacentChunkTop.needRelightning.get();
		boolean checkBottomBleeding = (adjacentChunkBottom != null) && !adjacentChunkBottom.needRelightning.get();
		boolean checkFrontBleeding = (adjacentChunkFront != null) && !adjacentChunkFront.needRelightning.get();
		boolean checkBackBleeding = (adjacentChunkBack != null) && !adjacentChunkBack.needRelightning.get();
		boolean checkLeftBleeding = (adjacentChunkLeft != null) && !adjacentChunkLeft.needRelightning.get();
		boolean checkRightBleeding = (adjacentChunkRight != null) && !adjacentChunkRight.needRelightning.get();
		while (blockSources.size() > 0)
		{
			//int xyz = blockSources.remove(blockSources.size() - 1);
			//int x = xyz >>> 16;
			//int z = (xyz >>> 8) & 0xFF;
			//int y = xyz & 0xFF;
			// ==== NUL ====
			int y = blockSources.pop();
			int z = blockSources.pop();
			int x = blockSources.pop();
			int voxeData = data[x * 1024 + y * 32 + z];
			int ll = (voxeData & 0x0F000000) >> 0x18;
			int cId = VoxelFormat.id(voxeData);

			if (VoxelTypes.get(cId).isVoxelOpaque())
				ll = 0;

			if (ll > 1)
			{
				// X-propagation
				if (x < 31)
				{
					int adj = data[(x + 1) * 1024 + y * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x0F000000) >> 0x18) < ll - 1)
					{
						data[(x + 1) * 1024 + y * 32 + z] = adj & 0xF0FFFFFF | (ll - 1) << 0x18;
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
			/*int xyz = sunSources.remove(sunSources.size() - 1);
			int x = xyz >>> 16;
			int z = (xyz >>> 8) & 0xFF;
			int y = xyz & 0xFF;*/

			int y = sunSources.pop();
			int z = sunSources.pop();
			int x = sunSources.pop();

			int voxeData = data[x * 1024 + y * 32 + z];
			int ll = (voxeData & 0x00F00000) >> 0x14;
			int cId = VoxelFormat.id(voxeData);
			//if(cId == 25)
			//	System.out.println("topkek A "+ll);
			if (VoxelTypes.get(cId).isVoxelOpaque())
				ll = 0;

			//if(cId == 25)
			//	System.out.println("topkek B "+ll);

			if (ll > 1)
			{
				//if(cId == 25)
				//	System.out.println("topkek B "+ll);

				// X-propagation
				if (x < 31)
				{
					int adj = data[(x + 1) * 1024 + y * 32 + z];
					//if(cId == 25)
					//	System.out.println(adj & 0xFFFF);
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						data[(x + 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (ll - 1) << 0x14;
						sunSources.push(x + 1);
						sunSources.push(z);
						sunSources.push(y);
						//sunSources.push(x + 1 << 16 | z << 8 | y);
					}
				}
				else if (checkRightBleeding)
				{
					int adjacentSunlight = (adjacentChunkRight.getDataAt(0, y, z) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkRight.needRelightning.set(true);
						checkRightBleeding = false;
					}
				}
				if (x > 0)
				{
					int adj = data[(x - 1) * 1024 + y * 32 + z];
					//int id = (adj & 0xFFFF);
					//if(id == 25)
					//	System.out.println("topikek"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						//if(id == 25)
						//	System.out.println("MAIS LEL TARACE"+VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() + " -> " +((adj & 0x00F00000) >> 0x14));
						data[(x - 1) * 1024 + y * 32 + z] = adj & 0xFF0FFFFF | (ll - 1) << 0x14;
						sunSources.push(x - 1);
						sunSources.push(z);
						sunSources.push(y);
						//sunSources.push(x - 1 << 16 | z << 8 | y);
					}
				}
				else if (checkLeftBleeding)
				{
					int adjacentSunlight = (adjacentChunkLeft.getDataAt(31, y, z) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkLeft.needRelightning.set(true);
						checkLeftBleeding = false;
					}
				}
				// Z-propagation
				if (z < 31)
				{
					int adj = data[x * 1024 + y * 32 + z + 1];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						data[x * 1024 + y * 32 + z + 1] = adj & 0xFF0FFFFF | (ll - 1) << 0x14;
						sunSources.push(x);
						sunSources.push(z + 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z + 1 << 8 | y);
					}
				}
				else if (checkFrontBleeding)
				{
					int adjacentSunlight = (adjacentChunkFront.getDataAt(x, y, 0) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkFront.needRelightning.set(true);
						checkFrontBleeding = false;
					}
				}
				if (z > 0)
				{
					int adj = data[x * 1024 + y * 32 + z - 1];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						data[x * 1024 + y * 32 + z - 1] = adj & 0xFF0FFFFF | (ll - 1) << 0x14;
						sunSources.push(x);
						sunSources.push(z - 1);
						sunSources.push(y);
						//sunSources.push(x << 16 | z - 1 << 8 | y);
					}
				}
				else if (checkBackBleeding)
				{
					int adjacentSunlight = (adjacentChunkBack.getDataAt(x, y, 31) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkBack.needRelightning.set(true);
						checkBackBleeding = false;
					}
				}
				// Y-propagation
				if (y < 31) // y = 254+1
				{
					int adj = data[x * 1024 + (y + 1) * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						data[x * 1024 + (y + 1) * 32 + z] = adj & 0xFF0FFFFF | (ll - 1) << 0x14;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y + 1);
						//sunSources.push(x << 16 | z << 8 | y + 1);
					}
				}
				else if (checkTopBleeding)
				{
					int adjacentSunlight = (adjacentChunkTop.getDataAt(x, 0, z) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkTop.needRelightning.set(true);
						checkTopBleeding = false;
					}
				}
				if (y > 0)
				{
					int adj = data[x * 1024 + (y - 1) * 32 + z];
					if (!VoxelTypes.get((adj & 0xFFFF)).isVoxelOpaque() && ((adj & 0x00F00000) >> 0x14) < ll - 1)
					{
						//TODO fix this filthy hack and do proper light shading
						data[x * 1024 + (y - 1) * 32 + z] = adj & 0xFF0FFFFF | (ll - ((((data[x * 1024 + y * 32 + z] & 0x000000FF) == 128)) ? 1 : 0)) << 0x14;
						sunSources.push(x);
						sunSources.push(z);
						sunSources.push(y - 1);
						//sunSources.push(x << 16 | z << 8 | y - 1);
					}
				}
				else if (checkBottomBleeding)
				{
					int adjacentSunlight = (adjacentChunkBottom.getDataAt(x, 31, z) & 0xFF0FFFFF) << 0x14;
					if (ll > adjacentSunlight + 1)
					{
						adjacentChunkBottom.needRelightning.set(true);
						checkBottomBleeding = false;
					}
				}
			}
		}
		long totalLightTime = System.nanoTime() - lightInitTime;
		totalLightTimings += totalLightTime;
		totalLightRuns++;
		//System.out.println("Thsi time : " + totalLightTime / 1000f + " Avg : " + (totalLightTimings / totalLightRuns) / 1000f);
	}
}
