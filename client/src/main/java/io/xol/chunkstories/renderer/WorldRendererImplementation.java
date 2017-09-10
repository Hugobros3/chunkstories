package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.*;

import java.util.Iterator;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.math.Math2;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.renderer.chunks.ChunkMeshesRenderer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRendererImplementation;
import io.xol.chunkstories.renderer.lights.ComputedShadowMap;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.renderer.sky.DefaultSkyRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainMeshRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainNoMeshRenderer;
import io.xol.chunkstories.renderer.terrain.SummariesArrayTexture;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.fbo.FrameBufferObjectGL;
import io.xol.engine.graphics.textures.CubemapGL;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;
import io.xol.engine.graphics.textures.TexturesHandler;

/** A tragically huge behemoth, held responsible of actually displaying all that mess */
public class WorldRendererImplementation implements WorldRenderer
{
	//Final CameraInterface mainCamera;
	//Final RenderingContext renderingInterface;
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

	public WorldRendererImplementation(WorldClientCommon world, ClientInterface client)
	{
		this.world = world;
		this.gameWindow = client.getGameWindow();
		
		//Creates all these fancy render buffers
		renderBuffers = new RenderBuffers();
		
		//Create a holder for the general world rendering textures
		worldTextures = new WorldTextures();

		//Creates subsystems
		this.chunksRenderer = new ChunkMeshesRenderer(this);
		
		this.entitiesRenderer = new EntitiesRenderer(world);
		this.particlesRenderer = new ClientParticlesRenderer(world);
		this.farTerrainRenderer = new FarTerrainNoMeshRenderer(this);
		this.summariesTexturesHolder = new SummariesArrayTexture(client);
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
			cubemapRenderer.renderWorldCubemap(renderingInterface, renderBuffers.environmentMap, 128, true);
		
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
		ComputedShadowMap sun_shadowMap = RenderingConfig.doShadows ? shadower.generateSunShadowMap(renderingInterface, skyRenderer) : null;
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
		decalsRenderer.renderDecals(renderingInterface);
		
		// Shade the stuff
		particlesRenderer.render(renderingInterface, true);
		renderingInterface.flush();
		
		gbuffers_water_chunk_meshes(renderingInterface);
		
		currentPass = RenderingPass.INTERNAL;
		renderingInterface.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);
		renderBuffers.fboShadedBuffer.setEnabledRenderTargets();
		
		//Render the deffered light
		renderShadedBlocks(renderingInterface, sun_shadowMap);
		renderLightsDeffered(renderingInterface);
		
		//Add forward rendered stuff
		farTerrainRenderer.renderTerrain(renderingInterface, chunksRenderer.getRenderedChunksMask(mainCamera));
		particlesRenderer.render(renderingInterface, false);
		
