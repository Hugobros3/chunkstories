package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.ChunkMeshesBaker;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool.RecyclableByteBuffer;
import io.xol.chunkstories.api.Content.Voxels;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.math.LoopingMathHelper;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.BufferUtils;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkMeshesBakerThread extends Thread implements ChunkMeshesBaker
{
	private AtomicBoolean die = new AtomicBoolean();

	private final WorldClient world;

	public Deque<int[]> todoQueue = new ConcurrentLinkedDeque<int[]>();
	//public Queue<MeshedChunkData> doneQueue = new ConcurrentLinkedQueue<MeshedChunkData>();

	public ByteBufferPool buffersPool;

	int worldSizeInChunks;

	public int[][] cache = new int[27][];

	Deque<Integer> blockSources = new ArrayDeque<Integer>();
	Deque<Integer> sunSources = new ArrayDeque<Integer>();

	//Work buffers
	ByteBuffer[][][] byteBuffers;
	RenderByteBuffer[][][] byteBuffersWrappers;
	
	//Nasty !
	int i = 0, j = 0, k = 0;
	
	Chunk cached;
	
	DefaultVoxelRenderer defaultVoxelRenderer;

	public ChunkMeshesBakerThread(WorldClient world)
	{
		this.world = world;
		
		//8 buffers of 8Mb each (64Mb) for uploading to VRAM temporary
		//TODO as this isn't a quite realtime thread, consider not pooling those to improve memory usage efficiency.
		buffersPool = new ByteBufferPool(8, 0x800000);
		
		byteBuffers = new ByteBuffer[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		byteBuffersWrappers = new RenderByteBuffer[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		
		//Allocate dedicated sizes for relevant buffers
		byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.ANY.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x800000);
		byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.LOW.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x400000);
		byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.HIGH.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x800000);

		byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.ANY.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x800000);
		byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.LOW.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x400000);
		byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.HIGH.ordinal()][ShadingType.OPAQUE.ordinal()] = BufferUtils.createByteBuffer(0x400000);
		
		//Allocate more reasonnable size for other buffers and give them all a wrapper
		for(int i = 0; i < ChunkMeshDataSubtypes.VertexLayout.values().length; i++)
		{
			for(int j = 0; j < ChunkMeshDataSubtypes.LodLevel.values().length; j++)
			{
				for(int k = 0; k < ChunkMeshDataSubtypes.ShadingType.values().length; k++)
				{
					if(byteBuffers[i][j][k] == null)
						byteBuffers[i][j][k] = BufferUtils.createByteBuffer(0x100000);
					
					byteBuffersWrappers[i][j][k] = new RenderByteBuffer(byteBuffers[i][j][k]);
				}
			}
		}
		
		defaultVoxelRenderer = new DefaultVoxelRenderer();
		
		this.start();
	}

	public void requestChunkRender(ChunkRenderable chunk)
	{
		if (!(chunk.isMarkedForReRender() /*|| chunk.needsLightningUpdates()*/) || chunk.isRenderAleadyInProgress())
			return;

		int[] request = new int[] { chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ() };
		boolean priority = false; //chunk.need_render_fast.get();

		Iterator<int[]> iter = todoQueue.iterator();
		int[] lelz;
		while (iter.hasNext())
		{
			lelz = iter.next();
			if (lelz[0] == request[0] && lelz[1] == request[1] && lelz[2] == request[2])
			{
				if (!priority)
					return;
				else
				{
					//System.out.println("answer");
					iter.remove();
				}
			}
		}

		// If it has been queued then it can't be asked again
		chunk.markRenderInProgress(true);
		// Reset the priority flag

		//chunk.need_render.set(false);

		if (priority)
			todoQueue.addFirst(request);
		else
			todoQueue.addLast(request);

		//System.out.println("Added "+chunk);

		synchronized (this)
		{
			notifyAll();
		}
	}

	public void clear()
	{
		todoQueue.clear();
	}

	public void purgeUselessWork(int pCX, int pCY, int pCZ, int sizeInChunks, int chunksViewDistance)
	{
		Iterator<int[]> iter = todoQueue.iterator();
		int[] request;
		while (iter.hasNext())
		{
			request = iter.next();
			if ((LoopingMathHelper.moduloDistance(request[0], pCX, sizeInChunks) > chunksViewDistance + 1) || (LoopingMathHelper.moduloDistance(request[2], pCZ, sizeInChunks) > chunksViewDistance + 1) || (Math.abs(request[1] - pCY) > 4))
			{
				Chunk freed = world.getChunk(request[0], request[1], request[2]);
				if (freed != null && freed instanceof ChunkRenderable)
					((ChunkRenderable) freed).markRenderInProgress(false);

				//System.out.println("Removed "+freed);
				iter.remove();
			}
		}
	}

	/*public MeshedChunkData getNextRenderedChunkData()
	{
		return doneQueue.poll();
	}*/

	@Override
	public void run()
	{
		System.out.println("Starting Chunk Renderer thread !");
		Thread.currentThread().setName("Chunk Renderer");
		Thread.currentThread().setPriority(Constants.CHUNKS_RENDERER_THREAD_PRIORITY);

		worldSizeInChunks = world.getSizeInChunks();
		while (!die.get())
		{
			int[] task = todoQueue.pollFirst();
			if (task == null)
			{
				//System.out.println("cuck");
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
					//System.out.println("cuck");

					if (world.isChunkLoaded(task[0], task[1], task[2]))
					{
						ChunkRenderable work = (ChunkRenderable) world.getChunk(task[0], task[1], task[2]);
						if (work.isMarkedForReRender() || work.needsLightningUpdates())
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
								RecyclableByteBuffer buffer = buffersPool.requestByteBuffer();
								//int buffer_id = buffersPool.requestByteBuffer();
								while (buffer == null)
								//while (buffer_id == -1)
								{
									try
									{
										Thread.sleep(200L);
									}
									catch (InterruptedException e)
									{
										e.printStackTrace();
									}

									buffer = buffersPool.requestByteBuffer();
									//buffer_id = buffersPool.requestByteBuffer();
								}
								if (work instanceof CubicChunk)
									try
									{
										renderChunk((CubicChunk) work, buffer);
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
							}
							else
							{
								//System.out.println("unfit");
								// Reschedule it ?
								work.markRenderInProgress(false);
							}
						}
						else
						{
							System.out.println("For some reason this chunk is in the renderer todo pool, but doesnt want to be rendered.");
							work.markRenderInProgress(false);
							// If can't do it, reschedule it
							// System.out.println("Forget about "+task[0]+":"+task[1]+":"+task[2]+", not circled ");
							/*
							 * synchronized(todo) { todo.add(task); }
							 */
						}
						work.markRenderInProgress(false);
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
			int[] target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			x &= 0x1F;
			y &= 0x1F;
			z &= 0x1F;
			if (target != null)
				data = target[x * 1024 + y * 32 + z];
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			data = Client.world.getVoxelData(c.getChunkX() * 32 + x, c.getChunkY() * 32 + y, c.getChunkZ() * 32 + z);
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

	private int getSunlight(Chunk c, int x, int y, int z)
	{
		int data = 0;
		if (x >= -32 && z >= -32 && y >= -32 && y < 64 && x < 64 && z < 64)
		{
			int relx = x < 0 ? 0 : (x >= 32 ? 2 : 1);
			int rely = y < 0 ? 0 : (y >= 32 ? 2 : 1);
			int relz = z < 0 ? 0 : (z >= 32 ? 2 : 1);
			int[] target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			if (target != null)
			{
				x &= 0x1F;
				y &= 0x1F;
				z &= 0x1F;
				data = target[x * 1024 + y * 32 + z];
				int blockID = VoxelFormat.id(data);
				return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? -1 : VoxelFormat.sunlight(data);
			}
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			return 0;
		}

		x += c.getChunkX() * 32;
		y += c.getChunkY() * 32;
		z += c.getChunkZ() * 32;

		// Look for a chunk with relevant lightning data
		cached = Client.world.getChunk(x / 32, y / 32, z / 32);
		if (cached != null && !cached.isAirChunk())
		{
			data = cached.getVoxelData(x, y, z);

			int blockID = VoxelFormat.id(data);
			return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? -1 : VoxelFormat.sunlight(data);
		}

		// If all else fails, just use the heightmap information
		return Client.world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) <= y ? 15 : 0;
	}

	private int getBlocklight(Chunk c, int x, int y, int z)
	{
		int data = 0;

		// Is it in cache range ?
		if (x >= -32 && z >= -32 && y >= -32 && y < 64 && x < 64 && z < 64)
		{
			int relx = x < 0 ? 0 : (x >= 32 ? 2 : 1);
			int rely = y < 0 ? 0 : (y >= 32 ? 2 : 1);
			int relz = z < 0 ? 0 : (z >= 32 ? 2 : 1);
			int[] target = cache[((relx) * 3 + (rely)) * 3 + (relz)];
			x &= 0x1F;
			y &= 0x1F;
			z &= 0x1F;
			if (target != null)
				data = target[x * 1024 + y * 32 + z];
		}
		else
		{
			System.out.println("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
			System.out.println("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
			data = Client.world.getVoxelData(c.getChunkX() * 32 + x, c.getChunkY() * 32 + y, c.getChunkZ() * 32 + z);
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
		return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? 0 : VoxelFormat.blocklight(data);
	}

	public static float[] bakeLightColors(int bl1, int bl2, int bl3, int bl4, int sl1, int sl2, int sl3, int sl4)
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

	private void addQuadTop(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
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

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + (sx % texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + (sz % texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, wavy);
	}

	private void addQuadBottom(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		int llMs = getSunlight(c, sx, sy - 1, sz);
		int llMb = getBlocklight(c, sx, sy - 1, sz);

		int llAb = getBlocklight(c, sx + 1, sy - 1, sz);
		int llBb = getBlocklight(c, sx + 1, sy - 1, sz + 1);
		int llCb = getBlocklight(c, sx, sy - 1, sz + 1);
		int llDb = getBlocklight(c, sx - 1, sy - 1, sz + 1);

		int llEb = getBlocklight(c, sx - 1, sy - 1, sz);
		int llFb = getBlocklight(c, sx - 1, sy - 1, sz - 1);
		int llGb = getBlocklight(c, sx, sy - 1, sz - 1);
		int llHb = getBlocklight(c, sx + 1, sy - 1, sz - 1);

		int llAs = getSunlight(c, sx + 1, sy - 1, sz);
		int llBs = getSunlight(c, sx + 1, sy - 1, sz + 1);
		int llCs = getSunlight(c, sx, sy - 1, sz + 1);
		int llDs = getSunlight(c, sx - 1, sy - 1, sz + 1);

		int llEs = getSunlight(c, sx - 1, sy - 1, sz);
		int llFs = getSunlight(c, sx - 1, sy - 1, sz - 1);
		int llGs = getSunlight(c, sx, sy - 1, sz - 1);
		int llHs = getSunlight(c, sx + 1, sy - 1, sz - 1);

		float[] aoA = new float[] { 1f, 1f, 1f };
		float[] aoB = new float[] { 1f, 1f, 1f };
		float[] aoC = new float[] { 1f, 1f, 1f };
		float[] aoD = new float[] { 1f, 1f, 1f };

		aoA = bakeLightColors(llCb, llBb, llAb, llMb, llCs, llBs, llAs, llMs);

		aoD = bakeLightColors(llCb, llDb, llEb, llMb, llCs, llDs, llEs, llMs);

		aoB = bakeLightColors(llGb, llHb, llAb, llMb, llGs, llHs, llAs, llMs);

		aoC = bakeLightColors(llEb, llFb, llGb, llMb, llEs, llFs, llGs, llMs);

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + (sx % texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + (sz % texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx + 1, sy, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, wavy);
	}

	private void addQuadRight(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
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

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sz, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(1023 /* intifyNormal(1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);
	}

	private int mod(int a, int b)
	{
		int c = a % b;
		if (c >= 0)
			return c;
		return c += b;
	}

	private void addQuadLeft(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
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

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sz, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz + 1);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(0 /* intifyNormal(-1) */, 511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, wavy);

	}

	private void addQuadFront(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
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

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sx, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 1023 /* intifyNormal(1) */, wavy);

	}

	private void addQuadBack(Chunk c, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
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

		int offset = texture.getAtlasOffset() / texture.getTextureScale();
		int textureS = texture.getAtlasS() + mod(sx, texture.getTextureScale()) * offset;
		int textureT = texture.getAtlasT() + mod(-sy, texture.getTextureScale()) * offset;

		rbbf.addVerticeInt(sx, sy + 1, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT);
		rbbf.addColors(aoB);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx, sy - 0, sz);
		rbbf.addTexCoordInt(textureS + offset, textureT + offset);
		rbbf.addColors(aoC);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy + 1, sz);
		rbbf.addTexCoordInt(textureS, textureT);
		rbbf.addColors(aoA);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);

		rbbf.addVerticeInt(sx + 1, sy - 0, sz);
		rbbf.addTexCoordInt(textureS, textureT + offset);
		rbbf.addColors(aoD);
		rbbf.addNormalsInt(511 /* intifyNormal(0) */, 511 /* intifyNormal(0) */, 0 /* intifyNormal(-1) */, wavy);
	}

	private boolean shallBuildWallArround(VoxelContext renderInfo, int face)
	{
		//int baseID = renderInfo.data;
		Voxel facing = VoxelsStore.get().getVoxelById(renderInfo.getSideId(face));
		Voxel voxel = renderInfo.getVoxel();

		if (voxel.getType().isLiquid() && !facing.getType().isLiquid())
			return true;
		//if (voxel.getType().isLiquid() && facing.getType().isLiquid())
		//	return false;
		if (!facing.getType().isOpaque() && (!voxel.sameKind(facing) || !voxel.getType().isSelfOpaque()))
			return true;
		return false;
	}

	private void renderChunk(CubicChunk chunk, RecyclableByteBuffer recyclableByteBuffer)
	{
		//TODO only requests a ByteBuffer when it is sure it will actually need one
		ByteBuffer recyclableByteBufferData = recyclableByteBuffer.accessByteBuffer();

		// Update lightning as well if needed
		if (chunk == null)
		{
			recyclableByteBuffer.recycle();
			//buffersPool.releaseByteBuffer(buffer);
			return;
		}

		if (chunk.needRelightning.getAndSet(false))
			chunk.bakeVoxelLightning(true);

		//System.out.println("k");

		// Don't bother
		if (!chunk.need_render.get())
		{
			recyclableByteBuffer.recycle();
			return;
		}

		long cr_start = System.nanoTime();

		//Don't waste time rendering void chunks m8
		if (chunk.isAirChunk())
			i = 32;
		
		int cx = chunk.getChunkX();
		int cy = chunk.getChunkY();
		int cz = chunk.getChunkZ();

		// Fill chunk caches ( saves much time avoiding slow-ass world->regions hashmap->chunk holder access for each vert )
		for (int relx = -1; relx <= 1; relx++)
			for (int rely = -1; rely <= 1; rely++)
				for (int relz = -1; relz <= 1; relz++)
				{
					CubicChunk chunk2 = (CubicChunk) world.getChunk(cx + relx, cy + rely, cz + relz);
					if (chunk2 != null)
						cache[((relx + 1) * 3 + (rely + 1)) * 3 + (relz + 1)] = chunk2.chunkVoxelData;
					else
						cache[((relx + 1) * 3 + (rely + 1)) * 3 + (relz + 1)] = null;
				}


		long cr_iter = System.nanoTime();
		Voxels store = world.getGameContext().getContent().voxels();
		
		//Make sure we clear each sub-buffer type.
		for(int i = 0; i < ChunkMeshDataSubtypes.VertexLayout.values().length; i++)
		{
			for(int j = 0; j < ChunkMeshDataSubtypes.LodLevel.values().length; j++)
			{
				for(int k = 0; k < ChunkMeshDataSubtypes.ShadingType.values().length; k++)
				{
					byteBuffers[i][j][k].clear();
				}
			}
		}
		
		//Creates wrapper/interfaces for all the elements
		ChunkRenderer chunkRendererOutput = new ChunkRenderer() {

			@Override
			public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return byteBuffersWrappers[VertexLayout.INTRICATE.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
			}

			@Override
			public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return byteBuffersWrappers[VertexLayout.WHOLE_BLOCKS.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
			}
			
		};
		
		VoxelContext voxelRenderingContext = new VoxelContext()
		{
			@Override
			public Voxel getVoxel()
			{
				return store.getVoxelById(getData());
			}

			@Override
			public int getData()
			{
				return getBlockData(chunk, i, k, j);
			}

			@Override
			public int getNeightborData(int side)
			{
				switch (side)
				{
				case (0):
					return getBlockData(chunk, i - 1, k, j);
				case (1):
					return getBlockData(chunk, i, k, j + 1);
				case (2):
					return getBlockData(chunk, i + 1, k, j);
				case (3):
					return getBlockData(chunk, i, k, j - 1);
				case (4):
					return getBlockData(chunk, i, k + 1, j);
				case (5):
					return getBlockData(chunk, i, k - 1, j);
				}
				throw new RuntimeException("Fuck off");
			}

			@Override
			public int getX()
			{
				return i;
			}

			@Override
			public int getY()
			{
				return k;
			}

			@Override
			public int getZ()
			{
				return j;
			}

		};

		ChunkRenderContext chunkRenderingContext = new ChunkRenderContext()
		{
			// For map borders
			boolean chunkTopLoaded = world.isChunkLoaded(cx, cy + 1, cz);
			boolean chunkBotLoaded = world.isChunkLoaded(cx, cy - 1, cz);
			boolean chunkRightLoaded = world.isChunkLoaded(cx + 1, cy, cz);
			boolean chunkLeftLoaded = world.isChunkLoaded(cx - 1, cy, cz);
			boolean chunkFrontLoaded = world.isChunkLoaded(cx, cy, cz + 1);
			boolean chunkBackLoaded = world.isChunkLoaded(cx, cy, cz - 1);

			@Override
			public boolean isTopChunkLoaded()
			{
				return chunkTopLoaded;
			}

			@Override
			public boolean isBottomChunkLoaded()
			{
				return chunkBotLoaded;
			}

			@Override
			public boolean isLeftChunkLoaded()
			{
				return chunkLeftLoaded;
			}

			@Override
			public boolean isRightChunkLoaded()
			{
				return chunkRightLoaded;
			}

			@Override
			public boolean isFrontChunkLoaded()
			{
				return chunkFrontLoaded;
			}

			@Override
			public boolean isBackChunkLoaded()
			{
				return chunkBackLoaded;
			}

		};

		for (i = 0; i < 32; i++)
		{
			for (j = 0; j < 32; j++)
			{
				for (k = 0; k < 32; k++)
				{
					int src = chunk.getVoxelData(i, k, j);
					int blockID = VoxelFormat.id(src);

					if (blockID == 0)
						continue;
					Voxel vox = VoxelsStore.get().getVoxelById(blockID);
					// Fill near-blocks info

					VoxelRenderer voxelRenderer = vox.getVoxelRenderer(voxelRenderingContext);
					if(voxelRenderer == null)
						voxelRenderer = defaultVoxelRenderer;
					
					voxelRenderer.renderInto(chunkRendererOutput, chunkRenderingContext, chunk, voxelRenderingContext);
					// System.out.println(blockID);
					/*if (vox.getType().isLiquid())
					{
						addVoxelUsingCustomModel(workChunk, waterRBBF, i, k, j, renderInfo);
					}
					//TODO this seems ugly af
					else if (vox.getVoxelRenderer(renderInfo) != null)
					{
						// Prop rendering
						addVoxelUsingCustomModel(workChunk, complexRBBF, i, k, j, renderInfo);
					}
					else if (blockID != 0)
					{
						byte extraByte = 0;
						if (shallBuildWallArround(renderInfo, 5))
						{
							if (!(k == 0 && !chunkBotLoaded))
							{
								addQuadBottom(workChunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BOTTOM, renderInfo), extraByte);
							}
						}
						if (shallBuildWallArround(renderInfo, 4))
						{
							if (!(k == 31 && !chunkTopLoaded))
							{
								addQuadTop(workChunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.TOP, renderInfo), extraByte);
							}
						}
						if (shallBuildWallArround(renderInfo, 2))
						{
							if (!(i == 31 && !chunkRightLoaded))
							{
								addQuadRight(workChunk, rawRBBF, i + 1, k, j, vox.getVoxelTexture(src, VoxelSides.RIGHT, renderInfo), extraByte);
							}
						}
						if (shallBuildWallArround(renderInfo, 0))
						{
							if (!(i == 0 && !chunkLeftLoaded))
							{
								addQuadLeft(workChunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.LEFT, renderInfo), extraByte);
							}
						}
						if (shallBuildWallArround(renderInfo, 1))
						{
							if (!(j == 31 && !chunkFrontLoaded))
							{
								addQuadFront(workChunk, rawRBBF, i, k, j + 1, vox.getVoxelTexture(src, VoxelSides.FRONT, renderInfo), extraByte);
							}
						}
						if (shallBuildWallArround(renderInfo, 3))
						{
							if (!(j == 0 && !chunkBackLoaded))
							{
								addQuadBack(workChunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BACK, renderInfo), extraByte);
							}
						}
					}*/
				}
			}
		}

		// Prepare output
		recyclableByteBufferData.clear();

		int[][][] sizes = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		int[][][] offsets = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		
		int currentOffset = 0;

		//For EACH section, make offset and shite
		for(VertexLayout vertexLayout : VertexLayout.values())
			for(LodLevel lodLevel : LodLevel.values())
				for(ShadingType renderPass : ShadingType.values())
				{
					int vertexLayoutIndex = vertexLayout.ordinal();
					int lodLevelIndex = lodLevel.ordinal();
					int renderPassIndex = renderPass.ordinal();

					//Else it gets really long for no reason
					final ByteBuffer relevantByteBuffer = this.byteBuffers[vertexLayoutIndex][lodLevelIndex][renderPassIndex];
					
					offsets[vertexLayoutIndex][lodLevelIndex][renderPassIndex] = currentOffset;
					sizes[vertexLayoutIndex][lodLevelIndex][renderPassIndex] = relevantByteBuffer.position() / vertexLayout.bytesPerVertex;

					//Move the offset accordingly
					currentOffset += relevantByteBuffer.position();
					
					//Limit the temporary byte buffer and put it's content inside
					relevantByteBuffer.limit(relevantByteBuffer.position());
					relevantByteBuffer.position(0);
					recyclableByteBufferData.put(relevantByteBuffer);
					
					//System.out.println("Doing chunk "+chunk+" -> "+vertexLayout+":"+lodLevel+":"+renderPass+" ; o="+offsets[vertexLayoutIndex][lodLevelIndex][renderPassIndex]+" s:"+sizes[vertexLayoutIndex][lodLevelIndex][renderPassIndex]);
				}
		
		//Move data in final buffer in correct orders
		/*rawBlocksBuffer.limit(rawBlocksBuffer.position());
		rawBlocksBuffer.position(0);
		byteBuffer.put(rawBlocksBuffer);

		waterBlocksBuffer.limit(waterBlocksBuffer.position());
		waterBlocksBuffer.position(0);
		byteBuffer.put(waterBlocksBuffer);

		complexBlocksBuffer.limit(complexBlocksBuffer.position());
		complexBlocksBuffer.position(0);
		byteBuffer.put(complexBlocksBuffer);*/

		recyclableByteBufferData.flip();
		
		VerticesObject verticesObject = new VerticesObject();
		verticesObject.uploadData(recyclableByteBuffer);

		ChunkMeshDataSections parent = chunk.getChunkRenderData().getData();
		ChunkMeshDataSections newRenderData = new ChunkMeshDataSections(parent, verticesObject, sizes, offsets);
		chunk.getChunkRenderData().setData(newRenderData);
		
		//chunk.getChunkRenderData().setChunkMeshes(new MeshedChunkData(chunk, recyclableByteBuffer, rawBlocksBuffer.position() / (16), complexBlocksBuffer.position() / (24), waterBlocksBuffer.position() / (24)));
		//doneQueue.add(new MeshedChunkData(work, buffer, rawBlocksBuffer.position() / (16), complexBlocksBuffer.position() / (24), waterBlocksBuffer.position() / (24)));

		totalChunksRendered.incrementAndGet();

		chunk.need_render.set(false);
		chunk.requestable.set(true);
	}

	class DefaultVoxelRenderer implements VoxelRenderer
	{
		@Override
		public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations)
		{
			Voxel vox = voxelInformations.getVoxel();
			int src = voxelInformations.getData();
			
			VoxelBakerCubic rawRBBF = chunkRenderer.getLowpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
			byte extraByte = 0;
			if (shallBuildWallArround(voxelInformations, 5))
			{
				if (k != 0 || bakingContext.isBottomChunkLoaded())
					addQuadBottom(chunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BOTTOM, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 4))
			{
				if (k != 31 || bakingContext.isTopChunkLoaded())
					addQuadTop(chunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.TOP, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 2))
			{
				if (i != 31 || bakingContext.isRightChunkLoaded())
					addQuadRight(chunk, rawRBBF, i + 1, k, j, vox.getVoxelTexture(src, VoxelSides.RIGHT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 0))
			{
				if (i != 0 || bakingContext.isLeftChunkLoaded())
					addQuadLeft(chunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.LEFT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 1))
			{
				if (j != 31 || bakingContext.isFrontChunkLoaded())
					addQuadFront(chunk, rawRBBF, i, k, j + 1, vox.getVoxelTexture(src, VoxelSides.FRONT, voxelInformations), extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 3))
			{
				if (j != 0 || bakingContext.isBackChunkLoaded())
					addQuadBack(chunk, rawRBBF, i, k, j, vox.getVoxelTexture(src, VoxelSides.BACK, voxelInformations), extraByte);
			}

			return 0;
		}

	}

	public static int intifyNormal(float n)
	{
		return (int) ((n + 1) * 511.5f);
	}

	public AtomicInteger totalChunksRendered = new AtomicInteger();

	@Override
	public void destroy()
	{
		die.set(true);
		synchronized (this)
		{
			notifyAll();
		}
	}
	
	public String toString()
	{
		return "[ChunkMeshesBaker todoList:"+this.todoQueue.size()+"]";
	}
}
