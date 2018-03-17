//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.chunks;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;

import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer.FarTerrainRenderer.ReadyVoxelMeshesMask;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.rendering.world.chunk.ChunksRenderer;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;
import io.xol.chunkstories.world.chunk.ClientChunk;

public class ChunkMeshesRenderer implements ChunksRenderer {
	@SuppressWarnings("unused")
	private final WorldRenderer worldRenderer;
	private final WorldClient world;

	private final int worldSizeInChunks;
	private final int wrapChunksDistance;

	private List<ChunkRenderCommand> culledChunksShadow;
	private List<ChunkRenderCommand> culledChunksNormal = new ArrayList<ChunkRenderCommand>();

	int cameraChunkX, cameraChunkY, cameraChunkZ;

	public ChunkMeshesRenderer(WorldRenderer worldRenderer) {
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();

		int nbThreads = -1;
		String configThreads = world.getClient().getConfiguration().getStringOption("client.game.workersThreads");//Client.getInstance().configDeprecated().getString("workersThreads", "auto");
		if (!configThreads.equals("auto")) {
			try {
				nbThreads = Integer.parseInt(configThreads);
			} catch (NumberFormatException e) {
			}
		}

		if (nbThreads <= 0) {
			nbThreads = Runtime.getRuntime().availableProcessors() / 2;

			// Fail-safe
			if (nbThreads < 1)
				nbThreads = 1;
		}

		// this.chunksBaker = new ClientTasksPool(world, nbThreads);

		this.worldSizeInChunks = world.getWorldInfo().getSize().sizeInChunks;

		this.wrapChunksDistance = worldSizeInChunks / 2;
	}

	@Override
	public void updatePVSSet(CameraInterface camera) {
		// Updates these once
		cameraChunkX = Math2.floor((camera.getCameraPosition().x()) / 32f);
		cameraChunkY = Math2.floor((camera.getCameraPosition().y()) / 32f);
		cameraChunkZ = Math2.floor((camera.getCameraPosition().z()) / 32f);

		Vector3fc cameraFloatPosition = new Vector3f((float) camera.getCameraPosition().x(),
				(float) camera.getCameraPosition().y(), (float) camera.getCameraPosition().z());

		// Do a floodfill arround the entity
		List<ClientChunk> floodFillResults = floodFillArround(cameraFloatPosition,
				(int) world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 32);

		culledChunksNormal.clear();
		// Check they have render data & submit them if they don't
		for (ClientChunk chunk : floodFillResults) {
			ChunkRenderCommand command = new ChunkRenderCommand(chunk, camera.getCameraPosition());

			// Cull against the camera, security to always render the chunk we're on
			boolean shouldShowChunk = chunk.getChunkX() == cameraChunkX && chunk.getChunkY() == cameraChunkY
					&& chunk.getChunkZ() == cameraChunkZ;
			if (!shouldShowChunk) {
				Vector3f center = new Vector3f(command.displayWorldX + 16, command.displayWorldY + 16,
						command.displayWorldZ + 16);
				shouldShowChunk = camera.isBoxInFrustrum(center, new Vector3f(32, 32, 32));
			}

			if (shouldShowChunk)
				culledChunksNormal.add(command);
		}

		culledChunksShadow = updateShadowPVS(camera.getCameraPosition());
	}

	private final int verticalDistance = 8;

	private final List<ClientChunk> floodFillSet = new ArrayList<ClientChunk>();
	// private final Set<Vector3d> floodFillMask = new HashSet<Vector3d>();
	private boolean[] floodFillMask = null;

	// private final Deque<Integer> floodFillDeque = new ArrayDeque<Integer>();
	private final IntDeque floodFillDeque = new IntArrayDeque();

