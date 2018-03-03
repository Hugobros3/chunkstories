//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.DEPTH_RENDERBUFFER;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.DEPTH_SHADOWMAP;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RED_8;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RED_8UI;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGBA_8BPP;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGB_8;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGB_HDR;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RG_8;

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture3D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.renderer.chunks.ChunkMeshesRenderer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRendererImplementation;
import io.xol.chunkstories.renderer.lights.ComputedShadowMap;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.renderer.sky.DefaultSkyRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainGSMeshRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainMeshRenderer;
//import io.xol.chunkstories.renderer.terrain.FarTerrainNoMeshRenderer;
import io.xol.chunkstories.renderer.terrain.SummariesArrayTexture;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.fbo.FrameBufferObjectGL;
import io.xol.engine.graphics.textures.CubemapGL;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;
import io.xol.engine.graphics.textures.Texture3DGL;
import io.xol.engine.graphics.textures.TexturesHandler;

/** A tragically huge behemoth, held responsible of actually displaying all that mess */
public class WorldRendererImplementation implements WorldRenderer
{
	protected final Logger logger = LoggerFactory.getLogger("renderer.world");
	
	private final GameWindow gameWindow;
	private final WorldClientCommon world;
	
	private ChunkMeshesRenderer chunksRenderer;
	
	EntitiesRenderer entitiesRenderer;
	DefaultSkyRenderer skyRenderer;
	DecalsRendererImplementation decalsRenderer;
	ClientParticlesRenderer particlesRenderer;
	FarTerrainRenderer farTerrainRenderer;
	SummariesTexturesHolder summariesTexturesHolder;
	WorldEffectsRenderer weatherEffectsRenderer;
	ShadowMapRenderer shadower;
	BloomRenderer bloomRenderer;
	ReflectionsRenderer reflectionsRenderer;
	CubemapRenderer cubemapRenderer;
	
	float animationTimer = 0.0f;
	float apertureModifier = 1.0f;
	
	public RenderBuffers renderBuffers;
	public WorldTextures worldTextures;
	
	RenderingPass currentPass = null;

	private ComputedShadowMap sun_shadowMap;

	public WorldRendererImplementation(WorldClientCommon world, ClientInterface client)
	{
		this.world = world;
		this.gameWindow = client.getGameWindow();
		
		//this.logger = client.logger();
		
		//Creates all these fancy render buffers
		renderBuffers = new RenderBuffers();
		
		//Create a holder for the general world rendering textures
		worldTextures = new WorldTextures();

		//Creates subsystems
		this.chunksRenderer = new ChunkMeshesRenderer(this);
		
		this.entitiesRenderer = new EntitiesRenderer(world);
		this.particlesRenderer = new ClientParticlesRenderer(world);
		this.farTerrainRenderer = new FarTerrainGSMeshRenderer(this);
		this.summariesTexturesHolder = new SummariesArrayTexture(client, world);
		this.weatherEffectsRenderer = new DefaultWeatherEffectsRenderer(world, this);
		this.skyRenderer = new DefaultSkyRenderer(world);
		this.decalsRenderer = new DecalsRendererImplementation(this);
		this.shadower = new ShadowMapRenderer(this);
		this.bloomRenderer = new BloomRenderer(this);
		this.reflectionsRenderer = new ReflectionsRenderer(this);
		this.cubemapRenderer = new CubemapRenderer(this);
	}

	@Override
	public WorldClient getWorld()
	{
		return world;
	}

	@Override
	public DefaultSkyRenderer getSky()
	{
		return skyRenderer;
	}

	@Override
	public void renderWorld(RenderingInterface renderingInterface)
	{
		((SummariesArrayTexture) summariesTexturesHolder).update();
		
		if(RenderingConfig.doDynamicCubemaps)
			cubemapRenderer.renderWorldCubemap(renderingInterface, renderBuffers.rbEnvironmentMap, 128, true);
		
		//Step one, set the camera to the proper spot
		CameraInterface mainCamera = renderingInterface.getCamera();
		EntityControllable entity = Client.getInstance().getPlayer().getControlledEntity();
		
		if (entity != null)
			entity.setupCamera(renderingInterface);
		
		animationTimer = ((float)(System.currentTimeMillis() & 0x7FFF)) / 100.0f;
		
		//TODO remove entirely
		FakeImmediateModeDebugRenderer.setCamera(mainCamera);
		
		// Prepare matrices
		mainCamera.setupUsingScreenSize(gameWindow.getWidth(), gameWindow.getHeight());
		
		this.renderWorldInternal(renderingInterface);
	}
		
