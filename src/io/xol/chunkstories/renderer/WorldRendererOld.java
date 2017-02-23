package io.xol.chunkstories.renderer;

import static io.xol.engine.graphics.textures.TextureFormat.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;

import io.xol.engine.base.InputAbstractor;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fbo.FrameBufferObject;
import io.xol.engine.graphics.geometry.ByteBufferAttributeSource;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture2DRenderTarget;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureFormat;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.PBOPacker;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.math.Math2;
import io.xol.engine.math.MatrixHelper;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.core.events.WorldPostRenderingEvent;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkMeshDataSections;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;
import io.xol.chunkstories.renderer.chunks.ChunkRenderable;
import io.xol.chunkstories.renderer.chunks.ChunkMeshesBakerThread;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRenderer;
import io.xol.chunkstories.renderer.lights.LightsRenderer;
import io.xol.chunkstories.renderer.sky.SkyRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainRenderer;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.PolygonFillMode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldRendererOld
{
	// World pointer
	WorldClientCommon world;

	// Worker thread
	public ChunkMeshesBakerThread chunksRenderer;

	//Chunk space position
	public int cameraChunkX, cameraChunkY, cameraChunkZ;

	// camera object ( we need it so much)
	private final Camera camera = GameWindowOpenGL.getInstance().renderingContext.getCamera();

	private int sizeInChunks; // cache from world

	// Chunks to render
	private List<ChunkRenderable> renderList = new ArrayList<ChunkRenderable>();

	// Wheter to update the renderlist or not.
	private boolean chunksChanged = true;

	// Screen width & height
	private int scrW, scrH;

	// Shader programs
	private ShaderInterface terrainShader;
	private ShaderInterface opaqueBlocksShader;
	private ShaderInterface liquidBlocksShader;
	private ShaderInterface entitiesShader;
	private ShaderInterface shadowsPassShader;
	private ShaderInterface applyShadowsShader;
	private ShaderInterface lightShader;
	private ShaderInterface postProcess;

	private ShaderInterface bloomShader;
	private ShaderInterface ssaoShader;

	private ShaderInterface blurH;
	private ShaderInterface blurV;

	//Rendering context
	private RenderingContext renderingContext;

	// Main Rendertarget (HDR)
	private Texture2DRenderTarget shadedBuffer;

	// G-Buffers
	public Texture2DRenderTarget zBuffer;

	private Texture2DRenderTarget albedoBuffer, normalBuffer, materialBuffer;

	// Bloom texture
	private Texture2DRenderTarget bloomBuffer, ssaoBuffer;

	// FBOs
	private FrameBufferObject fboGBuffers, fboShadedBuffer, fboBloom, fboSSAO;

	private Texture2DRenderTarget blurIntermediateBuffer;
	private FrameBufferObject fboBlur;

	// 64x64 texture used to cull distant mesh
	private Texture2DRenderTarget loadedChunksMapTop, loadedChunksMapBot;
	private FrameBufferObject fboLoadedChunksTop, fboLoadedChunksBot;

	// Shadow maps
	private int shadowMapResolution = 0;
	private Texture2DRenderTarget shadowMapBuffer;
	private FrameBufferObject shadowMapFBO;

	//Environment map
	private int ENVMAP_SIZE = 128;
	private Cubemap environmentMap;

	//Temp buffers
	private Texture2DRenderTarget environmentMapBufferHDR, environmentMapBufferZ;
	private FrameBufferObject environmentMapFastFbo, environmentMapFBO;

	// Shadow transformation matrix
	private Matrix4f depthMatrix = new Matrix4f();

	//Entities
	private EntitiesRenderer entitiesRenderer;

	// Sky
	private SkyRenderer skyRenderer;

	// Decals
	private DecalsRenderer decalsRenderer;

	//Particles
	private ParticlesRenderer particlesRenderer;

	//Far terrain mesher
	private FarTerrainRenderer farTerrainRenderer;

	//Rain snow etc
	private WorldEffectsRenderer weatherEffectsRenderer;

	//For shaders animations
	float animationTimer = 0.0f;

	//Counters
	public int renderedVertices = 0;
	public int renderedVerticesShadow = 0;
	public int renderedChunks = 0;

	//Bloom avg color buffer
	// ByteBuffer shadedMipmapZeroLevelColor = BufferUtils.createByteBuffer(4 * 3);
	//Bloom aperture
	private PBOPacker illuminationDownloader;
	private PBOPacker.PBOPackerResult illuminationDownloadInProgress = null;
	float apertureModifier = 1f;

	//Sky stuff
	Texture2D sunGlowTexture;
	Texture2D skyTextureSunny;
	Texture2D skyTextureRaining;

	Texture2D lightmapTexture;
	Texture2D waterNormalTexture;

	//Blocks atlases
	Texture2D blocksAlbedoTexture;
	Texture2D blocksNormalTexture;
	Texture2D blocksMaterialTexture;

	//SSAO (disabled)
	Vector3fm ssao_kernel[];
	int ssao_kernel_size;
	int ssao_noiseTex = -1;

	//8K buffer of 2D coordinates to draw the local map
	ByteBuffer localMapCommands = BufferUtils.createByteBuffer(8192);

	long lastEnvmapRender = 0L;
	
	//Constructor and modificators
	public WorldRendererOld(WorldClientCommon world)
	{
		// Link world
		this.world = world;
		this.renderingContext = GameWindowOpenGL.getInstance().getRenderingContext();
		this.sizeInChunks = world.getSizeInChunks();

		//Loads texture atlases
		blocksAlbedoTexture = Client.getInstance().getContent().voxels().textures().getDiffuseAtlasTexture();
		blocksNormalTexture = Client.getInstance().getContent().voxels().textures().getNormalAtlasTexture();
		blocksMaterialTexture = Client.getInstance().getContent().voxels().textures().getMaterialAtlasTexture();

		//Load initial textures
		sunGlowTexture = TexturesHandler.getTexture("./textures/environement/glow.png");
		skyTextureSunny = TexturesHandler.getTexture("./textures/environement/sky.png");
		skyTextureRaining = TexturesHandler.getTexture("./textures/environement/sky_rain.png");

		lightmapTexture = TexturesHandler.getTexture("./textures/environement/light.png");
		waterNormalTexture = TexturesHandler.getTexture("./textures/water/shallow.png");

		//Creates subsystems
		entitiesRenderer = new EntitiesRenderer(world);
		particlesRenderer = new ParticlesRenderer(world);
		farTerrainRenderer = new FarTerrainRenderer(world, null);
		//weatherEffectsRenderer = new DefaultWeatherEffectsRenderer(world, this);
		skyRenderer = new SkyRenderer(world);
		//decalsRenderer = new DecalsRenderer(this);

		//Pre-load shaders
		opaqueBlocksShader = ShadersLibrary.getShaderProgram("blocks_opaque");
		entitiesShader = ShadersLibrary.getShaderProgram("entities");
		shadowsPassShader = ShadersLibrary.getShaderProgram("shadows");
		applyShadowsShader = ShadersLibrary.getShaderProgram("shadows_apply");
		lightShader = ShadersLibrary.getShaderProgram("light");
		postProcess = ShadersLibrary.getShaderProgram("postprocess");
		terrainShader = ShadersLibrary.getShaderProgram("terrain");

		bloomShader = ShadersLibrary.getShaderProgram("bloom");
		ssaoShader = ShadersLibrary.getShaderProgram("ssao");
		blurH = ShadersLibrary.getShaderProgram("blurH");
		blurV = ShadersLibrary.getShaderProgram("blurV");

		//Creates G-Buffers and fbo layouts
		initializeGBuffers();
		resizeShadowMaps();

		//PBOs
		illuminationDownloader = new PBOPacker();
		//for (int i = 0; i < illDownBuffers; i++)
		//	illuminationDownloader[i] = new PBOPacker();

		chunksRenderer = new ChunkMeshesBakerThread(world);
		chunksRenderer.start();
	}

	private void initializeGBuffers()
	{
		// Main Rendertarget (HDR)
		shadedBuffer = new Texture2DRenderTarget(RGB_HDR, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		zBuffer = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		albedoBuffer = new Texture2DRenderTarget(RGBA_8BPP, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		normalBuffer = new Texture2DRenderTarget(RGBA_3x10_2, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		materialBuffer = new Texture2DRenderTarget(RGBA_8BPP, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());

		// Bloom texture
		bloomBuffer = new Texture2DRenderTarget(RGB_HDR, renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2);
		ssaoBuffer = new Texture2DRenderTarget(RGBA_8BPP, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());

		// FBOs
		fboGBuffers = new FrameBufferObject(zBuffer, albedoBuffer, normalBuffer, materialBuffer);

		fboShadedBuffer = new FrameBufferObject(zBuffer, shadedBuffer);
		fboBloom = new FrameBufferObject(null, bloomBuffer);
		fboSSAO = new FrameBufferObject(null, ssaoBuffer);

		blurIntermediateBuffer = new Texture2DRenderTarget(RGB_HDR, renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2);
		fboBlur = new FrameBufferObject(null, blurIntermediateBuffer);

		// 64x64 texture used to cull distant mesh
		loadedChunksMapTop = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, 64, 64);
		fboLoadedChunksTop = new FrameBufferObject(loadedChunksMapTop);
		loadedChunksMapBot = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, 64, 64);
		fboLoadedChunksBot = new FrameBufferObject(loadedChunksMapBot);

		// Shadow maps
		shadowMapBuffer = new Texture2DRenderTarget(DEPTH_SHADOWMAP, 256, 256);
		shadowMapFBO = new FrameBufferObject(shadowMapBuffer);

		//Environment map
		environmentMap = new Cubemap(TextureFormat.RGB_HDR, ENVMAP_SIZE);
		//Temp buffers
		environmentMapBufferHDR = new Texture2DRenderTarget(RGB_HDR, ENVMAP_SIZE, ENVMAP_SIZE);
		environmentMapBufferZ = new Texture2DRenderTarget(DEPTH_RENDERBUFFER, ENVMAP_SIZE, ENVMAP_SIZE);

		environmentMapFastFbo = new FrameBufferObject(environmentMapBufferZ, environmentMapBufferHDR);
		environmentMapFBO = new FrameBufferObject(null, environmentMap.getFace(0));
	}

	public void setupRenderSize(int width, int height)
	{
		scrW = width;
		scrH = height;
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

		System.out.println(RenderingConfig.shadowMapResolutions);
		shadowMapResolution = RenderingConfig.shadowMapResolutions;
		shadowMapBuffer.resize(shadowMapResolution, shadowMapResolution);
	}

	public void flagModified()
	{
		chunksChanged = true;
	}

	public RenderingContext getRenderingContext()
	{
		return renderingContext;
	}

	//Rendering main calls
	public void renderWorldAtCamera()
	{
		if (RenderingConfig.doDynamicCubemaps)
			renderWorldCubemap(environmentMap, ENVMAP_SIZE, true);
		renderWorldAtCameraInternal();
	}

	public void renderWorldAtCameraInternal()
	{
		OverlayRenderer.setCamera(camera);
		// Debug/Dev : reset vertex counts
		renderedChunks = 0;
		renderedVertices = 0;
		renderedVerticesShadow = 0;

		Client.profiler.startSection("updates");
		// Load/Unload required parts of the world, update the display list etc
		updateRender(camera);

		// Shadows pre-pass
		if (RenderingConfig.doShadows)
			shadowPass();
		
		// Prepare matrices
		camera.setupUsingScreenSize(scrW, scrH);

		// Clear G-Buffers and bind shaded HDR rendertarget
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboGBuffers);
		renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);

		skyRenderer.time = (world.getTime() % 10000) / 10000f;
		skyRenderer.render(renderingContext);

		// Move camera to relevant position
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboGBuffers);
		fboGBuffers.setEnabledRenderTargets();

		// Render world
		renderWorld(false);

		// Render weather
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
		fboShadedBuffer.setEnabledRenderTargets();
		weatherEffectsRenderer.renderEffects(renderingContext);

		Client.getInstance().getPluginManager().fireEvent(new WorldPostRenderingEvent(world, this, renderingContext));

		// Do bloom
		//if (RenderingConfig.doBloom)
		//	renderBloom();

		//Bind shaded buffer in case some other rendering is to be done
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
		fboShadedBuffer.setEnabledRenderTargets();

		Client.profiler.startSection("done");
	}
	
	/** Pre-rendering function : Uploads the finished meshes from ChunksRenderer; Grabs all the loaded chunks and add them to the rendering queue Updates far terrain meshes if needed Draws the chunksLoadedMap */
	public void updateRender(CameraInterface camera)
	{
		Vector3dm pos = new Vector3dm(camera.getCameraPosition().getX(), camera.getCameraPosition().getY(), camera.getCameraPosition().getZ());
		// Called every frame, this method takes care of updating the world :
		// It will keep up to date the camera position, as well as a list of
		// to-render chunks in order to fill empty VBO space

		// Update view
		int newCX = Math2.floor((pos.getX()) / 32);
		int newCY = Math2.floor((pos.getY()) / 32);
		int newCZ = Math2.floor((pos.getZ()) / 32);

		//Rebuild the chunks in the render list
		if (cameraChunkX != newCX || cameraChunkY != newCY || cameraChunkZ != newCZ || chunksChanged)
		{
			farTerrainRenderer.markFarTerrainMeshDirty();

			//Updates current chunk location
			cameraChunkX = newCX;
			cameraChunkY = newCY;
			cameraChunkZ = newCZ;
			int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);

			//Iterates over all loaded chunks to generate list and map
			renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboLoadedChunksBot);
			renderingContext.getRenderTargetManager().clearBoundRenderTargetZ(1.0f);

			renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboLoadedChunksTop);
			renderingContext.getRenderTargetManager().clearBoundRenderTargetZ(0.0f);
			
			this.loadedChunksMapTop.setLinearFiltering(false);
			this.loadedChunksMapTop.setMipMapping(false);
			this.loadedChunksMapTop.setTextureWrapping(false);
			//
			this.loadedChunksMapBot.setLinearFiltering(false);
			this.loadedChunksMapBot.setMipMapping(false);
			this.loadedChunksMapBot.setTextureWrapping(false);
			//fboLoadedChunks.resizeFBO(32, 32);

			renderingContext.useShader("loaded_map");
			localMapCommands.clear();

			renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
			renderingContext.setCullingMode(CullingMode.DISABLED);
			renderingContext.setBlendMode(BlendMode.DISABLED);
			renderingContext.setDepthTestMode(DepthTestMode.GREATER_OR_EQUAL);

			int localMapElements = 0;

			Set<Chunk> floodFillSet = new HashSet<Chunk>();
			Set<Vector3dm> floodFillMask = new HashSet<Vector3dm>();

			Deque<Integer> floodFillDeque = new ArrayDeque<Integer>();

			int ajustedCameraChunkX = cameraChunkX;
			int ajustedCameraChunkZ = cameraChunkZ;
			if (cameraChunkX - cameraChunkX > chunksViewDistance)
				ajustedCameraChunkX += -sizeInChunks;
			if (cameraChunkX - cameraChunkX < -chunksViewDistance)
				ajustedCameraChunkX += sizeInChunks;
			if (cameraChunkZ - cameraChunkZ > chunksViewDistance)
				ajustedCameraChunkZ += -sizeInChunks;
			if (cameraChunkZ - cameraChunkZ < -chunksViewDistance)
				ajustedCameraChunkZ += sizeInChunks;

			floodFillDeque.push(ajustedCameraChunkX);
			floodFillDeque.push(cameraChunkY);
			floodFillDeque.push(ajustedCameraChunkZ);
			floodFillDeque.push(-1);

			int verticalDistance = 8;
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
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][2]) && (ajustedChunkX - cameraChunkX) < chunksViewDistance && !floodFillMask.contains(new Vector3dm(chunkX + 1, chunkY, chunkZ)))
					{
						floodFillDeque.push(ajustedChunkX + 1);
						floodFillDeque.push(chunkY);
						floodFillDeque.push(ajustedChunkZ);
						floodFillDeque.push(0);
					}
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][0]) && -(ajustedChunkX - cameraChunkX) < chunksViewDistance && !floodFillMask.contains(new Vector3dm(chunkX - 1, chunkY, chunkZ)))
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

					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][1]) && (ajustedChunkZ - cameraChunkZ) < chunksViewDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY, chunkZ + 1)))
					{
						floodFillDeque.push(ajustedChunkX);
						floodFillDeque.push(chunkY);
						floodFillDeque.push(ajustedChunkZ + 1);
						floodFillDeque.push(3);
					}
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][3]) && -(ajustedChunkZ - cameraChunkZ) < chunksViewDistance && !floodFillMask.contains(new Vector3dm(chunkX, chunkY, chunkZ - 1)))
					{
						floodFillDeque.push(ajustedChunkX);
						floodFillDeque.push(chunkY);
						floodFillDeque.push(ajustedChunkZ - 1);
						floodFillDeque.push(1);
					}
				}
			}

			renderList.clear();
			for (Chunk chunk : floodFillSet)
			{
				if (chunk == null || !(chunk instanceof ChunkRenderable))
					continue;
				ChunkRenderable renderableChunk = (ChunkRenderable) chunk;

				if (LoopingMathHelper.moduloDistance(chunk.getChunkX(), cameraChunkX, world.getSizeInChunks()) <= chunksViewDistance)
					if (LoopingMathHelper.moduloDistance(chunk.getChunkZ(), cameraChunkZ, world.getSizeInChunks()) <= chunksViewDistance)
					{
						if (LoopingMathHelper.moduloDistance(chunk.getChunkX(), cameraChunkX, world.getSizeInChunks()) < chunksViewDistance - 1)
							if (LoopingMathHelper.moduloDistance(chunk.getChunkZ(), cameraChunkZ, world.getSizeInChunks()) < chunksViewDistance - 1)
							{
								if (!renderableChunk.isAirChunk() && renderableChunk.getChunkRenderData().getData() != null)
								//if ((renderableChunk.getChunkRenderData() != null && renderableChunk.getChunkRenderData().isUploaded()))
								{

									int ajustedChunkX = chunk.getChunkX();
									int ajustedChunkZ = chunk.getChunkZ();
									if (chunk.getChunkX() - cameraChunkX > chunksViewDistance)
										ajustedChunkX += -sizeInChunks;
									if (chunk.getChunkX() - cameraChunkX < -chunksViewDistance)
										ajustedChunkX += sizeInChunks;
									if (chunk.getChunkZ() - cameraChunkZ > chunksViewDistance)
										ajustedChunkZ += -sizeInChunks;
									if (chunk.getChunkZ() - cameraChunkZ < -chunksViewDistance)
										ajustedChunkZ += sizeInChunks;

									localMapCommands.put((byte) (ajustedChunkX - cameraChunkX));
									localMapCommands.put((byte) (ajustedChunkZ - cameraChunkZ));
									localMapCommands.put((byte) (chunk.getChunkY()));
									localMapCommands.put((byte) 0x00);

									localMapElements++;

								}
							}

						renderList.add(renderableChunk);
					}
			}
			//Sort front to back
			renderList.sort(new Comparator<ChunkRenderable>()
			{
				@Override
				public int compare(ChunkRenderable a, ChunkRenderable b)
				{
					int distanceA = LoopingMathHelper.moduloDistance(a.getChunkX(), cameraChunkX, world.getSizeInChunks()) + LoopingMathHelper.moduloDistance(a.getChunkZ(), cameraChunkZ, world.getSizeInChunks());
					int distanceB = LoopingMathHelper.moduloDistance(b.getChunkX(), cameraChunkX, world.getSizeInChunks()) + LoopingMathHelper.moduloDistance(b.getChunkZ(), cameraChunkZ, world.getSizeInChunks());
					return distanceA - distanceB;
				}
			});

			localMapCommands.flip();

			renderingContext.bindAttribute("vertexIn", new ByteBufferAttributeSource(localMapCommands, VertexFormat.BYTE, 3, 4));

			renderingContext.draw(Primitive.POINT, 0, localMapElements);
			//Two maps

			renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

			renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboLoadedChunksBot);
			//fboLoadedChunksBot.bind();

			renderingContext.draw(Primitive.POINT, 0, localMapElements);

			renderingContext.getRenderTargetManager().setCurrentRenderTarget(null);
			//FrameBufferObject.unbind();

			// Now delete from the worker threads what we won't need anymore
			chunksRenderer.purgeUselessWork(cameraChunkX, cameraChunkY, cameraChunkZ, sizeInChunks, chunksViewDistance);
			//farTerrainRenderer.uploadGeneratedMeshes();

			chunksChanged = false;
		}
		// Cleans free vbos
		//ChunkRenderData.deleteUselessVBOs();
	}

	public void shadowPass()
	{
		Client.profiler.startSection("shadows");

		if (this.getShadowVisibility() == 0f)
			return; // No shadows at night :)

		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

		int shadowMapTextureSize = shadowMapBuffer.getWidth();

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(shadowMapFBO);
		//shadowMapFBO.bind();
		renderingContext.getRenderTargetManager().clearBoundRenderTargetZ(1.0f);
		//glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		shadowsPassShader = renderingContext.useShader("shadows");

		int shadowRange = 128;
		if (shadowMapTextureSize > 1024)
			shadowRange = 192;
		else if (shadowMapTextureSize > 2048)
			shadowRange = 256;

		int shadowDepthRange = 200;
		Matrix4f depthProjectionMatrix = MatrixHelper.getOrthographicMatrix(-shadowRange, shadowRange, -shadowRange, shadowRange, -shadowDepthRange, shadowDepthRange);

		Matrix4f depthViewMatrix = MatrixHelper.getLookAtMatrix(skyRenderer.getSunPosition(), new Vector3fm(0, 0, 0), new Vector3fm(0, 1, 0));

		Matrix4f.mul(depthProjectionMatrix, depthViewMatrix, depthMatrix);
		Matrix4f shadowMVP = new Matrix4f(depthMatrix);

		//System.out.println(depthViewMatrix);

		shadowMVP.translate(new Vector3fm(camera.getCameraPosition()).negate());

		shadowsPassShader.setUniformMatrix4f("depthMVP", shadowMVP);

		shadowsPassShader.setUniform1f("allowForWavyStuff", 1);
		renderWorld(true);
	}

	public void renderTerrain(boolean ignoreWorldCulling)
	{
		// Terrain

		terrainShader = renderingContext.useShader("terrain");
		renderingContext.setBlendMode(BlendMode.DISABLED);
		camera.setupShader(terrainShader);
		skyRenderer.setupShader(terrainShader);

		terrainShader.setUniform3f("sunPos", skyRenderer.getSunPosition());
		terrainShader.setUniform1f("time", animationTimer);
		terrainShader.setUniform1f("terrainHeight", world.getRegionsSummariesHolder().getHeightAtWorldCoordinates((int) (double) camera.getCameraPosition().getX(), (int) (double) camera.getCameraPosition().getZ()));
		terrainShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
		terrainShader.setUniform1f("shadowVisiblity", getShadowVisibility());
		waterNormalTexture.setLinearFiltering(true);
		waterNormalTexture.setMipMapping(true);

		renderingContext.bindCubemap("environmentCubemap", environmentMap);
		renderingContext.bindTexture2D("sunSetRiseTexture", sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", skyTextureRaining);
		renderingContext.bindTexture2D("blockLightmap", lightmapTexture);
		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");

		renderingContext.bindTexture2D("lightColors", lightColors);
		renderingContext.bindTexture2D("normalTexture", waterNormalTexture);
		setupShadowColors(terrainShader);
		terrainShader.setUniform1f("time", skyRenderer.time);

		renderingContext.bindTexture2D("vegetationColorTexture", getGrassTexture());
		terrainShader.setUniform1f("mapSize", sizeInChunks * 32);
		renderingContext.bindTexture2D("loadedChunksMapTop", loadedChunksMapTop);
		renderingContext.bindTexture2D("loadedChunksMapBot", loadedChunksMapBot);

		terrainShader.setUniform2f("playerCurrentChunk", this.cameraChunkX, this.cameraChunkY);
		terrainShader.setUniform1f("ignoreWorldCulling", ignoreWorldCulling ? 1f : 0f);

		if (Keyboard.isKeyDown(Keyboard.KEY_F10))
			renderingContext.setPolygonFillMode(PolygonFillMode.WIREFRAME);

		//if (!(InputAbstractor.isKeyDown(org.lwjgl.input.Keyboard.KEY_F9) && RenderingConfig.isDebugAllowed))
		//	renderedVertices += farTerrainRenderer.drawTerrainBits(renderingContext, null, terrainShader);

		renderingContext.setPolygonFillMode(PolygonFillMode.FILL);

		renderingContext.flush();
	}

	public void renderWorld(boolean isShadowPass)
	{
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		animationTimer = (float) (((System.currentTimeMillis() % 100000) / 200f) % 100.0);

		Vector3fm sunPos = skyRenderer.getSunPosition();
		float shadowVisiblity = getShadowVisibility();
		int wrapChunksistance = sizeInChunks / 2;

		if (!isShadowPass)
		{
			renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboGBuffers);

			Client.profiler.startSection("blocks");
			this.fboGBuffers.setEnabledRenderTargets();

			renderingContext.useShader("blocks_opaque");
			
			//Set materials
			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			renderingContext.bindNormalTexture(blocksNormalTexture);
			renderingContext.bindMaterialTexture(blocksMaterialTexture);

			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			renderingContext.bindTexture2D("vegetationColorTexture", getGrassTexture());

			//Set texturing arguments
			blocksAlbedoTexture.setTextureWrapping(false);
			blocksAlbedoTexture.setLinearFiltering(false);
			blocksAlbedoTexture.setMipMapping(false);
			blocksAlbedoTexture.setMipmapLevelsRange(0, 4);

			blocksNormalTexture.setTextureWrapping(false);
			blocksNormalTexture.setLinearFiltering(false);
			blocksNormalTexture.setMipMapping(false);
			blocksNormalTexture.setMipmapLevelsRange(0, 4);

			blocksMaterialTexture.setTextureWrapping(false);
			blocksMaterialTexture.setLinearFiltering(false);
			blocksMaterialTexture.setMipMapping(false);
			blocksMaterialTexture.setMipmapLevelsRange(0, 4);

			//Shadows parameters
			opaqueBlocksShader.setUniform1f("shadowVisiblity", getShadowVisibility());

			//Camera-related stuff
			opaqueBlocksShader.setUniform2f("screenSize", scrW, scrH);

			//World stuff
			opaqueBlocksShader.setUniform1f("mapSize", sizeInChunks * 32);
			opaqueBlocksShader.setUniform1f("time", animationTimer);

			opaqueBlocksShader.setUniform1f("overcastFactor", world.getWeather());
			opaqueBlocksShader.setUniform1f("wetness", getWorldWetness());

			camera.setupShader(opaqueBlocksShader);

			renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		}
		else
		{
			shadowsPassShader.setUniform1f("time", animationTimer);

			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			renderingContext.setObjectMatrix(null);
		}

		renderingContext.setObjectMatrix(new Matrix4f());
		// Alpha blending is disabled because certain G-Buffer rendertargets can output a 0 for alpha
		//renderingContext.setIsShadowPass(isShadowPass);

		int chunksRendered = 0;
		for (ChunkRenderable chunk : renderList)
		{
			ChunkRenderDataHolder chunkRenderData = chunk.getChunkRenderData();
			chunksRendered++;
			
			int vboDekalX = 0;
			int vboDekalZ = 0;
			// Adjustements so border chunks show at the correct place.
			vboDekalX = chunk.getChunkX() * 32;
			vboDekalZ = chunk.getChunkZ() * 32;
			if (chunk.getChunkX() - cameraChunkX > wrapChunksistance)
				vboDekalX += -sizeInChunks * 32;
			if (chunk.getChunkX() - cameraChunkX < -wrapChunksistance)
				vboDekalX += sizeInChunks * 32;
			if (chunk.getChunkZ() - cameraChunkZ > wrapChunksistance)
				vboDekalZ += -sizeInChunks * 32;
			if (chunk.getChunkZ() - cameraChunkZ < -wrapChunksistance)
				vboDekalZ += sizeInChunks * 32;
			// Update if chunk was modified
			if ((chunk.isMarkedForReRender() || chunk.needsLightningUpdates()) && !chunk.isAirChunk())
				chunksRenderer.requestChunkRender(chunk);

			ChunkMeshDataSections data = chunk.getChunkRenderData().getData();
			if (data != null)
				data.isReady();

			// Don't bother if it don't render anything
			if (chunkRenderData == null)// || !chunkRenderData.isUploaded() || chunkRenderData.vboSizeFullBlocks + chunkRenderData.vboSizeCustomBlocks == 0)
				continue;

			// If we're doing shadows
			if (isShadowPass)
			{
				// TODO : make proper orthogonal view checks etc
				float distanceX = LoopingMathHelper.moduloDistance(cameraChunkX, chunk.getChunkX(), sizeInChunks);
				float distanceZ = LoopingMathHelper.moduloDistance(cameraChunkZ, chunk.getChunkZ(), sizeInChunks);

				int maxShadowDistance = 4;
				if (shadowMapResolution >= 2048)
					maxShadowDistance = 5;
				if (shadowMapResolution >= 4096)
					maxShadowDistance = 6;

				if (distanceX > maxShadowDistance || distanceZ > maxShadowDistance)
					continue;
			}
			else
			{
				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.getChunkY();
				int correctedCZ = vboDekalZ / 32;
				//Always show the chunk we're standing in no matter what
				boolean shouldShowChunk = ((int) (camera.getCameraPosition().getX() / 32) == chunk.getChunkX()) && ((int) (camera.getCameraPosition().getY() / 32) == correctedCY) && ((int) (camera.getCameraPosition().getZ() / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ);
				if (!shouldShowChunk)
					continue;
			}
			if (!isShadowPass)
			{
				Matrix4f matrix = new Matrix4f();
				matrix.translate(new Vector3fm(vboDekalX, chunk.getChunkY() * 32f, vboDekalZ));
				this.renderingContext.setObjectMatrix(matrix);
				opaqueBlocksShader.setUniform3f("objectPosition", vboDekalX, chunk.getChunkY() * 32f, vboDekalZ);
			}
			else
			{

				Matrix4f matrix = new Matrix4f();
				matrix.translate(new Vector3fm(vboDekalX, chunk.getChunkY() * 32f, vboDekalZ));
				this.renderingContext.setObjectMatrix(matrix);

			}

			if (Keyboard.isKeyDown(Keyboard.KEY_F4) && RenderingConfig.isDebugAllowed)
				continue;
			
			if (isShadowPass)
			{
				renderedVerticesShadow += chunkRenderData.renderPass(renderingContext, RenderLodLevel.HIGH, ShadingType.OPAQUE);
			}
			else
			{
				renderedChunks++;
				renderedVertices += chunkRenderData.renderPass(renderingContext, RenderLodLevel.HIGH, ShadingType.OPAQUE);
			}
			
			if (Keyboard.isKeyDown(Keyboard.KEY_F4) && (Client.username.equals("Alexix200")))
			{
				Ingame ig = ((Ingame) Client.getInstance().getGameWindow().getCurrentScene());
				ig.chat.insert("#60FF30Alexix sombre merde raclure de \ncheater incorrigible");
			}

		}

		renderingContext.flush();
		Client.profiler.startSection("entities");

		// Done looping chunks, now entities
		if (!isShadowPass)
		{

			// Select shader
			renderingContext.useShader("entities");

			//entitiesShader.setUniformMatrix4f("localTansform", new Matrix4f());
			//entitiesShader.setUniformMatrix3f("localTransformNormal", new Matrix3f());

			entitiesShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
			entitiesShader.setUniform1f("shadowVisiblity", shadowVisiblity);

			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			lightmapTexture.setTextureWrapping(false);
			entitiesShader.setUniform2f("screenSize", scrW, scrH);
			entitiesShader.setUniform3f("sunPos", sunPos.getX(), sunPos.getY(), sunPos.getZ());
			entitiesShader.setUniform3f("blockColor", 1f, 1f, 1f);
			entitiesShader.setUniform1f("time", animationTimer);

			entitiesShader.setUniform1f("overcastFactor", world.getWeather());
			entitiesShader.setUniform1f("wetness", getWorldWetness());

			renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
			renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);

			camera.setupShader(entitiesShader);

		}
		else
		{
			shadowsPassShader.setUniform1f("entity", 1);
		}

		renderingContext.setCullingMode(CullingMode.DISABLED);
		// Render entities

		if (!Keyboard.isKeyDown(Keyboard.KEY_F6) || !RenderingConfig.isDebugAllowed)
			entitiesRenderer.renderEntities(renderingContext);

		if (isShadowPass)
			return;

		//Add decals
		decalsRenderer.renderDecals(renderingContext);
		// Solid blocks done, now render water & lights

		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setCullingMode(CullingMode.DISABLED);

		// We do water in two passes : one for computing the refracted color and putting it in shaded buffer, and another one
		// to read it back and blend it
		for (int pass = 1; pass < 3; pass++)
		{
			liquidBlocksShader = renderingContext.useShader("blocks_liquid_pass" + (pass));

			liquidBlocksShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);

			liquidBlocksShader.setUniform1f("yAngle", (float) (camera.rotationY * Math.PI / 180f));
			liquidBlocksShader.setUniform1f("shadowVisiblity", shadowVisiblity);

			renderingContext.bindTexture2D("normalTextureDeep", TexturesHandler.getTexture("./textures/water/deep.png"));
			renderingContext.bindTexture2D("normalTextureShallow", waterNormalTexture);

			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			liquidBlocksShader.setUniform2f("screenSize", scrW, scrH);
			liquidBlocksShader.setUniform3f("sunPos", sunPos.getX(), sunPos.getY(), sunPos.getZ());
			liquidBlocksShader.setUniform1f("time", animationTimer);

			camera.setupShader(liquidBlocksShader);

			//Underwater flag
			Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData((int) (double) camera.getCameraPosition().getX(), (int) (double) camera.getCameraPosition().getY(), (int) (double) camera.getCameraPosition().getZ()));
			liquidBlocksShader.setUniform1f("underwater", vox.getType().isLiquid() ? 1 : 0);

			if (pass == 1)
			{
				renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
				//fboShadedBuffer.bind();
				fboShadedBuffer.setEnabledRenderTargets(true);
				renderingContext.bindTexture2D("readbackAlbedoBufferTemp", this.albedoBuffer);
				renderingContext.bindTexture2D("readbackMetaBufferTemp", this.materialBuffer);
				renderingContext.bindTexture2D("readbackDepthBufferTemp", this.zBuffer);

				renderingContext.getRenderTargetManager().setDepthMask(false);
				//glDepthMask(false);
			}
			else if (pass == 2)
			{
				renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboGBuffers);
				//fboGBuffers.bind();
				fboGBuffers.setEnabledRenderTargets();
				renderingContext.bindTexture2D("readbackShadedBufferTemp", this.shadedBuffer);
				renderingContext.bindTexture2D("readbackDepthBufferTemp", this.zBuffer);

				renderingContext.getRenderTargetManager().setDepthMask(true);
				//glDepthMask(true);
			}
			for (ChunkRenderable chunk : renderList)
			{
				ChunkRenderDataHolder chunkRenderData = chunk.getChunkRenderData();
				if (chunkRenderData == null)// || !chunkRenderData.isUploaded() || chunkRenderData.vboSizeWaterBlocks == 0)
					continue;

				int vboDekalX = chunk.getChunkX() * 32;
				int vboDekalZ = chunk.getChunkZ() * 32;

				if (chunk.getChunkX() - cameraChunkX > wrapChunksistance)
					vboDekalX += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.getChunkX() - cameraChunkX < -wrapChunksistance)
					vboDekalX += sizeInChunks * 32;
				if (chunk.getChunkZ() - cameraChunkZ > wrapChunksistance)
					vboDekalZ += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.getChunkZ() - cameraChunkZ < -wrapChunksistance)
					vboDekalZ += sizeInChunks * 32;

				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.getChunkY();
				int correctedCZ = vboDekalZ / 32;

				boolean shouldShowChunk = ((int) (camera.getCameraPosition().getX() / 32) == chunk.getChunkX()) && ((int) (camera.getCameraPosition().getY() / 32) == correctedCY) && ((int) (camera.getCameraPosition().getZ() / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ);
				if (!shouldShowChunk)
					continue;

				liquidBlocksShader.setUniform3f("objectPosition", vboDekalX, chunk.getChunkY() * 32, vboDekalZ);

				renderedVertices += chunkRenderData.renderPass(renderingContext, RenderLodLevel.HIGH, ShadingType.LIQUIDS);
			}
		}

		// Particles rendering
		((ParticlesRenderer) this.world.getParticlesManager()).render(renderingContext);
		this.renderingContext.flush();

		// Draw world shaded with sunlight and vertex light
		//glDepthMask(false);
		renderShadedBlocks();
		//glDepthMask(true);

		// Compute SSAO
		if (RenderingConfig.ssaoQuality > 0)
			this.SSAO(RenderingConfig.ssaoQuality);

		renderLightsDeffered();
		//renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
		//fboShadedBuffer.bind();
		//fboShadedBuffer.setEnabledRenderTargets(true);

		Client.profiler.startSection("terrain");
		renderTerrain(false);
	}

	private void renderLightsDeffered()
	{
		Client.profiler.startSection("lights");

		//We work on the shaded buffer
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
		//this.fboShadedBuffer.bind();
		// Deffered lightning
		// Disable depth read/write

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.getRenderTargetManager().setDepthMask(false);
		//glDepthMask(false);

		lightShader = renderingContext.useShader("light");

		//Required info
		renderingContext.bindTexture2D("depthBuffer", this.zBuffer);
		renderingContext.bindTexture2D("diffuseBuffer", this.albedoBuffer);
		renderingContext.bindTexture2D("normalBuffer", this.normalBuffer);

		//Parameters
		lightShader.setUniform1f("powFactor", 5f);
		camera.setupShader(lightShader);
		//Blend parameters

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.ADD);

		renderingContext.getLightsRenderer().renderPendingLights(renderingContext);
		//Cleanup
		renderingContext.getRenderTargetManager().setDepthMask(true);
		//glDepthMask(true);

		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
	}

	/**
	 * Uses G-Buffers data to spit out shaded solid blocks ( shadows etc )
	 */
	public void renderShadedBlocks()
	{
		applyShadowsShader = renderingContext.useShader("shadows_apply");
		setupShadowColors(applyShadowsShader);

		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", getWorldWetness());

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);

		Vector3fm sunPos = skyRenderer.getSunPosition();

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
		//fboShadedBuffer.bind();

		float lightMultiplier = 1.0f;

		applyShadowsShader.setUniform1f("brightnessMultiplier", lightMultiplier);

		renderingContext.bindTexture2D("albedoBuffer", albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", zBuffer);
		renderingContext.bindTexture2D("normalBuffer", normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", materialBuffer);
		renderingContext.bindTexture2D("blockLightmap", lightmapTexture);
		renderingContext.bindTexture2D("shadowMap", shadowMapBuffer);

		renderingContext.bindTexture2D("sunSetRiseTexture", sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", skyTextureRaining);

		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");
		renderingContext.bindTexture2D("lightColors", lightColors);

		//TODO if SSAO
		renderingContext.bindTexture2D("ssaoBuffer", ssaoBuffer);

		renderingContext.bindCubemap("environmentCubemap", environmentMap);

		applyShadowsShader.setUniform1f("time", skyRenderer.time);

		applyShadowsShader.setUniform1f("shadowMapResolution", shadowMapResolution);
		applyShadowsShader.setUniform1f("shadowVisiblity", getShadowVisibility());
		applyShadowsShader.setUniformMatrix4f("shadowMatrix", depthMatrix);
		applyShadowsShader.setUniform3f("sunPos", sunPos);

		// Matrices for screen-space transformations
		camera.setupShader(applyShadowsShader);
		skyRenderer.setupShader(applyShadowsShader);

		renderingContext.getRenderTargetManager().setDepthMask(false);
		renderingContext.drawFSQuad();
		renderingContext.getRenderTargetManager().setDepthMask(true);

		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
	}

	//Post-process effects

	private int frameLumaDownloadDelay = 0;
	private final static int FRAME_DELAY_FOR_LUMA_MIP_GRAB = 2;
	private Vector3fm averageColorForAllFrame = new Vector3fm(1.0);

	private int downloadSize;
	/**
	 * Renders the final image to the screen
	 */
	public void blitScreen(float pauseFade)
	{
		if (RenderingConfig.doBloom)
		{
			shadedBuffer.setMipMapping(true);

			int maxMipLevel = shadedBuffer.getMaxMipmapLevel();
			shadedBuffer.setMipmapLevelsRange(0, maxMipLevel);

			int divisor = 1 << maxMipLevel;
			int mipWidth = shadedBuffer.getWidth() / divisor;
			int mipHeight = shadedBuffer.getHeight() / divisor;

			if (illuminationDownloadInProgress == null)
			{
				//Start mipmap calculation for next frame
				shadedBuffer.computeMipmaps();
				frameLumaDownloadDelay = FRAME_DELAY_FOR_LUMA_MIP_GRAB + 6;

				//To avoid crashing when the windows size change, we send the PBO copy request immediately and instead wait to look for it
				this.illuminationDownloadInProgress = this.illuminationDownloader.copyTexure(shadedBuffer, maxMipLevel);

				//We remember the size of the data to expect since that may change due to windows resizes and whatnot
				downloadSize = mipWidth * mipHeight;
			}

			if (illuminationDownloadInProgress != null && illuminationDownloadInProgress.isTraversable())
			{
				//Wait a few frames before actually reading the PBO
				if (frameLumaDownloadDelay > 0)
					frameLumaDownloadDelay--;
				else
				{

					ByteBuffer minMipmapBuffer = illuminationDownloadInProgress.readPBO();
					minMipmapBuffer.flip();

					//System.out.println("Obtained: "+minMipmapBuffer);
					averageColorForAllFrame = new Vector3fm(0.0);
					for (int i = 0; i < downloadSize; i++)
						averageColorForAllFrame.add(minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat());

					averageColorForAllFrame.scale(1.0f / downloadSize);
					//System.out.println(averageColorForAllFrame);

					//Throw that out
					illuminationDownloadInProgress = null;
				}
			}

			//Do continous luma adapation
			float luma = averageColorForAllFrame.getX() * 0.2125f + averageColorForAllFrame.getY() * 0.7154f + averageColorForAllFrame.getZ() * 0.0721f;

			luma *= apertureModifier;
			luma = (float) Math.pow(luma, 1d / 2.2);

			float targetLuma = 0.65f;
			float lumaMargin = 0.15f;

			if (luma < targetLuma - lumaMargin)
			{
				if (apertureModifier < 2.0)
					apertureModifier *= 1.001;
			}
			else if (luma > targetLuma + lumaMargin)
			{
				if (apertureModifier > 0.99)
					apertureModifier *= 0.999;
			}
			else
			{
				float clamped = (float) Math.min(Math.max(1 / apertureModifier, 0.998), 1.002);
				apertureModifier *= clamped;
			}
			shadedBuffer.setMipmapLevelsRange(0, 0);
			
			this.renderBloom_();
		}
		else
			apertureModifier = 1.0f;

		// We render to the screen.
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(null);

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		postProcess = renderingContext.useShader("postprocess");

		renderingContext.bindTexture2D("shadedBuffer", this.shadedBuffer);
		renderingContext.bindTexture2D("albedoBuffer", this.albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", this.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", this.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", this.materialBuffer);
		renderingContext.bindTexture2D("shadowMap", this.shadowMapBuffer);
		renderingContext.bindTexture2D("bloomBuffer", this.bloomBuffer);
		renderingContext.bindTexture2D("ssaoBuffer", this.ssaoBuffer);
		renderingContext.bindTexture2D("pauseOverlayTexture", TexturesHandler.getTexture("./textures/gui/darker.png"));
		//renderingContext.bindTexture2D("debugBuffer", (System.currentTimeMillis() % 1000 < 500) ? this.loadedChunksMapTop : this.loadedChunksMapBot);
		renderingContext.bindTexture2D("debugBuffer", this.materialBuffer);

		Voxel vox = VoxelsStore.get().getVoxelById(world.getVoxelData(camera.getCameraPosition()));
		postProcess.setUniform1f("underwater", vox.getType().isLiquid() ? 1 : 0);
		postProcess.setUniform1f("time", animationTimer);
		postProcess.setUniform1f("pauseOverlayFade", pauseFade);

		camera.setupShader(postProcess);

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

	private void SSAO(int quality)
	{
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboSSAO);
		//fboSSAO.bind();
		fboSSAO.setEnabledRenderTargets();

		renderingContext.useShader("ssao");

		renderingContext.bindTexture2D("normalTexture", this.normalBuffer);
		renderingContext.bindTexture2D("deptBuffer", this.zBuffer);

		ssaoShader.setUniform1f("viewWidth", scrW);
		ssaoShader.setUniform1f("viewHeight", scrH);

		ssaoShader.setUniform1i("kernelsPerFragment", quality * 8);

		camera.setupShader(ssaoShader);

		if (ssao_kernel == null)
		{
			ssao_kernel_size = 16;//applyShadowsShader.getConstantInt("KERNEL_SIZE"); nvm too much work
			ssao_kernel = new Vector3fm[ssao_kernel_size];
			for (int i = 0; i < ssao_kernel_size; i++)
			{
				Vector3fm vec = new Vector3fm((float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f, (float) Math.random());
				vec.normalize();
				float scale = ((float) i) / ssao_kernel_size;
				scale = Math2.mix(0.1f, 1.0f, scale * scale);
				vec.scale(scale);
				ssao_kernel[i] = vec;
				if (RenderingConfig.debugPasses)
					System.out.println("lerp " + scale + "x " + vec.getX());
			}
		}

		for (int i = 0; i < ssao_kernel_size; i++)
		{
			ssaoShader.setUniform3f("ssaoKernel[" + i + "]", ssao_kernel[i].getX(), ssao_kernel[i].getY(), ssao_kernel[i].getZ());
		}

		renderingContext.drawFSQuad();

		// Blur the thing

		// Vertical pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBlur);
		//fboBlur.bind();

		blurV = renderingContext.useShader("blurV");
		blurV.setUniform2f("screenSize", scrW, scrH);
		blurV.setUniform1f("lookupScale", 2);
		renderingContext.bindTexture2D("inputTexture", this.ssaoBuffer);
		renderingContext.drawFSQuad();

		// Horizontal pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboSSAO);
		//this.fboSSAO.bind();

		blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", scrW, scrH);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		renderingContext.drawFSQuad();
	}

	private void renderBloom_()
	{
		this.shadedBuffer.setLinearFiltering(true);
		this.bloomBuffer.setLinearFiltering(true);
		this.blurIntermediateBuffer.setLinearFiltering(true);

		bloomShader = renderingContext.useShader("bloom");

		renderingContext.bindTexture2D("shadedBuffer", this.shadedBuffer);
		bloomShader.setUniform1f("apertureModifier", apertureModifier);
		bloomShader.setUniform2f("screenSize", scrW / 2f, scrH / 2f);

		//int max_mipmap = (int) (Math.ceil(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
		//bloomShader.setUniform1f("max_mipmap", max_mipmap);

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBloom);
		//this.fboBloom.bind();
		this.fboBloom.setEnabledRenderTargets();
		renderingContext.drawFSQuad();

		// Blur bloom
		// Vertical pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBlur);
		//fboBlur.bind();

		blurV = renderingContext.useShader("blurV");
		blurV.setUniform2f("screenSize", scrW / 2f, scrH / 2f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", this.bloomBuffer);
		renderingContext.drawFSQuad();

		// Horizontal pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBloom);
		//this.fboBloom.bind();

		blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", scrW / 2f, scrH / 2f);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		renderingContext.drawFSQuad();

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBlur);
		//fboBlur.bind();

		blurV = renderingContext.useShader("blurV");

		blurV.setUniform2f("screenSize", scrW / 4f, scrH / 4f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", this.bloomBuffer);
		renderingContext.drawFSQuad();

		// Horizontal pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboBloom);
		//this.fboBloom.bind();

		blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", scrW / 4f, scrH / 4f);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		renderingContext.drawFSQuad();
	}

	/**
	 * Renders the whole scene into either a cubemap or saved on disk
	 * 
	 * @param resolution
	 * @param cubemap
	 *            the cubemap to render to, or null to save to disk
	 */
	public void renderWorldCubemap(Cubemap cubemap, int resolution, boolean onlyTerrain)
	{
		lastEnvmapRender = System.currentTimeMillis();
		Client.profiler.startSection("cubemap");

		boolean useFastBuffer = true;

		// Save state
		boolean oldBloom = RenderingConfig.doBloom;
		float oldViewDistance = RenderingConfig.viewDistance;
		RenderingConfig.doBloom = false;
		int oldW = scrW;
		int oldH = scrH;
		float camX = camera.rotationX;
		float camY = camera.rotationY;
		float camZ = camera.rotationZ;
		float fov = camera.fov;
		camera.fov = 45;
		// Setup cubemap resolution

		if (!useFastBuffer)
			this.setupRenderSize(resolution, resolution);
		else
		{
			scrW = resolution;
			scrH = resolution;
		}

		String[] names = { "front", "back", "top", "bottom", "right", "left" };

		String time = null;
		if (cubemap == null)
		{
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			time = sdf.format(cal.getTime());
		}

		for (int z = 0; z < 6; z++)
		{
			// Camera location
			switch (z)
			{
			case 0:
				camera.rotationX = 0.0f;
				camera.rotationY = 0f;
				break;
			case 1:
				camera.rotationX = 0;
				camera.rotationY = 180;
				break;
			case 2:
				camera.rotationX = -90;
				camera.rotationY = 0;
				break;
			case 3:
				camera.rotationX = 90;
				camera.rotationY = 0;
				break;
			case 4:
				camera.rotationX = 0;
				camera.rotationY = 90;
				break;
			case 5:
				camera.rotationX = 0;
				camera.rotationY = 270;
				break;
			}

			if (useFastBuffer)
				renderingContext.getRenderTargetManager().setCurrentRenderTarget(environmentMapFastFbo);
			//environmentMapFastFbo.bind();
			else
				renderingContext.getRenderTargetManager().setCurrentRenderTarget(fboShadedBuffer);
			//this.fboShadedBuffer.bind();

			renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();
			//glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Scene rendering
			if (useFastBuffer)
			{
				camera.setupUsingScreenSize(scrW, scrH);

				//Draw sky
				skyRenderer.time = (world.getTime() % 10000) / 10000f;
				skyRenderer.render(renderingContext);

				this.renderTerrain(true);
			}
			else
				this.renderWorldAtCameraInternal();

			if (cubemap != null)
			{
				int t[] = new int[] { 4, 5, 3, 2, 0, 1 };
				int f = t[z];

				renderingContext.useShader("blit");

				renderingContext.getRenderTargetManager().setCurrentRenderTarget(environmentMapFBO);
				//this.environmentMapFBO.bind();
				this.environmentMapFBO.setColorAttachement(0, cubemap.getFace(f));

				if (useFastBuffer)
					renderingContext.bindTexture2D("diffuseTexture", environmentMapBufferHDR);
				else
					renderingContext.bindTexture2D("diffuseTexture", shadedBuffer);

				renderingContext.currentShader().setUniform2f("screenSize", resolution, resolution);

				renderingContext.drawFSQuad();
			}
			else
			{
				// GL access
				//glBindTexture(GL_TEXTURE_2D, shadedBuffer.getId());
				glBindTexture(GL_TEXTURE_2D, environmentMapBufferHDR.getId());
				//shadedBuffer.bind();

				// File access
				File image = new File(GameDirectory.getGameFolderPath() + "/skyboxscreens/" + time + "/" + names[z] + ".png");
				image.mkdirs();

				ByteBuffer bbuf = ByteBuffer.allocateDirect(scrW * scrH * 4 * 4).order(ByteOrder.nativeOrder());
				System.out.println(bbuf);
				glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_FLOAT, bbuf);
				System.out.println(bbuf);

				BufferedImage pixels = new BufferedImage(scrW, scrH, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < scrW; x++)
					for (int y = 0; y < scrH; y++)
					{
						int i = 4 * (x + scrW * y);
						int r = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int g = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int b = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 8)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						pixels.setRGB(x, scrH - 1 - y, (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | b & 0xFF);

						//System.out.println(bbuf.getFloat(i * 4));
					}
				try
				{
					ImageIO.write(pixels, "PNG", image);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

		}

		// Revert previous data
		RenderingConfig.viewDistance = oldViewDistance;
		RenderingConfig.doBloom = oldBloom;
		camera.rotationX = camX;
		camera.rotationY = camY;
		camera.rotationZ = camZ;
		camera.fov = fov;
		//camera.setupUsingScreenSize(oldW, oldH);

		if (!useFastBuffer)
			this.setupRenderSize(oldW, oldH);
		else
		{
			scrW = oldW;
			scrH = oldH;
		}
	}

	/**
	 * Takes a screenshot and saves it.
	 * 
	 * @return
	 */
	public String screenShot()
	{
		ByteBuffer bbuf = ByteBuffer.allocateDirect(scrW * scrH * 4).order(ByteOrder.nativeOrder());

		glReadBuffer(GL_FRONT);
		glReadPixels(0, 0, scrW, scrH, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());

		File image = new File(GameDirectory.getGameFolderPath() + "/screenshots/" + time + ".png");

		image.mkdirs();

		BufferedImage pixels = new BufferedImage(scrW, scrH, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < scrW; x++)
			for (int y = 0; y < scrH; y++)
			{
				int i = 4 * (x + scrW * y);
				int r = bbuf.get(i) & 0xFF;
				int g = bbuf.get(i + 1) & 0xFF;
				int b = bbuf.get(i + 2) & 0xFF;
				pixels.setRGB(x, scrH - 1 - y, (0xFF << 24) | (r << 16) | (g << 8) | b);
			}
		try
		{
			ImageIO.write(pixels, "PNG", image);
			return "#FFFF00Saved screenshot as " + time + ".png";
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "#FF0000Failed to take screenshot ! (" + e.toString() + ")";
		}
	}

	//Visual properties functions
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

	private void setupShadowColors(ShaderInterface shader)
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

	private float getShadowVisibility()
	{
		float worldTime = (world.getTime() % 10000 + 10000) % 10000;
		int start = 2500;
		int end = 7500;
		if (worldTime < start || worldTime > end + 500)
			return 0;
		else if (worldTime < start + 500)
			return (worldTime - start) / 500f;
		else if (worldTime > end)
			return 1 - (worldTime - end) / 500f;
		else
			return 1;
	}

	//Math helper functions
	private boolean checkChunkOcclusion(Chunk chunk, int correctedCX, int correctedCY, int correctedCZ)
	{
		Vector3fm center = new Vector3fm(correctedCX * 32 + 16, correctedCY * 32 + 15, correctedCZ * 32 + 16);
		return camera.isBoxInFrustrum(center, new Vector3fm(32, 32, 32));
	}

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

	public void destroy()
	{
		skyRenderer.destroy();
		particlesRenderer.destroy();
		farTerrainRenderer.destroy();
		entitiesRenderer.clearLoadedEntitiesRenderers();
		chunksRenderer.destroy();
	}

	public WorldClientCommon getWorld()
	{
		return world;
	}

	public DecalsRenderer getDecalsRenderer()
	{
		return decalsRenderer;
	}

	public FarTerrainRenderer getFarTerrainRenderer()
	{
		return farTerrainRenderer;
	}

	public ParticlesRenderer getParticlesRenderer()
	{
		return particlesRenderer;
	}

	public SkyRenderer getSky()
	{
		return skyRenderer;
	}

	public void reloadContentSpecificStuff()
	{
		farTerrainRenderer.markVoxelTexturesSummaryDirty();
		entitiesRenderer.clearLoadedEntitiesRenderers();

		blocksAlbedoTexture = Client.getInstance().getContent().voxels().textures().getDiffuseAtlasTexture();
		blocksNormalTexture = Client.getInstance().getContent().voxels().textures().getNormalAtlasTexture();
		blocksMaterialTexture = Client.getInstance().getContent().voxels().textures().getMaterialAtlasTexture();
	}

	public WorldEffectsRenderer getWorldEffectsRenderer()
	{
		return this.weatherEffectsRenderer;
	}
}