	private int indexInto(int x, int y, int z, int maxDistance) {
		int horizontalSize = maxDistance * 2 + 3;

		int dx = x - cameraChunkX;
		int dz = z - cameraChunkZ;

		if (dx > world.getSizeInChunks() / 2)
			dx = world.getSizeInChunks() / 2 - dx;

		if (dz > world.getSizeInChunks() / 2)
			dz = world.getSizeInChunks() / 2 - dz;

		int dy = y - cameraChunkY;
		int i = (dx + maxDistance + 1) * horizontalSize * horizontalSize + (dz + maxDistance + 1) * horizontalSize
				+ (dy + verticalDistance);

		return i;
	}

	private List<ClientChunk> floodFillArround(Vector3fc vector3, int maxDistance) {
		floodFillSet.clear();
		// floodFillMask.clear();
		floodFillDeque.clear();

		int horizontalSize = maxDistance * 2 + 3;
		int expectedLength = horizontalSize * horizontalSize * horizontalSize;
		if (floodFillMask == null || floodFillMask.length != expectedLength) {
			floodFillMask = new boolean[expectedLength];
		} else {
			for (int i = 0; i < floodFillMask.length; i++)
				floodFillMask[i] = false;
		}

		floodFillDeque.addLast(cameraChunkX);
		floodFillDeque.addLast(cameraChunkY);
		floodFillDeque.addLast(cameraChunkZ);
		floodFillDeque.addLast(-1);

		while (!floodFillDeque.isEmpty()) {
			int sideFrom = floodFillDeque.removeLast();
			int chunkZ = floodFillDeque.removeLast();
			int chunkY = floodFillDeque.removeLast();
			int chunkX = floodFillDeque.removeLast();
			sideFrom = -1;

			int ajustedChunkX = chunkX;
			int ajustedChunkZ = chunkZ;

			// TODO made forced cast irrelevant
			ClientChunk chunk = (ClientChunk) world.getChunk(chunkX, chunkY, chunkZ);

			int index = indexInto(chunkX, chunkY, chunkZ, maxDistance);

			// if (floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ)))
			if (floodFillMask[index])
				continue;

			floodFillMask[index] = true;
			// floodFillMask.add(new Vector3d(chunkX, chunkY, chunkZ));

			if (chunk != null) {
				chunk.occlusion.spawnUpdateTaskIfNeeded();
				boolean[][] occlusionSides = chunk.occlusion.occlusionSides;

				if (chunk.isAirChunk() || occlusionSides == null)
					sideFrom = -1;

				floodFillSet.add(chunk);
				if ((sideFrom == -1 || occlusionSides[sideFrom][2])
						&& LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance
						&& !floodFillMask[indexInto(chunkX + 1, chunkY, chunkZ, maxDistance)]) {
					floodFillDeque.addLast(ajustedChunkX + 1);
					floodFillDeque.addLast(chunkY);
					floodFillDeque.addLast(ajustedChunkZ);
					floodFillDeque.addLast(0);
				}
				if ((sideFrom == -1 || occlusionSides[sideFrom][0])
						&& LoopingMathHelper.moduloDistance(chunkX, cameraChunkX, worldSizeInChunks) < maxDistance
						&& !floodFillMask[indexInto(chunkX - 1, chunkY, chunkZ, maxDistance)]) {
					floodFillDeque.addLast(ajustedChunkX - 1);
					floodFillDeque.addLast(chunkY);
					floodFillDeque.addLast(ajustedChunkZ);
					floodFillDeque.addLast(2);
				}

				if ((sideFrom == -1 || occlusionSides[sideFrom][4]) && chunkY < world.getMaxHeight() / 32
						&& (chunkY - cameraChunkY) < verticalDistance
						&& !floodFillMask[indexInto(chunkX, chunkY + 1, chunkZ, maxDistance)])
				{
					floodFillDeque.addLast(ajustedChunkX);
					floodFillDeque.addLast(chunkY + 1);
					floodFillDeque.addLast(ajustedChunkZ);
					floodFillDeque.addLast(5);
				}
				if ((sideFrom == -1 || occlusionSides[sideFrom][5]) && chunkY > 0
						&& (cameraChunkY - chunkY) < verticalDistance
						&& !floodFillMask[indexInto(chunkX, chunkY - 1, chunkZ, maxDistance)])
				{
					floodFillDeque.addLast(ajustedChunkX);
					floodFillDeque.addLast(chunkY - 1);
					floodFillDeque.addLast(ajustedChunkZ);
					floodFillDeque.addLast(4);
				}

				if ((sideFrom == -1 || occlusionSides[sideFrom][1])
						&& LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance
						&& !floodFillMask[indexInto(chunkX, chunkY, chunkZ + 1, maxDistance)])
				{
					floodFillDeque.addLast(ajustedChunkX);
					floodFillDeque.addLast(chunkY);
					floodFillDeque.addLast(ajustedChunkZ + 1);
					floodFillDeque.addLast(3);
				}
				if ((sideFrom == -1 || occlusionSides[sideFrom][3])
						&& LoopingMathHelper.moduloDistance(chunkZ, cameraChunkZ, worldSizeInChunks) < maxDistance
						&& !floodFillMask[indexInto(chunkX, chunkY, chunkZ - 1, maxDistance)])
				{
					floodFillDeque.addLast(ajustedChunkX);
					floodFillDeque.addLast(chunkY);
					floodFillDeque.addLast(ajustedChunkZ - 1);
					floodFillDeque.addLast(1);
				}
			}
		}

		return floodFillSet;
	}

