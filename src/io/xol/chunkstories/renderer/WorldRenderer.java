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

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;

import io.xol.engine.base.InputAbstractor;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fbo.FrameBufferObject;
import io.xol.engine.graphics.geometry.ByteBufferAttributeSource;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.GBufferTexture;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureFormat;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.PBOPacker;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.math.Math2;
import io.xol.engine.math.MatrixHelper;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.renderer.chunks.ChunkRenderable;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRenderer;
import io.xol.chunkstories.renderer.lights.LightsRenderer;
import io.xol.chunkstories.renderer.sky.SkyRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainRenderer;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityHUD;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.PolygonFillMode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldRenderer
{
	// World pointer
	WorldClientCommon world;

	// Worker thread
	public ChunksRenderer chunksRenderer;

	//Chunk space position
	public int cameraChunkX, cameraChunkY, cameraChunkZ;

	// camera object ( we need it so much)
	private Camera camera;

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
	private GBufferTexture shadedBuffer = new GBufferTexture(RGB_HDR, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private int illDownIndex = 0;
	private int illDownBuffers = 1;
	private long lastIllCalc = 8;
	private PBOPacker illuminationDownloader[] = new PBOPacker[illDownBuffers];

	// G-Buffers
	public GBufferTexture zBuffer = new GBufferTexture(DEPTH_RENDERBUFFER, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private GBufferTexture albedoBuffer = new GBufferTexture(RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private GBufferTexture normalBuffer = new GBufferTexture(RGBA_3x10_2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private GBufferTexture materialBuffer = new GBufferTexture(RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);

	// Bloom texture
	private GBufferTexture bloomBuffer = new GBufferTexture(RGB_HDR, GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
	private GBufferTexture ssaoBuffer = new GBufferTexture(RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);

	// FBOs
	private FrameBufferObject fboGBuffers = new FrameBufferObject(zBuffer, albedoBuffer, normalBuffer, materialBuffer);

	private FrameBufferObject fboShadedBuffer = new FrameBufferObject(zBuffer, shadedBuffer);
	private FrameBufferObject fboBloom = new FrameBufferObject(null, bloomBuffer);
	private FrameBufferObject fboSSAO = new FrameBufferObject(null, ssaoBuffer);

	private GBufferTexture blurIntermediateBuffer = new GBufferTexture(RGB_HDR, GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
	private FrameBufferObject fboBlur = new FrameBufferObject(null, blurIntermediateBuffer);

	// 64x64 texture used to cull distant mesh
	private GBufferTexture loadedChunksMapTop = new GBufferTexture(DEPTH_RENDERBUFFER, 64, 64);
	private FrameBufferObject fboLoadedChunksTop = new FrameBufferObject(loadedChunksMapTop);
	private GBufferTexture loadedChunksMapBot = new GBufferTexture(DEPTH_RENDERBUFFER, 64, 64);
	private FrameBufferObject fboLoadedChunksBot = new FrameBufferObject(loadedChunksMapBot);

	// Shadow maps
	private int shadowMapResolution = 0;
	private GBufferTexture shadowMapBuffer = new GBufferTexture(DEPTH_SHADOWMAP, 256, 256);
	private FrameBufferObject shadowMapFBO = new FrameBufferObject(shadowMapBuffer);

	//Environment map
	private int ENVMAP_SIZE = 128;
	private Cubemap environmentMap = new Cubemap(TextureFormat.RGB_HDR, ENVMAP_SIZE);
	//private Cubemap environmentMapBlurry = new Cubemap(TextureType.RGB_HDR, ENVMAP_SIZE);
	//Temp buffers
	private GBufferTexture environmentMapBufferHDR = new GBufferTexture(RGB_HDR, ENVMAP_SIZE, ENVMAP_SIZE);
	private GBufferTexture environmentMapBufferZ = new GBufferTexture(DEPTH_RENDERBUFFER, ENVMAP_SIZE, ENVMAP_SIZE);

	private FrameBufferObject environmentMapFastFbo = new FrameBufferObject(environmentMapBufferZ, environmentMapBufferHDR);
	private FrameBufferObject environmentMapFBO = new FrameBufferObject(null, environmentMap.getFace(0));

	// Shadow transformation matrix
	private Matrix4f depthMatrix = new Matrix4f();

	//Entities
	private EntitiesRenderer entitiesRenderer;
	
	// Sky
	private SkyRenderer sky;

	// Decals
	private DecalsRenderer decalsRenderer;

	//Far terrain mesher
	private FarTerrainRenderer farTerrainRenderer;

	//Rain snow etc
	public WeatherEffectsRenderer weatherEffectsRenderer;

	//For shaders animations
	float animationTimer = 0.0f;

	//Counters
	public int renderedVertices = 0;
	public int renderedVerticesShadow = 0;
	public int renderedChunks = 0;

	//Bloom avg color buffer
	ByteBuffer shadedMipmapZeroLevelColor = null;//BufferUtils.createByteBuffer(4 * 3);
	//Bloom aperture
	float apertureModifier = 1f;

	//Sky stuff
	Texture2D sunGlowTexture = TexturesHandler.getTexture("environement/glow.png");
	Texture2D skyTextureSunny = TexturesHandler.getTexture("environement/sky.png");
	Texture2D skyTextureRaining = TexturesHandler.getTexture("environement/sky_rain.png");

	Texture2D lightmapTexture = TexturesHandler.getTexture("environement/light.png");
	Texture2D waterNormalTexture = TexturesHandler.getTexture("water/shallow.png");

	//Blocks atlases
	Texture2D blocksAlbedoTexture = TexturesHandler.getTexture("tiles_merged_albedo.png");
	Texture2D blocksNormalTexture = TexturesHandler.getTexture("tiles_merged_normal.png");
	Texture2D blocksMaterialTexture = TexturesHandler.getTexture("tiles_merged_material.png");

	//SSAO (disabled)
	Vector3f ssao_kernel[];
	int ssao_kernel_size;
	int ssao_noiseTex = -1;

	//8K buffer of 2D coordinates to draw the local map
	ByteBuffer localMapCommands = BufferUtils.createByteBuffer(8192);

	long lastEnvmapRender = 0L;

	//Constructor and modificators

	public WorldRenderer(WorldClientCommon w)
	{
		// Link world
		world = w;
		world.linkWorldRenderer(this);
		entitiesRenderer = new EntitiesRenderer(world);
		farTerrainRenderer = new FarTerrainRenderer(world);
		weatherEffectsRenderer = new WeatherEffectsRenderer(world, this);
		sky = new SkyRenderer(world, this);
		decalsRenderer = new DecalsRenderer(this);
		sizeInChunks = world.getSizeInChunks();
		resizeShadowMaps();

		renderingContext = GameWindowOpenGL.getInstance().getRenderingContext();
		GameWindowOpenGL.instance.renderingContext = renderingContext;

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

		for (int i = 0; i < illDownBuffers; i++)
			illuminationDownloader[i] = new PBOPacker();

		chunksRenderer = new ChunksRenderer(world);
		chunksRenderer.start();
	}

	public void setupRenderSize(int width, int height)
	{
		scrW = width;
		scrH = height;
		this.fboGBuffers.resizeFBO(width, height);
		this.fboShadedBuffer.resizeFBO(width, height);
		// this.composite_pass_gbuffers_waterfp.resizeFBO(width, height);
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

	public void renderWorldAtCamera(Camera camera)
	{
		Client.profiler.startSection("kekupdates");
		this.camera = camera;
		if (RenderingConfig.doDynamicCubemaps)// && (System.currentTimeMillis() - lastEnvmapRender) > 2000L)// * Math.pow(30.0f / XolioWindow.getFPS(), 1.0f))
			renderWorldCubemap(environmentMap, ENVMAP_SIZE, true);
		renderWorldAtCameraInternal(camera, -1);
	}

	public void renderWorldAtCameraInternal(Camera camera, int chunksToRenderLimit)
	{
		// Set camera
		this.camera = camera;
		OverlayRenderer.setCamera(camera);
		// Debug/Dev : reset vertex counts
		renderedChunks = 0;
		renderedVertices = 0;
		renderedVerticesShadow = 0;

		Client.profiler.startSection("updates");
		// Load/Unload required parts of the world, update the display list etc
		updateRender(camera);

		Client.profiler.startSection("next");
		// Shadows pre-pass
		if (RenderingConfig.doShadows && chunksToRenderLimit == -1)
			shadowPass();
		// Prepare matrices
		camera.justSetup(scrW, scrH);
		camera.translate();

		
		
		// Clear G-Buffers and bind shaded HDR rendertarget
		fboGBuffers.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		fboShadedBuffer.bind();

		// Draw sky
		if (RenderingConfig.debugPasses)
			glFinish();
		long t = System.nanoTime();
		sky.time = (world.worldTime % 10000) / 10000f;
		//sky.skyShader.use(true);
		//sky.skyShader.setUniformSamplerCubemap(7, "environmentCubemap", environmentMap);
		//glViewport(0, 0, scrW, scrH);
		sky.render(renderingContext);

		if (RenderingConfig.debugPasses)
			glFinish();
		if (RenderingConfig.debugPasses)
			System.out.println("sky took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		// Move camera to relevant position
		// fboGBuffers.setEnabledRenderTargets(true, false, false);

		fboGBuffers.bind();
		fboGBuffers.setEnabledRenderTargets();
		
		// Render world
		renderWorld(false, chunksToRenderLimit);

		// Render weather
		fboShadedBuffer.bind();
		fboShadedBuffer.setEnabledRenderTargets();
		weatherEffectsRenderer.renderEffects(renderingContext);

		// Debug
		if (RenderingConfig.debugPasses)
			glFinish();
		if (RenderingConfig.debugPasses)
			System.out.println("total took " + (System.nanoTime() - t) / 1000000.0 + "ms ( " + 1 / ((System.nanoTime() - t) / 1000000000.0) + " fps)");

		//Disable depth check
		//glDisable(GL_DEPTH_TEST);

		// Do bloom
		if (RenderingConfig.doBloom)
			renderBloom();

		//Bind shaded buffer in case some other rendering is to be done
		fboShadedBuffer.bind();
		fboShadedBuffer.setEnabledRenderTargets();

		Client.profiler.startSection("done");
	}

	//Rendering passes

	/**
	 * Pre-rendering function : Uploads the finished meshes from ChunksRenderer; Grabs all the loaded chunks and add them to the rendering queue Updates far terrain meshes if needed Draws the chunksLoadedMap
	 * 
	 * @param camera
	 */
	public void updateRender(Camera camera)
	{
		Vector3d pos = new Vector3d(camera.pos).negate();
		// Called every frame, this method takes care of updating the world :
		// It will keep up to date the camera position, as well as a list of
		// to-render chunks in order to fill empty VBO space
		// Upload generated chunks data to GPU
		//updateProfiler.reset("vbo upload");
		ChunkRenderData chunkRenderData = chunksRenderer.getNextRenderedChunkData();
		while (chunkRenderData != null)
		{
			//CubicChunk c = world.getChunk(toload.x, toload.y, toload.z, false);
			CubicChunk c = chunkRenderData.chunk;
			if (c != null && c instanceof ChunkRenderable)
			{
				((ChunkRenderable) c).setChunkRenderData(chunkRenderData);

				//Upload data
				
				//chunkRenderData.upload();
				
				chunksChanged = true;
			}
			else
			{
				if (RenderingConfig.debugPasses)
					System.out.println("ChunkRenderer outputted a chunk render for a not loaded chunk : ");
				//if (FastConfig.debugPasses)
				//	System.out.println("Chunks coordinates : X=" + toload.x + " Y=" + toload.y + " Z=" + toload.z);
				//if (FastConfig.debugPasses)
				//	System.out.println("Render information : vbo size =" + toload.s_normal + " and water size =" + toload.s_water);
				chunkRenderData.free();
			}
			chunkRenderData = chunksRenderer.getNextRenderedChunkData();
		}
		// Update view
		//viewRotH = view_rotx;
		//viewRotV = view_roty;
		int newCX = Math2.floor((pos.getX()) / 32);
		int newCY = Math2.floor((pos.getY()) / 32);
		int newCZ = Math2.floor((pos.getZ()) / 32);
		// Fill the VBO array with chunks VBO ids if the player changed chunk
		
		if (cameraChunkX != newCX || cameraChunkY != newCY || cameraChunkZ != newCZ || chunksChanged)
		{
			farTerrainRenderer.markFarTerrainMeshDirty();
			//if (newCX != cameraChunkX || newCZ != cameraChunkZ)
			//	farTerrainRenderer.startAsynchSummaryRegeneration(camera);
			
			//Updates current chunk location
			cameraChunkX = newCX;
			cameraChunkY = newCY;
			cameraChunkZ = newCZ;
			int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);

			
			// Unload too far chunks
			//updateProfiler.startSection("unloadFar");
			//long usageBefore = Runtime.getRuntime().freeMemory();

			//Iterates over all loaded chunks to generate list and map
			fboLoadedChunksBot.bind();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			fboLoadedChunksTop.bind();
			//glViewport(0, 0, 64, 64);
			glClearDepth(0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glClearDepth(1f);
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
			renderingContext.setBlendMode(BlendMode.ALPHA_TEST);
			renderingContext.setDepthTestMode(DepthTestMode.GREATER_OR_EQUAL);
			
			
			/*glEnable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_ALPHA_TEST);
			glDepthFunc(GL_GEQUAL);*/
			int localMapElements = 0;

			Set<Chunk> floodFillSet = new HashSet<Chunk>();
			Set<Vector3d> floodFillMask = new HashSet<Vector3d>();

			Deque<Integer> deque = new ArrayDeque<Integer>();

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

			deque.push(ajustedCameraChunkX);
			deque.push(cameraChunkY);
			deque.push(ajustedCameraChunkZ);
			deque.push(-1);

			int verticalDistance = 8;
			while (!deque.isEmpty())
			{
				int sideFrom = deque.pop();
				int chunkZ = deque.pop();
				int chunkY = deque.pop();
				int chunkX = deque.pop();
				//sideFrom = -1;

				int ajustedChunkX = chunkX;
				int ajustedChunkZ = chunkZ;

				Chunk chunk = world.getChunk(chunkX, chunkY, chunkZ);

				if (floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ)))
					continue;
				floodFillMask.add(new Vector3d(chunkX, chunkY, chunkZ));

				if (chunk != null)
				{
					if (chunk == null || chunk.isAirChunk())
						sideFrom = -1;

					floodFillSet.add(chunk);
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][2]) && (ajustedChunkX - cameraChunkX) < chunksViewDistance && !floodFillMask.contains(new Vector3d(chunkX + 1, chunkY, chunkZ)))
					{
						deque.push(ajustedChunkX + 1);
						deque.push(chunkY);
						deque.push(ajustedChunkZ);
						deque.push(0);
					}
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][0]) && -(ajustedChunkX - cameraChunkX) < chunksViewDistance && !floodFillMask.contains(new Vector3d(chunkX - 1, chunkY, chunkZ)))
					{
						deque.push(ajustedChunkX - 1);
						deque.push(chunkY);
						deque.push(ajustedChunkZ);
						deque.push(2);
					}

					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][4]) && (chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY + 1, chunkZ)))
					{
						deque.push(ajustedChunkX);
						deque.push(chunkY + 1);
						deque.push(ajustedChunkZ);
						deque.push(5);
					}
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][5]) && -(chunkY - cameraChunkY) < verticalDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY - 1, chunkZ)))
					{
						deque.push(ajustedChunkX);
						deque.push(chunkY - 1);
						deque.push(ajustedChunkZ);
						deque.push(4);
					}

					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][1]) && (ajustedChunkZ - cameraChunkZ) < chunksViewDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ + 1)))
					{
						deque.push(ajustedChunkX);
						deque.push(chunkY);
						deque.push(ajustedChunkZ + 1);
						deque.push(3);
					}
					if ((sideFrom == -1 || ((CubicChunk) chunk).occlusionSides[sideFrom][3]) && -(ajustedChunkZ - cameraChunkZ) < chunksViewDistance && !floodFillMask.contains(new Vector3d(chunkX, chunkY, chunkZ - 1)))
					{
						deque.push(ajustedChunkX);
						deque.push(chunkY);
						deque.push(ajustedChunkZ - 1);
						deque.push(1);
					}
				}
			}

			//System.out.println(new Vector3d(0.025, 0.0, 1.0).equals(new Vector3d(0.025, 0.0, 1.0)));

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
								if ((renderableChunk.getChunkRenderData() != null && renderableChunk.getChunkRenderData().isUploaded()))
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
									//System.out.println(chunk.chunkY);
									localMapCommands.put((byte) 0x00);

									localMapElements++;

								}
							}

						if (renderableChunk.isMarkedForReRender() && !chunk.isAirChunk())
						{
							//chunksRenderer.requestChunkRender(chunk);
							//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
						}
						renderList.add(renderableChunk);
					}
			}
			//Sort 
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

			//renderingContext.resetAllVertexAttributesLocations();
			//renderingContext.enableVertexAttribute(vertexIn);
			//glBindBuffer(GL_ARRAY_BUFFER, 0);
			localMapCommands.flip();

			renderingContext.bindAttribute("vertexIn", new ByteBufferAttributeSource(localMapCommands, VertexFormat.BYTE, 3, 4));
			//renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_BYTE, false, 4, localMapCommands);
			
			renderingContext.draw(Primitive.POINT, 0, localMapElements);
			//Two maps
			
			renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
			
			//glDepthFunc(GL_LEQUAL);
			fboLoadedChunksBot.bind();

			renderingContext.draw(Primitive.POINT, 0, localMapElements);

			//renderingContext.setBlendMode(BlendMode.MIX);
			//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			FrameBufferObject.unbind();

			// Now delete from the worker threads what we won't need anymore
			chunksRenderer.purgeUselessWork(cameraChunkX, cameraChunkY, cameraChunkZ, sizeInChunks, chunksViewDistance);

			//world.ioHandler.requestChunksUnload(cameraChunkX, cameraChunkY, cameraChunkZ, sizeInChunks, chunksViewDistance);

			farTerrainRenderer.uploadGeneratedMeshes();

			//world.getRegionSummaries().removeFurther(cameraChunkX, cameraChunkZ, 33);

			chunksChanged = false;
			// Load nearby chunks
			//TODO kek me up inside
		}
		// Cleans free vbos
		ChunkRenderData.deleteUselessVBOs();
	}

	public void shadowPass()
	{
		Client.profiler.startSection("shadows");
		// float worldTime = (world.worldTime%1000+1000)%1000;
		if (this.getShadowVisibility() == 0f)
			return; // No shadows at night :)
		
		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setBlendMode(BlendMode.ALPHA_TEST);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		/*glCullFace(GL_BACK);
		glEnable(GL_CULL_FACE);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);*/

		int size = (shadowMapBuffer).getWidth();
		//glViewport(0, 0, size, size);

		shadowMapFBO.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		shadowsPassShader = renderingContext.useShader("shadows");
		//renderingContext.setCurrentShader(shadowsPassShader);
		//shadowsPassShader.use(true);
		int fun = 10;// / hdPass ? 3 : 8;
		if (size > 1024)
			fun = 15;
		else if (size > 2048)
			fun = 20;

		int fun2 = 200;// hdPass ? 100 : 200;
		Matrix4f depthProjectionMatrix = MatrixHelper.getOrthographicMatrix(-fun * 10, fun * 10, -fun * 10, fun * 10, -fun2, fun2);
		Matrix4f depthViewMatrix = MatrixHelper.getLookAtMatrix(sky.getSunPosition(), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

		Matrix4f.mul(depthProjectionMatrix, depthViewMatrix, depthMatrix);
		Matrix4f shadowMVP = new Matrix4f(depthMatrix);

		//System.out.println(depthViewMatrix);
		//depthMatrix.translate(new Vector3f((float) Math.floor(camera.pos.x), (float) Math.floor(camera.pos.y), (float) Math.floor(camera.pos.z)));
		//shadowMVP.translate(new Vector3f((float) Math.floor(camera.pos.x), (float) Math.floor(camera.pos.y), (float) Math.floor(camera.pos.z)));
		shadowMVP.translate(new Vector3f((float) camera.pos.getX(), (float) camera.pos.getY(), (float) camera.pos.getZ()));

		shadowsPassShader.setUniformMatrix4f("depthMVP", shadowMVP);
		shadowsPassShader.setUniformMatrix4f("localTransform", new Matrix4f());
		shadowsPassShader.setUniform1f("entity", 0);
		renderWorld(true, -1);
		//glViewport(0, 0, scrW, scrH);
	}

	public void renderTerrain(boolean ignoreWorldCulling)
	{
		// Terrain
		Client.profiler.startSection("terrain");
		
		//glDisable(GL_BLEND);

		terrainShader = renderingContext.useShader("terrain");
		renderingContext.setBlendMode(BlendMode.ALPHA_TEST);
		//renderingContext.setCurrentShader(terrainShader);
		//terrainShader.use(true);
		camera.setupShader(terrainShader);
		sky.setupShader(terrainShader);

		//terrainShader.setUniformFloat3("vegetationColor", vegetationColor[0] / 255f, vegetationColor[1] / 255f, vegetationColor[2] / 255f);
		terrainShader.setUniform3f("sunPos", sky.getSunPosition());
		terrainShader.setUniform1f("time", animationTimer);
		terrainShader.setUniform1f("terrainHeight", world.getRegionsSummariesHolder().getHeightAtWorldCoordinates((int) camera.pos.getX(), (int) camera.pos.getZ()));
		terrainShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
		terrainShader.setUniform1f("shadowVisiblity", getShadowVisibility());
		waterNormalTexture.setLinearFiltering(true);
		waterNormalTexture.setMipMapping(true);
		
		renderingContext.bindCubemap("environmentCubemap", environmentMap);
		//terrainShader.setUniformSamplerCubemap(9, "environmentCubemap", environmentMap);
		renderingContext.bindTexture2D("sunSetRiseTexture", sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", skyTextureRaining);
		renderingContext.bindTexture2D("blockLightmap", lightmapTexture);
		/*terrainShader.setUniformSampler(8, "sunSetRiseTexture", sunGlowTexture);
		terrainShader.setUniformSampler(7, "skyTextureSunny", skyTextureSunny);
		terrainShader.setUniformSampler(12, "skyTextureRaining", skyTextureRaining);
		terrainShader.setUniformSampler(6, "blockLightmap", lightmapTexture);*/
		Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");

		renderingContext.bindTexture2D("lightColors", lightColors);
		renderingContext.bindTexture2D("normalTexture", waterNormalTexture);
		//terrainShader.setUniformSampler(11, "lightColors", lightColors);
		//terrainShader.setUniformSampler(10, "normalTexture", waterNormalTexture);
		setupShadowColors(terrainShader);
		terrainShader.setUniform1f("time", sky.time);


		renderingContext.bindTexture2D("vegetationColorTexture", getGrassTexture());
		//terrainShader.setUniformSampler(3, "vegetationColorTexture", getGrassTexture());
		terrainShader.setUniform1f("mapSize", sizeInChunks * 32);
		renderingContext.bindTexture2D("loadedChunksMapTop", loadedChunksMapTop);
		renderingContext.bindTexture2D("loadedChunksMapBot", loadedChunksMapBot);
		//terrainShader.setUniformSampler(4, "loadedChunksMapTop", loadedChunksMapTop);
		//terrainShader.setUniformSampler(5, "loadedChunksMapBot", loadedChunksMapBot);
		
		terrainShader.setUniform2f("playerCurrentChunk", this.cameraChunkX, this.cameraChunkY);
		terrainShader.setUniform1f("ignoreWorldCulling", ignoreWorldCulling ? 1f : 0f);

		if (Keyboard.isKeyDown(Keyboard.KEY_F10))
			renderingContext.setPolygonFillMode(PolygonFillMode.WIREFRAME);

		if (RenderingConfig.debugPasses)
			glFinish();
		long t = System.nanoTime();
		if (!InputAbstractor.isKeyDown(org.lwjgl.input.Keyboard.KEY_F9))
			renderedVertices += farTerrainRenderer.draw(renderingContext, terrainShader);

		renderingContext.setPolygonFillMode(PolygonFillMode.FILL);

		if (RenderingConfig.debugPasses)
			glFinish();
		if (RenderingConfig.debugPasses)
			System.out.println("terrain took " + (System.nanoTime() - t) / 1000000.0 + "ms");
		
		renderingContext.flush();
	}

	public void renderWorld(boolean isShadowPass, int chunksToRenderLimit)
	{
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		//renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		//System.out.println(renderingContext.getPipelineConfiguration().getBlendMode() + ":" + renderingContext.getPipelineConfiguration().getCullingMode() + ":" + renderingContext.getPipelineConfiguration().getDepthTestMode() + ":" + renderingContext.getPipelineConfiguration().getPolygonFillMode());


		long t;
		animationTimer = (float) (((System.currentTimeMillis() % 100000) / 200f) % 100.0);

		int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);

		//skyTextureSunny = TexturesHandler.getTexture(world.isRaining() ? "environement/sky_rain.png" : "environement/sky.png");

		Vector3f sunPos = sky.getSunPosition();
		float shadowVisiblity = getShadowVisibility();
		chunksViewDistance = sizeInChunks / 2;

		if (!isShadowPass)
		{
			this.fboShadedBuffer.bind();

			Client.profiler.startSection("blocks");
			this.fboGBuffers.setEnabledRenderTargets();

			renderingContext.useShader("blocks_opaque");
			//renderingContext.setCurrentShader(opaqueBlocksShader);
			//opaqueBlocksShader.use(true);

			//Set materials
			
			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			renderingContext.bindNormalTexture(blocksNormalTexture);
			renderingContext.bindMaterialTexture(blocksMaterialTexture);
			//opaqueBlocksShader.setUniformSampler(0, "diffuseTexture", blocksAlbedoTexture);
			//opaqueBlocksShader.setUniformSampler(1, "normalTexture", blocksNormalTexture);
			//opaqueBlocksShader.setUniformSampler(2, "materialTexture", blocksMaterialTexture);
			
			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			renderingContext.bindTexture2D("vegetationColorTexture", getGrassTexture());
			//opaqueBlocksShader.setUniformSampler(3, "lightColors", lightmapTexture);
			//opaqueBlocksShader.setUniformSampler(4, "vegetationColorTexture", getGrassTexture());

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
			//opaqueBlocksShader.setUniformFloat("wetness", world.isRaining() ? 0.5f : 0.0f);

			camera.setupShader(opaqueBlocksShader);

			// Prepare for gbuffer pass
			
			renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
			//renderingContext.setBlendMode(BlendMode.ALPHA_TEST);
			
			/*glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			glDisable(GL_BLEND);*/
		}
		else
		{
			//renderingContext.useShader("shadows");
			//renderingContext.setCurrentShader(shadowsPassShader);
			//shadowsPassShader.use(true);
			shadowsPassShader.setUniform1f("time", animationTimer);
			
			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			renderingContext.setObjectMatrix(null);
			//opaqueBlocksShader.setUniformSampler(0, "albedoTexture", blocksAlbedoTexture);
		}

		renderingContext.setObjectMatrix(new Matrix4f());
		// Alpha blending is disabled because certain G-Buffer rendertargets can output a 0 for alpha
		
		//glAlphaFunc(GL_GREATER, 0.0f);
		//glDisable(GL_ALPHA_TEST);
		
		renderingContext.setIsShadowPass(isShadowPass);

		if (RenderingConfig.debugPasses)
			glFinish();
		t = System.nanoTime();

		int chunksRendered = 0;
		for (int XXX = 0; XXX < 1; XXX++)
			for (ChunkRenderable chunk : renderList)
			{
				ChunkRenderData chunkRenderData = chunk.getChunkRenderData();
				chunksRendered++;
				if (chunksToRenderLimit != -1 && chunksRendered > chunksToRenderLimit)
					break;
				int vboDekalX = 0;
				int vboDekalZ = 0;
				// Adjustements so border chunks show at the correct place.
				vboDekalX = chunk.getChunkX() * 32;
				vboDekalZ = chunk.getChunkZ() * 32;
				if (chunk.getChunkX() - cameraChunkX > chunksViewDistance)
					vboDekalX += -sizeInChunks * 32;
				if (chunk.getChunkX() - cameraChunkX < -chunksViewDistance)
					vboDekalX += sizeInChunks * 32;
				if (chunk.getChunkZ() - cameraChunkZ > chunksViewDistance)
					vboDekalZ += -sizeInChunks * 32;
				if (chunk.getChunkZ() - cameraChunkZ < -chunksViewDistance)
					vboDekalZ += sizeInChunks * 32;
				// Update if chunk was modified
				if ((chunk.isMarkedForReRender() || chunk.needsLightningUpdates()) && !chunk.isAirChunk())
					chunksRenderer.requestChunkRender(chunk);
				// Don't bother if it don't render anything
				if (chunkRenderData == null || !chunkRenderData.isUploaded()|| chunkRenderData.vboSizeFullBlocks + chunkRenderData.vboSizeCustomBlocks == 0)
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
					boolean shouldShowChunk = ((int) (camera.pos.getX() / 32) == chunk.getChunkX()) && ((int) (camera.pos.getY() / 32) == correctedCY) && ((int) (camera.pos.getZ() / 32) == correctedCZ);
					if (!shouldShowChunk)
						shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ);
					if (!shouldShowChunk)
						continue;
				}
				if (!isShadowPass)
				{
					opaqueBlocksShader.setUniform3f("objectPosition", vboDekalX, chunk.getChunkY() * 32f, vboDekalZ);
				}
				else
					shadowsPassShader.setUniform3f("objectPosition", vboDekalX, chunk.getChunkY() * 32f, vboDekalZ);

				//glBindBuffer(GL_ARRAY_BUFFER, chunkRenderData.vboId);

				if (!Keyboard.isKeyDown(Keyboard.KEY_F4))
					if (isShadowPass)
						renderedVerticesShadow += chunkRenderData.renderCubeSolidBlocks(renderingContext);
					else
					{
						renderedChunks++;
						renderedVertices += chunkRenderData.renderCubeSolidBlocks(renderingContext);
					}

				if (!Keyboard.isKeyDown(Keyboard.KEY_F5))
					if (isShadowPass)
						renderedVerticesShadow += chunkRenderData.renderCustomSolidBlocks(renderingContext);
					else
					{
						renderedChunks++;
						renderedVertices += chunkRenderData.renderCustomSolidBlocks(renderingContext);
					}

			}
		
		renderingContext.flush();

		//glDepthFunc(GL_LEQUAL);
		
		// Done looping chunks, now entities
		if (!isShadowPass)
		{
			if (RenderingConfig.debugPasses)
				glFinish();
			if (RenderingConfig.debugPasses)
				System.out.println("blocks took " + (System.nanoTime() - t) / 1000000.0 + "ms");

			/*renderingContext.disableVertexAttribute(vertexIn);
			renderingContext.disableVertexAttribute(texCoordIn);
			renderingContext.disableVertexAttribute(colorIn);
			renderingContext.disableVertexAttribute(normalIn);*/

			// Select shader
			renderingContext.useShader("entities");
			//renderingContext.setCurrentShader(entitiesShader);
			//entitiesShader.use(true);

			/*renderingContext.enableVertexAttribute(vertexIn);
			renderingContext.enableVertexAttribute(texCoordIn);
			renderingContext.enableVertexAttribute(normalIn);
			renderingContext.enableVertexAttribute("colorIn");*/

			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, -1, normalIn);

			entitiesShader.setUniformMatrix4f("localTansform", new Matrix4f());
			entitiesShader.setUniformMatrix3f("localTransformNormal", new Matrix3f());

			entitiesShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);
			entitiesShader.setUniform1f("shadowVisiblity", shadowVisiblity);
			
			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			//entitiesShader.setUniformSampler(4, "lightColors", lightmapTexture);
			lightmapTexture.setTextureWrapping(false);
			entitiesShader.setUniform2f("screenSize", scrW, scrH);
			entitiesShader.setUniform3f("sunPos", sunPos.x, sunPos.y, sunPos.z);
			entitiesShader.setUniform3f("blockColor", 1f, 1f, 1f);
			entitiesShader.setUniform1f("time", animationTimer);

			entitiesShader.setUniform1f("overcastFactor", world.getWeather());
			entitiesShader.setUniform1f("wetness", getWorldWetness());

			renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
			renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);
			//entitiesShader.setUniformFloat("wetness", world.isRaining() ? 0.5f : 0.0f);

			camera.setupShader(entitiesShader);

			//TexturesHandler.bindTexture("res/textures/normal.png");
		}
		else
		{
			shadowsPassShader.setUniform1f("entity", 1);
		}
		
		//glEnable(GL_CULL_FACE);
		//glDisable(GL_CULL_FACE);
		
		renderingContext.setCullingMode(CullingMode.DISABLED);
		// Render entities
		

		if (!Keyboard.isKeyDown(Keyboard.KEY_F6))
			entitiesRenderer.renderEntities(renderingContext);

		//System.out.println(entitiesRendered);

		/*renderingContext.disableVertexAttribute(normalIn);
		renderingContext.disableVertexAttribute(vertexIn);
		renderingContext.disableVertexAttribute(texCoordIn);
		renderingContext.disableVertexAttribute("colorIn");*/

		if (isShadowPass)
			return;

		//Add decals
		decalsRenderer.renderDecals(renderingContext);

		// Solid blocks done, now render water & lights
		
		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setCullingMode(CullingMode.DISABLED);
		//glDisable(GL_CULL_FACE);
		//glDisable(GL_ALPHA_TEST);

		// We do water in two passes : one for computing the refracted color and putting it in shaded buffer, and another one
		// to read it back and blend it
		
		//glDepthFunc(GL_LEQUAL);
		for (int pass = 1; pass < 3; pass++)
		{
			liquidBlocksShader = renderingContext.useShader("blocks_liquid_pass" + (pass));
			//liquidBlocksShader = ShadersLibrary.getShaderProgram("blocks_liquid_pass" + (pass));
			//renderingContext.setCurrentShader(liquidBlocksShader);
			//liquidBlocksShader.use(true);

			liquidBlocksShader.setUniform1f("viewDistance", RenderingConfig.viewDistance);

			liquidBlocksShader.setUniform1f("yAngle", (float) (camera.rotationY * Math.PI / 180f));
			liquidBlocksShader.setUniform1f("shadowVisiblity", shadowVisiblity);
			// liquidBlocksShader.setUniformSamplerCube(3, "skybox",
			// TexturesHandler.idCubemap("textures/skybox"));
			
			//liquidBlocksShader.setUniformSampler(1, "normalTextureDeep", TexturesHandler.getTexture("water/deep.png"));
			//liquidBlocksShader.setUniformSampler(2, "normalTextureShallow", waterNormalTexture);
			renderingContext.bindTexture2D("normalTextureDeep", TexturesHandler.getTexture("water/deep.png"));
			renderingContext.bindTexture2D("normalTextureShallow", waterNormalTexture);
			
			//liquidBlocksShader.setUniformSampler(3, "lightColors", lightmapTexture);
			renderingContext.bindTexture2D("lightColors", lightmapTexture);
			renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
			//liquidBlocksShader.setUniformSampler(0, "diffuseTexture", blocksAlbedoTexture);
			liquidBlocksShader.setUniform2f("screenSize", scrW, scrH);
			liquidBlocksShader.setUniform3f("sunPos", sunPos.x, sunPos.y, sunPos.z);
			liquidBlocksShader.setUniform1f("time", animationTimer);

			camera.setupShader(liquidBlocksShader);

			// Vertex attributes setup

			//renderingContext.setCurrentShader(liquidBlocksShader);
			
			/*renderingContext.enableVertexAttribute(vertexIn);
			if (texCoordIn != -1)
				renderingContext.enableVertexAttribute(texCoordIn);
			if (colorIn != -1)
				renderingContext.enableVertexAttribute(colorIn);
			if (normalIn != -1)
				renderingContext.enableVertexAttribute(normalIn);*/

			// Set rendering context.
			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, colorIn, normalIn);

			Voxel vox = Voxels.get(world.getVoxelData((int) -camera.pos.getX(), (int) (-camera.pos.getY() + 0), (int) -camera.pos.getZ()));
			liquidBlocksShader.setUniform1f("underwater", vox.isVoxelLiquid() ? 1 : 0);

			//liquidBlocksShader.setUniformInt("pass", pass-1);
			if (pass == 1)
			{
				fboShadedBuffer.bind();
				fboShadedBuffer.setEnabledRenderTargets(true);
				renderingContext.bindTexture2D("readbackAlbedoBufferTemp", this.albedoBuffer);
				renderingContext.bindTexture2D("readbackMetaBufferTemp", this.materialBuffer);
				renderingContext.bindTexture2D("readbackDepthBufferTemp", this.zBuffer);
				//renderingContext.bindTexture2D("alb2o", blocksAlbedoTexture);
				//glEnable(GL_ALPHA_TEST);
				//glDisable(GL_ALPHA_TEST);
				glDepthMask(false);
			}
			else if (pass == 2)
			{
				//composite_pass_gbuffers_waterfp.unbind();
				fboGBuffers.bind();
				fboGBuffers.setEnabledRenderTargets();
				//System.out.println("Race (tm)");
				renderingContext.bindTexture2D("readbackShadedBufferTemp", this.shadedBuffer);
				renderingContext.bindTexture2D("readbackDepthBufferTemp", this.zBuffer);
				glDepthMask(true);
			}
			for (ChunkRenderable chunk : renderList)
			{
				ChunkRenderData chunkRenderData = chunk.getChunkRenderData();
				if (chunkRenderData == null || !chunkRenderData.isUploaded() || chunkRenderData.vboSizeWaterBlocks == 0)
					continue;

				int vboDekalX = chunk.getChunkX() * 32;
				int vboDekalZ = chunk.getChunkZ() * 32;

				if (chunk.getChunkX() - cameraChunkX > chunksViewDistance)
					vboDekalX += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.getChunkX() - cameraChunkX < -chunksViewDistance)
					vboDekalX += sizeInChunks * 32;
				if (chunk.getChunkZ() - cameraChunkZ > chunksViewDistance)
					vboDekalZ += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.getChunkZ() - cameraChunkZ < -chunksViewDistance)
					vboDekalZ += sizeInChunks * 32;

				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.getChunkY();
				int correctedCZ = vboDekalZ / 32;

				boolean shouldShowChunk = ((int) (camera.pos.getX() / 32) == chunk.getChunkX()) && ((int) (camera.pos.getY() / 32) == correctedCY) && ((int) (camera.pos.getZ() / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ);
				if (!shouldShowChunk)
					continue;

				liquidBlocksShader.setUniform3f("objectPosition", vboDekalX, chunk.getChunkY() * 32, vboDekalZ);

				//glBindBuffer(GL_ARRAY_BUFFER, chunkRenderData.vboId);
				renderedVertices += chunkRenderData.renderWaterBlocks(renderingContext);
			}

			// Disable vertex attributes
			/*renderingContext.disableVertexAttribute(vertexIn);
			if (texCoordIn != -1)
				renderingContext.disableVertexAttribute(texCoordIn);
			if (colorIn != -1)
				renderingContext.disableVertexAttribute(colorIn);
			if (normalIn != -1)
				renderingContext.disableVertexAttribute(normalIn);*/
			//renderingContext.doneWithVertexInputs();
		}

		// Particles rendering
		((ParticlesRenderer) this.world.getParticlesManager()).render(renderingContext);

		// Draw world shaded with sunlight and vertex light
		glDepthMask(false);
		renderShadedBlocks();
		glDepthMask(true);

		// Compute SSAO
		if (RenderingConfig.ssaoQuality > 0)
			this.SSAO(RenderingConfig.ssaoQuality);

		renderLightsDeffered();
		fboShadedBuffer.bind();
		fboShadedBuffer.setEnabledRenderTargets(true);
		renderTerrain(chunksToRenderLimit != -1);
	}

	private void renderLightsDeffered()
	{
		Client.profiler.startSection("lights");

		//We work on the shaded buffer
		this.fboShadedBuffer.bind();
		// Deffered lightning
		// Disable depth read/write
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		//glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		
		lightShader = renderingContext.useShader("light");
		//renderingContext.setCurrentShader(lightShader);
		//lightShader.use(true);

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
		//glEnable(GL_BLEND);
		//glEnable(GL_ALPHA_TEST);
		//glDisable(GL_DEPTH_TEST);
		//glBlendFunc(GL_ONE, GL_ONE);

		LightsRenderer.renderPendingLights(renderingContext);
		//Cleanup
		glDepthMask(true);
		
		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		/*glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);*/
		//renderingContext.lights.clear();
	}

	/**
	 * Uses G-Buffers data to spit out shaded solid blocks ( shadows etc )
	 */
	public void renderShadedBlocks()
	{
		if (Keyboard.isKeyDown(Keyboard.KEY_F7))
			return;
		if (RenderingConfig.debugPasses)
			glFinish();

		long t = System.nanoTime();

		applyShadowsShader = renderingContext.useShader("shadows_apply");
		//renderingContext.setCurrentShader(applyShadowsShader);
		//applyShadowsShader.use(true);
		setupShadowColors(applyShadowsShader);

		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", getWorldWetness());

		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		//glEnable(GL_ALPHA_TEST);
		//glDisable(GL_DEPTH_TEST);

		Vector3f sunPos = sky.getSunPosition();

		fboShadedBuffer.bind();

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

		applyShadowsShader.setUniform1f("time", sky.time);

		applyShadowsShader.setUniform1f("shadowMapResolution", shadowMapResolution);
		applyShadowsShader.setUniform1f("shadowVisiblity", getShadowVisibility());
		applyShadowsShader.setUniformMatrix4f("shadowMatrix", depthMatrix);
		applyShadowsShader.setUniform3f("sunPos", sunPos);

		// Matrices for screen-space transformations
		camera.setupShader(applyShadowsShader);
		sky.setupShader(applyShadowsShader);

		renderingContext.drawFSQuad();
		//drawFSQuad();

		if (RenderingConfig.debugPasses)
			glFinish();

		if (RenderingConfig.debugPasses)
			System.out.println("shadows pass took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		//glDisable(GL_BLEND);
		//glEnable(GL_DEPTH_TEST);
	}

	//Post-process effects

	/**
	 * Renders the final image to the screen
	 */
	public void blitScreen()
	{
		if (RenderingConfig.debugPasses)
			glFinish();
		long t = System.nanoTime();

		// We render to the screen.
		FrameBufferObject.unbind();
		
		//glDisable(GL_DEPTH_TEST);
		//glDisable(GL_BLEND);
		//glDisable(GL_CULL_FACE);
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		
		postProcess = renderingContext.useShader("postprocess");
		//renderingContext.setCurrentShader(postProcess);
		//postProcess.use(true);

		renderingContext.bindTexture2D("shadedBuffer", this.shadedBuffer);
		renderingContext.bindTexture2D("albedoBuffer", this.albedoBuffer);
		renderingContext.bindTexture2D("depthBuffer", this.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", this.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", this.materialBuffer);
		renderingContext.bindTexture2D("shadowMap", this.shadowMapBuffer);
		renderingContext.bindTexture2D("bloomBuffer", this.bloomBuffer);
		renderingContext.bindTexture2D("ssaoBuffer", this.ssaoBuffer);
		//postProcess.setUniformSampler(8, "debugBuffer", (System.currentTimeMillis() % 1000 < 500 ) ? this.loadedChunksMapD : this.loadedChunksMap);
		renderingContext.bindTexture2D("debugBuffer", (System.currentTimeMillis() % 1000 < 500) ? this.loadedChunksMapTop : this.loadedChunksMapBot);
		//renderingContext.bindTexture2D("debugBuffer", this.shadowMapBuffer);

		Voxel vox = Voxels.get(world.getVoxelData(camera.pos.negate()));
		postProcess.setUniform1f("underwater", vox.isVoxelLiquid() ? 1 : 0);
		postProcess.setUniform1f("time", animationTimer);

		Vector3f sunPos = sky.getSunPosition();
		postProcess.setUniform3f("sunPos", sunPos.x, sunPos.y, sunPos.z);

		camera.setupShader(postProcess);

		postProcess.setUniform1f("viewWidth", scrW);
		postProcess.setUniform1f("viewHeight", scrH);

		postProcess.setUniform1f("apertureModifier", apertureModifier);

		renderingContext.drawFSQuad();
		//drawFSQuad();

		if (RenderingConfig.debugPasses)
			glFinish();

		if (RenderingConfig.debugPasses)
			System.out.println("final blit took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		if (RenderingConfig.doBloom)
		{
			//glBindTexture(GL_TEXTURE_2D, shadedBuffer.getId());
			shadedBuffer.bind();
			
			shadedBuffer.setMipMapping(true);
			int max_mipmap = (int) (Math.ceil(Math.log(Math.max(scrH, scrW)) / Math.log(2))) - 1;
			shadedBuffer.setMipmapLevelsRange(0, max_mipmap);
			try
			{
				//int max_mipmap = (int) (Math.floor(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
				//System.out.println(fBuffer + " " + max_mipmap);
				//shadedMipmapZeroLevelColor.rewind();

				//illDownIndex++;
				//if (illDownIndex % 50 == 0)
				if (System.currentTimeMillis() - lastIllCalc > 1000)
				{
					lastIllCalc = System.currentTimeMillis();
					//shadedMipmapZeroLevelColor = BufferUtils.createByteBuffer(12);
					//if(!shadedMipmapZeroLevelColor.hasRemaining())
					//	shadedMipmapZeroLevelColor.rewind();
					//glGetTexImage(GL_TEXTURE_2D, max_mipmap, GL_RGB, GL_FLOAT, shadedMipmapZeroLevelColor);
					//System.out.println("ill");
					illDownBuffers = 1;
					//long nanoC = System.nanoTime();
					illuminationDownloader[(illDownIndex / 50 + illDownBuffers - 1) % illDownBuffers].copyTexure(shadedBuffer, max_mipmap);
					//long nanoR = System.nanoTime();
					//System.out.println("copy took "+Math.floor((nanoR-nanoC)/10f)/100f+"µs ");
					if (illDownIndex / 10 >= illDownBuffers)
					{
						//ByteBuffer tmpBuffer = illuminationDownloader[illDownIndex/10 % illDownBuffers].readPBO();
						//shadedMipmapZeroLevelColor = BufferUtils.createByteBuffer(tmpBuffer.capacity());
						//shadedMipmapZeroLevelColor.put(tmpBuffer);
						shadedMipmapZeroLevelColor = illuminationDownloader[illDownIndex / 50 % illDownBuffers].readPBO();
						//System.out.println("read took "+Math.floor((System.nanoTime()-nanoR)/10f)/100f+"µs ");
						//System.out.println("Read "+shadedMipmapZeroLevelColor.capacity() + "bytes.");
						//System.out.println("glError : "+glGetError());
						illuminationDownloader[illDownIndex / 10 % illDownBuffers].doneWithReading();
						//shadedMipmapZeroLevelColor.rewind();

						//float luma = shadedMipmapZeroLevelColor.getFloat() * 0.2125f + shadedMipmapZeroLevelColor.getFloat() * 0.7154f + shadedMipmapZeroLevelColor.getFloat() * 0.0721f;
						//System.out.println("read luma : "+luma);
					}
				}
				else if (shadedMipmapZeroLevelColor != null)
					shadedMipmapZeroLevelColor.rewind();

				this.shadedBuffer.computeMipmaps();

				if (shadedMipmapZeroLevelColor != null)
				{
					if (!shadedMipmapZeroLevelColor.hasRemaining())
						shadedMipmapZeroLevelColor.rewind();
					//System.out.println(shadedMipmapZeroLevelColor);
					float luma = 0.0f;
					for (int i = 0; i < 1; i++)
						luma += shadedMipmapZeroLevelColor.getFloat() * 0.2125f + shadedMipmapZeroLevelColor.getFloat() * 0.7154f + shadedMipmapZeroLevelColor.getFloat() * 0.0721f;

					//System.out.println(luma);
					//luma /= 4;
					luma *= apertureModifier;
					luma = (float) Math.pow(luma, 1d / 2.2);
					//System.out.println("luma:"+luma + " aperture:"+ this.apertureModifier);

					float targetLuma = 0.55f;
					float lumaMargin = 0.15f;

					if (luma < targetLuma - lumaMargin)
					{
						if (apertureModifier < 2.0)
							apertureModifier *= 1.001;
					}
					else if (luma > targetLuma + lumaMargin)
					{
						if (apertureModifier > 1.0)
							apertureModifier *= 0.999;
					}
					else
					{
						float clamped = (float) Math.min(Math.max(1 / apertureModifier, 0.998), 1.002);
						apertureModifier *= clamped;
					}
				}
			}
			catch (Throwable th)
			{
				th.printStackTrace();
			}
		}
		else
			apertureModifier = 1.0f;

		//Draw entities Huds
		Iterator<Entity> ei = world.getAllLoadedEntities();
		Entity e;
		while (ei.hasNext())
		{
			e = ei.next();
			if (e instanceof EntityHUD)
				((EntityHUD) e).drawHUD(renderingContext);
		}
	}

	private void SSAO(int quality)
	{
		fboSSAO.bind();
		fboSSAO.setEnabledRenderTargets();

		renderingContext.useShader("ssao");
		//renderingContext.setCurrentShader(ssaoShader);
		//ssaoShader.use(true);

		renderingContext.bindTexture2D("normalTexture", this.normalBuffer);
		renderingContext.bindTexture2D("deptBuffer", this.zBuffer);

		ssaoShader.setUniform1f("viewWidth", scrW);
		ssaoShader.setUniform1f("viewHeight", scrH);

		ssaoShader.setUniform1i("kernelsPerFragment", quality * 8);

		camera.setupShader(ssaoShader);

		if (ssao_kernel == null)
		{
			ssao_kernel_size = 16;//applyShadowsShader.getConstantInt("KERNEL_SIZE"); nvm too much work
			ssao_kernel = new Vector3f[ssao_kernel_size];
			for (int i = 0; i < ssao_kernel_size; i++)
			{
				Vector3f vec = new Vector3f((float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f, (float) Math.random());
				vec.normalise(vec);
				float scale = ((float) i) / ssao_kernel_size;
				scale = Math2.mix(0.1f, 1.0f, scale * scale);
				vec.scale(scale);
				ssao_kernel[i] = vec;
				if (RenderingConfig.debugPasses)
					System.out.println("lerp " + scale + "x " + vec.x);
			}
		}

		for (int i = 0; i < ssao_kernel_size; i++)
		{
			ssaoShader.setUniform3f("ssaoKernel[" + i + "]", ssao_kernel[i].x, ssao_kernel[i].y, ssao_kernel[i].z);
		}

		renderingContext.drawFSQuad();

		// Blur the thing

		// Vertical pass
		fboBlur.bind();
		
		blurV = renderingContext.useShader("blurV");
		//renderingContext.setCurrentShader(blurV);
		//blurV.use(true);
		blurV.setUniform2f("screenSize", scrW, scrH);
		blurV.setUniform1f("lookupScale", 2);
		renderingContext.bindTexture2D("inputTexture", this.ssaoBuffer);
		renderingContext.drawFSQuad();
		//drawFSQuad();

		// Horizontal pass
		this.fboSSAO.bind();
		
		blurH = renderingContext.useShader("blurH");
		//renderingContext.setCurrentShader(blurH);
		//blurH.use(true);
		blurH.setUniform2f("screenSize", scrW, scrH);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		renderingContext.drawFSQuad();
		//drawFSQuad();

	}

	private void renderBloom()
	{
		this.shadedBuffer.setLinearFiltering(true);
		this.bloomBuffer.setLinearFiltering(true);
		this.blurIntermediateBuffer.setLinearFiltering(true);

		bloomShader = renderingContext.useShader("bloom");
		
		renderingContext.bindTexture2D("shadedBuffer", this.shadedBuffer);
		bloomShader.setUniform1f("apertureModifier", apertureModifier);
		bloomShader.setUniform2f("screenSize", scrW / 2f, scrH / 2f);

		int max_mipmap = (int) (Math.ceil(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
		bloomShader.setUniform1f("max_mipmap", max_mipmap);

		this.fboBloom.bind();
		this.fboBloom.setEnabledRenderTargets();
		//glViewport(0, 0, scrW / 2, scrH / 2);
		renderingContext.drawFSQuad();

		// Blur bloom
		// Vertical pass
		fboBlur.bind();
		
		blurV = renderingContext.useShader("blurV");
		//renderingContext.setCurrentShader(blurV);
		//blurV.use(true);
		blurV.setUniform2f("screenSize", scrW / 2f, scrH / 2f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", this.bloomBuffer);
		//drawFSQuad();
		renderingContext.drawFSQuad();

		// Horizontal pass
		this.fboBloom.bind();
		
		blurH = renderingContext.useShader("blurH");
		//renderingContext.setCurrentShader(blurH);
		//blurH.use(true);
		blurH.setUniform2f("screenSize", scrW / 2f, scrH / 2f);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		//drawFSQuad();
		renderingContext.drawFSQuad();

		fboBlur.bind();
		
		blurV = renderingContext.useShader("blurV");
		//renderingContext.setCurrentShader(blurV);
		//blurV.use(true);
		blurV.setUniform2f("screenSize", scrW / 4f, scrH / 4f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", this.bloomBuffer);
		//drawFSQuad();
		renderingContext.drawFSQuad();

		// Horizontal pass
		this.fboBloom.bind();
		
		blurH = renderingContext.useShader("blurH");
		//renderingContext.setCurrentShader(blurH);
		//blurH.use(true);
		blurH.setUniform2f("screenSize", scrW / 4f, scrH / 4f);
		renderingContext.bindTexture2D("inputTexture", blurIntermediateBuffer);
		//drawFSQuad();
		renderingContext.drawFSQuad();

		// Done blooming
		//glViewport(0, 0, scrW, scrH);
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
				environmentMapFastFbo.bind();
			else
				this.fboShadedBuffer.bind();

			//glDisable(GL_DEPTH_TEST);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Scene rendering
			if (onlyTerrain)
			{
				camera.justSetup(scrW, scrH);
				camera.translate();

				//Draw sky
				sky.time = (world.worldTime % 10000) / 10000f;
				//sky.skyShader.use(true);
				//sky.skyShader.setUniformSamplerCubemap(7, "environmentCubemap", environmentMap);
				//glViewport(0, 0, scrW, scrH);
				sky.render(renderingContext);

				this.renderTerrain(true);
			}
			else
				this.renderWorldAtCameraInternal(camera, cubemap == null ? -1 : 0);

			if (cubemap != null)
			{
				//System.out.println(cubemap.getID());

				//glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap.getID());

				int t[] = new int[] { 4, 5, 3, 2, 0, 1 };
				int f = t[z];

				//glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + f, 0, GL_RGBA, resolution, resolution, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

				/*glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				// Anti seam
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);*/

				renderingContext.useShader("blit");
				//renderingContext.setCurrentShader(ShadersLibrary.getShaderProgram("blit"));

				this.environmentMapFBO.bind();
				this.environmentMapFBO.setColorAttachement(0, cubemap.getFace(f));

				if (useFastBuffer)
					renderingContext.bindTexture2D("diffuseTexture", environmentMapBufferHDR);
				else
					renderingContext.bindTexture2D("diffuseTexture", shadedBuffer);

				renderingContext.currentShader().setUniform2f("screenSize", resolution, resolution);

				//renderingContext.enableVertexAttribute(renderingContext.currentShader().getVertexAttributeLocation("texCoord"));
				renderingContext.drawFSQuad();
				//glFinish();
			}
			else
			{
				// GL access
				//glBindTexture(GL_TEXTURE_2D, shadedBuffer.getId());
				shadedBuffer.bind();

				// File access
				File image = new File(GameDirectory.getGameFolderPath() + "/skyboxscreens/" + time + "/" + names[z] + ".png");
				image.mkdirs();

				ByteBuffer bbuf = ByteBuffer.allocateDirect(resolution * resolution * 4 * 4).order(ByteOrder.nativeOrder());

				glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_FLOAT, bbuf);

				BufferedImage pixels = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < resolution; x++)
					for (int y = 0; y < resolution; y++)
					{
						int i = 4 * (x + resolution * y);
						int r = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int g = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int b = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 8)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						pixels.setRGB(x, resolution - 1 - y, (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | b & 0xFF);
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
		camera.justSetup(oldW, oldH);

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
		if (vegetationTexture == null || vegetationTexture.getId() == -1)
			vegetationTexture = TexturesHandler.getTexture("./textures/environement/grassColor.png");
		vegetationTexture.setMipMapping(true);
		vegetationTexture.setLinearFiltering(true);
		return vegetationTexture;
	}

	private void setupShadowColors(ShaderInterface terrainShader2)
	{
		float sunLightFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.0f) / 1.0f, 1.0f);

		terrainShader2.setUniform1f("shadowStrength", 1.0f);
		float x = 1.2f;
		terrainShader2.setUniform3f("sunColor", Math2.mix(new Vector3f(x * 255 / 255f, x * 255 / 255f, x * 255 / 255f), new Vector3f(0.5f), sunLightFactor));
		terrainShader2.setUniform3f("shadowColor", new Vector3f(0.50f, 0.50f, 0.50f));
	}

	private float getShadowVisibility()
	{
		float worldTime = (world.worldTime % 10000 + 10000) % 10000;
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
		Vector3f center = new Vector3f(correctedCX * 32 + 16, correctedCY * 32 + 15, correctedCZ * 32 + 16);
		return camera.isBoxInFrustrum(center, new Vector3f(32, 32, 32));
	}

	private float getWorldWetness()
	{
		float wetFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.5f) / 0.3f, 1.0f);
		return wetFactor;
	}

	public void destroy()
	{
		sky.destroy();
		chunksRenderer.killThread();
		farTerrainRenderer.destroy();
		entitiesRenderer.clearLoadedEntitiesRenderers();
	}

	public World getWorld()
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
	
	public SkyRenderer getSky()
	{
		return sky;
	}
	
	public void reloadContentSpecificStuff()
	{
		farTerrainRenderer.markVoxelTexturesSummaryDirty();
		entitiesRenderer.clearLoadedEntitiesRenderers();
	}
}
