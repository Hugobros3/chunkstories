package io.xol.chunkstories.renderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static io.xol.engine.graphics.textures.TextureFormat.*;

import java.util.Iterator;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.renderer.WorldRenderer.RenderingPass;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRenderer;
import io.xol.chunkstories.renderer.lights.ComputedShadowMap;
import io.xol.chunkstories.renderer.sky.SkyRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.fbo.FrameBufferObject;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.Texture2DRenderTarget;
import io.xol.engine.graphics.textures.TextureFormat;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.Math2;

/** A tragically huge behemoth, held responsible of actually displaying all that mess */
public class WorldRendererImplementation implements WorldRenderer
{
	//Final CameraInterface mainCamera;
	//Final RenderingContext renderingInterface;
	private final GameWindow gameWindow;
	private final WorldClientCommon world;
	
	private ChunkMeshesRenderer chunksRenderer;
	
	EntitiesRenderer entitiesRenderer;
	SkyRenderer skyRenderer;
	DecalsRenderer decalsRenderer;
	ParticlesRenderer particlesRenderer;
	FarTerrainRenderer farTerrainRenderer;
	WorldEffectsRenderer weatherEffectsRenderer;
	ShadowMapRenderer shadower;
	BloomRenderer bloomRenderer;
	
	float animationTimer = 0.0f;
	float apertureModifier = 1.0f;
	
	public RenderBuffers renderBuffers;
	public WorldTextures worldTextures;
	
	RenderingPass currentPass = null;

	public WorldRendererImplementation(WorldClientCommon world, ClientInterface client)
	{
		this.world = world;
		this.gameWindow = client.getGameWindow();
		//this.renderingInterface = GameWindowOpenGL.getInstance().getRenderingContext();
		//this.mainCamera = renderingInterface.getCamera();

		//Creates subsystems
		this.chunksRenderer = new ChunkMeshesRenderer(this);
		
		entitiesRenderer = new EntitiesRenderer(world);
		particlesRenderer = new ParticlesRenderer(world);
		farTerrainRenderer = new FarTerrainRenderer(world, this);
		weatherEffectsRenderer = new DefaultWeatherEffectsRenderer(world, this);
		skyRenderer = new SkyRenderer(world);
		decalsRenderer = new DecalsRenderer(this);
		shadower = new ShadowMapRenderer(this);
		bloomRenderer = new BloomRenderer(this);
		
		//Creates all these fancy render buffers
		renderBuffers = new RenderBuffers();
		//Create a holder for the general world rendering textures
		worldTextures = new WorldTextures();
	}

	@Override
	public WorldClient getWorld()
	{
		return world;
	}

	@Override
	public SkyRenderer getSky()
	{
		return skyRenderer;
	}

	@Override
	public void renderWorld(RenderingInterface renderingInterface)
	{
		//Step one, set the camera to the proper spot
		CameraInterface mainCamera = renderingInterface.getCamera();
		EntityControllable entity = Client.getInstance().getPlayer().getControlledEntity();
		if (entity != null)
			entity.setupCamera(mainCamera);
		
		//TODO remove entirely
		OverlayRenderer.setCamera(mainCamera);
		
		// Prepare matrices
		mainCamera.setupUsingScreenSize(gameWindow.getWidth(), gameWindow.getHeight());

		//Update PVS
		chunksRenderer.updatePVSSet(mainCamera);
		
		//Generate a shadowmap for the sun if such an option is enabled
		currentPass = RenderingPass.SHADOW;
		ComputedShadowMap sun_shadowMap = RenderingConfig.doShadows ? shadower.generateSunShadowMap(renderingInterface, skyRenderer) : null;
		currentPass = RenderingPass.INTERNAL;
		
		// Clear G-Buffers and bind shaded HDR rendertarget
		renderingInterface.getRenderTargetManager().setCurrentRenderTarget(renderBuffers.fboGBuffers);
		renderingInterface.getRenderTargetManager().clearBoundRenderTargetAll();
		renderingInterface.getRenderTargetManager().setCurrentRenderTarget(renderBuffers.fboShadedBuffer);
		
		// Update sky and render it
		skyRenderer.time = (world.getTime() % 10000) / 10000f;
		skyRenderer.render(renderingInterface);

		// Fill up the G-Buffers
		currentPass = RenderingPass.NORMAL_OPAQUE;
		renderingInterface.getRenderTargetManager().setCurrentRenderTarget(renderBuffers.fboGBuffers);
		renderBuffers.fboGBuffers.setEnabledRenderTargets();
		
		gbuffers_opaque_chunk_meshes(renderingInterface);
		gbuffers_opaque_entities(renderingInterface);
		decalsRenderer.renderDecals(renderingInterface);
		
		// Shade the stuff
		particlesRenderer.render(renderingInterface);
		renderingInterface.flush();
		
		currentPass = RenderingPass.INTERNAL;
		renderingInterface.getRenderTargetManager().setCurrentRenderTarget(renderBuffers.fboShadedBuffer);
		renderBuffers.fboShadedBuffer.setEnabledRenderTargets();
		
		renderShadedBlocks(renderingInterface, sun_shadowMap);
		renderLightsDeffered(renderingInterface);
		
		//Add forward rendered stuff
		weatherEffectsRenderer.renderEffects(renderingInterface);
		farTerrainRenderer.renderTerrain(renderingInterface, chunksRenderer.getRenderedChunksMask(mainCamera));
		gbuffers_water_chunk_meshes(renderingInterface);
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
		// Set fixed-function parameters
		renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
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
		renderingInterface.setBlendMode(BlendMode.DISABLED);
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

		Vector3fm sunPos = skyRenderer.getSunPosition();

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(renderBuffers.fboShadedBuffer);
		//fboShadedBuffer.bind();

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

		applyShadowsShader.setUniform1f("time", skyRenderer.time);

		applyShadowsShader.setUniform1f("shadowMapResolution", RenderingConfig.shadowMapResolutions);
		applyShadowsShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());
		
