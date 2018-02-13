package io.xol.chunkstories.renderer.chunks;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Vector3dc;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.exceptions.tasks.UnexecutableTaskException;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDynamicallyRendered;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.components.VoxelComponentDynamicRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.chunks.ChunkMeshDataSections.DynamicallyRenderedVoxelClass;
import io.xol.chunkstories.renderer.chunks.ClientWorkerThread.ChunkMeshingBuffers;
import io.xol.chunkstories.world.cell.ScratchCell;
import io.xol.chunkstories.world.chunk.ClientChunk;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.base.MemFreeByteBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TaskBakeChunk extends Task {

	protected final Logger logger = LoggerFactory.getLogger("renderer.chunksbaker");
	protected final ClientChunk chunk;
	
	private final WorldClient world;
	
	//Nasty !
	protected int i = 0, j = 0, k = 0;
	protected int bakedBlockId;
	
	protected ChunkMeshingBuffers cmd;
	
	public TaskBakeChunk(ClientChunk chunk) {
		super();
		//this.baker = baker;
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
			throw new UnexecutableTaskException(this, "This class requires to be executed by a BakeChunkTaskExecutor");
		
		this.cmd = ((BakeChunkTaskExecutor)taskExecutor).getBuffers();
		
		if(chunk == null) {
			throw new RuntimeException("Fuck off no");
		}
		
		Vector3dc camera = ((WorldClient) chunk.getWorld()).getWorldRenderer().getRenderingInterface().getCamera().getCameraPosition();
	
		//Check we aren't too far from the camera, and thus that our request hasn't been yet cancelled
		int vx = Math2.floor(camera.x() / 32);
		int vy = Math2.floor(camera.y() / 32);
		int vz = Math2.floor(camera.z() / 32);
		int dx = LoopingMathHelper.moduloDistance(chunk.getChunkX(), vx, chunk.getWorld().getSizeInChunks());
		int dz = LoopingMathHelper.moduloDistance(chunk.getChunkZ(), vz, chunk.getWorld().getSizeInChunks());
		int dy = Math.abs(chunk.getChunkY() - vy);
		
		int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);
		
		//System.out.println("heil" + chunk);
		
		if(dx > chunksViewDistance || dz > chunksViewDistance || dy > 2) {
			logger.info("unscheduled chunk mesh render task for it being too far to be rendered anyway");
			return true;
		}

		//Require the chunk and nearby ones to be already loaded in the world
		ChunkRenderable chunkWithinWorld = (ChunkRenderable) world.getChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
		if (chunkWithinWorld != null)
		{
			//Require the chunks ARROUND it to be already loaded in the world
			int nearChunks = 0;
			if (world.isChunkLoaded(chunk.getChunkX() + 1, chunk.getChunkY(), chunk.getChunkZ()))
				nearChunks++;
			if (world.isChunkLoaded(chunk.getChunkX() - 1, chunk.getChunkY(), chunk.getChunkZ()))
				nearChunks++;
			if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ() + 1))
				nearChunks++;
			if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ() - 1))
				nearChunks++;
			if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY() + 1, chunk.getChunkZ()) || chunk.getChunkY() == world.getWorldInfo().getSize().heightInChunks - 1)
				nearChunks++;
			if (world.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY() - 1, chunk.getChunkZ()) || chunk.getChunkY() == 0)
				nearChunks++;

			if (nearChunks != 6) {
				
				//We wait until that's the case
				return false;
			}
		}
		else {
			
			//We wait until the chunk is loaded in the world ( or destroyed, then the task is cancelled )
			return false;
		}

		// If the chunk has pending light updates, wait until THOSE are done
		if(chunk.lightBaker.pendingUpdates() > 0) {
			chunk.lightBaker.spawnUpdateTaskIfNeeded();
			return false;
		}
		
		int updatesToConsider = chunk.chunkRenderData.unbakedUpdates.get();
		
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
				return (VoxelBakerHighPoly) cmd.byteBuffersWrappers[VertexLayout.INTRICATE.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
			}

			@Override
			public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return (VoxelBakerCubic) cmd.byteBuffersWrappers[VertexLayout.WHOLE_BLOCKS.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
			}
			
		};
		
		/*CellData voxelRenderingContext = new CellData()
		{
			@Override
			public Voxel getVoxel()
			{
				return store.getVoxelById(getData());
			}

			public int getData()
			{
				return cmd.getBlockData(chunk, i, k, j);
			}

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

		};*/

		ChunkBakerRenderContext chunkRenderingContext = new ChunkBakerRenderContext(chunk, cx, cy, cz);
		
		bakedBlockId = -1;
		
		Map<Voxel, DynamicallyRenderedVoxelClass> dynamicVoxels = new HashMap<>();
		
		ScratchCell cell = new ScratchCell(world);
		//Render the fucking thing!
		for (i = 0; i < 32; i++)
		{
			for (j = 0; j < 32; j++)
			{
				for (k = 0; k < 32; k++)
				{
					peek(i, k, j, cell);
					
					if (cell.voxel.isAir())
						continue;
					
					// Fill near-blocks info
					// chunkRenderingContext.prepareVoxelLight(); // lol nope
					
					VoxelRenderer voxelRenderer = cell.getVoxelRenderer();
					if(voxelRenderer == null)
						voxelRenderer = world.getContent().voxels().getDefaultVoxelRenderer();
					
					// Run the VoxelRenderer
					voxelRenderer.renderInto(chunkRendererOutput, chunkRenderingContext, chunk, cell);
					
					// We handle voxels with a dynamic renderer here too - we just add them to a list !
					if(cell.voxel instanceof VoxelDynamicallyRendered) {
						DynamicallyRenderedVoxelClass vClass = dynamicVoxels.get(cell.voxel);
						if(vClass == null) {
							vClass = new DynamicallyRenderedVoxelClass();
							
							//TODO cache it world-wide 
							VoxelComponentDynamicRenderer component = ((VoxelDynamicallyRendered)cell.voxel).getDynamicRendererComponent(chunk.peek(i, k, j));
							
							if(component != null) {
								vClass.renderer = component.getVoxelDynamicRenderer();
								if(vClass.renderer != null)
									dynamicVoxels.put(cell.voxel, vClass);
							}
						}
						vClass.indexes.add(i * 1024 + k * 32 + j);
					}
					
					bakedBlockId++;
				}
			}
		}

		//Parse output neatly
		int[][][] sizes = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		int[][][] offsets = new int[ChunkMeshDataSubtypes.VertexLayout.values().length][ChunkMeshDataSubtypes.LodLevel.values().length][ChunkMeshDataSubtypes.ShadingType.values().length];;
		
		int currentOffset = 0;
		
		//Compute total size to create final bytebuffer
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
		MemFreeByteBuffer wrappedBuffer = new MemFreeByteBuffer(finalData);
		
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
					
					//Limit the temporary byte buffer and fill the main buffer with it
					relevantByteBuffer.limit(relevantByteBuffer.position());
					relevantByteBuffer.position(0);
					finalData.put(relevantByteBuffer);
				}

		finalData.flip();
		
		ChunkMeshDataSections newRenderData = new ChunkMeshDataSections(wrappedBuffer, sizes, offsets);
		newRenderData.dynamicallyRenderedVoxels = dynamicVoxels;
		
		chunk.getChunkRenderData().setData(newRenderData);
		
		chunk.chunkRenderData.unbakedUpdates.addAndGet(-updatesToConsider);
		
		//Wait until data is actually uploaded to not accidentally OOM while it struggles uploading it
		if(Client.getInstance().configDeprecated().getBoolean("waitForChunkMeshDataUploadBeforeStartingTheNext", true))
			newRenderData.fence.traverse();
		
		//System.out.println(wrappedBuffer);
		
		return true;
	}
	
	private void peek(int x, int y, int z, ScratchCell cell) {
		cell.x = (x & 0x1F) + (chunk.getChunkX() << 5);
		cell.y = (y & 0x1F) + (chunk.getChunkY() << 5);
		cell.z = (z & 0x1F) + (chunk.getChunkZ() << 5);
		
		int rawData = world.peekRaw(cell.x, cell.y, cell.z);
		cell.voxel = world.getContentTranslator().getVoxelForId(VoxelFormat.id(rawData));
		cell.sunlight = VoxelFormat.sunlight(rawData);
		cell.blocklight = VoxelFormat.blocklight(rawData);
		cell.metadata = VoxelFormat.meta(rawData);
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
