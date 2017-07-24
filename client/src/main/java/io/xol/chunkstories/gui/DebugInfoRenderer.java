package io.xol.chunkstories.gui;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.chunks.RenderableChunk;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.Texture2DGL;

public class DebugInfoRenderer {
	
	ClientInterface client;
	WorldClientCommon world;
	
	public DebugInfoRenderer(ClientInterface client, WorldClientCommon world) {
		super();
		this.client = client;
		this.world = world;
	}
	
	public void drawF3debugMenu(RenderingInterface renderingInterface)
	{
		CameraInterface camera = renderingInterface.getCamera();
		Entity playerEntity = client.getPlayer().getControlledEntity();
		
		/*int timeTook = Client.profiler.timeTook();
		String debugInfo = Client.profiler.reset("gui").toString();
		if (timeTook > 400)
			System.out.println("Lengty frame, printing debug information : \n" + debugInfo);*/

		//Memory usage
		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		//By default use the camera position
		int bx = ((int)camera.getCameraPosition().x());
		int by = ((int)camera.getCameraPosition().y());
		int bz = ((int)camera.getCameraPosition().z());
		
		//If the player can look use that
		if(playerEntity != null && playerEntity instanceof EntityControllable) {
			Location loc = ((EntityControllable) playerEntity).getBlockLookingAt(true);
			if(loc != null) {
				bx = (int)loc.x();
				by = (int)loc.y();
				bz = (int)loc.z();
			}
		}
		
		int data = world.getVoxelData(bx, by, bz);
		int id = VoxelFormat.id(data);
		int meta = VoxelFormat.meta(data);
		int bl = VoxelFormat.blocklight(data);
		int sl = VoxelFormat.sunlight(data);
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx, bz);
		
		//Obtain the angle the player is facing
		VoxelSides side = VoxelSides.TOP;
		float angleX = -1;
		if (playerEntity != null && playerEntity instanceof EntityLiving)
			angleX = Math.round(((EntityLiving) playerEntity).getEntityRotationComponent().getHorizontalRotation());
		
		double dx = Math.sin(angleX / 360 * 2.0 * Math.PI);
		double dz = Math.cos(angleX / 360 * 2.0 * Math.PI);

		if (Math.abs(dx) > Math.abs(dz)) {
			if (dx > 0)
				side = VoxelSides.RIGHT;
			else
				side = VoxelSides.LEFT;
		}
		else {
			if (dz > 0)
				side = VoxelSides.FRONT;
			else
				side = VoxelSides.BACK;
		}

		//Count all the entities
		int ec = 0;
		IterableIterator<Entity> i = world.getAllLoadedEntities();
		while (i.hasNext()) {
			i.next();
			ec++;
		}

		Chunk current = world.getChunk(cx, cy, cz);
		int x_top = renderingInterface.getWindow().getHeight() - 16;
		FontRenderer2.drawTextUsingSpecificFont(20,
				x_top - 1 * 16, 0, 16, GLCalls.getStatistics() + " Chunks in view : " + formatBigAssNumber("" + world.getWorldRenderer().getChunkMeshesRenderer().getChunksVisibleForPass(WorldRenderer.RenderingPass.NORMAL_OPAQUE)) + " Entities " + ec + " Particles :" + ((ClientParticlesRenderer) world.getParticlesManager()).count()
						+ " #FF0000Render FPS: " + Client.getInstance().getGameWindow().getFPS() + " avg: " + Math.floor(10000.0 / Client.getInstance().getGameWindow().getFPS()) / 10.0 + " #00FFFFSimulation FPS: " + world.getWorldRenderer().getWorld().getGameLogic().getSimulationFps(),
				BitmapFont.SMALLFONTS);

		//FontRenderer2.drawTextUsingSpecificFont(20, x_top - 2 * 16, 0, 16, "Frame timings : " + debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 3 * 16, 0, 16, "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used, chunks loaded in ram: " + world.getRegionsHolder().countChunksWithData() + "/"
				+ world.getRegionsHolder().countChunks() + " " + Math.floor(world.getRegionsHolder().countChunksWithData() * 4 * 32 * 32 * 32 / (1024L * 1024 / 100f)) / 100f + "Mb used by chunks"

		, BitmapFont.SMALLFONTS);

		
		long totalVram = (renderingInterface.getTotalVramUsage()) / 1024 / 1024;
		
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + totalVram + "Mb as " + Texture2DGL.getTotalNumberOfTextureObjects() + " textures using " + Texture2DGL.getTotalVramUsage() / 1024 / 1024 + "Mb + "
				+ VertexBufferGL.getTotalNumberOfVerticesObjects() + " Vertices objects using " + renderingInterface.getVertexDataVramUsage() / 1024 / 1024 + " Mb", BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 5 * 16, 0, 16, "Chunks to bake : " + world.getWorldRenderer().getChunkMeshesRenderer().getBaker() + " - " + world.ioHandler.toString(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 6 * 16, 0, 16,
				"Position : x:" + bx + " y:" + by + " z:" + bz + " dir: " + angleX + " side: " + side + " #FF0000Block looking at#FFFFFF : pos: "+bx + ": " + by + ": " + bz +" data: "+data+" id: "+id+" meta: "+meta+" bl:" + bl + " sl:" + sl + " csh:" + csh, BitmapFont.SMALLFONTS);

		if (current == null)
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk null", BitmapFont.SMALLFONTS);
		else if (current instanceof ChunkRenderable)
		{
			ChunkRenderDataHolder chunkRenderData = ((RenderableChunk) current).getChunkRenderData();
			if (chunkRenderData != null)
			{
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk : " + current + " - " + chunkRenderData.toString(), BitmapFont.SMALLFONTS);
			}
			else
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk : " + current + " - No rendering data", BitmapFont.SMALLFONTS);
		}

		if (playerEntity != null && playerEntity instanceof EntityLiving)
		{
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 8 * 16, 0, 16, "Current Region : " + playerEntity.getWorld().getRegionChunkCoordinates(cx, cy, cz), BitmapFont.SMALLFONTS);
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 9 * 16, 0, 16, "Controlled Entity : " + playerEntity, BitmapFont.SMALLFONTS);
		}
	}

	@SuppressWarnings("unused")
	private String getLoadedChunksVramFootprint()
	{
		int nbChunks = 0;
		long octelsTotal = 0;

		ChunksIterator i = world.getAllLoadedChunks();
		Chunk c;
		while (i.hasNext())
		{
			c = i.next();
			if (c == null)
				continue;
			if (c instanceof ChunkRenderable)
			{
				ChunkRenderDataHolder chunkRenderData = ((RenderableChunk) c).getChunkRenderData();
				if (chunkRenderData != null)
				{
					nbChunks++;
					//octelsTotal += chunkRenderData.getVramUsage();
				}
			}
		}
		return nbChunks + " chunks";//, storing " + octelsTotal / 1024 / 1024 + "Mb of vertex data.";
	}

	@SuppressWarnings("unused")
	private String getLoadedTerrainVramFootprint()
	{
		int nbChunks = world.getRegionsSummariesHolder().countSummaries();
		long octelsTotal = nbChunks * 256 * 256 * (1 + 1) * 4;

		return nbChunks + " regions, storing " + octelsTotal / 1024 / 1024 + "Mb of data";
	}

	public String formatBigAssNumber(String in)
	{
		String formatted = "";
		for (int i = 0; i < in.length(); i++)
		{
			if (i > 0 && i % 3 == 0)
				formatted = "." + formatted;
			formatted = in.charAt(in.length() - i - 1) + formatted;
		}
		return formatted;
	}
}