	protected void renderWorldInternal(RenderingInterface renderingInterface)
	{
		//Step one, set the camera to the proper spot
		CameraInterface mainCamera = renderingInterface.getCamera();

		//Update PVS
		chunksRenderer.updatePVSSet(mainCamera);
		
		//Generate a shadowmap for the sun if such an option is enabled
		currentPass = RenderingPass.SHADOW;
		
		sun_shadowMap = RenderingConfig.doShadows ? shadower.generateSunShadowMap(renderingInterface, skyRenderer) : null;
		currentPass = RenderingPass.INTERNAL;
		
		// Clear G-Buffers and bind shaded HDR rendertarget
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboGBuffers);
		renderingInterface.getRenderTargetManager().clearBoundRenderTargetAll();
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);
		
		// Update sky and render it
		skyRenderer.time = (world.getTime() % 10000) / 10000f;
		skyRenderer.render(renderingInterface);

		// Fill up the G-Buffers
		currentPass = RenderingPass.NORMAL_OPAQUE;
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboGBuffers);
		renderBuffers.fboGBuffers.setEnabledRenderTargets();
		
		gbuffers_opaque_chunk_meshes(renderingInterface);
		gbuffers_opaque_entities(renderingInterface);

		particlesRenderer.render(renderingInterface, true);
		//renderingInterface.flush();
		
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboOnlyAlbedoBuffer);
		renderingInterface.setBlendMode(BlendMode.MIX);
		decalsRenderer.renderDecals(renderingInterface);
		
		//System.out.println("h");
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboGBuffers);
		renderBuffers.fboGBuffers.setEnabledRenderTargets();
		gbuffers_water_chunk_meshes(renderingInterface);
		
		currentPass = RenderingPass.INTERNAL;
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);
		renderBuffers.fboShadedBuffer.setEnabledRenderTargets();
		
		//Render the deffered light
		renderShadedBlocks(renderingInterface, sun_shadowMap);
		renderLightsDeffered(renderingInterface);
		
		//Add forward rendered stuff
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBufferWithSpecular);
		farTerrainRenderer.renderTerrain(renderingInterface, chunksRenderer.getRenderedChunksMask(mainCamera));

		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);
		particlesRenderer.render(renderingInterface, false);
		
		//Render SSR if enabled
		//if(RenderingConfig.doRealtimeReflections)
		reflectionsRenderer.renderReflections(renderingInterface);
		
		weatherEffectsRenderer.renderEffects(renderingInterface);
	}

	private void gbuffers_opaque_chunk_meshes(RenderingInterface renderingInterface)
	{
		// Set fixed-function parameters
		renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		renderingInterface.setBlendMode(BlendMode.DISABLED);
		renderingInterface.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		
		ShaderInterface opaqueBlocksShader = renderingInterface.useShader("blocks_opaque");
		
		renderingInterface.bindAlbedoTexture(worldTextures.blocksAlbedoTexture);
		renderingInterface.bindNormalTexture(worldTextures.blocksNormalTexture);
		renderingInterface.bindMaterialTexture(worldTextures.blocksMaterialTexture);

		renderingInterface.bindTexture2D("lightColors", worldTextures.lightmapTexture);
		renderingInterface.bindTexture2D("vegetationColorTexture", world.getGenerator().getEnvironment().getGrassTexture(renderingInterface));
		//renderingInterface.bindTexture2D("vegetationColorTexture", getGrassTexture());

		//Set texturing arguments
		worldTextures.blocksAlbedoTexture.setTextureWrapping(false);
		worldTextures.blocksAlbedoTexture.setLinearFiltering(false);
		worldTextures.blocksAlbedoTexture.setMipMapping(false);
		worldTextures.blocksAlbedoTexture.setMipmapLevelsRange(0, 4);

		worldTextures.blocksNormalTexture.setTextureWrapping(false);
		worldTextures.blocksNormalTexture.setLinearFiltering(false);
		worldTextures.blocksNormalTexture.setMipMapping(false);
		worldTextures.blocksNormalTexture.setMipmapLevelsRange(0, 4);

		worldTextures.blocksMaterialTexture.setTextureWrapping(false);
		worldTextures.blocksMaterialTexture.setLinearFiltering(false);
		worldTextures.blocksMaterialTexture.setMipMapping(false);
		worldTextures.blocksMaterialTexture.setMipmapLevelsRange(0, 4);

		//World stuff
		opaqueBlocksShader.setUniform1f("mapSize", world.getSizeInChunks() * 32);
		opaqueBlocksShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());
		opaqueBlocksShader.setUniform1f("overcastFactor", world.getWeather());
		opaqueBlocksShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderingInterface.getCamera().getCameraPosition()));
		opaqueBlocksShader.setUniform1f("time", animationTimer);

		opaqueBlocksShader.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		renderingInterface.getCamera().setupShader(opaqueBlocksShader);

		renderingInterface.setObjectMatrix(new Matrix4f());
		
		chunksRenderer.renderChunks(renderingInterface, WorldRenderer.RenderingPass.NORMAL_OPAQUE);
	}
	
	private void gbuffers_water_chunk_meshes(RenderingInterface renderingInterface)
	{
		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.setCullingMode(CullingMode.DISABLED);

		// We do water in two passes : one for computing the refracted color and putting it in shaded buffer, and another one
		// to read it back and blend it
		for (int pass = 1; pass < 3; pass++)
		{
			ShaderInterface liquidBlocksShader = renderingInterface.useShader("blocks_liquid_pass" + pass);

			liquidBlocksShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);

			//liquidBlocksShader.setUniform1f("yAngle", (float) (renderingContext.getCamera().rotationY * Math.PI / 180f));
			liquidBlocksShader.setUniform1f("shadowVisiblity", this.shadower.getShadowVisibility());

			renderingInterface.bindTexture2D("normalTextureDeep", TexturesHandler.getTexture("./textures/water/deep.png"));
			renderingInterface.bindTexture2D("normalTextureShallow", worldTextures.waterNormalTexture);

			renderingInterface.bindTexture2D("lightColors", worldTextures.lightmapTexture);
			renderingInterface.bindAlbedoTexture(worldTextures.blocksAlbedoTexture);
			liquidBlocksShader.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
			skyRenderer.setupShader(liquidBlocksShader);
			liquidBlocksShader.setUniform1f("time", animationTimer);

			renderingInterface.getCamera().setupShader(liquidBlocksShader);

			//Underwater flag
			
			Voxel vox = world.peekSafely(renderingInterface.getCamera().getCameraPosition()).getVoxel();
			//Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData((int) (double) renderingInterface.getCamera().getCameraPosition().x(), (int) (double) renderingInterface.getCamera().getCameraPosition().y(), (int) (double) renderingInterface.getCamera().getCameraPosition().z()));
			liquidBlocksShader.setUniform1f("underwater", vox.getDefinition().isLiquid() ? 1 : 0);

			if (pass == 1)
			{
				renderingInterface.getRenderTargetManager().setConfiguration(this.renderBuffers.fboShadedBuffer);
				//fboShadedBuffer.bind();
				this.renderBuffers.fboShadedBuffer.setEnabledRenderTargets(true);
				renderingInterface.bindTexture2D("readbackAlbedoBufferTemp", this.renderBuffers.rbAlbedo);
				renderingInterface.bindTexture2D("readbackVoxelLightBufferTemp", this.renderBuffers.rbVoxelLight);
				renderingInterface.bindTexture2D("readbackSpecularityBufferTemp", this.renderBuffers.rbSpecularity);
				renderingInterface.bindTexture2D("readbackDepthBufferTemp", this.renderBuffers.rbZBuffer);

				renderingInterface.getRenderTargetManager().setDepthMask(false);
				//glDepthMask(false);
			}
			else if (pass == 2)
			{
				renderingInterface.getRenderTargetManager().setConfiguration(this.renderBuffers.fboGBuffers);
				//fboGBuffers.bind();
				this.renderBuffers.fboGBuffers.setEnabledRenderTargets();
				renderingInterface.setBlendMode(BlendMode.DISABLED);
				renderingInterface.bindTexture2D("readbackShadedBufferTemp", this.renderBuffers.rbShaded);
				renderingInterface.bindTexture2D("readbackDepthBufferTemp", this.renderBuffers.rbZBuffer);

				renderingInterface.getRenderTargetManager().setDepthMask(true);
				//glDepthMask(true);
			}

			renderingInterface.setObjectMatrix(new Matrix4f());
			chunksRenderer.renderChunks(renderingInterface, WorldRenderer.RenderingPass.NORMAL_LIQUIDS_PASS_1);
		}
		// Set fixed-function parameters
		/*renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		
		ShaderInterface waterShader = renderingInterface.useShader("water");
		
		renderingInterface.bindAlbedoTexture(worldTextures.blocksAlbedoTexture);
		renderingInterface.bindNormalTexture(worldTextures.blocksNormalTexture);
		renderingInterface.bindMaterialTexture(worldTextures.blocksMaterialTexture);

		renderingInterface.bindTexture2D("lightColors", worldTextures.lightmapTexture);
		renderingInterface.bindTexture2D("vegetationColorTexture", getGrassTexture());

		//World stuff
		waterShader.setUniform1f("mapSize", world.getSizeInChunks() * 32);
		waterShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());
		waterShader.setUniform1f("overcastFactor", world.getWeather());
		waterShader.setUniform1f("wetness", getWorldWetness());
		waterShader.setUniform1f("time", animationTimer);

		waterShader.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		renderingInterface.getCamera().setupShader(waterShader);

		renderingInterface.setObjectMatrix(new Matrix4f());
		
		chunksRenderer.renderChunks(renderingInterface, WorldRenderer.RenderingPass.NORMAL_LIQUIDS_PASS_1);
		renderingInterface.setBlendMode(BlendMode.DISABLED);*/
	}

	private void gbuffers_opaque_entities(RenderingInterface renderingContext) {
		
		// Select shader
		ShaderInterface entitiesShader = renderingContext.useShader("entities");

		//entitiesShader.setUniformMatrix4f("localTansform", new Matrix4f());
		//entitiesShader.setUniformMatrix3f("localTransformNormal", new Matrix3f());

		entitiesShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
		entitiesShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());

		renderingContext.bindTexture2D("lightColors", worldTextures.lightmapTexture);
		entitiesShader.setUniform3f("blockColor", 1f, 1f, 1f);
		entitiesShader.setUniform1f("time", animationTimer);

		entitiesShader.setUniform1f("overcastFactor", world.getWeather());
		entitiesShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderingContext.getCamera().getCameraPosition()));

		renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
		renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);

		renderingContext.getCamera().setupShader(entitiesShader);

		chunksRenderer.renderChunksExtras(renderingContext, RenderingPass.NORMAL_OPAQUE);
		
		entitiesRenderer.renderEntities(renderingContext);
	}
	
	/**
	 * Uses G-Buffers data to spit out shaded solid blocks ( shadows etc )
	 * @param sun_shadowMap 
	 */
	public void renderShadedBlocks(RenderingInterface renderingContext, ComputedShadowMap sun_shadowMap)
	{
		ShaderInterface applyShadowsShader = renderingContext.useShader("shadows_apply");
		
		world.getGenerator().getEnvironment().setupShadowColors(renderingContext, applyShadowsShader);
		//setupShadowColors(applyShadowsShader);

		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderingContext.getCamera().getCameraPosition()));

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		renderingContext.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);

		float lightMultiplier = 1.0f;

		applyShadowsShader.setUniform1f("brightnessMultiplier", lightMultiplier);

		renderingContext.bindTexture2D("albedoBuffer", renderBuffers.rbAlbedo);
		renderingContext.bindTexture2D("depthBuffer", renderBuffers.rbZBuffer);
		renderingContext.bindTexture2D("normalBuffer", renderBuffers.rbNormal);
		//TODO materials
		//renderingContext.bindTexture2D("specularityBuffer", renderBuffers.rbSpecularity);
		renderingContext.bindTexture2D("voxelLightBuffer", renderBuffers.rbVoxelLight);
		
		renderingContext.bindTexture2D("shadowMap", renderBuffers.rbShadowMap);
		
		renderingContext.bindTexture2D("blockLightmap", worldTextures.lightmapTexture);

		renderingContext.bindTexture2D("sunSetRiseTexture", worldTextures.sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", worldTextures.skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", worldTextures.skyTextureRaining);

		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");
		renderingContext.bindTexture2D("lightColors", lightColors);

		//TODO if SSAO
		renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.rbSSAO);

		renderingContext.bindCubemap("environmentCubemap", renderBuffers.rbEnvironmentMap);

		applyShadowsShader.setUniform1f("dayTime", skyRenderer.time);

		applyShadowsShader.setUniform1f("shadowMapResolution", RenderingConfig.shadowMapResolutions);
		applyShadowsShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());
		
		if(sun_shadowMap != null)
		{
			applyShadowsShader.setUniformMatrix4f("shadowMatrix", sun_shadowMap.getShadowTransformationMatrix());
		}

		// Matrices for screen-space transformations
		renderingContext.getCamera().setupShader(applyShadowsShader);
		skyRenderer.setupShader(applyShadowsShader);

		renderingContext.getRenderTargetManager().setDepthMask(false);
		renderingContext.drawFSQuad();
		renderingContext.getRenderTargetManager().setDepthMask(true);

		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
	}
	
	private void renderLightsDeffered(RenderingInterface renderingInterface)
	{
		//Client.profiler.startSection("lights");

		//We work on the shaded buffer
		renderingInterface.getRenderTargetManager().setConfiguration(this.renderBuffers.fboShadedBuffer);
		
		// Deffered lightning
		// Disable depth read/write
		renderingInterface.setDepthTestMode(DepthTestMode.DISABLED);
		renderingInterface.getRenderTargetManager().setDepthMask(false);

		ShaderInterface lightShader = renderingInterface.useShader("light");

		//Required info
		renderingInterface.bindTexture2D("depthBuffer", this.renderBuffers.rbZBuffer);
		renderingInterface.bindTexture2D("diffuseBuffer", this.renderBuffers.rbAlbedo);
		renderingInterface.bindTexture2D("normalBuffer", this.renderBuffers.rbNormal);

		//Parameters
		lightShader.setUniform1f("powFactor", 5f);
		renderingInterface.getCamera().setupShader(lightShader);
		//Blend parameters

		renderingInterface.setDepthTestMode(DepthTestMode.DISABLED);
		renderingInterface.setBlendMode(BlendMode.ADD);

		renderingInterface.getLightsRenderer().renderPendingLights(renderingInterface);
		//Cleanup
		renderingInterface.getRenderTargetManager().setDepthMask(true);

		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
	}
	
	Texture3D test = null;
	
	int bx,by,bz;
	
	@Override
	public void blitFinalImage(RenderingInterface renderingContext, boolean hideGui)
	{
		if(test == null) {
			test = new Texture3DGL(TextureFormat.RGBA_8BPP, 32, 32, 32);
		}

		final int SIZE = 256;
		final int mod = SIZE / 32;

		int offCenter = SIZE / 2;
		
		int chunkX = (int) ((renderingContext.getCamera().getCameraPosition().x() - offCenter) / 32);
		int chunkY = (int) ((renderingContext.getCamera().getCameraPosition().y() - offCenter) / 32);
		int chunkZ = (int) ((renderingContext.getCamera().getCameraPosition().z() - offCenter) / 32);
		
		int offsetX = chunkX % mod;
		int offsetY = chunkY % mod;
		int offsetZ = chunkZ % mod;
		
		if(bx != chunkX || by != chunkY || bz != chunkZ) {
			
			bx = chunkX;
			by = chunkY;
			bz = chunkZ;
			ByteBuffer bb = MemoryUtil.memAlloc(4 * SIZE * SIZE * SIZE);
			
			byte[] empty = {0,0,0,0};
			byte[] notempty = {-1,-1,-1,-1};
			
			CubicChunk zChunk = null;

			Vector4f col = new Vector4f();
			for(int a = 0; a*32 < SIZE; a++)
				for(int b = 0; b*32 < SIZE; b++)
					for(int c = 0; c*32 < SIZE; c++) {
						
						zChunk = world.getChunk(chunkX + a, chunkY + b, chunkZ + c);
						
						if(zChunk != null) {
							for(int z = 0; z < 32; z++)
								for(int y = 0; y < 32; y++) {
	
									int dx = (0 + a) % mod;
									int dy = (0 + b) % mod;
									int dz = (0 + c) % mod;
									
									bb.position(4 * ((dz*32 + z) * SIZE * SIZE + (dy*32 + y) * SIZE + 0 + dx*32));
									
									for(int x = 0; x < 32; x++) {
										CellData cell = zChunk.peek(x, y, z);
										Voxel voxel = cell.getVoxel();//zChunk.peekSimple(x, y, z);
										
										if(voxel.isAir() || !voxel.getDefinition().isSolid() && !voxel.getDefinition().isLiquid()) {
											bb.put(empty);
										} else {
											col.set(voxel.getVoxelTexture(VoxelSides.TOP, cell).getColor());
											if(col.w() < 1.0) {
												col.mul(new Vector4f(0.1f, 0.3f, 0.1f, 1.0f));
											}
											
											bb.put((byte)(int)(col.x() * 255));
											bb.put((byte)(int)(col.y() * 255));
											bb.put((byte)(int)(col.z() * 255));
											bb.put((byte) -1);
										} 
									}
								}
						}
					}
			
			bb.position(0);
			bb.limit(bb.capacity());
			test.uploadTextureData(SIZE, SIZE, SIZE, bb);
			test.setTextureWrapping(true);
			MemoryUtil.memFree(bb);
		}
		
		//TODO mix in the reflections earlier ?
		Texture2D bloomRendered = RenderingConfig.doBloom ? bloomRenderer.renderBloom(renderingContext) : null;
		
		Layer layer = renderingContext.getWindow().getLayer().getRootLayer();
		float pauseFade = (layer instanceof Ingame) ? ((Ingame)layer).getPauseOverlayFade() : 0;
		
		// We render to the screen.
		renderingContext.getRenderTargetManager().setConfiguration(null);

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		ShaderInterface postProcess = renderingContext.useShader("postprocess");

		renderingContext.bindTexture3D("currentChunk", test);
		postProcess.setUniform1i("voxel_size", SIZE);
		postProcess.setUniform1f("voxel_sizef", 0.0f + SIZE);
		postProcess.setUniform3f("voxelOffset", offsetX * 32, offsetY * 32, offsetZ * 32);
		
		renderingContext.bindTexture2D("shadedBuffer", renderBuffers.rbShaded);
		renderingContext.bindTexture2D("albedoBuffer", renderBuffers.rbAlbedo);
		renderingContext.bindTexture2D("depthBuffer", renderBuffers.rbZBuffer);
		renderingContext.bindTexture2D("normalBuffer", renderBuffers.rbNormal);
		renderingContext.bindTexture2D("voxelLightBuffer", renderBuffers.rbVoxelLight);
		renderingContext.bindTexture2D("specularityBuffer", renderBuffers.rbSpecularity);
		renderingContext.bindTexture2D("materialBuffer", renderBuffers.rbMaterial);
		renderingContext.bindTexture2D("shadowMap", renderBuffers.rbShadowMap);
		
		renderingContext.bindTexture2D("reflectionsBuffer", renderBuffers.rbReflections);
		renderingContext.bindCubemap("environmentMap", renderBuffers.rbEnvironmentMap);
		
		//If we enable bloom
		if(bloomRendered != null)
			renderingContext.bindTexture2D("bloomBuffer", bloomRendered);
		
		renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.rbSSAO);
		renderingContext.bindTexture2D("pauseOverlayTexture", TexturesHandler.getTexture("./textures/gui/darker.png"));
		//renderingContext.bindTexture2D("debugBuffer", (System.currentTimeMillis() % 1000 < 500) ? this.loadedChunksMapTop : this.loadedChunksMapBot);
		renderingContext.bindTexture2D("debugBuffer", renderBuffers.rbReflections);

		Voxel vox = world.peekSafely(renderingContext.getCamera().getCameraPosition()).getVoxel();
		//Voxel vox = VoxelsStore.get().getVoxelById(world.peekSimple((int)(double)renderingContext.getCamera().getCameraPosition().x(),
		//		(int)(double)renderingContext.getCamera().getCameraPosition().y(), (int)(double)renderingContext.getCamera().getCameraPosition().z()));
		
		postProcess.setUniform1f("underwater", vox.getDefinition().isLiquid() ? 1 : 0);
		postProcess.setUniform1f("animationTimer", animationTimer);
		postProcess.setUniform1f("pauseOverlayFade", pauseFade);

		postProcess.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());

		if(sun_shadowMap != null) {
			postProcess.setUniformMatrix4f("shadowMatrix", sun_shadowMap.getShadowTransformationMatrix());
		}
			
		renderingContext.getCamera().setupShader(postProcess);
		skyRenderer.setupShader(postProcess);
		
		renderingContext.bindTexture2D("sunSetRiseTexture", worldTextures.sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", worldTextures.skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", worldTextures.skyTextureRaining);

		postProcess.setUniform1f("apertureModifier", apertureModifier);

		renderingContext.drawFSQuad();

		//Draw entities Huds
		if(!hideGui) {
			world.entitiesLock.readLock().lock();
			Iterator<Entity> ei = world.getAllLoadedEntities();
			Entity e;
			while (ei.hasNext())
			{
				e = ei.next();
				if (e instanceof EntityOverlay) {
					((EntityOverlay) e).drawEntityOverlay(renderingContext);
				}
			}
			world.entitiesLock.readLock().unlock();
		}
	}
	
	public void destroy()
	{
		skyRenderer.destroy();
		particlesRenderer.destroy();
		farTerrainRenderer.destroy();
		summariesTexturesHolder.destroy();
		entitiesRenderer.clearLoadedEntitiesRenderers();
		chunksRenderer.destroy();
		renderBuffers.destroy();
	}

	public class RenderBuffers
	{
		// Main Rendertarget (HDR)
		public final Texture2DRenderTargetGL rbShaded;

		// G-Buffers
		public final Texture2DRenderTargetGL rbZBuffer;
		public final Texture2DRenderTargetGL rbAlbedo, rbNormal, rbVoxelLight, rbSpecularity, rbMaterial;

		// Bloom texture & SSAO buffer
		public final Texture2DRenderTargetGL rbBloom, rbSSAO;
		
		public final Texture2DRenderTargetGL rbReflections;

		// FBOs
		public final RenderTargetAttachementsConfiguration fboGBuffers, fboOnlyAlbedoBuffer, fboShadedBuffer, fboShadedBufferWithSpecular, fboBloom, fboSSAO, fboSSR;

		public final Texture2DRenderTargetGL rbBlurTemp;
		public final RenderTargetAttachementsConfiguration fboBlur;

		// Shadow maps
		public int shadowMapResolution = 0;
		public final Texture2DRenderTargetGL rbShadowMap;
		public final RenderTargetAttachementsConfiguration fboShadowMap;

		//Environment map
		public int ENVMAP_SIZE = 128;
		public final CubemapGL rbEnvironmentMap;
		//public final RenderTargetAttachementsConfiguration environmentMapFBO;
		public final RenderTargetAttachementsConfiguration[] fbosEnvMap = new RenderTargetAttachementsConfiguration[6];

		//Temp buffers
		public final Texture2DRenderTargetGL rbEnvMapTemp, rbEnvMapZBuffer;
		public final RenderTargetAttachementsConfiguration fboTempBufferEnvMap;

		RenderBuffers()
		{
			// Main Rendertarget (HDR)
			rbShaded = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
			rbZBuffer = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, gameWindow.getWidth(), gameWindow.getHeight());
			rbAlbedo = new Texture2DRenderTargetGL(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
			
			rbNormal = new Texture2DRenderTargetGL(RGB_8, gameWindow.getWidth(), gameWindow.getHeight());
			rbVoxelLight = new Texture2DRenderTargetGL(RG_8, gameWindow.getWidth(), gameWindow.getHeight());
			rbSpecularity = new Texture2DRenderTargetGL(RED_8, gameWindow.getWidth(), gameWindow.getHeight());
			rbMaterial = new Texture2DRenderTargetGL(RED_8UI, gameWindow.getWidth(), gameWindow.getHeight());

			// Bloom texture
			rbBloom = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			rbSSAO = new Texture2DRenderTargetGL(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
			
			// Reflections (HDR)
			rbReflections = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());

			// FBOs
			fboGBuffers = new FrameBufferObjectGL(rbZBuffer, rbAlbedo, rbNormal, rbVoxelLight, rbSpecularity, rbMaterial);
			fboOnlyAlbedoBuffer = new FrameBufferObjectGL(rbZBuffer, rbAlbedo);
			
			fboShadedBuffer = new FrameBufferObjectGL(rbZBuffer, rbShaded);
			fboShadedBufferWithSpecular = new FrameBufferObjectGL(rbZBuffer, rbShaded, rbSpecularity);
			
			fboSSR = new FrameBufferObjectGL(null, rbReflections);
			fboBloom = new FrameBufferObjectGL(null, rbBloom);
			fboSSAO = new FrameBufferObjectGL(null, rbSSAO);

			rbBlurTemp = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			fboBlur = new FrameBufferObjectGL(null, rbBlurTemp);

			// Shadow maps
			rbShadowMap = new Texture2DRenderTargetGL(DEPTH_SHADOWMAP, RenderingConfig.shadowMapResolutions, RenderingConfig.shadowMapResolutions);
			fboShadowMap = new FrameBufferObjectGL(rbShadowMap);

			//Environment map
			rbEnvironmentMap = new CubemapGL(TextureFormat.RGB_HDR, ENVMAP_SIZE);
			//Temp buffers
			rbEnvMapTemp = new Texture2DRenderTargetGL(RGB_HDR, ENVMAP_SIZE, ENVMAP_SIZE);
			rbEnvMapZBuffer = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, ENVMAP_SIZE, ENVMAP_SIZE);

			fboTempBufferEnvMap = new FrameBufferObjectGL(rbEnvMapZBuffer, rbEnvMapTemp);
			
			//environmentMapFBO = new FrameBufferObjectGL(null, rbEnvironmentMap.getFace(0));
			for(int i = 0; i < 6; i++) {
				fbosEnvMap[i] = new FrameBufferObjectGL(rbEnvMapZBuffer, rbEnvironmentMap.getFace(i));
			}
		}

		public void resizeBuffers(int width, int height)
		{
			this.fboGBuffers.resizeFBO(width, height);
			this.fboShadedBuffer.resizeFBO(width, height);
			this.fboSSR.resizeFBO(width, height);
			// Resize bloom components
			this.fboBlur.resizeFBO(width / 2, height / 2);
			this.fboBloom.resizeFBO(width / 2, height / 2);
			this.fboSSAO.resizeFBO(width, height);
		}

		public void resizeShadowMaps()
		{
			// Only if necessary
			if (shadowMapResolution == RenderingConfig.shadowMapResolutions)
				return;

			logger.debug("Resizing shadow maps to "+RenderingConfig.shadowMapResolutions);
			shadowMapResolution = RenderingConfig.shadowMapResolutions;
			rbShadowMap.resize(shadowMapResolution, shadowMapResolution);
		}
		
		public void destroy() {
			//Destroy FBOs
			this.fboShadowMap.destroy(false);
			this.fboGBuffers.destroy(false);
			this.fboShadedBuffer.destroy(false);
			this.fboBloom.destroy(false);
			this.fboBlur.destroy(false);
			this.fboSSAO.destroy(false);
			this.fboSSR.destroy(false);
			this.fboShadedBufferWithSpecular.destroy(false);
			this.fboOnlyAlbedoBuffer.destroy(false);
			this.fboTempBufferEnvMap.destroy(false);
			for(int i = 0; i < 6; i++) {
				fbosEnvMap[i].destroy(false);
			}
			
			//Destroy render buffers
			this.rbAlbedo.destroy();
			this.rbBloom.destroy();
			this.rbBlurTemp.destroy();
			this.rbEnvironmentMap.destroy();
			this.rbEnvMapTemp.destroy();
			this.rbEnvMapZBuffer.destroy();
			this.rbMaterial.destroy();
			this.rbNormal.destroy();
			this.rbSpecularity.destroy();
			this.rbVoxelLight.destroy();
			this.rbReflections.destroy();
			this.rbShaded.destroy();
			this.rbShadowMap.destroy();
			this.rbSSAO.destroy();
			this.rbZBuffer.destroy();
		}
	}
	
	public class WorldTextures {
		
		//Sky stuff
		public Texture2D sunGlowTexture;
		public Texture2D skyTextureSunny;
		public Texture2D skyTextureRaining;

		public Texture2D lightmapTexture;
		public Texture2D waterNormalTexture;

		//Blocks atlases
		public Texture2D blocksAlbedoTexture;
		public Texture2D blocksNormalTexture;
		public Texture2D blocksMaterialTexture;
		
		WorldTextures() {
			reload();
		}

		public void reload()
		{
			//Loads texture atlases
			blocksAlbedoTexture = Client.getInstance().getContent().voxels().textures().getDiffuseAtlasTexture();
			blocksNormalTexture = Client.getInstance().getContent().voxels().textures().getNormalAtlasTexture();
			blocksMaterialTexture = Client.getInstance().getContent().voxels().textures().getMaterialAtlasTexture();
			
			sunGlowTexture = TexturesHandler.getTexture("./textures/environement/glow.png");
			skyTextureSunny = TexturesHandler.getTexture("./textures/environement/sky.png");
			skyTextureRaining = TexturesHandler.getTexture("./textures/environement/sky_rain.png");

			lightmapTexture = TexturesHandler.getTexture("./textures/environement/light.png");
			lightmapTexture.setTextureWrapping(false);
			waterNormalTexture = TexturesHandler.getTexture("./textures/water/shallow.png");
		}
	}
	
	@Override
	public void flagChunksModified()
	{
		//Nothing. We do our PVS every frame anyways
	}

	@Override
	public void resizeShadowMaps()
	{
		this.renderBuffers.resizeShadowMaps();
	}

	@Override
	public void setupRenderSize()
	{
		int width = gameWindow.getWidth();
		int height = gameWindow.getHeight();
		this.setupRenderSize(width, height);
	}
	
	public void setupRenderSize(int width, int height) {
		this.renderBuffers.resizeBuffers(width, height);
	}

	/** Debug-related, usually not called in gameplay */
	public void reloadContentSpecificStuff()
	{
		//TODO REMOVE REMOVE REMOVE
		if(farTerrainRenderer instanceof FarTerrainMeshRenderer)
			((FarTerrainMeshRenderer) farTerrainRenderer).markVoxelTexturesSummaryDirty();
		
		entitiesRenderer.clearLoadedEntitiesRenderers();
		
		worldTextures.reload();
	}

	//Component getters
	
	@Override
	public FarTerrainRenderer getFarTerrainRenderer()
	{
		return this.farTerrainRenderer;
	}

	@Override
	public DecalsRendererImplementation getDecalsRenderer()
	{
		return this.decalsRenderer;
	}

	@Override
	public ClientParticlesRenderer getParticlesRenderer()
	{
		return this.particlesRenderer;
	}

	@Override
	public WorldEffectsRenderer getWorldEffectsRenderer()
	{
		return this.weatherEffectsRenderer;
	}

	@Override
	public String screenShot()
	{
		return gameWindow.takeScreenshot();
	}
	
	public ChunkMeshesRenderer getChunkMeshesRenderer()
	{
		return this.chunksRenderer;
	}

	@Override
	public RenderingPass getCurrentRenderingPass()
	{
		return currentPass;
	}

	public ShadowMapRenderer getShadowRenderer()
	{
		return this.shadower;
	}

	public CubemapRenderer getCubemapRenderer() {
		return this.cubemapRenderer;
	}

	@Override
	public SummariesTexturesHolder getSummariesTexturesHolder() {
		return summariesTexturesHolder;
	}

	@Override
	public RenderingInterface getRenderingInterface() {
		return Client.getInstance().getGameWindow().getRenderingContext();
	}
}
