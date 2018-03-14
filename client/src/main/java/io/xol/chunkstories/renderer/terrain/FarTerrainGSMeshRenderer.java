//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.joml.Vector2d;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.StateMachine.PolygonFillMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer.FarTerrainRenderer;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.base.MemFreeByteBuffer;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.TexturesHandler;

/** Idea: stop with the idea of building meshes on the CPU. Just use pre-computed grids or a geometry shader and a big array texture with all the summaries
 * to draw this shit insanely fast.
 */
public class FarTerrainGSMeshRenderer implements FarTerrainRenderer {

	final WorldClient world;
	final WorldRendererImplementation worldRenderer;
	
	private VoxelTexturesColours colours;
	
	public FarTerrainGSMeshRenderer(WorldRendererImplementation worldRenderer) {
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
		
		for(int i = 0; i < detailLevels.length; i++) {
			grids[i] = generateGrid(detailLevels[i]);
		}
		
		colours = new VoxelTexturesColours(worldRenderer.getWorld());
	}
	
	@Override
	public void markFarTerrainMeshDirty() {
		
	}

	VertexBufferGL generateGrid(int gridSubdivisions) {
		VertexBufferGL grid = new VertexBufferGL();
		
		ByteBuffer bb = MemoryUtil.memAlloc((gridSubdivisions + 1) * (gridSubdivisions + 1) * 1 * (3 * 4));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		MemFreeByteBuffer mbb = new MemFreeByteBuffer(bb);
		int s = 32 / gridSubdivisions;
		for(int a = 0; a < gridSubdivisions + 1; a++)
			for(int b = 0; b < gridSubdivisions + 1; b++) {
				int i = a * s;
				int j = b * s;
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				/*bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + s);
				
				bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + 0);
				
				bb.putFloat(i + 0);
				bb.putFloat(0);
				bb.putFloat(j + s);
				
				bb.putFloat(i + s);
				bb.putFloat(0);
				bb.putFloat(j + s);*/
			}
		bb.flip();
		grid.uploadData(mbb);
		
		return grid;
	}
	
	int detailLevels[] = {1, 2, 4, 8, 16, 32};
	VertexBufferGL grids[] = new VertexBufferGL[detailLevels.length];
	//VertexBufferGL grid = null;
	VertexBufferGL gridAttributes = new VertexBufferGL();
	
