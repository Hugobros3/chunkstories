package io.xol.chunkstories.renderer;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ByteBufferPool;
import io.xol.engine.misc.FloatBufferPool;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunksRenderer extends Thread
{
	AtomicBoolean die = new AtomicBoolean();

	World world;

	public Deque<int[]> todo = new ConcurrentLinkedDeque<int[]>();
	public Queue<VBOData> done = new ConcurrentLinkedQueue<VBOData>();

	public ByteBufferPool buffersPool;

	public class VBOData
	{
		int x, y, z;
		//ByteBuffer buf;
		int bufferId;
		int s_normal;
		int s_water;
		int s_complex;
	}

	public void requestChunkRender(CubicChunk chunk)
	{
		if (!chunk.requestable.get())
			return;

		int[] request = new int[] { chunk.chunkX, chunk.chunkY, chunk.chunkZ };
		boolean priority = chunk.need_render_fast.get();

		Iterator<int[]> iter = todo.iterator();
		int[] lelz;
		while (iter.hasNext())
		{
			lelz = iter.next();
			if (lelz[0] == request[0] && lelz[1] == request[1] && lelz[2] == request[2])
			{
				if (!priority)
					return;
				else
					iter.remove();
			}
		}

		// If it has been queued then it can't be asked again
		chunk.requestable.set(false);
		// Reset the priority flag
		chunk.need_render_fast.set(false);
		//chunk.need_render.set(false);

		if (priority)
			todo.addFirst(request);
		else
			todo.addLast(request);
		synchronized (this)
		{
			notifyAll();
		}
	}

	public void clear()
	{
		todo.clear();
	}

	public void purgeUselessWork(int pCX, int pCY, int pCZ, int sizeInChunks, int chunksViewDistance)
	{
		Iterator<int[]> iter = todo.iterator();
		int[] request;
		while (iter.hasNext())
		{
			request = iter.next();
			if ((LoopingMathHelper.moduloDistance(request[0], pCX, sizeInChunks) > chunksViewDistance + 1) || (LoopingMathHelper.moduloDistance(request[2], pCZ, sizeInChunks) > chunksViewDistance + 1) || (Math.abs(request[1] - pCY) > 4))
			{
				CubicChunk freed = world.getChunk(request[0], request[1], request[2], false);
				if (freed != null)
					freed.requestable.set(true);
				iter.remove();
			}
		}
	}

	public VBOData doneChunk()
	{
		return done.poll();
	}

	int worldSizeInChunks;

	public ChunksRenderer(World w)
	{
		world = w;
		// 8 buffers of 8Mb each (64Mb) for temp/scratch buffer memory
		buffersPool = new ByteBufferPool(8, 0x800000);
	}

	public void run()
	{
		System.out.println("Starting Chunk Renderer thread !");
		Thread.currentThread().setName("Chunk Renderer");
		worldSizeInChunks = world.getSizeInChunks();
		while (!die.get())
		{
			int[] task = todo.pollFirst();
			if (task == null)
			{
				try
				{
					synchronized (this)
					{
						//System.out.println("Nothing left to do. Sleep time");
						wait();
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				// long t = System.nanoTime();
				try
				{

					if (world.isChunkLoaded(task[0], task[1], task[2]))
					{
						CubicChunk work = world.getChunk(task[0], task[1], task[2], false);
						if (work.need_render.get())
						{
							int nearChunks = 0;
							if (world.isChunkLoaded(task[0] + 1, task[1], task[2]))
								nearChunks++;
							if (world.isChunkLoaded(task[0] - 1, task[1], task[2]))
								nearChunks++;
							if (world.isChunkLoaded(task[0], task[1], task[2] + 1))
								nearChunks++;
							if (world.isChunkLoaded(task[0], task[1], task[2] - 1))
								nearChunks++;

							if (nearChunks == 4)
							{
								int buffer_id = buffersPool.requestByteBuffer();
								while (buffer_id == -1)
								{
									try
									{
										Thread.sleep(200L);
									}
									catch (InterruptedException e)
									{
										e.printStackTrace();
									}
									buffer_id = buffersPool.requestByteBuffer();
								}
								renderChunk(work, buffer_id);
							}
							else
							{
								// Reschedule it ?
								work.requestable.set(true);
							}
						}
						else
						{
							System.out.println("For some reason this chunk is in the renderer todo pool, but doesnt want to be rendered.");
							work.requestable.set(true);
							// If can't do it, reschedule it
							// System.out.println("Forget about "+task[0]+":"+task[1]+":"+task[2]+", not circled ");
							/*
							 * synchronized(todo) { todo.add(task); }
							 */
						}
						work.requestable.set(true);
					}
				}
				catch (NullPointerException npe)
				{
					System.out.println("npe in chunks rendering (moving too fast ?)");
				}
			}
		}
		System.out.println("Stopping Chunk Renderer thread !");
	}

	private int getBlockData(CubicChunk c, int x, int y, int z)
	{
		int data = 0;

		if (x >= -32 && z >= -32 && y >= -32 && y < 64 && x < 64 && z < 64)
		{
			int relx = x < 0 ? 0 : (x >= 32 ? 2 : 1);
			int rely = y < 0 ? 0 : (y >= 32 ? 2 : 1);
			int relz = z < 0 ? 0 : (z >= 32 ? 2 : 1);
			CubicChunk target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			if (target != null)
				data = target.getDataAt(x, y, z);
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			data = Client.world.getDataAt(c.chunkX * 32 + x, c.chunkY * 32 + y, c.chunkZ * 32 + z);
		}
		/*if (x > 0 && z > 0 && y > 0 && y < 32 && x < 32 && z < 32)
		{
			data = c.getDataAt(x, y, z);
		}
		else
			data = Client.world.getDataAt(c.chunkX * 32 + x, c.chunkY * 32 + y, c.chunkZ * 32 + z);
		*/
		return data;
	}

	CubicChunk cached;

	private int getSunlight(CubicChunk c, int x, int y, int z)
	{
		int data = 0;
		if (x >= -32 && z >= -32 && y >= -32 && y < 64 && x < 64 && z < 64)
		{
			int relx = x < 0 ? 0 : (x >= 32 ? 2 : 1);
			int rely = y < 0 ? 0 : (y >= 32 ? 2 : 1);
			int relz = z < 0 ? 0 : (z >= 32 ? 2 : 1);
			CubicChunk target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			if (target != null && target.dataPointer != -1)
			{
				data = target.getDataAt(x, y, z);
				int blockID = VoxelFormat.id(data);
				return VoxelTypes.get(blockID).isVoxelOpaque() ? -1 : VoxelFormat.sunlight(data);
			}
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			return 0;
		}

		x += c.chunkX * 32;
		y += c.chunkY * 32;
		z += c.chunkZ * 32;

		// Look for a chunk with relevant lightning data
		cached = Client.world.getChunk(x / 32, y / 32, z / 32, false);
		if (cached != null && cached.dataPointer >= 0)
		{
			data = cached.getDataAt(x, y, z);

			int blockID = VoxelFormat.id(data);
			return VoxelTypes.get(blockID).isVoxelOpaque() ? -1 : VoxelFormat.sunlight(data);
		}

		// If all else fails, just use the heightmap information
		return Client.world.chunkSummaries.getHeightAt(x, z) <= y ? 15 : 0;
	}

	private int getBlocklight(CubicChunk c, int x, int y, int z)
	{
		int data = 0;

		// Is it in cache range ?
		if (x >= -32 && z >= -32 && y >= -32 && y < 64 && x < 64 && z < 64)
		{
			int relx = x < 0 ? 0 : (x >= 32 ? 2 : 1);
			int rely = y < 0 ? 0 : (y >= 32 ? 2 : 1);
			int relz = z < 0 ? 0 : (z >= 32 ? 2 : 1);
			CubicChunk target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			if (target != null)
				data = target.getDataAt(x, y, z);
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			data = Client.world.getDataAt(c.chunkX * 32 + x, c.chunkY * 32 + y, c.chunkZ * 32 + z);
		}

		/*if (y < 0 && c.chunkY == 0)
			y = 0;
		if (y > 255)
			y = 255;
		if (x > 0 && z > 0 && y > 0 && y < 32 && x < 32 && z < 32)
		{
			data = c.getDataAt(x, y, z);
		}
		else
			data = Client.world.getDataAt(c.chunkX * 32 + x, c.chunkY * 32 + y, c.chunkZ * 32 + z);
		*/
		int blockID = VoxelFormat.id(data);
		return VoxelTypes.get(blockID).isVoxelOpaque() ? 0 : VoxelFormat.blocklight(data);
	}

	float[] bakeLightColors(int bl1, int bl2, int bl3, int bl4, int sl1, int sl2, int sl3, int sl4)
	{
		float blocklightFactor = 0;

		float sunlightFactor = 0;

		float aoFactor = 4;

		if (sl1 >= 0) // If sunlight = -1 then it's a case of occlusion
		{
			blocklightFactor += bl1;
			sunlightFactor += sl1;
			aoFactor--;
		}
		if (sl2 >= 0)
		{
			blocklightFactor += bl2;
			sunlightFactor += sl2;
			aoFactor--;
		}
		if (sl3 >= 0)
		{
			blocklightFactor += bl3;
			sunlightFactor += sl3;
			aoFactor--;
		}
		if (sl4 >= 0)
		{
			blocklightFactor += bl4;
			sunlightFactor += sl4;
			aoFactor--;
		}
		if (aoFactor < 4) // If we're not 100% occlusion
		{
			blocklightFactor /= (4 - aoFactor);
			sunlightFactor /= (4 - aoFactor);
		}
		return new float[] { blocklightFactor / 15f, sunlightFactor / 15f, aoFactor / 4f };
	}

	private void addQuadTop(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{

		int llMs = getSunlight(c, sx, sy + 1, sz);
		int llMb = getBlocklight(c, sx, sy + 1, sz);

		int llAs = getSunlight(c, sx + 1, sy + 1, sz);
		int llBs = getSunlight(c, sx + 1, sy + 1, sz + 1);
		int llCs = getSunlight(c, sx, sy + 1, sz + 1);
		int llDs = getSunlight(c, sx - 1, sy + 1, sz + 1);

		int llEs = getSunlight(c, sx - 1, sy + 1, sz);
		int llFs = getSunlight(c, sx - 1, sy + 1, sz - 1);
		int llGs = getSunlight(c, sx, sy + 1, sz - 1);
		int llHs = getSunlight(c, sx + 1, sy + 1, sz - 1);

		int llAb = getBlocklight(c, sx + 1, sy + 1, sz);
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz + 1);
		int llCb = getBlocklight(c, sx, sy + 1, sz + 1);
		int llDb = getBlocklight(c, sx - 1, sy + 1, sz + 1);

		int llEb = getBlocklight(c, sx - 1, sy + 1, sz);
		int llFb = getBlocklight(c, sx - 1, sy + 1, sz - 1);
		int llGb = getBlocklight(c, sx, sy + 1, sz - 1);
		int llHb = getBlocklight(c, sx + 1, sy + 1, sz - 1);

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		// float amB = (llCb+llBb+llAb+llMb)/15f/4f;
		// float amS = (llCs+llBs+llAs+llMs)/15f/4f;
		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		// amB = (llCb+llDb+llEb+llMb)/15f/4f;
		// amS = (llCs+llDs+llEs+llMs)/15f/4f;
		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);
		// aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs,
		// llMs);

		// amB = (llGb+llHb+llAb+llMb)/15f/4f;
		// amS = (llGs+llHs+llAs+llMs)/15f/4f;
		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);
		// aoB = bakeLightColors(llGb, llHb, llAb ,llMb, llGs, llHs, llAs,
		// llMs);

		// amB = (llEb+llFb+llGb+llMb)/15f/4f;
		// amS = (llEs+llFs+llGs+llMs)/15f/4f;
		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);
		// aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs,
		// llMs);

		// float s = (llMs)/15f;
		// aoA = aoB = aoC = aoD = new float[]{s,s,s};

		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + (sx % texture.textureScale) * offset;
		int textureT = texture.atlasT + (sz % texture.textureScale) * offset;

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);
	}

	private void addQuadBottom(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{
		int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		int llAb = getBlocklight(c, sx + 1, sy, sz);
		int llBb = getBlocklight(c, sx + 1, sy, sz + 1);
		int llCb = getBlocklight(c, sx, sy, sz + 1);
		int llDb = getBlocklight(c, sx - 1, sy, sz + 1);

		int llEb = getBlocklight(c, sx - 1, sy, sz);
		int llFb = getBlocklight(c, sx - 1, sy, sz - 1);
		int llGb = getBlocklight(c, sx, sy, sz - 1);
		int llHb = getBlocklight(c, sx + 1, sy, sz - 1);

		int llAs = getSunlight(c, sx + 1, sy, sz);
		int llBs = getSunlight(c, sx + 1, sy, sz + 1);
		int llCs = getSunlight(c, sx, sy, sz + 1);
		int llDs = getSunlight(c, sx - 1, sy, sz + 1);

		int llEs = getSunlight(c, sx - 1, sy, sz);
		int llFs = getSunlight(c, sx - 1, sy, sz - 1);
		int llGs = getSunlight(c, sx, sy, sz - 1);
		int llHs = getSunlight(c, sx + 1, sy, sz - 1);

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);
		
		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + (sx % texture.textureScale) * offset;
		int textureT = texture.atlasT + (sz % texture.textureScale) * offset;
		
		rbbf.addVerticeInt(sx + 1, sy, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
	}

	private void addQuadRight(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{
		// ++x for dekal

		// +1 -1 0
		int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		int llAs = getSunlight(c, sx, sy + 1, sz); // ok
		int llBs = getSunlight(c, sx, sy + 1, sz + 1); // 1 1
		int llCs = getSunlight(c, sx, sy, sz + 1); // . 1
		int llDs = getSunlight(c, sx, sy - 1, sz + 1); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx, sy, sz - 1); // ok
		int llHs = getSunlight(c, sx, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz); // ok
		int llBb = getBlocklight(c, sx, sy + 1, sz + 1); // 1 1
		int llCb = getBlocklight(c, sx, sy, sz + 1); // . 1
		int llDb = getBlocklight(c, sx, sy - 1, sz + 1); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx, sy, sz - 1); // ok
		int llHb = getBlocklight(c, sx, sy + 1, sz - 1); // 1 -1
		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);

		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + mod(sz, texture.textureScale) * offset;
		int textureT = texture.atlasT + mod(-sy, texture.textureScale) * offset;
		
		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 1, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz + 1 );
		rbbf.addTexCoordInt( textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
	}

	private int mod(int a, int b)
	{
		int c = a % b;
		if (c >= 0)
			return c;
		return c += b;
	}

	private void addQuadLeft(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{
		int llMs = getSunlight(c, sx - 1, sy, sz);
		int llMb = getBlocklight(c, sx - 1, sy, sz);

		int llAs = getSunlight(c, sx - 1, sy + 1, sz); // 1 .
		int llBs = getSunlight(c, sx - 1, sy + 1, sz + 1); // 1 1
		int llCs = getSunlight(c, sx - 1, sy, sz + 1); // . 1
		int llDs = getSunlight(c, sx - 1, sy - 1, sz + 1); // -1 1

		int llEs = getSunlight(c, sx - 1, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz - 1); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx - 1, sy + 1, sz); // 1 .
		int llBb = getBlocklight(c, sx - 1, sy + 1, sz + 1); // 1 1
		int llCb = getBlocklight(c, sx - 1, sy, sz + 1); // . 1
		int llDb = getBlocklight(c, sx - 1, sy - 1, sz + 1); // -1 1

		int llEb = getBlocklight(c, sx - 1, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz - 1); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);



		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + mod(sz, texture.textureScale) * offset;
		int textureT = texture.atlasT + mod(-sy, texture.textureScale) * offset;
		
		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 1, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz + 1 );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

	}

	private void addQuadFront(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{
		int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		int llAs = getSunlight(c, sx, sy + 1, sz); // 1 .
		int llBs = getSunlight(c, sx + 1, sy + 1, sz); // 1 1
		int llCs = getSunlight(c, sx + 1, sy, sz); // . 1
		int llDs = getSunlight(c, sx + 1, sy - 1, sz); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz); // 1 .
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz); // 1 1
		int llCb = getBlocklight(c, sx + 1, sy, sz); // . 1
		int llDb = getBlocklight(c, sx + 1, sy - 1, sz); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);

		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + mod(sx, texture.textureScale) * offset;
		int textureT = texture.atlasT + mod(-sy, texture.textureScale) * offset;
		
		rbbf.addVerticeInt(sx, sy - 1, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);
		
		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 1, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);
		
		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

	}

	private void addQuadBack(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, VoxelTexture texture, boolean wavy)
	{

		int llMs = getSunlight(c, sx, sy, sz - 1);
		int llMb = getBlocklight(c, sx, sy, sz - 1);

		int llAs = getSunlight(c, sx, sy + 1, sz - 1); // 1 .
		int llBs = getSunlight(c, sx + 1, sy + 1, sz - 1); // 1 1
		int llCs = getSunlight(c, sx + 1, sy, sz - 1); // . 1
		int llDs = getSunlight(c, sx + 1, sy - 1, sz - 1); // -1 1

		int llEs = getSunlight(c, sx, sy - 1, sz - 1); // -1 .
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGs = getSunlight(c, sx - 1, sy, sz - 1); // . -1
		int llHs = getSunlight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		int llAb = getBlocklight(c, sx, sy + 1, sz - 1); // 1 .
		int llBb = getBlocklight(c, sx + 1, sy + 1, sz - 1); // 1 1
		int llCb = getBlocklight(c, sx + 1, sy, sz - 1); // . 1
		int llDb = getBlocklight(c, sx + 1, sy - 1, sz - 1); // -1 1

		int llEb = getBlocklight(c, sx, sy - 1, sz - 1); // -1 .
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1); // -1 -1
		int llGb = getBlocklight(c, sx - 1, sy, sz - 1); // . -1
		int llHb = getBlocklight(c, sx - 1, sy + 1, sz - 1); // 1 -1

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);
		// aoA = blendLights(amB,amS);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);

		int offset = texture.atlasOffset / texture.textureScale;
		int textureS = texture.atlasS + mod(sx, texture.textureScale) * offset;
		int textureT = texture.atlasT + mod(-sy, texture.textureScale) * offset;
		
		rbbf.addVerticeInt(sx, sy, sz );
		rbbf.addTexCoordInt(textureS, textureT );
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
		
		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 1, sz );
		rbbf.addTexCoordInt(textureS, textureT + offset );
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy - 1, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT + offset );
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
		
		rbbf.addVerticeInt(sx + 1, sy, sz );
		rbbf.addTexCoordInt(textureS + offset, textureT );
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
	}

	private void addVoxelUsingCustomModel(CubicChunk c, RenderByteBuffer rbbf, int sx, int sy, int sz, BlockRenderInfo info)
	{
		// Basic light for now
		// TODO interpolation
		int llMs = getSunlight(c, sx, sy, sz);
		int llMb = getBlocklight(c, sx, sy, sz);

		float[] lightColors = bakeLightColors(llMb, llMb, llMb, llMb, llMs, llMs, llMs, llMs);

		VoxelTexture texture = info.getTexture();
		VoxelModel model = info.getModel();

		int textureS = texture.atlasS;// +mod(sx,texture.textureScale)*offset;
		int textureT = texture.atlasT;// +mod(sz,texture.textureScale)*offset;

		Voxel occTest;

		sy--;

		float[] vert;
		float[] tex;
		float[] normal;

		boolean[] cullingCache = new boolean[6];
		for (int j = 0; j < 6; j++)
		{
			int id = VoxelFormat.id(info.neightborhood[j]);
			int meta = VoxelFormat.meta(info.neightborhood[j]);
			occTest = VoxelTypes.get(id);
			// If it is, don't draw it.
			cullingCache[j] = (occTest.isVoxelOpaque() || occTest.isFaceOpaque(j, info.neightborhood[j])) || occTest.isFaceOpaque(j, info.neightborhood[j])
					|| (info.voxelType.isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData());
			//System.out.println("generating culling cache for voxel "+VoxelFormat.id(info.data)+"y:"+sy+"model"+model.name+" cull:"+j+":"+cullingCache[j]);
		}

		if (model == null)
			return;

		float dx = 0f, dy = 0f, dz = 0f;
		if (model.jitterX != 0.0f)
			dx = (float) ((Math.random() * 2.0 - 1.0) * model.jitterX);
		if (model.jitterY != 0.0f)
			dy = (float) ((Math.random() * 2.0 - 1.0) * model.jitterY);
		if (model.jitterZ != 0.0f)
			dz = (float) ((Math.random() * 2.0 - 1.0) * model.jitterZ);

		for (int i = 0; i < model.vertices.length; i++)
		{
			vert = model.vertices[i];
			tex = model.texCoords[i];
			normal = model.normals[i];

			/*
			 * How culling works :
			 * culling[][] array contains [vertices.len/3][faces (6)] booleans
			 * for each triangle (vert/3) it checks for i 0 -> 6 that either ![v][i] or [v][i] && info.neightbours[i] is solid
			 * if any cull condition fails then it doesn't render this triangle.<
			 */
			int cullIndex = i / 3;
			boolean drawFace = true;
			for (int j = 0; j < 6; j++)
			{
				// Should check if face occluded ?
				if (model.culling[cullIndex][j])
				{
					/*int id = VoxelFormat.id(info.neightborhood[j]);
					int meta = VoxelFormat.meta(info.neightborhood[j]);
					occTest = VoxelTypes.get(id);
					// If it is, don't draw it.
					if(occTest.isVoxelOpaque() || (info.voxelType.isVoxelOpaqueWithItself() && id == VoxelFormat.id(info.data) && meta == info.getMetaData()))
						drawFace = false;*/

					if (cullingCache[j])
						drawFace = false;
				}
			}

			if (drawFace)
			{
				rbbf.addVerticeFloat(vert[0] + sx + dx, vert[1] + sy + dy, vert[2] + sz + dz);
				//vertices.add(new float[] { vert[0] + sx + dx, vert[1] + sy + dy, vert[2] + sz + dz });
				rbbf.addTexCoordInt((int) (textureS + tex[0] * texture.atlasOffset), (int) (textureT + tex[1] * texture.atlasOffset));
				//texcoords.add(new int[] { (int) (textureS + tex[0] * texture.atlasOffset), (int) (textureT + tex[1] * texture.atlasOffset) });
				rbbf.addColors(lightColors);
				//colors.add(lightColors);
				rbbf.addNormalsInt(intifyNormal(normal[0]), intifyNormal(normal[1]), intifyNormal(normal[2]), info.isWavy());
				//normals.add(normal);
				//if (isWavy != null)
				//	isWavy.add(info.isWavy());
			}
			else
			{
				//Skip the 2 other vertices
				i += 2;
			}
		}
	}

	private boolean shallBuildWallArround(BlockRenderInfo renderInfo, int face)
	{
		//int baseID = renderInfo.data;
		Voxel facing = VoxelTypes.get(renderInfo.getSideId(face));
		Voxel voxel = VoxelTypes.get(renderInfo.data);

		if (voxel.isVoxelLiquid() && !facing.isVoxelLiquid())
			return true;
		//if (voxel.isVoxelLiquid() && facing.isVoxelLiquid())
		//	return false;
		if (!facing.isVoxelOpaque() && (!voxel.sameKind(facing) || !voxel.isVoxelOpaqueWithItself()))
			return true;
		return false;
	}

	public static long renderStart = 0;

	public CubicChunk[] cache = new CubicChunk[27];

	Deque<Integer> blockSources = new ArrayDeque<Integer>();
	Deque<Integer> sunSources = new ArrayDeque<Integer>();

	//Work buffers
	ByteBuffer rawBlocksBuffer = BufferUtils.createByteBuffer(0x600000);
	ByteBuffer waterBlocksBuffer = BufferUtils.createByteBuffer(0x200000);
	ByteBuffer complexBlocksBuffer = BufferUtils.createByteBuffer(0x600000);

	@SuppressWarnings("unused")
	private void renderChunk(CubicChunk work, int byteBufferId)
	{
		ByteBuffer byteBuffer = buffersPool.accessByteBuffer(byteBufferId);

		// Update lightning as well if needed
		if (work == null)
		{
			buffersPool.releaseByteBuffer(byteBufferId);
			return;
		}

		if (work.needRelightning.getAndSet(false))
			work.doLightning(true, blockSources, sunSources);

		// Don't bother
		if (!work.need_render.get())
		{
			buffersPool.releaseByteBuffer(byteBufferId);
			return;
		}

		long cr_start = System.nanoTime();

		int cx = work.chunkX;
		int cy = work.chunkY;
		int cz = work.chunkZ;

		// For map borders
		boolean chunkTopLoaded = work.world.isChunkLoaded(cx, cy + 1, cz);
		boolean chunkBotLoaded = work.world.isChunkLoaded(cx, cy - 1, cz);
		// Useless ? ( no borders except ceiling in current worlds )
		boolean chunkRightLoaded = work.world.isChunkLoaded(cx + 1, cy, cz);
		boolean chunkLeftLoaded = work.world.isChunkLoaded(cx - 1, cy, cz);
		boolean chunkFrontLoaded = work.world.isChunkLoaded(cx, cy, cz + 1);
		boolean chunkBackLoaded = work.world.isChunkLoaded(cx, cy, cz - 1);

		// Fill chunk caches ( saves much time avoiding slow-ass world->chunkholders hashmap->chunk holder access for each vert )
		for (int relx = -1; relx <= 1; relx++)
			for (int rely = -1; rely <= 1; rely++)
				for (int relz = -1; relz <= 1; relz++)
					cache[((relx + 1) * 3 + (rely + 1)) * 3 + (relz + 1)] = work.world.getChunk(cx + relx, cy + rely, cz + relz, true);

		// Expensive bullshit

		rawBlocksBuffer.clear();
		RenderByteBuffer rawRBBF = new RenderByteBuffer(rawBlocksBuffer);

		waterBlocksBuffer.clear();
		RenderByteBuffer waterRBBF = new RenderByteBuffer(waterBlocksBuffer);

		complexBlocksBuffer.clear();
		RenderByteBuffer complexRBBF = new RenderByteBuffer(complexBlocksBuffer);
		

		long cr_iter = System.nanoTime();

		BlockRenderInfo renderInfo = new BlockRenderInfo(0);

		int i, j, k;
		//Don't waste time rendering void chunks m8
		if (work.dataPointer == -1)
			i = 32;
		for (i = 0; i < 32; i++)
		{
			for (j = 0; j < 32; j++)
			{
				for (k = 0; k < 32; k++)
				{
					int src = work.getDataAt(i, k, j);
					int blockID = VoxelFormat.id(src);

					if (blockID == 0)
						continue;
					Voxel vox = VoxelTypes.get(blockID);
					// Fill near-blocks info
					renderInfo.data = src;
					renderInfo.voxelType = vox;

					renderInfo.neightborhood[0] = getBlockData(work, i - 1, k, j);
					renderInfo.neightborhood[1] = getBlockData(work, i, k, j + 1);
					renderInfo.neightborhood[2] = getBlockData(work, i + 1, k, j);
					renderInfo.neightborhood[3] = getBlockData(work, i, k, j - 1);
					renderInfo.neightborhood[4] = getBlockData(work, i, k + 1, j);
					renderInfo.neightborhood[5] = getBlockData(work, i, k - 1, j);

					// System.out.println(blockID);
					if (vox.isVoxelLiquid())
					{
						addVoxelUsingCustomModel(work, waterRBBF, i, k, j, renderInfo);
					}
					else if (vox.isVoxelUsingCustomModel())
					{
						// Prop rendering
						addVoxelUsingCustomModel(work, complexRBBF, i, k, j, renderInfo);
					}
					else if (blockID != 0)
					{
						if (shallBuildWallArround(renderInfo, 5))
						{
							if (!(k == 0 && !chunkBotLoaded))
							{
								addQuadBottom(work, rawRBBF, i, k - 1, j, vox.getVoxelTexture(src, 1, renderInfo), renderInfo.isWavy());
							}
						}
						if (shallBuildWallArround(renderInfo, 4))
						{
							if (!(k == 31 && !chunkTopLoaded))
							{
								addQuadTop(work, rawRBBF, i, k, j, vox.getVoxelTexture(src, 0, renderInfo), renderInfo.isWavy());
							}
						}
						if (shallBuildWallArround(renderInfo, 2))
						{
							if (!(i == 31 && !chunkRightLoaded))
							{
								addQuadRight(work, rawRBBF, i + 1, k, j, vox.getVoxelTexture(src, 2, renderInfo), renderInfo.isWavy());
							}
						}
						if (shallBuildWallArround(renderInfo, 0))
						{
							if (!(i == 0 && !chunkLeftLoaded))
							{
								addQuadLeft(work, rawRBBF, i, k, j, vox.getVoxelTexture(src, 3, renderInfo), renderInfo.isWavy());
							}
						}
						if (shallBuildWallArround(renderInfo, 1))
						{
							if (!(j == 31 && !chunkFrontLoaded))
							{
								addQuadFront(work, rawRBBF, i, k, j + 1, vox.getVoxelTexture(src, 4, renderInfo), renderInfo.isWavy());
							}
						}
						if (shallBuildWallArround(renderInfo, 3))
						{
							if (!(j == 0 && !chunkBackLoaded))
							{
								addQuadBack(work, rawRBBF, i, k, j, vox.getVoxelTexture(src, 5, renderInfo), renderInfo.isWavy());
							}
						}
					}
				}
			}
		}

		long cr_convert = System.nanoTime();

		// Convert to floatBuffer
		VBOData rslt = new VBOData();
		rslt.x = work.chunkX;
		rslt.y = work.chunkY;
		rslt.z = work.chunkZ;

		byteBuffer.clear();
		rslt.bufferId = byteBufferId;// = byteBuffer;//BufferUtils.createByteBuffer(bufferTotalSize);

		long cr_buffer = System.nanoTime();
		
		rslt.s_normal = rawBlocksBuffer.position()/(16);
		rslt.s_complex = complexBlocksBuffer.position()/(24);
		rslt.s_water = waterBlocksBuffer.position()/(24);

		rawBlocksBuffer.limit(rawBlocksBuffer.position());
		rawBlocksBuffer.position(0);
		byteBuffer.put(rawBlocksBuffer);
		
		waterBlocksBuffer.limit(waterBlocksBuffer.position());
		waterBlocksBuffer.position(0);
		byteBuffer.put(waterBlocksBuffer);

		complexBlocksBuffer.limit(complexBlocksBuffer.position());
		complexBlocksBuffer.position(0);
		byteBuffer.put(complexBlocksBuffer);

		int count = 0;

		/*for (float[] f : vertices_water)
		{
			for (float z : f)
				byteBuffer.putFloat(z);
		}
		for (int[] f : texcoords_water)
		{
			for (int z : f)
			{
				byteBuffer.put((byte) (z & 0xFF));
				byteBuffer.put((byte) ((z >> 8) & 0xFF));
			}
		}
		for (float[] f : colors_water)
		{
			for (float z : f)
				byteBuffer.put((byte) (z * 255));
			// Padding
			byteBuffer.put((byte) 0);
		}
		for (float[] f : normals_water)
		{
			int a = (int) ((f[0] + 1) * 511.5f) & 0x3FF;
			int b = ((int) ((f[1] + 1) * 511.5f) & 0x3FF) << 10;
			int c = ((int) ((f[2] + 1) * 511.5f) & 0x3FF) << 20;
			int kek = a | b | c;
			byteBuffer.putInt(kek);
			count++;
		}

		// Complex objects
		for (float[] f : vertices_complex)
		{
			for (float z : f)
				byteBuffer.putFloat(z);
		}
		for (int[] f : texcoords_complex)
		{
			for (int z : f)
			{
				byteBuffer.put((byte) ((z) & 0xFF));
				byteBuffer.put((byte) ((z >> 8) & 0xFF));
			}
			// 2x2
		}
		for (float[] f : colors_complex)
		{
			for (float z : f)
				byteBuffer.put((byte) (z * 255));
			// Padding
			byteBuffer.put((byte) 0);
			// 3x1 + 1
		}
		count = 0;
		for (float[] f : normals_complex)
		{
			int a = (int) ((f[0] + 1) * .5f) & 0x3FF;
			int b = ((int) ((f[1] + 1) * 511.5f) & 0x3FF) << 10;
			int c = ((int) ((f[2] + 1) * 511.5f) & 0x3FF) << 20;
			boolean booleanProp = isWavy_complex.get(count);
			int d = (booleanProp ? 1 : 0) << 30;
			int kek = a | b | c | d;
			byteBuffer.putInt(kek);
			count++;
		}*/
		byteBuffer.flip();
		// System.out.println("Final vbo size = "+rsltSize*4);
		long lol = 0;
		//System.out.println("Took "+(System.nanoTime() - cr_start)+"ms total ; "+(cr_iter-cr_start)+" init, "+(cr_convert-cr_iter)+" iter, "
		//		+(cr_buffer-cr_convert)+" buffer, "+(System.nanoTime()-cr_buffer)+" convert since RS:"+(System.nanoTime()-ChunksRenderer.renderStart)+" ratio S/C : "+(1f+vertices.size())/(1f+vertices_complex.size())) ;

		done.add(rslt);

		totalChunksRendered.incrementAndGet();

		work.need_render.set(false);
		work.requestable.set(true);
	}

	int intifyNormal(float n)
	{
		return (int) ((n + 1) * 511.5f);
	}

	public AtomicInteger totalChunksRendered = new AtomicInteger();

	/*List<float[]> vertices = new ArrayList<float[]>();
	List<int[]> texcoords = new ArrayList<int[]>();
	List<float[]> colors = new ArrayList<float[]>();
	List<Boolean> isWavy = new ArrayList<Boolean>();
	List<int[]> normals = new ArrayList<int[]>();*/

	/*List<float[]> vertices_water = new ArrayList<float[]>();
	List<int[]> texcoords_water = new ArrayList<int[]>();
	List<float[]> colors_water = new ArrayList<float[]>();
	List<float[]> normals_water = new ArrayList<float[]>();

	List<float[]> vertices_complex = new ArrayList<float[]>();
	List<int[]> texcoords_complex = new ArrayList<int[]>();
	List<float[]> colors_complex = new ArrayList<float[]>();
	List<Boolean> isWavy_complex = new ArrayList<Boolean>();
	List<float[]> normals_complex = new ArrayList<float[]>();*/

	public void die()
	{
		die.set(true);
		synchronized (this)
		{
			notifyAll();
		}
	}
}
