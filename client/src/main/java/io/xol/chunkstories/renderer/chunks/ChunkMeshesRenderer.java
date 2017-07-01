package io.xol.chunkstories.renderer.chunks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
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

		this.chunksBaker = new ChunkMeshesBakerThread(world);

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
		cameraChunkX = Math2.floor((camera.getCameraPosition().getX()) / 32f);
		cameraChunkY = Math2.floor((camera.getCameraPosition().getY()) / 32f);
		cameraChunkZ = Math2.floor((camera.getCameraPosition().getZ()) / 32f);

		//Do a floodfill arround the entity
		List<Chunk> floodFillResults = floodFillArround(camera.getCameraPosition(), (int) RenderingConfig.viewDistance / 32);

		culledChunksNormal.clear();
		//Check they have render data & submit them if they don't
		for (Chunk chunk : floodFillResults)
		{
			ChunkRenderCommand command = new ChunkRenderCommand(chunk);

			//Cull against the camera, security to always render the chunk we're on
			boolean shouldShowChunk = chunk.getChunkX() == cameraChunkX && chunk.getChunkY() == cameraChunkY && chunk.getChunkZ() == cameraChunkZ;
			if (!shouldShowChunk)
			{
				Vector3fm center = new Vector3fm(command.displayWorldX + 16, command.displayWorldY + 15, command.displayWorldZ + 16);
				shouldShowChunk = camera.isBoxInFrustrum(center, new Vector3fm(32, 32, 32));
			}
			
			if(shouldShowChunk)
				culledChunksNormal.add(command);
		}

		culledChunksShadow = updateShadowPVS(camera.getCameraPosition());
	}

	private final int verticalDistance = 8;

	private List<Chunk> floodFillArround(Vector3<Double> vector3, int maxDistance)
	{
		List<Chunk> floodFillSet = new ArrayList<Chunk>();
		Set<Vector3dm> floodFillMask = new HashSet<Vector3dm>();

		Deque<Integer> floodFillDeque = new ArrayDeque<Integer>();

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

			if (floodFillMask.contains(new Vector3dm(chunkX, chunkY, chunkZ)))
				continue;
			floodFillMask.add(new Vector3dm(chunkX, chunkY, chunkZ));

			if (chunk != null)
			{
				if (chunk == null || chunk.isAirChunk())
					sideFrom = -1;

				floodFillSet.add(chunk);
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][2]) && LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3dm(chunkX + 1, chunkY, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX + 1);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(0);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][0]) && LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3dm(chunkX - 1, chunkY, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX - 1);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(2);
				}

				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][4]) && (chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY + 1, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY + 1);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(5);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][5]) && -(chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY - 1, chunkZ)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY - 1);
					floodFillDeque.push(ajustedChunkZ);
					floodFillDeque.push(4);
				}

				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][1]) && LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY, chunkZ + 1)))
				{
					floodFillDeque.push(ajustedChunkX);
					floodFillDeque.push(chunkY);
					floodFillDeque.push(ajustedChunkZ + 1);
					floodFillDeque.push(3);
				}
				if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][3]) && LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY, chunkZ - 1)))
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

	private List<ChunkRenderCommand> updateShadowPVS(Vector3<Double> vector3)
	{
		List<ChunkRenderCommand> shadowChunks = new ArrayList<ChunkRenderCommand>();

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

		Matrix4f matrix = new Matrix4f();
		for (ChunkRenderCommand command : culledChunks)
		{
			matrix.setIdentity();
			matrix.translate(new Vector3fm(command.displayWorldX, command.displayWorldY, command.displayWorldZ));
			
			renderingInterface.setObjectMatrix(matrix);
			
			Vector3dm chunkPos = new Vector3dm(command.displayWorldX + 16, command.displayWorldY + 16, command.displayWorldZ + 16);
			double distance = renderingInterface.getCamera().getCameraPosition().distanceTo(chunkPos);
			
			RenderLodLevel lodToUse;
			/*if(chunkMeshesPass.equals(RenderingPass.SHADOW))
				lodToUse = RenderLodLevel.LOW;
			else*/
				lodToUse = distance < Math.max(64, RenderingConfig.viewDistance / 4.0) ? RenderLodLevel.HIGH : RenderLodLevel.LOW;
			
			((RenderableChunk) command.chunk).getChunkRenderData().renderPass(renderingInterface, lodToUse, shadingType);
		}
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
			if ((this.chunk.isMarkedForReRender() || chunk.needsLightningUpdates()) && !chunk.isAirChunk())
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
		
		RenderedChunksMask(CameraInterface camera, int xz_dimension, int y_dimension) {
			this.xz_dimension = xz_dimension;
			this.y_dimension = y_dimension;
			
			mask = new boolean[xz_dimension * 2 + 1][y_dimension * 2 + 1][xz_dimension * 2 + 1];
			
			centerChunkX = Math2.floor((camera.getCameraPosition().getX()) / 32);
			centerChunkY = Math2.floor((camera.getCameraPosition().getY()) / 32);
			centerChunkZ = Math2.floor((camera.getCameraPosition().getZ()) / 32);
			
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
		
		int centerChunkX, centerChunkY, centerChunkZ;
		
		int xz_dimension;
		int y_dimension;
		boolean[][][] mask;
		
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