		if(sun_shadowMap != null)
		{
			applyShadowsShader.setUniformMatrix4f("shadowMatrix", sun_shadowMap.getShadowTransformationMatrix());
		}
		applyShadowsShader.setUniform3f("sunPos", sunPos);

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
		renderingInterface.getRenderTargetManager().setCurrentRenderTarget(this.renderBuffers.fboShadedBuffer);
		
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
		
		//TODO read from scene
		float pauseFade = 0.0f;
		
		// We render to the screen.
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(null);

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		ShaderInterface postProcess = renderingContext.useShader("postprocess");

		renderingContext.bindTexture2D("shadedBuffer", renderBuffers.shadedBuffer);
		renderingContext.bindTexture2D("albedoBuffer", renderBuffers.albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", renderBuffers.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", renderBuffers.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", renderBuffers.materialBuffer);
		renderingContext.bindTexture2D("shadowMap", renderBuffers.shadowMapBuffer);
		
		//If we enable bloom
		if(bloomRendered != null)
			renderingContext.bindTexture2D("bloomBuffer", bloomRendered);
		
		renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.ssaoBuffer);
		renderingContext.bindTexture2D("pauseOverlayTexture", TexturesHandler.getTexture("./textures/gui/darker.png"));
		//renderingContext.bindTexture2D("debugBuffer", (System.currentTimeMillis() % 1000 < 500) ? this.loadedChunksMapTop : this.loadedChunksMapBot);
		renderingContext.bindTexture2D("debugBuffer", renderBuffers.shadowMapBuffer);

		Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData((int)(double)renderingContext.getCamera().getCameraPosition().getX(),
				(int)(double)renderingContext.getCamera().getCameraPosition().getY(), (int)(double)renderingContext.getCamera().getCameraPosition().getZ()));
		
		postProcess.setUniform1f("underwater", vox.getType().isLiquid() ? 1 : 0);
		postProcess.setUniform1f("time", animationTimer);
		postProcess.setUniform1f("pauseOverlayFade", pauseFade);

		renderingContext.getCamera().setupShader(postProcess);

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
		entitiesRenderer.clearLoadedEntitiesRenderers();
		chunksRenderer.destroy();
	}

	public class RenderBuffers
	{
		// Main Rendertarget (HDR)
		public Texture2DRenderTarget shadedBuffer;

		// G-Buffers
		public Texture2DRenderTarget zBuffer;

		public Texture2DRenderTarget albedoBuffer, normalBuffer, materialBuffer;

		// Bloom texture
		public Texture2DRenderTarget bloomBuffer, ssaoBuffer;

		// FBOs
		public FrameBufferObject fboGBuffers, fboShadedBuffer, fboBloom, fboSSAO;

		public Texture2DRenderTarget blurIntermediateBuffer;
		public FrameBufferObject fboBlur;

		// 64x64 texture used to cull distant mesh
		public Texture2DRenderTarget loadedChunksMapTop, loadedChunksMapBot;
		public FrameBufferObject fboLoadedChunksTop, fboLoadedChunksBot;

		// Shadow maps
		public int shadowMapResolution = 0;
		public Texture2DRenderTarget shadowMapBuffer;
		public FrameBufferObject shadowMapFBO;

		//Environment map
		public int ENVMAP_SIZE = 128;
		public Cubemap environmentMap;

		//Temp buffers
		public Texture2DRenderTarget environmentMapBufferHDR, environmentMapBufferZ;
		public FrameBufferObject environmentMapFastFbo, environmentMapFBO;

		RenderBuffers()
		{
			// Main Rendertarget (HDR)
			shadedBuffer = new Texture2DRenderTarget(RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
			zBuffer = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, gameWindow.getWidth(), gameWindow.getHeight());
			albedoBuffer = new Texture2DRenderTarget(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
			normalBuffer = new Texture2DRenderTarget(RGBA_3x10_2, gameWindow.getWidth(), gameWindow.getHeight());
			materialBuffer = new Texture2DRenderTarget(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());

			// Bloom texture
			bloomBuffer = new Texture2DRenderTarget(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			ssaoBuffer = new Texture2DRenderTarget(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());

			// FBOs
			fboGBuffers = new FrameBufferObject(zBuffer, albedoBuffer, normalBuffer, materialBuffer);

			fboShadedBuffer = new FrameBufferObject(zBuffer, shadedBuffer);
			fboBloom = new FrameBufferObject(null, bloomBuffer);
			fboSSAO = new FrameBufferObject(null, ssaoBuffer);

			blurIntermediateBuffer = new Texture2DRenderTarget(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			fboBlur = new FrameBufferObject(null, blurIntermediateBuffer);

			// 64x64 texture used to cull distant mesh
			loadedChunksMapTop = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, 64, 64);
			fboLoadedChunksTop = new FrameBufferObject(loadedChunksMapTop);
			loadedChunksMapBot = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, 64, 64);
			fboLoadedChunksBot = new FrameBufferObject(loadedChunksMapBot);

			// Shadow maps
			shadowMapBuffer = new Texture2DRenderTarget(DEPTH_SHADOWMAP, RenderingConfig.shadowMapResolutions, RenderingConfig.shadowMapResolutions);
			shadowMapFBO = new FrameBufferObject(shadowMapBuffer);

			//Environment map
			environmentMap = new Cubemap(TextureFormat.RGB_HDR, ENVMAP_SIZE);
			//Temp buffers
			environmentMapBufferHDR = new Texture2DRenderTarget(RGB_HDR, ENVMAP_SIZE, ENVMAP_SIZE);
			environmentMapBufferZ = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, ENVMAP_SIZE, ENVMAP_SIZE);

			environmentMapFastFbo = new FrameBufferObject(environmentMapBufferZ, environmentMapBufferHDR);
			environmentMapFBO = new FrameBufferObject(null, environmentMap.getFace(0));
		}

		public void resizeBuffers()
		{
			int width = gameWindow.getWidth();
			int height = gameWindow.getHeight();
			this.fboGBuffers.resizeFBO(width, height);
			this.fboShadedBuffer.resizeFBO(width, height);
			// Resize bloom components
			fboBlur.resizeFBO(width / 2, height / 2);
			fboBloom.resizeFBO(width / 2, height / 2);
			fboSSAO.resizeFBO(width, height);
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
			return wetFactor * (1f - Math2.clamp((e.getLocation().getY() - 110) / 20, 0, 1));
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

		//shader.setUniform3f("sunColor", Math2.mix(new Vector3fm(0.80f, 0.80f, 0.69f), new Vector3fm(0.5f), sunLightFactor));
		shader.setUniform3f("sunColor", Math2.mix(new Vector3fm(1.0f), new Vector3fm(0.5f), sunLightFactor));

		float shadowBrightness = 0.5f / 255f;
		Vector3fm shadowColorSunny = new Vector3fm(0.0f, 88f * shadowBrightness, 150f * shadowBrightness);
		shadowColorSunny = Math2.mix(shadowColorSunny, new Vector3fm(shadowBrightness * 255f), 0.5f);

		Vector3fm shadowColor = Math2.mix(shadowColorSunny, new Vector3fm(0.5f), sunLightFactor);
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
		this.renderBuffers.resizeBuffers();
	}

	@Override
	public FarTerrainMeshRenderer getFarTerrainRenderer()
	{
		return this.farTerrainRenderer;
	}

	public void reloadContentSpecificStuff()
	{
		farTerrainRenderer.markVoxelTexturesSummaryDirty();
		entitiesRenderer.clearLoadedEntitiesRenderers();
		
		worldTextures.reload();
	}

	@Override
	public DecalsRenderer getDecalsRenderer()
	{
		return this.decalsRenderer;
	}

	@Override
	public ParticlesRenderer getParticlesRenderer()
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
		// TODO Auto-generated method stub
		return "Not implemented";
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
}