	private final List<ChunkRenderCommand> shadowChunks = new ArrayList<ChunkRenderCommand>();

	private List<ChunkRenderCommand> updateShadowPVS(Vector3dc vector3) {
		// Micro-optimization: Moved to a field
		// List<ChunkRenderCommand> shadowChunks = new ArrayList<ChunkRenderCommand>();
		shadowChunks.clear();

		int maxShadowDistance = 4;
		if (world.getClient().getConfiguration().getIntOption("client.rendering.shadowsResolution") >= 2048)
			maxShadowDistance = 5;
		if (world.getClient().getConfiguration().getIntOption("client.rendering.shadowsResolution") >= 4096)
			maxShadowDistance = 60;

		int maxVerticalShadowDistance = 4;

		for (int x = cameraChunkX - maxShadowDistance; x < cameraChunkX + maxShadowDistance; x++)
			for (int y = cameraChunkY - maxVerticalShadowDistance; y < cameraChunkY + maxVerticalShadowDistance; y++)
				for (int z = cameraChunkZ - maxShadowDistance; z < cameraChunkZ + maxShadowDistance; z++) {
					// TODO have this cast made irrelevant
					ClientChunk chunk = (ClientChunk) world.getChunk(x, y, z);
					if (chunk != null)
						shadowChunks.add(new ChunkRenderCommand(chunk, vector3));
				}

		return shadowChunks;
	}

	@Override
	public void renderChunks(RenderingInterface renderingInterface) {
		RenderPass currentPass = renderingInterface.getCurrentPass();
		List<ChunkRenderCommand> culledChunks = currentPass.name.startsWith("shadow") ? culledChunksShadow : culledChunksNormal;

		ShadingType shadingType = currentPass.name.startsWith("water") ? ShadingType.LIQUIDS : ShadingType.OPAQUE;

		if (currentPass.name.startsWith("shadow"))
			renderingInterface.currentShader().setUniform1f("useVoxelCoordinates", 1f);

		Matrix4f matrix = new Matrix4f();
		for (ChunkRenderCommand command : culledChunks) {
			matrix.identity();
			matrix.translate(new Vector3f(command.displayWorldX, command.displayWorldY, command.displayWorldZ));

			renderingInterface.setObjectMatrix(matrix);

			Vector3d chunkPos = new Vector3d(command.displayWorldX + 16, command.displayWorldY + 16,
					command.displayWorldZ + 16);
			double distance = renderingInterface.getCamera().getCameraPosition().distance(chunkPos);

			RenderLodLevel lodToUse;
			
			lodToUse = distance < Math.max(64, world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 4.0) ? RenderLodLevel.HIGH : RenderLodLevel.LOW;

			((ClientChunk) command.chunk).getChunkRenderData().renderPass(renderingInterface, lodToUse, shadingType);
		}

		if (currentPass.name.startsWith("shadow"))
			renderingInterface.currentShader().setUniform1f("useVoxelCoordinates", 0f);
	}