		//Render SSR if enabled
		if(RenderingConfig.doRealtimeReflections)
			reflectionsRenderer.renderRealtimeScreenSpaceReflections(renderingInterface);
		
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
		renderingInterface.bindTexture2D("vegetationColorTexture", getGrassTexture());

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
		opaqueBlocksShader.setUniform1f("wetness", getWorldWetness());
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
			Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData((int) (double) renderingInterface.getCamera().getCameraPosition().x(), (int) (double) renderingInterface.getCamera().getCameraPosition().y(), (int) (double) renderingInterface.getCamera().getCameraPosition().z()));
			liquidBlocksShader.setUniform1f("underwater", vox.getType().isLiquid() ? 1 : 0);

			if (pass == 1)
			{
				renderingInterface.getRenderTargetManager().setConfiguration(this.renderBuffers.fboShadedBuffer);
				//fboShadedBuffer.bind();
				this.renderBuffers.fboShadedBuffer.setEnabledRenderTargets(true);
				renderingInterface.bindTexture2D("readbackAlbedoBufferTemp", this.renderBuffers.albedoBuffer);
				renderingInterface.bindTexture2D("readbackMetaBufferTemp", this.renderBuffers.materialBuffer);
				renderingInterface.bindTexture2D("readbackDepthBufferTemp", this.renderBuffers.zBuffer);

				renderingInterface.getRenderTargetManager().setDepthMask(false);
				//glDepthMask(false);
			}
			else if (pass == 2)
			{
				renderingInterface.getRenderTargetManager().setConfiguration(this.renderBuffers.fboGBuffers);
				//fboGBuffers.bind();
				this.renderBuffers.fboGBuffers.setEnabledRenderTargets();
				renderingInterface.bindTexture2D("readbackShadedBufferTemp", this.renderBuffers.shadedBuffer);
				renderingInterface.bindTexture2D("readbackDepthBufferTemp", this.renderBuffers.zBuffer);

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
		entitiesShader.setUniform1f("wetness", getWorldWetness());

		renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
		renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);

		renderingContext.getCamera().setupShader(entitiesShader);

		entitiesRenderer.renderEntities(renderingContext);
	}
	
	/**
	 * Uses G-Buffers data to spit out shaded solid blocks ( shadows etc )
	 * @param sun_shadowMap 
	 */
	public void renderShadedBlocks(RenderingInterface renderingContext, ComputedShadowMap sun_shadowMap)
	{
		ShaderInterface applyShadowsShader = renderingContext.useShader("shadows_apply");
		setupShadowColors(applyShadowsShader);

		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", getWorldWetness());

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		renderingContext.getRenderTargetManager().setConfiguration(renderBuffers.fboShadedBuffer);

		float lightMultiplier = 1.0f;

		applyShadowsShader.setUniform1f("brightnessMultiplier", lightMultiplier);

		renderingContext.bindTexture2D("albedoBuffer", renderBuffers.albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", renderBuffers.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", renderBuffers.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", renderBuffers.materialBuffer);
		renderingContext.bindTexture2D("blockLightmap", worldTextures.lightmapTexture);
		renderingContext.bindTexture2D("shadowMap", renderBuffers.shadowMapBuffer);

		renderingContext.bindTexture2D("sunSetRiseTexture", worldTextures.sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", worldTextures.skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", worldTextures.skyTextureRaining);

		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");
		renderingContext.bindTexture2D("lightColors", lightColors);

		//TODO if SSAO
		renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.ssaoBuffer);

		renderingContext.bindCubemap("environmentCubemap", renderBuffers.environmentMap);

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
		renderingInterface.bindTexture2D("depthBuffer", this.renderBuffers.zBuffer);
		renderingInterface.bindTexture2D("diffuseBuffer", this.renderBuffers.albedoBuffer);
		renderingInterface.bindTexture2D("normalBuffer", this.renderBuffers.normalBuffer);

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
	
	@Override
	public void blitFinalImage(RenderingInterface renderingContext)
	{
		Texture2D bloomRendered = RenderingConfig.doBloom ? bloomRenderer.renderBloom(renderingContext) : null;
		
		Layer layer = renderingContext.getWindow().getLayer().getRootLayer();
		float pauseFade = (layer instanceof Ingame) ? ((Ingame)layer).getPauseOverlayFade() : 0;
		
		// We render to the screen.
		renderingContext.getRenderTargetManager().setConfiguration(null);

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		ShaderInterface postProcess = renderingContext.useShader("postprocess");

		renderingContext.bindTexture2D("shadedBuffer", renderBuffers.shadedBuffer);
		renderingContext.bindTexture2D("albedoBuffer", renderBuffers.albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", renderBuffers.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", renderBuffers.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", renderBuffers.materialBuffer);
		renderingContext.bindTexture2D("shadowMap", renderBuffers.shadowMapBuffer);
		
		renderingContext.bindTexture2D("reflectionsBuffer", renderBuffers.reflectionsBuffer);
		renderingContext.bindCubemap("environmentMap", renderBuffers.environmentMap);
		
		//If we enable bloom
		if(bloomRendered != null)
			renderingContext.bindTexture2D("bloomBuffer", bloomRendered);
		
		renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.ssaoBuffer);
		renderingContext.bindTexture2D("pauseOverlayTexture", TexturesHandler.getTexture("./textures/gui/darker.png"));
		//renderingContext.bindTexture2D("debugBuffer", (System.currentTimeMillis() % 1000 < 500) ? this.loadedChunksMapTop : this.loadedChunksMapBot);
		renderingContext.bindTexture2D("debugBuffer", renderBuffers.reflectionsBuffer);

		Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData((int)(double)renderingContext.getCamera().getCameraPosition().x(),
				(int)(double)renderingContext.getCamera().getCameraPosition().y(), (int)(double)renderingContext.getCamera().getCameraPosition().z()));
		
		postProcess.setUniform1f("underwater", vox.getType().isLiquid() ? 1 : 0);
		postProcess.setUniform1f("animationTimer", animationTimer);
		postProcess.setUniform1f("pauseOverlayFade", pauseFade);

		renderingContext.getCamera().setupShader(postProcess);
		skyRenderer.setupShader(postProcess);
		
		renderingContext.bindTexture2D("sunSetRiseTexture", worldTextures.sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", worldTextures.skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", worldTextures.skyTextureRaining);

		postProcess.setUniform1f("apertureModifier", apertureModifier);

		renderingContext.drawFSQuad();

		//Draw entities Huds
		//TODO entitiesRenderer
		world.entitiesLock.readLock().lock();
		Iterator<Entity> ei = world.getAllLoadedEntities();
		Entity e;
		while (ei.hasNext())
		{
			e = ei.next();
			if (e instanceof EntityOverlay)
				((EntityOverlay) e).drawEntityOverlay(renderingContext);
		}
		world.entitiesLock.readLock().unlock();
	}
	
	public void destroy()
	{
		skyRenderer.destroy();
		particlesRenderer.destroy();
		farTerrainRenderer.destroy();
		summariesTexturesHolder.destroy();
		entitiesRenderer.clearLoadedEntitiesRenderers();
		chunksRenderer.destroy();
	}

	public class RenderBuffers
	{
		// Main Rendertarget (HDR)
		public final Texture2DRenderTargetGL shadedBuffer;

		// G-Buffers
		public final Texture2DRenderTargetGL zBuffer;
		public final Texture2DRenderTargetGL albedoBuffer, normalBuffer, materialBuffer;

		// Bloom texture & SSAO buffer
		public final Texture2DRenderTargetGL bloomBuffer, ssaoBuffer;
		
		public final Texture2DRenderTargetGL reflectionsBuffer;

		// FBOs
		public final RenderTargetAttachementsConfiguration fboGBuffers, fboShadedBuffer, fboBloom, fboSSAO, fboSSR;

		public final Texture2DRenderTargetGL blurIntermediateBuffer;
		public final RenderTargetAttachementsConfiguration fboBlur;

		// 64x64 texture used to cull distant mesh
		public final Texture2DRenderTargetGL loadedChunksMapTop, loadedChunksMapBot;
		public final RenderTargetAttachementsConfiguration fboLoadedChunksTop, fboLoadedChunksBot;

		// Shadow maps
		public int shadowMapResolution = 0;
		public final Texture2DRenderTargetGL shadowMapBuffer;
		public final RenderTargetAttachementsConfiguration shadowMapFBO;

		//Environment map
		public int ENVMAP_SIZE = 128;
		public final CubemapGL environmentMap;

		//Temp buffers
		public final Texture2DRenderTargetGL environmentMapBufferHDR, environmentMapBufferZ;
		public final RenderTargetAttachementsConfiguration environmentMapFastFbo, environmentMapFBO;

		RenderBuffers()
		{
			// Main Rendertarget (HDR)
			shadedBuffer = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
			zBuffer = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, gameWindow.getWidth(), gameWindow.getHeight());
			albedoBuffer = new Texture2DRenderTargetGL(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
			normalBuffer = new Texture2DRenderTargetGL(RGBA_3x10_2, gameWindow.getWidth(), gameWindow.getHeight());
			materialBuffer = new Texture2DRenderTargetGL(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());

			// Bloom texture
			bloomBuffer = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			ssaoBuffer = new Texture2DRenderTargetGL(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
			
			// Reflections (HDR)
			reflectionsBuffer = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());

			// FBOs
			fboGBuffers = new FrameBufferObjectGL(zBuffer, albedoBuffer, normalBuffer, materialBuffer);

			fboShadedBuffer = new FrameBufferObjectGL(zBuffer, shadedBuffer);
			
			fboSSR = new FrameBufferObjectGL(null, reflectionsBuffer);
			fboBloom = new FrameBufferObjectGL(null, bloomBuffer);
			fboSSAO = new FrameBufferObjectGL(null, ssaoBuffer);

			blurIntermediateBuffer = new Texture2DRenderTargetGL(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			fboBlur = new FrameBufferObjectGL(null, blurIntermediateBuffer);

			// 64x64 texture used to cull distant mesh
			loadedChunksMapTop = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, 64, 64);
			fboLoadedChunksTop = new FrameBufferObjectGL(loadedChunksMapTop);
			loadedChunksMapBot = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, 64, 64);
			fboLoadedChunksBot = new FrameBufferObjectGL(loadedChunksMapBot);

			// Shadow maps
			shadowMapBuffer = new Texture2DRenderTargetGL(DEPTH_SHADOWMAP, RenderingConfig.shadowMapResolutions, RenderingConfig.shadowMapResolutions);
			shadowMapFBO = new FrameBufferObjectGL(shadowMapBuffer);

			//Environment map
			environmentMap = new CubemapGL(TextureFormat.RGB_HDR, ENVMAP_SIZE);
			//Temp buffers
			environmentMapBufferHDR = new Texture2DRenderTargetGL(RGB_HDR, ENVMAP_SIZE, ENVMAP_SIZE);
			environmentMapBufferZ = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, ENVMAP_SIZE, ENVMAP_SIZE);

			environmentMapFastFbo = new FrameBufferObjectGL(environmentMapBufferZ, environmentMapBufferHDR);
			environmentMapFBO = new FrameBufferObjectGL(null, environmentMap.getFace(0));
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

			System.out.println("Resizing shadow maps to "+RenderingConfig.shadowMapResolutions);
			shadowMapResolution = RenderingConfig.shadowMapResolutions;
			shadowMapBuffer.resize(shadowMapResolution, shadowMapResolution);
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
	
	//TODO Make that a world generator returned value
	private float getWorldWetness()
	{
		float wetFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.5f) / 0.3f, 1.0f);

		//Special case of cancelling out by snow
		Entity e = Client.getInstance().getPlayer().getControlledEntity();
		if (e != null)
		{
			return wetFactor * (1f - Math2.clamp((e.getLocation().y() - 110) / 20, 0, 1));
		}

		return wetFactor;
	}
	
	//TODO ask the world generator the location of that
	public Texture2D getGrassTexture()
	{
		Texture2D vegetationTexture = null;
		if (world.getFolderPath() != null)
			vegetationTexture = TexturesHandler.getTexture(world.getFolderPath() + "/grassColor.png");
		if (vegetationTexture == null || vegetationTexture == TexturesHandler.nullTexture())
			vegetationTexture = TexturesHandler.getTexture("./textures/environement/grassColor.png");
		vegetationTexture.setMipMapping(false);
		vegetationTexture.setLinearFiltering(true);
		return vegetationTexture;
	}
	
	//TODO make those configurables by the world generator
	public void setupShadowColors(ShaderInterface shader)
	{
		float sunLightFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.0f) / 1.0f, 1.0f);

		shader.setUniform1f("shadowStrength", 1.0f);

		//shader.setUniform3f("sunColor", Math2.mix(new Vector3f(0.80f, 0.80f, 0.69f), new Vector3f(0.5f), sunLightFactor));
		shader.setUniform3f("sunColor", Math2.mix(new Vector3f(1.0f), new Vector3f(0.5f), sunLightFactor));

		float shadowBrightness = 0.5f / 255f;
		Vector3f shadowColorSunny = new Vector3f(0.0f, 88f * shadowBrightness, 150f * shadowBrightness);
		shadowColorSunny = Math2.mix(shadowColorSunny, new Vector3f(shadowBrightness * 255f), 0.5f);

		Vector3f shadowColor = Math2.mix(shadowColorSunny, new Vector3f(0.5f), sunLightFactor);
		shader.setUniform3f("shadowColor", shadowColor);
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
