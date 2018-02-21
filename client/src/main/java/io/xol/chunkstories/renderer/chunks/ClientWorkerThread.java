package io.xol.chunkstories.renderer.chunks;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.rendering.world.chunk.vertexlayout.BaseLayoutBaker;
import io.xol.chunkstories.api.rendering.world.chunk.vertexlayout.IntricateLayoutBaker;
import io.xol.chunkstories.api.rendering.world.chunk.vertexlayout.WholeBlocksLayoutBaker;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.workers.WorkerThread;

/** Client worker threads, with the added facility that they can do the chunk rendering ( they have more buffers ) */
public class ClientWorkerThread extends WorkerThread implements BakeChunkTaskExecutor {
	
	final ClientTasksPool pool;
	final ClientContent content;
	final ChunkMeshingBuffers cmd;
	
	private static final Logger logger = LoggerFactory.getLogger("client.workers");
	
	protected ClientWorkerThread(ClientTasksPool pool, int id)
	{	
		super(pool, id);
		this.pool = pool;
		this.content = pool.client.getContent();
		this.setName("Worker thread #"+id);
		logger.info("Initialized "+this.getName());
		
		//Init CMD
		this.cmd = new ChunkMeshingBuffers();
		
		//this.start();
	}

	@Override
	public ChunkMeshingBuffers getBuffers() {
		return cmd;
	}
	
	class ChunkMeshingBuffers {
		//protected ByteBufferPool buffersPool;

		protected ByteBuffer[][][] byteBuffers;
		protected BaseLayoutBaker[][][] byteBuffersWrappers;
		
		//protected VoxelRenderer defaultVoxelRenderer;
		
		//Don't care if gc'd
		protected final int[][] cache = new int[27][];
		
		ChunkMeshingBuffers() {
			
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
							logger.error("Failed to allocate memory for client worker");
							logger.error("MemoryUtil: "+ " A:"+MemoryUtil.getAllocator());
							System.exit(-1);
						}
						
						switch(ChunkMeshDataSubtypes.VertexLayout.values()[i]) {
						case INTRICATE:
							byteBuffersWrappers[i][j][k] = new IntricateLayoutBaker(content, byteBuffers[i][j][k]);
							break;
						case WHOLE_BLOCKS:
							byteBuffersWrappers[i][j][k] = new WholeBlocksLayoutBaker(content, byteBuffers[i][j][k]);
							break;
						default:
								pool.logger().error("NO SPECIFIC LAYOUT BAKER FOR : " + ChunkMeshDataSubtypes.VertexLayout.values()[i]);
								System.exit(-400);
						}
						//byteBuffersWrappers[i][j][k] = new RenderByteBuffer(byteBuffers[i][j][k]);
					}
				}
			}
		}
		
		final static int IGNORE = -1;

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
					return c.getWorld().getContentTranslator().getVoxelForId(blockID).getDefinition().isOpaque() ? IGNORE : VoxelFormat.sunlight(data);
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
			/*Chunk cached = c.getWorld().getChunk(x / 32, y / 32, z / 32);
			if (cached != null && !cached.isAirChunk())
			{
				data = cached.peekRaw(x, y, z);
				int blockID = VoxelFormat.id(data);
				return c.getWorld().getContentTranslator().getVoxelForId(blockID).getDefinition().isOpaque() ? -1 : VoxelFormat.sunlight(data);
			}*/

			// If all else fails, just use the heightmap information
			return c.getWorld().getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) <= y ? 15 : 0;
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
				logger.warn("Warning ! Chunk " + c + " rendering process asked information about a block more than 32 blocks away from the chunk itself");
				logger.warn("This should not happen when rendering normal blocks and may be caused by a weird or buggy mod.");
				data = c.getWorld().peekRaw(c.getChunkX() * 32 + x, c.getChunkY() * 32 + y, c.getChunkZ() * 32 + z);
			}

			int blockID = VoxelFormat.id(data);
			return c.getWorld().getContentTranslator().getVoxelForId(blockID).getDefinition().isOpaque() ? IGNORE : VoxelFormat.blocklight(data);
		}
		
		private final void cleanup() {
			
			//Give back subscribers money
			for(int i = 0; i < ChunkMeshDataSubtypes.VertexLayout.values().length; i++)
				for(int j = 0; j < ChunkMeshDataSubtypes.LodLevel.values().length; j++)
					for(int k = 0; k < ChunkMeshDataSubtypes.ShadingType.values().length; k++)
						MemoryUtil.memFree(byteBuffers[i][j][k]);
			
			logger.info("Freed ChunkMeshingBuffers memory");				
		}
	}
	
	@Override
	protected void cleanup() {
		cmd.cleanup();
	}
}
