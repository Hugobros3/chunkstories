package io.xol.chunkstories.renderer.chunks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.WorldRenderer.RenderingPass;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.chunks.RenderableChunk;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class ChunkMeshesRenderer
{
	public ChunkMeshesRenderer(WorldRenderer worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();

		//this.chunksBaker = new ChunkMeshesBakerThread(world);
		
		int nbThreads = -1;
		String configThreads = Client.getInstance().configDeprecated().getString("workersThreads", "auto");
		if(!configThreads.equals("auto")) {
			try {
				nbThreads = Integer.parseInt(configThreads);
			}
			catch(NumberFormatException e) {}
		}
		
		if(nbThreads <= 0) {
			nbThreads = Runtime.getRuntime().availableProcessors() / 2;
			
			//Fail-safe
			if(nbThreads < 1)
				nbThreads = 1;
		}
		
		this.chunksBaker = new ClientTasksPool(world, nbThreads);

		this.worldSizeInChunks = world.getWorldInfo().getSize().sizeInChunks;

		this.wrapChunksDistance = worldSizeInChunks / 2;
	}

	private final WorldRenderer worldRenderer;
	private final WorldClient world;

	private final ChunkMeshesBaker chunksBaker;

	private final int worldSizeInChunks;
	private final int wrapChunksDistance;

	private List<ChunkRenderCommand> culledChunksShadow;
	private List<ChunkRenderCommand> culledChunksNormal = new ArrayList<ChunkRenderCommand>();

	int cameraChunkX, cameraChunkY, cameraChunkZ;

	/** Computes wich parts of the world could be seen by said entity */
	public void updatePVSSet(CameraInterface camera)
	{
		//Updates these once
		cameraChunkX = Math2.floor((camera.getCameraPosition().x()) / 32f);
		cameraChunkY = Math2.floor((camera.getCameraPosition().y()) / 32f);
		cameraChunkZ = Math2.floor((camera.getCameraPosition().z()) / 32f);

		Vector3fc cameraFloatPosition = new Vector3f((float)camera.getCameraPosition().x(), (float)camera.getCameraPosition().y(), (float)camera.getCameraPosition().z());
		
		//Do a floodfill arround the entity
		List<Chunk> floodFillResults = floodFillArround(cameraFloatPosition, (int) RenderingConfig.viewDistance / 32);

		culledChunksNormal.clear();
		//Check they have render data & submit them if they don't
		for (Chunk chunk : floodFillResults)
		{
			ChunkRenderCommand command = new ChunkRenderCommand(chunk);

			//Cull against the camera, security to always render the chunk we're on
			boolean shouldShowChunk = chunk.getChunkX() == cameraChunkX && chunk.getChunkY() == cameraChunkY && chunk.getChunkZ() == cameraChunkZ;
			if (!shouldShowChunk)
			{
				Vector3f center = new Vector3f(command.displayWorldX + 16, command.displayWorldY + 15, command.displayWorldZ + 16);
				shouldShowChunk = camera.isBoxInFrustrum(center, new Vector3f(32, 32, 32));
			}
			
			if(shouldShowChunk)
				culledChunksNormal.add(command);
		}

		culledChunksShadow = updateShadowPVS(cameraFloatPosition);
	}

	private final int verticalDistance = 8;

	private final List<Chunk> floodFillSet = new ArrayList<Chunk>();
	private final Set<Vector3d> floodFillMask = new HashSet<Vector3d>();

	private final Deque<Integer> floodFillDeque = new ArrayDeque<Integer>();
	
	private List<Chunk> floodFillArround(Vector3fc vector3, int maxDistance)
	{
		floodFillSet.clear();
		floodFillMask.clear();
		floodFillDeque.clear();
		//Micro-optimization: Moved those to fields
		/*List<Chunk> floodFillSet = new ArrayList<Chunk>();
		Set<Vector3d> floodFillMask = new HashSet<Vector3d>();

		Deque<Integer> floodFillDeque = new ArrayDeque<Integer>();*/

		floodFillDeque.push(cameraChunkX);
		floodFillDeque.push(cameraChunkY);
		floodFillDeque.push(cameraChunkZ);
		floodFillDeque.push(-1);

		while (!floodFillDeque.isEmpty())
		{
			int sideFrom = floodFillDeque.pop();
			int chunkZ = floodFillDeque.pop();
			int chunkY = floodFillDeque.pop();
			int chunkX = floodFillDeque.pop();
			//sideFrom = -1;

			int ajustedChunkX = chunkX;
			int ajustedChunkZ = chunkZ;

			Chunk chunk = world.getChunk(chunkX, chunkY, chunkZ);

			if (floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ)))
				continue;
			floodFillMask.add(new Vector3d(chunkX, chunkY, chunkZ));

			if (chunk != null)
			{
				if (chunk == null || chunk.isAirChunk())
					sideFrom = -1;

				floodFillSet.add(chunk);
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][2]) && LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3d(chunkX + 1, chunkY, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX + 1);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(0);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][0]) && LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3d(chunkX - 1, chunkY, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX - 1);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(2);
				}

				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][4]) && (chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY + 1, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY + 1);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(5);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][5]) && -(chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY - 1, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY - 1);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(4);
				}

				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][1]) && LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ + 1)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ + 1);
					floodFillDeque.push(3);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][3]) && LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ - 1)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ - 1);
					floodFillDeque.push(1);
				}
			}
		}

		return floodFillSet;
	}

	private final List<ChunkRenderCommand> shadowChunks = new ArrayList<ChunkRenderCommand>();
	private List<ChunkRenderCommand> updateShadowPVS(Vector3fc vector3)
	{
		//Micro-optimization: Moved to a field
		//List<ChunkRenderCommand> shadowChunks = new ArrayList<ChunkRenderCommand>();
		shadowChunks.clear();

		int maxShadowDistance = 4;
		if (RenderingConfig.shadowMapResolutions >= 2048)
			maxShadowDistance = 5;
		if (RenderingConfig.shadowMapResolutions >= 4096)
			maxShadowDistance = 6;

		int maxVerticalShadowDistance = 4;

		for (int x = cameraChunkX - maxShadowDistance; x < cameraChunkX + maxShadowDistance; x++)
			for (int y = cameraChunkY - maxVerticalShadowDistance; y < cameraChunkY + maxVerticalShadowDistance; y++)
				for (int z = cameraChunkZ - maxShadowDistance; z < cameraChunkZ + maxShadowDistance; z++)
				{
					Chunk chunk = world.getChunk(x, y, z);
					if (chunk != null)
						shadowChunks.add(new ChunkRenderCommand(chunk));
				}

		return shadowChunks;
	}

	public void renderChunks(RenderingInterface renderingInterface, WorldRenderer.RenderingPass chunkMeshesPass)
	{
		List<ChunkRenderCommand> culledChunks = (chunkMeshesPass == WorldRenderer.RenderingPass.SHADOW) ? culledChunksShadow : culledChunksNormal;

		ShadingType shadingType = null;
		switch (chunkMeshesPass)
		{
			case SHADOW:
				shadingType = ShadingType.OPAQUE;
				break;
			case NORMAL_OPAQUE:
				shadingType = ShadingType.OPAQUE;
				break;
			case NORMAL_LIQUIDS_PASS_1:
				shadingType = ShadingType.LIQUIDS;
				break;
			case NORMAL_LIQUIDS_PASS_2:
				shadingType = ShadingType.LIQUIDS;
				break;
			default:
				throw new RuntimeException("Undefined ChunkMeshesPass "+chunkMeshesPass);
		}
		
		if(chunkMeshesPass == RenderingPass.SHADOW)
			renderingInterface.currentShader().setUniform1f("useVoxelCoordinates", 1f);

		Matrix4f matrix = new Matrix4f();
		for (ChunkRenderCommand command : culledChunks)
		{
			matrix.identity();
			matrix.translate(new Vector3f(command.displayWorldX, command.displayWorldY, command.displayWorldZ));
			
			renderingInterface.setObjectMatrix(matrix);
			
			Vector3d chunkPos = new Vector3d(command.displayWorldX + 16, command.displayWorldY + 16, command.displayWorldZ + 16);
			double distance = renderingInterface.getCamera().getCameraPosition().distance(chunkPos);
			
			RenderLodLevel lodToUse;
			/*if(chunkMeshesPass.equals(RenderingPass.SHADOW))
				lodToUse = RenderLodLevel.LOW;
			else*/
				lodToUse = distance < Math.max(64, RenderingConfig.viewDistance / 4.0) ? RenderLodLevel.HIGH : RenderLodLevel.LOW;
			
			((RenderableChunk) command.chunk).getChunkRenderData().renderPass(renderingInterface, lodToUse, shadingType);
		}

		if(chunkMeshesPass == RenderingPass.SHADOW)
			renderingInterface.currentShader().setUniform1f("useVoxelCoordinates", 0f);
	}
	
	public int getChunksVisibleForPass(WorldRenderer.RenderingPass chunkMeshesPass)
	{
		List<ChunkRenderCommand> culledChunks = (chunkMeshesPass == WorldRenderer.RenderingPass.SHADOW) ? culledChunksShadow : culledChunksNormal;
		return culledChunks.size();
	}

	private class ChunkRenderCommand
	{
		public ChunkRenderCommand(Chunk chunk)
		{
			this.chunk = (ChunkRenderable) chunk;

			//Request rendering them if they aren't already present
			if ((this.chunk.isMarkedForReRender() /*|| chunk.needsLightningUpdates()*/) && !chunk.isAirChunk())
				chunksBaker.requestChunkRender(this.chunk);

			this.displayWorldY = chunk.getChunkY() << 5;

			int displayWorldX = chunk.getChunkX() << 5;
			int displayWorldZ = chunk.getChunkZ() << 5;

			//We wrap the chunks if they are too far
			if (chunk.getChunkX() - cameraChunkX > wrapChunksDistance)
				displayWorldX += -worldSizeInChunks * 32;
			if (chunk.getChunkX() - cameraChunkX < -wrapChunksDistance)
				displayWorldX += worldSizeInChunks * 32;

			if (chunk.getChunkZ() - cameraChunkZ > wrapChunksDistance)
				displayWorldZ += -worldSizeInChunks * 32;
			if (chunk.getChunkZ() - cameraChunkZ < -wrapChunksDistance)
				displayWorldZ += worldSizeInChunks * 32;

			this.displayWorldX = displayWorldX;
			this.displayWorldZ = displayWorldZ;
		}

		final ChunkRenderable chunk;
		final int displayWorldX;
		final int displayWorldY;
		final int displayWorldZ;
	}

	public void destroy()
	{
		this.chunksBaker.destroy();
	}

	public ChunkMeshesBaker getBaker()
	{
		return this.chunksBaker;
	}
	
	public RenderedChunksMask getRenderedChunksMask(CameraInterface camera) {
		return new RenderedChunksMask(camera, Math.max(2, (int)(RenderingConfig.viewDistance / 32f) - 1), 5);
	}
	
	public class RenderedChunksMask {

		int centerChunkX, centerChunkY, centerChunkZ;
		
		int xz_dimension;
		int y_dimension;
		boolean[][][] mask;
		
		RenderedChunksMask(CameraInterface camera, int xz_dimension, int y_dimension) {
			this.xz_dimension = xz_dimension;
			this.y_dimension = y_dimension;
			
			mask = new boolean[xz_dimension * 2 + 1][y_dimension * 2 + 1][xz_dimension * 2 + 1];
			
			centerChunkX = Math2.floor((camera.getCameraPosition().x()) / 32);
			centerChunkY = Math2.floor((camera.getCameraPosition().y()) / 32);
			centerChunkZ = Math2.floor((camera.getCameraPosition().z()) / 32);
			
			for(int a = centerChunkX - xz_dimension; a <= centerChunkX + xz_dimension; a++)
				for(int b = centerChunkY - y_dimension; b < centerChunkY + y_dimension; b++)
					for(int c = centerChunkZ - xz_dimension; c <= centerChunkZ + xz_dimension; c++)
					{
						Chunk chunk = world.getChunk(a, b, c);
						if(chunk != null && (chunk.isAirChunk() || ((RenderableChunk)chunk).getChunkRenderData().getData() != null))
						{
							int dx = a - centerChunkX + xz_dimension;
							int dy = b - centerChunkY + y_dimension;
							int dz = c - centerChunkZ + xz_dimension;
							mask[dx][dy][dz] = true;
						}
					}
			
		}
		
		public boolean shouldMaskSlab(int chunkX, int chunkZ, int min, int max) {
			
			int dx = chunkX - centerChunkX;
			int dz = chunkZ - centerChunkZ;
			
			//Out of checkable bounds
			if(Math.abs(dx) >= xz_dimension || Math.abs(dz) >= xz_dimension)
				return false;
			
			for(int cy = (int)Math.floor(min / 32f); cy < (int)Math.ceil(max / 32f); cy++)
			{
				int dy = cy - centerChunkY;
				
				//Partially occluded or out of checkable bounds
				if(Math.abs(dy) >= y_dimension)
					return false;
				
				if(!mask[dx + xz_dimension][dy + y_dimension][dz + xz_dimension])
					return false;
				
				//System.out.println("checking array" + chunkX + ":"+chunkZ+mask[dx + xz_dimension][dy + y_dimension][dz + xz_dimension]);
			}
			return true;
		}
	}
}