	@Override
	public void renderChunksExtras(RenderingInterface renderingInterface) {
		RenderPass currentPass = renderingInterface.getCurrentPass();
		List<ChunkRenderCommand> culledChunks = currentPass.name.startsWith("shadow") ? culledChunksShadow
				: culledChunksNormal;

		for (ChunkRenderCommand command : culledChunks) {
			ChunkRenderDataHolder holder = ((ClientChunk) command.chunk).getChunkRenderData();
			holder.renderExtras(renderingInterface);
		}
	}

	public String getChunksVisible() {
		return "shadow: " + culledChunksShadow.size() + " usual:" + culledChunksNormal.size();
	}

	private class ChunkRenderCommand {
		public ChunkRenderCommand(ClientChunk chunk, Vector3dc camera) {
			this.chunk = (ChunkRenderable) chunk;

			this.displayWorldY = chunk.getChunkY() << 5;

			int displayWorldX = chunk.getChunkX() << 5;
			int displayWorldZ = chunk.getChunkZ() << 5;

			// Request rendering them if they aren't already present
			// if ((this.chunk.isMarkedForReRender() /*|| chunk.needsLightningUpdates()*/)
			// && !chunk.isAirChunk())
			// chunksBaker.requestChunkRender(this.chunk);
			if (!this.chunk.isAirChunk() && this.chunk.meshUpdater().pendingUpdates() > 0) {
				// Check it's not too distant from the camera
				int vx = Math2.floor(camera.x() / 32);
				int vy = Math2.floor(camera.y() / 32);
				int vz = Math2.floor(camera.z() / 32);
				int dx = LoopingMathHelper.moduloDistance(chunk.getChunkX(), vx, chunk.getWorld().getSizeInChunks());
				int dz = LoopingMathHelper.moduloDistance(chunk.getChunkZ(), vz, chunk.getWorld().getSizeInChunks());
				int dy = Math.abs(chunk.getChunkY() - vy);

				int chunksViewDistance = (int) (world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 32);

				if (dx <= chunksViewDistance && dz <= chunksViewDistance && dy <= 2)
					this.chunk.meshUpdater().spawnUpdateTaskIfNeeded();
			}

			// We wrap the chunks if they are too far
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

	public void destroy() {
	}

	@Override
	public RenderedChunksMask getRenderedChunksMask(CameraInterface camera) {
		return new RenderedChunksMask(camera, Math.max(2, (int) (world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 32f) - 1), 5);
	}

	public class RenderedChunksMask implements ReadyVoxelMeshesMask {

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

			for (int a = centerChunkX - xz_dimension; a <= centerChunkX + xz_dimension; a++)
				for (int b = centerChunkY - y_dimension; b < centerChunkY + y_dimension; b++)
					for (int c = centerChunkZ - xz_dimension; c <= centerChunkZ + xz_dimension; c++) {
						Chunk chunk = world.getChunk(a, b, c);
						// If the chunk is loaded & it either empty or has rendering data, we add it to
						// the mask
						if (chunk != null && (chunk.isAirChunk()
								|| ((ClientChunk) chunk).getChunkRenderData().isDataAvailable())) {
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

			// Out of checkable bounds
			if (Math.abs(dx) >= xz_dimension || Math.abs(dz) >= xz_dimension)
				return false;

			for (int cy = (int) Math.floor(min / 32f); cy < (int) Math.ceil(max / 32f); cy++) {
				int dy = cy - centerChunkY;

				// Partially occluded or out of checkable bounds
				if (Math.abs(dy) >= y_dimension)
					return false;

				if (!mask[dx + xz_dimension][dy + y_dimension][dz + xz_dimension])
					return false;

				// System.out.println("checking array" + chunkX + ":"+chunkZ+mask[dx +
				// xz_dimension][dy + y_dimension][dz + xz_dimension]);
			}
			return true;
		}
	}
}
