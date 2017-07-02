package io.xol.chunkstories.renderer.chunks;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Content.Voxels;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.RenderByteBuffer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.voxel.DefaultVoxelRenderer;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool;
import io.xol.chunkstories.renderer.chunks.ClientTasksPool.ClientWorkerThread.ChunkMeshingBuffers;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.workers.Task;
import io.xol.chunkstories.workers.TaskExecutor;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.base.MemFreeByteBuffer;
import io.xol.engine.graphics.geometry.VertexBufferGL;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TaskBakeChunk extends Task {

	private final ClientTasksPool baker;
	protected final RenderableChunk chunk;
	
	private final WorldClient world;
	
	//Nasty !
	protected int i = 0, j = 0, k = 0;
	protected int bakedBlockId;
	
	protected ChunkMeshingBuffers cmd;
	
	public TaskBakeChunk(ClientTasksPool baker, RenderableChunk chunk) {
		super();
		this.baker = baker;
		this.chunk = chunk;
		
		//Degenerate case for DIE object 
		if(chunk == null) {
			this.world = null;
			return;
		}
		
		this.world = (WorldClient)chunk.getWorld();
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		
		if(!(taskExecutor instanceof BakeChunkTaskExecutor))
			return false;
		
		this.cmd = ((BakeChunkTaskExecutor)taskExecutor).getBuffers();
		
		ChunkRenderable work = (ChunkRenderable) world.getChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
		
		//Second part is most likely redundant
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
				//Let task exec
				//result = task.run(this);
			}
			else {
				//Fast-fail

				chunk.markRenderInProgress(false);
				return true;
			}
		}
		else {
			//Fast-fail

			chunk.markRenderInProgress(false);
			return true;
		}

		// Update lightning as well if needed
		if (chunk == null)
		{
			//recyclableByteBuffer.recycle();
			//buffersPool.releaseByteBuffer(buffer);
			throw new RuntimeException("Fuck off no");
			//return false;
		}

		if (chunk.needRelightning.getAndSet(false))
			chunk.computeVoxelLightning(true);

		//System.out.println("k");

		// Don't bother
		if (!chunk.need_render.get())
		{
			//recyclableByteBuffer.recycle();

			chunk.markRenderInProgress(false);
			return true;
		}

		//RecyclableByteBuffer recyclableByteBuffer = cmd.buffersPool.requestByteBuffer();
		
		//TODO only requests a ByteBuffer when it is sure it will actually need one
		//ByteBuffer recyclableByteBufferData = recyclableByteBuffer.accessByteBuffer();
		
		//Wait until we get that
		//if(recyclableByteBufferData == null)
		//	return false;
		
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
						cmd.cache[((relx + 1) * 3 + (rely + 1)) * 3 + (relz + 1)] = chunk2.chunkVoxelData;
					else
						cmd.cache[((relx + 1) * 3 + (rely + 1)) * 3 + (relz + 1)] = null;
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
					cmd.byteBuffers[i][j][k].clear();
				}
			}
		}
		
		//Creates wrapper/interfaces for all the elements
		ChunkRenderer chunkRendererOutput = new ChunkRenderer() {

			@Override
			public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return cmd.byteBuffersWrappers[VertexLayout.INTRICATE.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
			}

			@Override
			public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return cmd.byteBuffersWrappers[VertexLayout.WHOLE_BLOCKS.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
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
				return cmd.getBlockData(chunk, i, k, j);
			}

			@Override
			public int getNeightborData(int side)
			{
				switch (side)
				{
				case (0):
					return cmd.getBlockData(chunk, i - 1, k, j);
				case (1):
					return cmd.getBlockData(chunk, i, k, j + 1);
				case (2):
					return cmd.getBlockData(chunk, i + 1, k, j);
				case (3):
					return cmd.getBlockData(chunk, i, k, j - 1);
				case (4):
					return cmd.getBlockData(chunk, i, k + 1, j);
				case (5):
					return cmd.getBlockData(chunk, i, k - 1, j);
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
		
		bakedBlockId = -1;
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
						voxelRenderer = cmd.defaultVoxelRenderer;
					
					voxelRenderer.renderInto(chunkRendererOutput, chunkRenderingContext, chunk, voxelRenderingContext);
					
					bakedBlockId++;
				}
			}
		}

		// Prepare output
		//recyclableByteBufferData.clear();

		int[][][] sizes = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		int[][][] offsets = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		
		int currentOffset = 0;

		
		//Compute total size
		int sizeInBytes = 0;
		for(VertexLayout vertexLayout : VertexLayout.values())
			for(LodLevel lodLevel : LodLevel.values())
				for(ShadingType renderPass : ShadingType.values())
					{
						int vertexLayoutIndex = vertexLayout.ordinal();
						int lodLevelIndex = lodLevel.ordinal();
						int renderPassIndex = renderPass.ordinal();
					
						final ByteBuffer relevantByteBuffer = cmd.byteBuffers[vertexLayoutIndex][lodLevelIndex][renderPassIndex];
						sizeInBytes += relevantByteBuffer.position();// / vertexLayout.bytesPerVertex;
					}
		ByteBuffer finalData = MemoryUtil.memAlloc(sizeInBytes);
		
		//For EACH section, make offset and shite
		for(VertexLayout vertexLayout : VertexLayout.values())
			for(LodLevel lodLevel : LodLevel.values())
				for(ShadingType renderPass : ShadingType.values())
				{
					int vertexLayoutIndex = vertexLayout.ordinal();
					int lodLevelIndex = lodLevel.ordinal();
					int renderPassIndex = renderPass.ordinal();

					//Else it gets really long for no reason
					final ByteBuffer relevantByteBuffer = cmd.byteBuffers[vertexLayoutIndex][lodLevelIndex][renderPassIndex];
					
					offsets[vertexLayoutIndex][lodLevelIndex][renderPassIndex] = currentOffset;
					sizes[vertexLayoutIndex][lodLevelIndex][renderPassIndex] = relevantByteBuffer.position() / vertexLayout.bytesPerVertex;

					//Move the offset accordingly
					currentOffset += relevantByteBuffer.position();
					
					//Limit the temporary byte buffer and put it's content inside
					relevantByteBuffer.limit(relevantByteBuffer.position());
					relevantByteBuffer.position(0);
					finalData.put(relevantByteBuffer);
					
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

		finalData.flip();
		
		VertexBuffer verticesObject = new VertexBufferGL();
		Fence fence = verticesObject.uploadData(new MemFreeByteBuffer(finalData));

		ChunkMeshDataSections parent = chunk.getChunkRenderData().getData();
		ChunkMeshDataSections newRenderData = new ChunkMeshDataSections(parent, verticesObject, sizes, offsets);
		chunk.getChunkRenderData().setData(newRenderData);
		
		//chunk.getChunkRenderData().setChunkMeshes(new MeshedChunkData(chunk, recyclableByteBuffer, rawBlocksBuffer.position() / (16), complexBlocksBuffer.position() / (24), waterBlocksBuffer.position() / (24)));
		//doneQueue.add(new MeshedChunkData(work, buffer, rawBlocksBuffer.position() / (16), complexBlocksBuffer.position() / (24), waterBlocksBuffer.position() / (24)));

		baker.totalChunksRendered.incrementAndGet();

		chunk.need_render.set(false);
		chunk.requestable.set(true);
		chunk.markRenderInProgress(false);
		
		//Wait until data is actually uploaded to not accidentally OOM while it struggles uploading it
		if(Client.getInstance().configDeprecated().getBoolean("waitForChunkMeshDataUploadBeforeStartingTheNext", true))
			fence.traverse();
		
		return true;
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
			int sl000 = cmd.getSunlight(chunk, i, k, j    );
			int sl00p = cmd.getSunlight(chunk, i, k, j + 1);
			int sl00m = cmd.getSunlight(chunk, i, k, j - 1);

			int sl0p0 = cmd.getSunlight(chunk, i, k + 1, j    );
			int sl0pp = cmd.getSunlight(chunk, i, k + 1, j + 1);
			int sl0pm = cmd.getSunlight(chunk, i, k + 1, j - 1);

			int sl0m0 = cmd.getSunlight(chunk, i, k - 1, j    );
			int sl0mp = cmd.getSunlight(chunk, i, k - 1, j + 1);
			int sl0mm = cmd.getSunlight(chunk, i, k - 1, j - 1);

			int slp00 = cmd.getSunlight(chunk, i + 1, k, j    );
			int slp0p = cmd.getSunlight(chunk, i + 1, k, j + 1);
			int slp0m = cmd.getSunlight(chunk, i + 1, k, j - 1);

			int slpp0 = cmd.getSunlight(chunk, i + 1, k + 1, j    );
			int slppp = cmd.getSunlight(chunk, i + 1, k + 1, j + 1);
			int slppm = cmd.getSunlight(chunk, i + 1, k + 1, j - 1);

			int slpm0 = cmd.getSunlight(chunk, i + 1, k - 1, j    );
			int slpmp = cmd.getSunlight(chunk, i + 1, k - 1, j + 1);
			int slpmm = cmd.getSunlight(chunk, i + 1, k - 1, j - 1);

			int slm00 = cmd.getSunlight(chunk, i - 1, k, j    );
			int slm0p = cmd.getSunlight(chunk, i - 1, k, j + 1);
			int slm0m = cmd.getSunlight(chunk, i - 1, k, j - 1);

			int slmp0 = cmd.getSunlight(chunk, i - 1, k + 1, j    );
			int slmpp = cmd.getSunlight(chunk, i - 1, k + 1, j + 1);
			int slmpm = cmd.getSunlight(chunk, i - 1, k + 1, j - 1);

			int slmm0 = cmd.getSunlight(chunk, i - 1, k - 1, j    );
			int slmmp = cmd.getSunlight(chunk, i - 1, k - 1, j + 1);
			int slmmm = cmd.getSunlight(chunk, i - 1, k - 1, j - 1);
			
			int bl000 = cmd.getBlocklight(chunk, i, k, j    );
			int bl00p = cmd.getBlocklight(chunk, i, k, j + 1);
			int bl00m = cmd.getBlocklight(chunk, i, k, j - 1);

			int bl0p0 = cmd.getBlocklight(chunk, i, k + 1, j    );
			int bl0pp = cmd.getBlocklight(chunk, i, k + 1, j + 1);
			int bl0pm = cmd.getBlocklight(chunk, i, k + 1, j - 1);

			int bl0m0 = cmd.getBlocklight(chunk, i, k - 1, j    );
			int bl0mp = cmd.getBlocklight(chunk, i, k - 1, j + 1);
			int bl0mm = cmd.getBlocklight(chunk, i, k - 1, j - 1);

			int blp00 = cmd.getBlocklight(chunk, i + 1, k, j    );
			int blp0p = cmd.getBlocklight(chunk, i + 1, k, j + 1);
			int blp0m = cmd.getBlocklight(chunk, i + 1, k, j - 1);

			int blpp0 = cmd.getBlocklight(chunk, i + 1, k + 1, j    );
			int blppp = cmd.getBlocklight(chunk, i + 1, k + 1, j + 1);
			int blppm = cmd.getBlocklight(chunk, i + 1, k + 1, j - 1);

			int blpm0 = cmd.getBlocklight(chunk, i + 1, k - 1, j    );
			int blpmp = cmd.getBlocklight(chunk, i + 1, k - 1, j + 1);
			int blpmm = cmd.getBlocklight(chunk, i + 1, k - 1, j - 1);

			int blm00 = cmd.getBlocklight(chunk, i - 1, k, j    );
			int blm0p = cmd.getBlocklight(chunk, i - 1, k, j + 1);
			int blm0m = cmd.getBlocklight(chunk, i - 1, k, j - 1);

			int blmp0 = cmd.getBlocklight(chunk, i - 1, k + 1, j    );
			int blmpp = cmd.getBlocklight(chunk, i - 1, k + 1, j + 1);
			int blmpm = cmd.getBlocklight(chunk, i - 1, k + 1, j - 1);

			int blmm0 = cmd.getBlocklight(chunk, i - 1, k - 1, j    );
			int blmmp = cmd.getBlocklight(chunk, i - 1, k - 1, j + 1);
			int blmmm = cmd.getBlocklight(chunk, i - 1, k - 1, j - 1);
			
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
	

}