	@Override
	public void renderTerrain(RenderingInterface renderer, ReadyVoxelMeshesMask mask) {
		
		Shader terrainShader = renderer.useShader("terrain_blocky");
		renderer.setBlendMode(BlendMode.DISABLED);
		renderer.getCamera().setupShader(terrainShader);
		worldRenderer.getSkyRenderer().setupShader(terrainShader);

		terrainShader.setUniform1f("viewDistance", world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance"));

		Texture2D waterTexture = renderer.textures().getTexture("./textures/water/shallow.png");
		waterTexture.setLinearFiltering(true);
		waterTexture.setMipMapping(true);

		//renderer.bindCubemap("environmentCubemap", worldRenderer.renderBuffers.rbEnvironmentMap);
		
		renderer.bindTexture2D("blockLightmap", TexturesHandler.getTexture("./textures/environement/light.png"));
		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");

		renderer.bindTexture2D("lightColors", lightColors);
		renderer.bindTexture2D("normalTexture", waterTexture);
		
		world.getGenerator().getEnvironment().setupShadowColors(renderer, terrainShader);

		renderer.bindTexture2D("vegetationColorTexture", world.getGenerator().getEnvironment().getGrassTexture(renderer));
		terrainShader.setUniform1f("mapSize", worldRenderer.getWorld().getSizeInChunks() * 32);
		
		renderer.bindArrayTexture("heights", worldRenderer.getSummariesTexturesHolder().getHeightsArrayTexture());
		renderer.bindArrayTexture("topVoxels", worldRenderer.getSummariesTexturesHolder().getTopVoxelsArrayTexture());
		
		renderer.bindTexture1D("blocksTexturesSummary", colours.get());

		//TODO hidden inputs ?
		
		if(renderer.getClient().getInputsManager().getInputByName("wireframeFarTerrain").isPressed() && RenderingConfig.isDebugAllowed)
			renderer.setPolygonFillMode(PolygonFillMode.WIREFRAME);

		if(!renderer.getClient().getInputsManager().getInputByName("hideFarTerrain").isPressed() && RenderingConfig.isDebugAllowed)
		{
			renderer.setCullingMode(CullingMode.DISABLED);
			//renderer.setCullingMode(CullingMode.COUNTERCLOCKWISE);
			renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
			
			Player player = Client.getInstance().getPlayer();
			
			Location playerPosition = player.getLocation();
			if(playerPosition == null)
				return; //We won't do shit with that going on
			
			Vector2d playerCenter = new Vector2d(playerPosition.x, playerPosition.z);
			
			int chunkX = (int) Math.floor(playerPosition.x / 32.0);
			int chunkZ = (int) Math.floor(playerPosition.z / 32.0);
			
			int regionX = chunkX / 8;
			int regionZ = chunkZ / 8;
			
			int[] lodInstanceCount = new int[detailLevels.length];
			ByteBuffer lodByteBuffer[] = new ByteBuffer[detailLevels.length];
			
			//ByteBuffer summariesAttributes = MemoryUtil.memAlloc(9 * 9 * (4 + 2 * 4));
			//MemFreeByteBuffer auto_free_summariesAttributes = new MemFreeByteBuffer(summariesAttributes);
			//int count = 0;

			double lodBias = -0.2;
			double lodExponent = 0.35;
			
			if(!world.getClient().getConfiguration().getBooleanOption("client.rendering.hqTerrain")) {
				lodBias = 0.3;
				lodExponent = 0.35;
			} else {
				lodBias = 0.0;
				lodExponent = 0.45;
			}
			
			if(mask == null) {
				lodExponent = 1.0;
				lodBias = 0.6;
			}
			
			Vector2d center = new Vector2d();
			for(int i = -4; i <= 4; i++)
				for(int j = -4; j <= 4; j++) {
					int regionI = regionX + i;
					int regionJ = regionZ + j;

					int index = worldRenderer.getSummariesTexturesHolder().getSummaryIndex(regionI, regionJ);
					if(index == -1)
						continue;
					
					//For the extra row of triangles to mask the seams
					int index10 = worldRenderer.getSummariesTexturesHolder().getSummaryIndex(regionI + 1, regionJ);
					int index01 = worldRenderer.getSummariesTexturesHolder().getSummaryIndex(regionI, regionJ + 1);
					int index11 = worldRenderer.getSummariesTexturesHolder().getSummaryIndex(regionI + 1, regionJ + 1);
					if(i < 4 && index10 == -1)
						continue;
					if(j < 4 && index01 == -1)
						continue;
					if(i < 4 && j < 4 && index11 == -1)
						continue;
					
					RegionSummaryImplementation sum = (RegionSummaryImplementation) world.getRegionsSummariesHolder().getRegionSummary(regionI, regionJ);
					
					//Early out
					if(sum == null || !sum.isLoaded())
						continue;
					
					if (!renderer.getCamera().isBoxInFrustrum(new CollisionBox(regionI * 256, 0, regionJ * 256, 256, 1024 /*+ sum.getHeightMipmapped(0, 0, 9)*/, 256)))
						continue;
					
					for(int l = 0; l < 8; l++)
						for(int m = 0; m < 8; m++) {
						
							if(mask != null)
							{
								if(mask.shouldMaskSlab(regionI * 8 + l, regionJ * 8 + m, sum.min[l][m], sum.max[l][m]))
									continue;
							}
							
							center.set(regionI * 256 + l * 32 + 16, regionJ * 256 + m * 32 + 16);
						
							int lod = detailLevels.length - (int) ((lodBias + Math.pow(Math.min(1024, center.distance(playerCenter)) / 1024, lodExponent)) * detailLevels.length);
							//System.out.println((Math.min(512, center.distance(playerCenter)) / 512f * detailLevels.length));
							//System.out.println(center.distance(playerCenter));
							if(lod <= 0) lod = 0;
							if(lod >= detailLevels.length)
								lod = detailLevels.length - 1;
							
							//lod = 2;
							
							//lod = 2;
							//System.out.println("lod:"+lod);
					
							//lod = Math.random() > 0.5 ? 1 : 2;
							ByteBuffer summariesAttributes = lodByteBuffer[lod];
							
							if(summariesAttributes == null) {
								summariesAttributes = MemoryUtil.memAlloc(9 * 9 * 8 * 8 * (4 + 2 * 4));
								lodByteBuffer[lod] = summariesAttributes;
							}
							
							summariesAttributes.putFloat(regionI * 256 + l * 32);
							summariesAttributes.putFloat(regionJ * 256 + m * 32);
							summariesAttributes.put((byte)index);
							summariesAttributes.put((byte)index10);
							summariesAttributes.put((byte)index01);
							summariesAttributes.put((byte)index11);
							
							lodInstanceCount[lod]++;
							
							//Always add both so lod 1 is drew under
							/*if(lod != 0) {
								lod = 0;
								
								summariesAttributes = lodByteBuffer[lod];
								
								if(summariesAttributes == null) {
									summariesAttributes = MemoryUtil.memAlloc(9 * 9 * 8 * 8 * (4 + 2 * 4));
									lodByteBuffer[lod] = summariesAttributes;
								}
								
								summariesAttributes.putFloat(regionI * 256 + l * 32);
								summariesAttributes.putFloat(regionJ * 256 + m * 32);
								summariesAttributes.put((byte)index);
								summariesAttributes.put((byte)index10);
								summariesAttributes.put((byte)index01);
								summariesAttributes.put((byte)index11);
								lodInstanceCount[lod]++;
							}*/
					}
				}
			
			//DC = # of lods
			//for(int lod = 0; lod < detailLevels.length; lod++) {
			for(int lod = detailLevels.length - 1; lod >= 0; lod--) {

				//Check if anything was supposed to be drew at this lod
				ByteBuffer summariesAttributes = lodByteBuffer[lod];
				if(summariesAttributes == null)
					continue;
				
				if(lod < 0) {
					MemoryUtil.memFree(summariesAttributes);
					continue;
				}
				
				//Flip buffer, box it for autodeletion, upload it
				summariesAttributes.flip();
				MemFreeByteBuffer auto_free_summariesAttributes = new MemFreeByteBuffer(summariesAttributes);
				gridAttributes.uploadData(auto_free_summariesAttributes);
				
				terrainShader.setUniform1i("lodLevel", lod);
				terrainShader.setUniform1f("textureLodLevel", lod-5);
				terrainShader.setUniform1i("maskPresence", mask == null ? 0 : 1);
				
				renderer.bindAttribute("vertexIn", grids[lod].asAttributeSource(VertexFormat.FLOAT, 3, 0, 0L));
				renderer.bindAttribute("displacementIn", gridAttributes.asAttributeSource(VertexFormat.FLOAT, 2, (4 + 2 * 4), 0L, 1));
				renderer.bindAttribute("indexIn", gridAttributes.asIntegerAttributeSource(VertexFormat.BYTE, 4, (4 + 2 * 4), 8L, 1));
				
				renderer.draw(Primitive.POINT, 0, (detailLevels[lod]+1)*(detailLevels[lod]+1)*1, lodInstanceCount[lod]);
			}
		}
		

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
