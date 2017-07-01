package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.core.voxel.DefaultVoxelRenderer;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool.PooledByteBuffer;
import io.xol.chunkstories.api.Content.Voxels;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.RenderByteBuffer;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.geometry.VertexBufferGL;

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

	public final int[][] cache = new int[27][];

	Deque<Integer> blockSources = new ArrayDeque<Integer>();
	Deque<Integer> sunSources = new ArrayDeque<Integer>();

	//Work buffers
	ByteBuffer[][][] byteBuffers;
	RenderByteBuffer[][][] byteBuffersWrappers;
	
	//Nasty !
	int i = 0, j = 0, k = 0;
	int bakedBlockId;
	
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
								PooledByteBuffer buffer = buffersPool.requestByteBuffer();
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
								if (work instanceof RenderableChunk)
									try
									{
										renderChunk((RenderableChunk) work, buffer);
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
								else if(work instanceof CubicChunk)
								{
									world.getGameContext().logger().log("Was asked to render a non-renderable (non-client) chunk ", LogType.RENDERING, LogLevel.CRITICAL);
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
			data = world.getVoxelData(c.getChunkX() * 32 + x, c.getChunkY() * 32 + y, c.getChunkZ() * 32 + z);
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

	private final int getSunlight(Chunk c, int x, int y, int z)
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
		cached = world.getChunk(x / 32, y / 32, z / 32);
		if (cached != null && !cached.isAirChunk())
		{
			data = cached.getVoxelData(x, y, z);

			int blockID = VoxelFormat.id(data);
			return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? -1 : VoxelFormat.sunlight(data);
		}

		// If all else fails, just use the heightmap information
		return world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) <= y ? 15 : 0;
	}

	private final int getBlocklight(Chunk c, int x, int y, int z)
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
			data = world.getVoxelData(c.getChunkX() * 32 + x, c.getChunkY() * 32 + y, c.getChunkZ() * 32 + z);
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

	private void renderChunk(RenderableChunk chunk, PooledByteBuffer recyclableByteBuffer)
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
			chunk.computeVoxelLightning(true);

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

			@Override
			public World getWorld() {
				return world;
			}

		};

		ChunkBakerRenderContext chunkRenderingContext = new ChunkBakerRenderContext(chunk, cx, cy, cz);
		
		bakedBlockId = 0;
		//Render the fucking thing!
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
					// chunkRenderingContext.prepareVoxelLight();
					
					VoxelRenderer voxelRenderer = vox.getVoxelRenderer(voxelRenderingContext);
					if(voxelRenderer == null)
						voxelRenderer = defaultVoxelRenderer;
					
					voxelRenderer.renderInto(chunkRendererOutput, chunkRenderingContext, chunk, voxelRenderingContext);
					
					bakedBlockId++;
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
		
		VertexBuffer verticesObject = new VertexBufferGL();
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

	class ChunkBakerRenderContext implements ChunkRenderContext {

		// For map borders
		boolean chunkTopLoaded;
		boolean chunkBotLoaded;
		boolean chunkRightLoaded;
		boolean chunkLeftLoaded;
		boolean chunkFrontLoaded;
		boolean chunkBackLoaded;
		VoxelLighter voxelLighter;
		CubicChunk chunk;
		
		int lastBlockBaked = 0;
		byte[] sunlightLevel = new byte[VoxelSides.Corners.values().length];
		byte[] blocklightLevel = new byte[VoxelSides.Corners.values().length];
		byte[] aoLevel = new byte[VoxelSides.Corners.values().length];
		
		public ChunkBakerRenderContext(CubicChunk chunk, int cx, int cy, int cz)
		{
			chunkTopLoaded = world.isChunkLoaded(cx, cy + 1, cz);
			chunkBotLoaded = world.isChunkLoaded(cx, cy - 1, cz);
			chunkRightLoaded = world.isChunkLoaded(cx + 1, cy, cz);
			chunkLeftLoaded = world.isChunkLoaded(cx - 1, cy, cz);
			chunkFrontLoaded = world.isChunkLoaded(cx, cy, cz + 1);
			chunkBackLoaded = world.isChunkLoaded(cx, cy, cz - 1);
			
			this.chunk = chunk;
			voxelLighter = new VoxelLighter() {

				@Override
				public byte getSunlightLevelForCorner(Corners corner)
				{
					return sunlightLevel[corner.ordinal()];
				}

				@Override
				public byte getBlocklightLevelForCorner(Corners corner)
				{
					return blocklightLevel[corner.ordinal()];
				}

				@Override
				public byte getAoLevelForCorner(Corners corner)
				{
					return aoLevel[corner.ordinal()];
				}

				
				private byte interp(byte[] array, float x, float y, float z)
				{
					float interpBotFront = Math2.mix(array[VoxelSides.Corners.BOTTOM_FRONT_LEFT.ordinal()], array[VoxelSides.Corners.BOTTOM_FRONT_RIGHT.ordinal()], Math2.clamp(x, 0.0, 1.0));
					float interpBotBack = Math2.mix(array[VoxelSides.Corners.BOTTOM_BACK_LEFT.ordinal()], array[VoxelSides.Corners.BOTTOM_BACK_RIGHT.ordinal()], Math2.clamp(x, 0.0, 1.0));

					float interpTopFront = Math2.mix(array[VoxelSides.Corners.TOP_FRONT_LEFT.ordinal()], array[VoxelSides.Corners.TOP_FRONT_RIGHT.ordinal()], Math2.clamp(x, 0.0, 1.0));
					float interpTopBack = Math2.mix(array[VoxelSides.Corners.TOP_BACK_LEFT.ordinal()], array[VoxelSides.Corners.TOP_BACK_RIGHT.ordinal()], Math2.clamp(x, 0.0, 1.0));
					
					float interpBot = Math2.mix(interpBotBack, interpBotFront, Math2.clamp(z, 0.0, 1.0));
					float interpTop = Math2.mix(interpTopBack, interpTopFront, Math2.clamp(z, 0.0, 1.0));
					
					return (byte)Math2.mix(interpBot, interpTop, Math2.clamp(y, 0.0, 1.0));
				}
				
				@Override
				public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return interp(sunlightLevel, vertX, vertY, vertZ);
				}

				@Override
				public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return interp(blocklightLevel, vertX, vertY, vertZ);
				}

				@Override
				public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return interp(aoLevel, vertX, vertY, vertZ);
				}
				
			};
		}

		public void prepareVoxelLight()
		{
			int sl000 = getSunlight(chunk, i, k, j    );
			int sl00p = getSunlight(chunk, i, k, j + 1);
			int sl00m = getSunlight(chunk, i, k, j - 1);

			int sl0p0 = getSunlight(chunk, i, k + 1, j    );
			int sl0pp = getSunlight(chunk, i, k + 1, j + 1);
			int sl0pm = getSunlight(chunk, i, k + 1, j - 1);

			int sl0m0 = getSunlight(chunk, i, k - 1, j    );
			int sl0mp = getSunlight(chunk, i, k - 1, j + 1);
			int sl0mm = getSunlight(chunk, i, k - 1, j - 1);

			int slp00 = getSunlight(chunk, i + 1, k, j    );
			int slp0p = getSunlight(chunk, i + 1, k, j + 1);
			int slp0m = getSunlight(chunk, i + 1, k, j - 1);

			int slpp0 = getSunlight(chunk, i + 1, k + 1, j    );
			int slppp = getSunlight(chunk, i + 1, k + 1, j + 1);
			int slppm = getSunlight(chunk, i + 1, k + 1, j - 1);

			int slpm0 = getSunlight(chunk, i + 1, k - 1, j    );
			int slpmp = getSunlight(chunk, i + 1, k - 1, j + 1);
			int slpmm = getSunlight(chunk, i + 1, k - 1, j - 1);

			int slm00 = getSunlight(chunk, i - 1, k, j    );
			int slm0p = getSunlight(chunk, i - 1, k, j + 1);
			int slm0m = getSunlight(chunk, i - 1, k, j - 1);

			int slmp0 = getSunlight(chunk, i - 1, k + 1, j    );
			int slmpp = getSunlight(chunk, i - 1, k + 1, j + 1);
			int slmpm = getSunlight(chunk, i - 1, k + 1, j - 1);

			int slmm0 = getSunlight(chunk, i - 1, k - 1, j    );
			int slmmp = getSunlight(chunk, i - 1, k - 1, j + 1);
			int slmmm = getSunlight(chunk, i - 1, k - 1, j - 1);
			
			int bl000 = getBlocklight(chunk, i, k, j    );
			int bl00p = getBlocklight(chunk, i, k, j + 1);
			int bl00m = getBlocklight(chunk, i, k, j - 1);

			int bl0p0 = getBlocklight(chunk, i, k + 1, j    );
			int bl0pp = getBlocklight(chunk, i, k + 1, j + 1);
			int bl0pm = getBlocklight(chunk, i, k + 1, j - 1);

			int bl0m0 = getBlocklight(chunk, i, k - 1, j    );
			int bl0mp = getBlocklight(chunk, i, k - 1, j + 1);
			int bl0mm = getBlocklight(chunk, i, k - 1, j - 1);

			int blp00 = getBlocklight(chunk, i + 1, k, j    );
			int blp0p = getBlocklight(chunk, i + 1, k, j + 1);
			int blp0m = getBlocklight(chunk, i + 1, k, j - 1);

			int blpp0 = getBlocklight(chunk, i + 1, k + 1, j    );
			int blppp = getBlocklight(chunk, i + 1, k + 1, j + 1);
			int blppm = getBlocklight(chunk, i + 1, k + 1, j - 1);

			int blpm0 = getBlocklight(chunk, i + 1, k - 1, j    );
			int blpmp = getBlocklight(chunk, i + 1, k - 1, j + 1);
			int blpmm = getBlocklight(chunk, i + 1, k - 1, j - 1);

			int blm00 = getBlocklight(chunk, i - 1, k, j    );
			int blm0p = getBlocklight(chunk, i - 1, k, j + 1);
			int blm0m = getBlocklight(chunk, i - 1, k, j - 1);

			int blmp0 = getBlocklight(chunk, i - 1, k + 1, j    );
			int blmpp = getBlocklight(chunk, i - 1, k + 1, j + 1);
			int blmpm = getBlocklight(chunk, i - 1, k + 1, j - 1);

			int blmm0 = getBlocklight(chunk, i - 1, k - 1, j    );
			int blmmp = getBlocklight(chunk, i - 1, k - 1, j + 1);
			int blmmm = getBlocklight(chunk, i - 1, k - 1, j - 1);
			
			bake(VoxelSides.Corners.TOP_FRONT_RIGHT.ordinal()   , sl000, slp00, sl0p0, sl00p, slpp0, sl0pp, slp0p, slppp
															    , bl000, blp00, bl0p0, bl00p, blpp0, bl0pp, blp0p, blppp);
			
			bake(VoxelSides.Corners.TOP_FRONT_LEFT.ordinal()    , sl000, slm00, sl0p0, sl00p, slmp0, sl0pp, slm0p, slmpp
					 										    , bl000, blm00, bl0p0, bl00p, blmp0, bl0pp, blm0p, blmpp);
			
			bake(VoxelSides.Corners.TOP_BACK_RIGHT.ordinal()    , sl000, slp00, sl0p0, sl00m, slpp0, sl0pm, slp0m, slppm
					 										    , bl000, blp00, bl0p0, bl00m, blpp0, bl0pm, blp0m, blppm);

			bake(VoxelSides.Corners.TOP_BACK_LEFT.ordinal()     , sl000, slm00, sl0p0, sl00m, slmp0, sl0pm, slm0m, slmpm
														        , bl000, blm00, bl0p0, bl00m, blmp0, bl0pm, blm0m, blmpm);
			
			bake(VoxelSides.Corners.BOTTOM_FRONT_RIGHT.ordinal(), sl000, slp00, sl0m0, sl00p, slpm0, sl0mp, slp0p, slpmp
															    , bl000, blp00, bl0m0, bl00p, blpm0, bl0mp, blp0p, blpmp);

			bake(VoxelSides.Corners.BOTTOM_FRONT_LEFT.ordinal() , sl000, slm00, sl0m0, sl00p, slmm0, sl0mp, slm0p, slmmp
															    , bl000, blm00, bl0m0, bl00p, blmm0, bl0mp, blm0p, blmmp);

			bake(VoxelSides.Corners.BOTTOM_BACK_RIGHT.ordinal() , sl000, slp00, sl0m0, sl00m, slpm0, sl0mm, slp0m, slpmm
															    , bl000, blp00, bl0m0, bl00m, blpm0, bl0mm, blp0m, blpmm);

			bake(VoxelSides.Corners.BOTTOM_BACK_LEFT.ordinal()  , sl000, slm00, sl0m0, sl00m, slmm0, sl0mm, slm0m, slmmm
															    , bl000, blm00, bl0m0, bl00m, blmm0, bl0mm, blm0m, blmmm);
			
			//Update this
			this.lastBlockBaked = bakedBlockId;
		}
		
		private void bake(int side, int slCenter, int slX, int slY, int slZ, int slXY, int slYZ, int slXZ, int slXYZ
								  , int blCenter, int blX, int blY, int blZ, int blXY, int blYZ, int blXZ, int blXYZ)
		{
			int slacc=0,blacc=0;
			int nonOpaqueBlocks=0;
			if(slCenter >= 0)
			{
				slacc += slCenter;
				blacc += blCenter;
				nonOpaqueBlocks++;
			}
			
			if(slX >= 0)
			{
				slacc += slX;
				blacc += blX;
				nonOpaqueBlocks++;
			}
			if(slY >= 0)
			{
				slacc += slY;
				blacc += blY;
				nonOpaqueBlocks++;
			}
			if(slZ >= 0)
			{
				slacc += slZ;
				blacc += blZ;
				nonOpaqueBlocks++;
			}

			if(slXY >= 0)
			{
				slacc += slXY;
				blacc += blXY;
				nonOpaqueBlocks++;
			}
			if(slYZ >= 0)
			{
				slacc += slYZ;
				blacc += blYZ;
				nonOpaqueBlocks++;
			}
			if(slXZ >= 0)
			{
				slacc += slXZ;
				blacc += blXZ;
				nonOpaqueBlocks++;
			}
			
			if(slXYZ >= 0)
			{
				slacc += slXYZ;
				blacc += blXYZ;
				nonOpaqueBlocks++;
			}
			
			if(nonOpaqueBlocks > 0)
			{
				sunlightLevel[side] = (byte)(slacc / nonOpaqueBlocks);
				blocklightLevel[side] = (byte)(blacc / nonOpaqueBlocks);
				aoLevel[side] = (byte) Math.max(0, 4 - nonOpaqueBlocks);
			}
			else
			{
				sunlightLevel[side] = 15;
				blocklightLevel[side] = 15;
				aoLevel[side] = (byte) 4;
			}
		}
		
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

		@Override
		public int getRenderedVoxelPositionInChunkX()
		{
			return i;
		}

		@Override
		public int getRenderedVoxelPositionInChunkY()
		{
			return k;
		}

		@Override
		public int getRenderedVoxelPositionInChunkZ()
		{
			return j;
		}

		@Override
		public VoxelLighter getCurrentVoxelLighter()
		{
			if(bakedBlockId != this.lastBlockBaked) {
				this.prepareVoxelLight();
			}
			
			return voxelLighter;
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
