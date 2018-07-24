//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.debug;

import org.joml.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitVoxelSelection;
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.opengl.GLCalls;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DGL;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.chunk.ClientChunk;

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
		
		int lx = bx, ly = by, lz = bz;
		
		//If the player can look use that
		Location blockLookingAt = playerEntity != null ? playerEntity.traits.tryWith(TraitVoxelSelection.class, tvc ->  tvc.getBlockLookingAt(true, false) ) : null;
		if(blockLookingAt != null) {
			lx = (int)blockLookingAt.x();
			ly = (int)blockLookingAt.y();
			lz = (int)blockLookingAt.z();
		}
		
		int raw_data = world.peekRaw(lx, ly, lz);
		CellData cell = world.peekSafely(lx, ly, lz);
		
		//System.out.println(VoxelFormat.id(raw_data));
		
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx, bz);
		
		//Obtain the angle the player is facing
		VoxelSide side = VoxelSide.TOP;
		TraitRotation er = playerEntity != null ? playerEntity.traits.get(TraitRotation.class) : null;
		float angleX = er != null ? Math.round(er.getHorizontalRotation()) : 0;
		
		double dx = Math.sin(angleX / 360 * 2.0 * Math.PI);
		double dz = Math.cos(angleX / 360 * 2.0 * Math.PI);

		if (Math.abs(dx) > Math.abs(dz)) {
			if (dx > 0)
				side = VoxelSide.RIGHT;
			else
				side = VoxelSide.LEFT;
		}
		else {
			if (dz > 0)
				side = VoxelSide.FRONT;
			else
				side = VoxelSide.BACK;
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
		
		Font font = null;
		
		font = renderingInterface.getFontRenderer().getFont("LiberationSans-Regular", 12);
		
		if(font == null)
			font = renderingInterface.getFontRenderer().getFont("LiberationSans-Regular", 12);
		
		int lineHeight = font.getLineHeight();
		
		int posx, posy;
		String text;
		
		posx = 8;
		posy = x_top - posx;
		text = GLCalls.getStatistics() + " Chunks in view : " + world.getWorldRenderer().getChunksRenderer().getChunksVisible() + " Entities " + ec + " Particles :" + ((ClientParticlesRenderer) world.getParticlesManager()).count()
				+ " #FF0000Render FPS: " + Client.getInstance().getGameWindow().getFPS() + " avg: " + Math.floor(10000.0 / Client.getInstance().getGameWindow().getFPS()) / 10.0 + " #00FFFFSimulation FPS: " + world.getWorldRenderer().getWorld().getGameLogic().getSimulationFps();
		
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));

		posy -= lineHeight;
		text = "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " MB used, chunks loaded in ram: " + world.getRegionsHolder().countChunksWithData() + "/"
				+ world.getRegionsHolder().countChunks() + " " + Math.floor(world.getRegionsHolder().countChunksWithData() * 4 * 32 * 32 * 32 / (1024L * 1024 / 100f)) / 100f + "MB used by chunks";

		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		
		long totalVram = (renderingInterface.getTotalVramUsage()) / 1024 / 1024;
		
		posy -= lineHeight;
		text = "VRAM usage : " + totalVram + "MB as " + Texture2DGL.getTotalNumberOfTextureObjects() + " textures using " + Texture2DGL.getTotalVramUsage() / 1024 / 1024 + "MB + "
				+ VertexBufferGL.getTotalNumberOfVerticesObjects() + " vbos using " + renderingInterface.getVertexDataVramUsage() / 1024 / 1024 + " MB";
				
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		
		posy -= lineHeight;
		text = "Worker threads: " + world.getGameContext().tasks() + " - " + world.ioHandler.toString();
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		
		posy -= lineHeight;
		text = "Position : x:" + bx + " y:" + by + " z:" + bz + " dir: " + angleX + " side: " + side + " #FF0000Block looking at#FFFFFF : pos: "+lx + ": " + ly + ": " + lz +
				" data: "+raw_data+" voxel_type: "+cell.getVoxel().getName() + " sl:" + cell.getSunlight()+" bl: "+cell.getBlocklight()+" meta:" + cell.getMetaData() + " csh:" + csh;
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));

		posy -= lineHeight;
		text = "Current Summary : " + world.getRegionsSummariesHolder().getHeightmapChunkCoordinates(cx, cz);
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		
		posy -= lineHeight;
		text = "Current Region : " + world.getRegionChunkCoordinates(cx, cy, cz);
		renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		
		if (current == null) {
			
			posy -= lineHeight;
			text = "Current chunk null";
			renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
		}
		else if (current instanceof ChunkRenderable)
		{
			ChunkRenderDataHolder chunkRenderData = ((ClientChunk) current).getChunkRenderData();
			if (chunkRenderData != null)
			{
				posy -= lineHeight;
				text = "Current Chunk : " + current + " - " + chunkRenderData.toString();
				renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
			}
			else {

				posy -= lineHeight;
				text = "Current Chunk : " + current + " - No rendering data";
				renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
			}
		}

		if (playerEntity != null && playerEntity instanceof Entity)
		{
			posy -= lineHeight;
			text = "Controlled Entity : " + playerEntity;
			renderingInterface.getFontRenderer().drawStringWithShadow(font, posx, posy, text, 1, 1, new Vector4f(1));
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
				ChunkRenderDataHolder chunkRenderData = ((ClientChunk) c).getChunkRenderData();
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
