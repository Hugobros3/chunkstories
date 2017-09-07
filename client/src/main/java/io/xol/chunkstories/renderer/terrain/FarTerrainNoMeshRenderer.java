package io.xol.chunkstories.renderer.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.joml.Vector2d;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.Primitive;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.WorldRenderer.FarTerrainRenderer;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.PolygonFillMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.engine.base.MemFreeByteBuffer;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.TexturesHandler;

/** Idea: stop with the idea of building meshes on the CPU. Just use pre-computed grids or a geometry shader and a big array texture with all the summaries
 * to draw this shit insanely fast.
 */
public class FarTerrainNoMeshRenderer implements FarTerrainRenderer {

	final WorldRendererImplementation worldRenderer;
	
	public FarTerrainNoMeshRenderer(WorldRendererImplementation worldRenderer) {
		this.worldRenderer = worldRenderer;
		
		for(int i = 0; i < detailLevels.length; i++) {
			grids[i] = generateGrid(detailLevels[i]);
		}
	}
	
	@Override
	public void markFarTerrainMeshDirty() {
		
	}

	VertexBufferGL generateGrid(int gridSubdivisions) {
		VertexBufferGL grid = new VertexBufferGL();
		
		ByteBuffer bb = MemoryUtil.memAlloc(gridSubdivisions * gridSubdivisions * 2 * 3 * (3 * 4));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		MemFreeByteBuffer mbb = new MemFreeByteBuffer(bb);
		int s = 256 / gridSubdivisions;
		for(int a = 0; a < gridSubdivisions; a++)
			for(int b = 0; b < gridSubdivisions; b++) {
				int i = a * s;
				int j = b * s;
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + s);
				
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + s);
				
				bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + s);
			}
		bb.flip();
		grid.uploadData(mbb);
		
		return grid;
	}
	
	int detailLevels[] = {1, 2, 4, 8, 16, 32, 64, 128, 256};
	VertexBufferGL grids[] = new VertexBufferGL[detailLevels.length];
	//VertexBufferGL grid = null;
	VertexBufferGL gridAttributes = new VertexBufferGL();
	
	@Override
	public void renderTerrain(RenderingInterface renderer, ReadyVoxelMeshesMask mask) {
		
		ShaderInterface terrainShader = renderer.useShader("terrain");
		renderer.setBlendMode(BlendMode.DISABLED);
		renderer.getCamera().setupShader(terrainShader);
		worldRenderer.getSky().setupShader(terrainShader);

		terrainShader.setUniform3f("sunPos", worldRenderer.getSky().getSunPosition());
		terrainShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
		terrainShader.setUniform1f("shadowVisiblity", worldRenderer.getShadowRenderer().getShadowVisibility());
		worldRenderer.worldTextures.waterNormalTexture.setLinearFiltering(true);
		worldRenderer.worldTextures.waterNormalTexture.setMipMapping(true);

		renderer.bindCubemap("environmentCubemap", worldRenderer.renderBuffers.environmentMap);
		renderer.bindTexture2D("sunSetRiseTexture", worldRenderer.worldTextures.sunGlowTexture);
		renderer.bindTexture2D("skyTextureSunny", worldRenderer.worldTextures.skyTextureSunny);
		renderer.bindTexture2D("skyTextureRaining", worldRenderer.worldTextures.skyTextureRaining);
		renderer.bindTexture2D("blockLightmap", worldRenderer.worldTextures.lightmapTexture);
		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");

		renderer.bindTexture2D("lightColors", lightColors);
		renderer.bindTexture2D("normalTexture", worldRenderer.worldTextures.waterNormalTexture);
		worldRenderer.setupShadowColors(terrainShader);
		terrainShader.setUniform1f("time", worldRenderer.getSky().time);

		renderer.bindTexture2D("vegetationColorTexture", worldRenderer.getGrassTexture());
		terrainShader.setUniform1f("mapSize", worldRenderer.getWorld().getSizeInChunks() * 32);
		
		renderer.bindArrayTexture("heights", worldRenderer.getSummariesTexturesHolder().getHeightsArrayTexture());
		renderer.bindArrayTexture("topVoxels", worldRenderer.getSummariesTexturesHolder().getTopVoxelsArrayTexture());

		//TODO hidden inputs ?
		
		//if(renderer.getClient().getInputsManager().getInputByName("wireframeFarTerrain").isPressed() && RenderingConfig.isDebugAllowed)
			renderer.setPolygonFillMode(PolygonFillMode.WIREFRAME);

		if(!renderer.getClient().getInputsManager().getInputByName("hideFarTerrain").isPressed() && RenderingConfig.isDebugAllowed)
		{
			renderer.setCullingMode(CullingMode.DISABLED);
			renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
			
			Player player = Client.getInstance().getPlayer();
			
			Location playerPosition = player.getLocation();
			if(playerPosition == null)
				return; //We won't do shit with that going on
			
			Vector2d playerCenter = new Vector2d(playerPosition.x, playerPosition.z);
			
			World world = playerPosition.getWorld();
			
			int chunkX = (int) Math.floor(playerPosition.x / 32.0);
			int chunkZ = (int) Math.floor(playerPosition.z / 32.0);
			
			int regionX = chunkX / 8;
			int regionZ = chunkZ / 8;
			
			int[] lodInstanceCount = new int[detailLevels.length];
			ByteBuffer lodByteBuffer[] = new ByteBuffer[detailLevels.length];
			
			//ByteBuffer summariesAttributes = MemoryUtil.memAlloc(9 * 9 * (4 + 2 * 4));
			//MemFreeByteBuffer auto_free_summariesAttributes = new MemFreeByteBuffer(summariesAttributes);
			//int count = 0;

			Vector2d center = new Vector2d();
			for(int i = -4; i <= 4; i++)
				for(int j = -4; j <= 4; j++) {
					int regionI = regionX + i;
					int regionJ = regionZ + j;
					
					center.set(regionI * 256 + 128, regionJ * 256 + 128);
					
					int lod = detailLevels.length - (int) (Math.pow(Math.min(1024, center.distance(playerCenter)) / 1024, 0.5) * detailLevels.length);
					//System.out.println((Math.min(512, center.distance(playerCenter)) / 512f * detailLevels.length));
					//System.out.println(center.distance(playerCenter));
					if(lod <= 1) lod = 1;
					if(lod >= detailLevels.length)
						lod = detailLevels.length - 1;
					//System.out.println("lod:"+lod);
			
					//lod = Math.random() > 0.5 ? 1 : 2;
					ByteBuffer summariesAttributes = lodByteBuffer[lod];
					
					int index = worldRenderer.getSummariesTexturesHolder().getSummaryIndex(regionI, regionJ);
					if(index == -1)
						continue;
					
					if(summariesAttributes == null) {
						summariesAttributes = MemoryUtil.memAlloc(9 * 9 * (4 + 2 * 4));
						lodByteBuffer[lod] = summariesAttributes;
					}
					
					summariesAttributes.putFloat(regionI * 256);
					summariesAttributes.putFloat(regionJ * 256);
					summariesAttributes.putInt(index);
					
					lodInstanceCount[lod]++;
				}
			
			//DC = # of lods
			for(int lod = 0; lod < detailLevels.length; lod++) {

				//Check if anything was supposed to be drew at this lod
				ByteBuffer summariesAttributes = lodByteBuffer[lod];
				if(summariesAttributes == null)
					continue;
				
				//Flip buffer, box it for autodeletion, upload it
				summariesAttributes.flip();
				MemFreeByteBuffer auto_free_summariesAttributes = new MemFreeByteBuffer(summariesAttributes);
				gridAttributes.uploadData(auto_free_summariesAttributes);
				
				renderer.bindAttribute("vertexIn", grids[lod].asAttributeSource(VertexFormat.FLOAT, 3, 0, 0L));
				renderer.bindAttribute("displacementIn", gridAttributes.asAttributeSource(VertexFormat.FLOAT, 2, (4 + 2 * 4), 0L, 1));
				renderer.bindAttribute("indexIn", gridAttributes.asIntegerAttributeSource(VertexFormat.INTEGER, 1, (4 + 2 * 4), 8L, 1));
				
				renderer.draw(Primitive.TRIANGLE, 0, detailLevels[lod]*detailLevels[lod]*2*3, lodInstanceCount[lod]);
				renderer.flush();
			}
		}
		
		renderer.flush();

		renderer.setPolygonFillMode(PolygonFillMode.FILL);
	}
	
	@Override
	public void destroy() {
		for(int i = 0; i < detailLevels.length; i++) {
			grids[i].destroy();
		}
		gridAttributes.destroy();
	}
}
