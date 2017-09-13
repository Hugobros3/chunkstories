package io.xol.chunkstories.renderer.chunks;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.voxel.models.layout.BaseLayoutBaker;
import io.xol.chunkstories.api.voxel.models.layout.IntricateLayoutBaker;
import io.xol.chunkstories.api.voxel.models.layout.WholeBlocksLayoutBaker;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.renderers.DefaultVoxelRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.workers.Task;
import io.xol.chunkstories.workers.TaskExecutor;
import io.xol.chunkstories.workers.TasksPool;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientTasksPool extends TasksPool<Task> implements ChunkMeshesBaker{

	protected AtomicInteger totalChunksRendered = new AtomicInteger();
	
	protected final WorldClient world;
	
	private int threadsCount;
	private ClientWorkerThread[] workers;
	
	public ClientTasksPool(WorldClient world, int threadsCount)
	{
		this.world = world;
		this.threadsCount = threadsCount;
		
		workers = new ClientWorkerThread[threadsCount];
		for(int i = 0; i < threadsCount; i++)
			workers[i] = new ClientWorkerThread(i);
	}
	
	//Virtual task the reference is used to signal threads to end.
	Task DIE = new Task() {

		@Override
		protected boolean task(TaskExecutor task)
		{
			return true;
		}
		
	};
	
	class ClientWorkerThread extends Thread implements TaskExecutor, BakeChunkTaskExecutor {
		
		final ChunkMeshingBuffers cmd;
		
		ClientWorkerThread(int id)
		{	
			this.setName("Worker thread #"+id);
			
			//Init CMD
			this.cmd = new ChunkMeshingBuffers();
			
			this.start();
		}
		
		public void run()
		{
			while(true)
			{
				//Aquire a work permit
				tasksCounter.acquireUninterruptibly();
				
				//If one such permit was found to exist, assert a task is readily avaible
				Task task = tasksQueue.poll();
				//RenderableChunk chunk = task.chunk;
				
				assert task != null;
				
				//Only DIE task can break the loop
				if(task == DIE)
					break;
				
				/*boolean result = true;
				
				ChunkRenderable work = (ChunkRenderable) world.getChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
				if (work != null && (work.isMarkedForReRender() || work.needsLightningUpdates()))
				{
					int nearChunks = 0;
					if (world.isChunkLoaded(chunk.getChunkX() + 1, chunk.getChunkY(), chunk.getChunkZ()))
						nearChunks++;
					if (world.isChunkLoaded(chunk.getChunkX() - 1, chunk.getChunkY(), chunk.getChunkZ()))
						nearChunks++;
					if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ() + 1))
						nearChunks++;
					if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ() - 1))
						nearChunks++;

					if (nearChunks == 4)
					{
						result = task.run(this);
					}
				}*/
				boolean result = task.run(this);
				
				tasksRan++;
				
				//Depending on the result we either reschedule the task or decrement the counter
				if(result == false)
					rescheduleTask(task);
				else
					tasksQueueSize.decrementAndGet();
			}
			
			//Clean-up.
			this.cmd.cleanup();
		}

		@Override
		public ChunkMeshingBuffers getBuffers() {
			return cmd;
		}
		
		class ChunkMeshingBuffers {
			//protected ByteBufferPool buffersPool;

			protected ByteBuffer[][][] byteBuffers;
			protected BaseLayoutBaker[][][] byteBuffersWrappers;
			
			protected DefaultVoxelRenderer defaultVoxelRenderer;
			
			//Don't care if gc'd
			protected final int[][] cache = new int[27][];
			
			ChunkMeshingBuffers() {
				//8 buffers of 8Mb each (64Mb) for uploading to VRAM temporary
				//TODO as this isn't a quite realtime thread, consider not pooling those to improve memory usage efficiency.
				//buffersPool = new ByteBufferPool(8, 0x800000);
				
				byteBuffers = new ByteBuffer[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
				byteBuffersWrappers = new BaseLayoutBaker[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
				
				//Allocate dedicated sizes for relevant buffers
				byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.ANY.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x800000);//BufferUtils.createByteBuffer(0x800000);
				byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.LOW.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x400000);//BufferUtils.createByteBuffer(0x400000);
				byteBuffers[VertexLayout.WHOLE_BLOCKS.ordinal()][LodLevel.HIGH.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x800000);//BufferUtils.createByteBuffer(0x800000);

				byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.ANY.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x800000);//BufferUtils.createByteBuffer(0x800000);
				byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.LOW.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x400000);//BufferUtils.createByteBuffer(0x400000);
				byteBuffers[VertexLayout.INTRICATE.ordinal()][LodLevel.HIGH.ordinal()][ShadingType.OPAQUE.ordinal()] = MemoryUtil.memAlloc(0x800000);//BufferUtils.createByteBuffer(0x400000);
				
				//Allocate more reasonnable size for other buffers and give them all a wrapper
				for(int i = 0; i < ChunkMeshDataSubtypes.VertexLayout.values().length; i++)
				{
					for(int j = 0; j < ChunkMeshDataSubtypes.LodLevel.values().length; j++)
					{
						for(int k = 0; k < ChunkMeshDataSubtypes.ShadingType.values().length; k++)
						{
							if(byteBuffers[i][j][k] == null)
								byteBuffers[i][j][k] = MemoryUtil.memAlloc(0x200000);//BufferUtils.createByteBuffer(0x100000);
							
							if(byteBuffers[i][j][k] == null)
							{
								System.out.println("Fucking out of memory");
								System.out.println("MemoryUtil: "+ " A:"+MemoryUtil.getAllocator());
								System.exit(-1);
							}
							
							switch(ChunkMeshDataSubtypes.VertexLayout.values()[i]) {
							case INTRICATE:
								byteBuffersWrappers[i][j][k] = new IntricateLayoutBaker(world.getClient().getContent(), byteBuffers[i][j][k]);
								break;
							case WHOLE_BLOCKS:
								byteBuffersWrappers[i][j][k] = new WholeBlocksLayoutBaker(world.getClient().getContent(), byteBuffers[i][j][k]);
								break;
							default:
									world.getClient().logger().log("NO SPECIFIC LAYOUT BAKER FOR : " + ChunkMeshDataSubtypes.VertexLayout.values()[i], LogType.INTERNAL, LogLevel.CRITICAL);
									System.exit(-400);
							}
							//byteBuffersWrappers[i][j][k] = new RenderByteBuffer(byteBuffers[i][j][k]);
						}
					}
				}
				
				defaultVoxelRenderer = new DefaultVoxelRenderer(world.getGameContext().getContent().voxels());
			}
			
			protected int getBlockData(CubicChunk c, int x, int y, int z)
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

			protected final int getSunlight(Chunk c, int x, int y, int z)
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
				Chunk cached = world.getChunk(x / 32, y / 32, z / 32);
				if (cached != null && !cached.isAirChunk())
				{
					data = cached.getVoxelData(x, y, z);

					int blockID = VoxelFormat.id(data);
					return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? -1 : VoxelFormat.sunlight(data);
				}

				// If all else fails, just use the heightmap information
				return world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) <= y ? 15 : 0;
			}

			protected final int getBlocklight(Chunk c, int x, int y, int z)
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

				int blockID = VoxelFormat.id(data);
				return VoxelsStore.get().getVoxelById(blockID).getType().isOpaque() ? 0 : VoxelFormat.blocklight(data);
			}
			
			private final void cleanup() {
				
				//Give back subscribers money
				for(int i = 0; i < ChunkMeshDataSubtypes.VertexLayout.values().length; i++)
					for(int j = 0; j < ChunkMeshDataSubtypes.LodLevel.values().length; j++)
						for(int k = 0; k < ChunkMeshDataSubtypes.ShadingType.values().length; k++)
							MemoryUtil.memFree(byteBuffers[i][j][k]);
				
				System.out.println("Freed ChunkMeshingBuffers memory");				
			}
		}
	}
	
	long tasksRan = 0;
	long tasksRescheduled = 0;
	
	void rescheduleTask(Task task)
	{
		tasksQueue.add(task);
		tasksCounter.release();
		
		tasksRescheduled++;
	}
	
	@Override
	public void requestChunkRender(ChunkRenderable chunk) {
		
		if(chunk == null)
			throw new NullPointerException();
		
		TaskBakeChunk task = new TaskBakeChunk(this, (RenderableChunk)chunk);
		
		this.scheduleTask(task);
	}

	public void destroy()
	{
		//Send threadsCount DIE orders
		for(int i = 0; i < threadsCount; i++)
			this.scheduleTask(DIE);
	}
	
	@Override
	public String toString() {
		return "[ClientTasksPool threads:"+threadsCount+" tasksQueue: "+tasksQueue.size()+" ]";
	}
	
}
