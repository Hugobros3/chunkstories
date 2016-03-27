package io.xol.chunkstories.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static io.xol.engine.textures.Texture.TextureType.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import io.xol.engine.base.InputAbstractor;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.math.MatrixHelper;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.Cubemap;
import io.xol.engine.textures.GBufferTexture;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.entity.EntityHUD;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldRenderer
{
	// World pointer
	World world;

	// Worker thread
	public ChunksRenderer chunksRenderer;

	// Current camera position
	public double viewX, viewY, viewZ;
	public float viewRotH, viewRotV;
	//Chunk space position
	public int pCX, pCY, pCZ;

	// camera object ( we need it so much)
	private Camera camera;

	//Viewer directions
	private Vector3f viewerPosVector;
	private Vector3f viewerCamDirVector;

	private int sizeInChunks; // cache from world

	// Chunks to render
	private List<CubicChunk> renderList = new ArrayList<CubicChunk>();

	// Wheter to update the renderlist or not.
	private boolean chunksChanged = true;

	// Screen width & height
	private int scrW, scrH;
	// Shader programs
	private ShaderProgram terrainShader;
	private ShaderProgram opaqueBlocksShader;
	private ShaderProgram liquidBlocksShader;
	private ShaderProgram entitiesShader;
	private ShaderProgram shadowsPassShader;
	private ShaderProgram applyShadowsShader;
	private ShaderProgram lightShader;
	private ShaderProgram postProcess;

	private ShaderProgram bloomShader;
	private ShaderProgram ssaoShader;

	private ShaderProgram blurH;
	private ShaderProgram blurV;

	//Rendering context
	RenderingContext renderingContext;

	// G-Buffers
	private GBufferTexture composite_albedo = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_zbuffer = new GBufferTexture(DEPTH_RENDERBUFFER, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_normal = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_meta = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);

	// Main Rendertarget (HDR)
	private GBufferTexture composite_shaded = new GBufferTexture(RGB_HDR, XolioWindow.frameW, XolioWindow.frameH);

	// Bloom texture
	private GBufferTexture composite_bloom = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW / 2, XolioWindow.frameH / 2);
	private GBufferTexture composite_ssao = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);

	// FBOs
	private FBO composite_pass_gbuffers = new FBO(composite_zbuffer, composite_albedo, composite_normal, composite_meta);
	private FBO composite_pass_gbuffers_waterfp = new FBO(composite_zbuffer, composite_shaded);

	private FBO composite_pass_shaded = new FBO(composite_zbuffer, composite_shaded);
	private FBO composite_pass_bloom = new FBO(null, composite_bloom);
	private FBO composite_pass_ssao = new FBO(null, composite_ssao);

	private GBufferTexture blurTemp = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW / 2, XolioWindow.frameH / 2);
	private FBO blurFBO = new FBO(null, blurTemp);

	// Normalized sun position
	Vector3f normSunPosition = new Vector3f();

	// Shadow maps
	private int smr = 0;
	private GBufferTexture shadow_map_near = new GBufferTexture(DEPTH_SHADOWMAP, 256, 256);
	// private GBufferTexture shadow_map_far = new GBufferTexture(1, 256, 256);
	private FBO shadow_map_renderer_near = new FBO(shadow_map_near);
	// private FBO shadow_map_renderer_far = new FBO(shadow_map_far);

	//Environment map
	Cubemap environmentMap;

	// Shadow transformation matrix
	private Matrix4f depthMatrix = new Matrix4f();

	// Sky
	public Sky sky;

	// Debug
	DebugProfiler updateProfiler = new DebugProfiler();

	// Light
	public List<DefferedLight> lights = new ArrayList<DefferedLight>();

	// Terrain at distance
	public TerrainSummarizer terrain;

	public WeatherEffectsRenderer wer;

	//For shaders animations
	float animationTimer = 0.0f;

	//Counters
	public int renderedVertices = 0;
	public int renderedVerticesShadow = 0;
	public int renderedChunks = 0;

	public WorldRenderer(World w)
	{
		// Link world
		world = w;
		world.linkWorldRenderer(this);
		terrain = new TerrainSummarizer(world);
		wer = new WeatherEffectsRenderer(world, this);
		sky = new Sky(world, this);
		sizeInChunks = world.getSizeInChunks();
		resizeShadowMaps();
		environmentMap = new Cubemap(Cubemap.CubemapType.RGBA_8BPP);

		renderingContext = XolioWindow.getInstance().getRenderingContext();
		XolioWindow.instance.renderingContext = renderingContext;
		
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

		//matrix44Buffer = BufferUtils.createFloatBuffer(16);

		if (FastConfig.debugGBuffers)
			System.out.println("Starting renderer thread");
		chunksRenderer = new ChunksRenderer(world);
		chunksRenderer.start();
	}

	public void resizeShadowMaps()
	{
		// Only if necessary
		if (smr == FastConfig.shadowMapResolutions)
			return;
		smr = FastConfig.shadowMapResolutions;
		shadow_map_near.resize(smr, smr);
	}

	long lastEnvmapRender = 0L;

	public void renderWorldAtCamera(Camera camera)
	{
		this.camera = camera;
		if (FastConfig.doDynamicCubemaps && (System.currentTimeMillis() - lastEnvmapRender) > 2000L)// * Math.pow(30.0f / XolioWindow.getFPS(), 1.0f))
			screenCubeMap(256, environmentMap);
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
		updateRender(-camera.camPosX, -camera.camPosY, -camera.camPosZ, -camera.view_rotx, -camera.view_roty);

		Client.profiler.startSection("next");
		// Shadows pre-pass
		if (FastConfig.doShadows && chunksToRenderLimit == -1)
			shadowPass();
		// Prepare matrices
		camera.justSetup(scrW, scrH);

		// Clear G-Buffers and bind shaded HDR rendertarget
		composite_pass_gbuffers.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		composite_pass_shaded.bind();

		// Draw sky
		if (FastConfig.debugGBuffers)
			glFinish();
		long t = System.nanoTime();
		sky.time = (world.worldTime % 10000) / 10000f;
		renderingContext.setCurrentShader(sky.skyShader);
		//sky.skyShader.use(true);
		//sky.skyShader.setUniformSamplerCubemap(7, "environmentCubemap", environmentMap);
		glViewport(0, 0, scrW, scrH);
		sky.render(renderingContext);

		if (FastConfig.debugGBuffers)
			glFinish();
		if (FastConfig.debugGBuffers)
			System.out.println("sky took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		// Move camera to relevant position
		camera.translate();
		composite_pass_gbuffers.setEnabledRenderTargets();

		// Render world
		renderWorld(false, chunksToRenderLimit);
		// Render weather
		composite_pass_shaded.bind();
		composite_pass_shaded.setEnabledRenderTargets();
		wer.renderEffects(renderingContext);

		// Debug
		if (FastConfig.debugGBuffers)
			glFinish();
		if (FastConfig.debugGBuffers)
			System.out.println("total took " + (System.nanoTime() - t) / 1000000.0 + "ms ( " + 1 / ((System.nanoTime() - t) / 1000000000.0) + " fps)");

		// Do bloom
		if (FastConfig.doBloom)
		{
			glDisable(GL_DEPTH_TEST);

			this.composite_shaded.setLinearFiltering(true);
			this.composite_bloom.setLinearFiltering(true);
			this.blurTemp.setLinearFiltering(true);

			renderingContext.setCurrentShader(bloomShader);
			//bloomShader.use(true);
			bloomShader.setUniformSampler(0, "shadedBuffer", this.composite_shaded);
			bloomShader.setUniformFloat("apertureModifier", apertureModifier);

			this.composite_pass_bloom.bind();
			this.composite_pass_bloom.setEnabledRenderTargets();
			glViewport(0, 0, scrW / 2, scrH / 2);
			ObjectRenderer.drawFSQuad(bloomShader.getVertexAttributeLocation("vertexIn"));

			// Blur bloom

			// Vertical pass
			blurFBO.bind();
			renderingContext.setCurrentShader(blurV);
			//blurV.use(true);
			blurV.setUniformFloat2("screenSize", scrW / 2f, scrH / 2f);
			blurV.setUniformFloat("lookupScale", 1);
			blurV.setUniformSampler(0, "inputTexture", this.composite_bloom);
			//drawFSQuad();
			ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));

			// Horizontal pass
			this.composite_pass_bloom.bind();
			renderingContext.setCurrentShader(blurH);
			//blurH.use(true);
			blurH.setUniformFloat2("screenSize", scrW / 2f, scrH / 2f);
			blurH.setUniformSampler(0, "inputTexture", blurTemp);
			//drawFSQuad();
			ObjectRenderer.drawFSQuad(blurH.getVertexAttributeLocation("vertexIn"));

			// Done blooming
			glViewport(0, 0, scrW, scrH);
		}
		composite_pass_shaded.bind();
		composite_pass_shaded.setEnabledRenderTargets();

		Client.profiler.startSection("done");
	}

	private int fastfloor(double x)
	{
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	public void updateRender(double camPosX, double camPosY, double camPosZ, float view_rotx, float view_roty)
	{
		// Called every frame, this method takes care of updating the world :
		// It will keep up to date the camera position, as well as a list of
		// to-render chunks in order to fill empty VBO space
		// Upload generated chunks data to GPU
		updateProfiler.reset("vbo upload");
		ChunkRenderData toLoad = chunksRenderer.doneChunk();
		int loadLimit = 16;
		while (toLoad != null)
		{
			//CubicChunk c = world.getChunk(toload.x, toload.y, toload.z, false);
			CubicChunk c = toLoad.chunk;
			if (c != null)
			{
				//Free replaced chunkRenderData if any
				if(c.chunkRenderData != null)
					c.chunkRenderData.free();
				//Upload data
				toLoad.upload();
				//Replace it
				c.chunkRenderData = toLoad;
				chunksChanged = true;
			}
			else
			{
				if (FastConfig.debugGBuffers)
					System.out.println("ChunkRenderer outputted a chunk render for a not loaded chunk : ");
				//if (FastConfig.debugGBuffers)
				//	System.out.println("Chunks coordinates : X=" + toload.x + " Y=" + toload.y + " Z=" + toload.z);
				//if (FastConfig.debugGBuffers)
				//	System.out.println("Render information : vbo size =" + toload.s_normal + " and water size =" + toload.s_water);
				toLoad.free();
			}
			loadLimit--;
			if (loadLimit > 0)
				toLoad = chunksRenderer.doneChunk();
			else
				toLoad = null;
		}
		// if(FastConfig.debugGBuffers ) glFinish();
		// Cleans free vbos
		ChunkRenderData.deleteUselessVBOs();
		// Update view
		viewX = camPosX;
		viewY = camPosY;
		viewZ = camPosZ;
		viewRotH = view_rotx;
		viewRotV = view_roty;
		int npCX = fastfloor((camPosX + 16) / 32);
		int npCY = fastfloor((camPosY) / 32);
		int npCZ = fastfloor((camPosZ + 16) / 32);
		// Fill the VBO array with chunks VBO ids if the player changed chunk
		if (pCX != npCX || pCY != npCY || pCZ != npCZ || chunksChanged || true)
		{
			int chunksViewDistance = (int) (FastConfig.viewDistance / 32);

			// Unload too far chunks
			updateProfiler.startSection("unloadFar");
			//long usageBefore = Runtime.getRuntime().freeMemory();
			world.trimRemovableChunks();
			renderList.clear();
			ChunksIterator it = Client.world.iterator();
			CubicChunk chunk;
			//int i = 0;
			while (it.hasNext())
			{
				//i++;
				chunk = it.next();

				if (LoopingMathHelper.moduloDistance(chunk.chunkX, npCX, world.getSizeInChunks()) <= chunksViewDistance)
					if (LoopingMathHelper.moduloDistance(chunk.chunkZ, npCZ, world.getSizeInChunks()) <= chunksViewDistance)
					{
						if (chunk.need_render.get() && chunk.dataPointer != -1)
						{
							//chunksRenderer.requestChunkRender(chunk);
							//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
						}
						renderList.add(chunk);
					}
			}
			//Sort 
			renderList.sort(new Comparator<CubicChunk>()
			{
				@Override
				public int compare(CubicChunk a, CubicChunk b)
				{
					int distanceA = LoopingMathHelper.moduloDistance(a.chunkX, npCX, world.getSizeInChunks()) + LoopingMathHelper.moduloDistance(a.chunkZ, npCZ, world.getSizeInChunks());
					int distanceB = LoopingMathHelper.moduloDistance(b.chunkX, npCX, world.getSizeInChunks()) + LoopingMathHelper.moduloDistance(b.chunkZ, npCZ, world.getSizeInChunks());
					return distanceA - distanceB;
					//return distanceB - distanceA;
				}
			});

			// Now delete from the worker threads what we won't need anymore
			chunksRenderer.purgeUselessWork(npCX, npCY, npCZ, sizeInChunks, chunksViewDistance);
			world.ioHandler.requestChunksUnload(npCX, npCY, npCZ, sizeInChunks, chunksViewDistance + 1);
			
			if(npCX != pCX || npCZ != pCZ)
				terrain.generateArround(-camera.camPosX, -camera.camPosZ);
			terrain.updateData();
			world.chunkSummaries.removeFurther(npCX, npCZ, 33);

			chunksChanged = false;
			// Load nearby chunks
			for (int t = (npCX - chunksViewDistance - 1); t < npCX + chunksViewDistance + 1; t++)
			{
				for (int g = (npCZ - chunksViewDistance - 1); g < npCZ + chunksViewDistance + 1; g++)
					for (int b = npCY - 3; b < npCY + 3; b++)
					{
						chunk = world.getChunk(t, b, g, true);
					}
			}
		}
		pCX = npCX;
		pCY = npCY;
		pCZ = npCZ;
		// It will send chunks to be rendered in the ChunksRenderer thread and
		// then re-insert the data in the done chunk
	}

	public void shadowPass()
	{
		Client.profiler.startSection("shadows");
		// float worldTime = (world.worldTime%1000+1000)%1000;
		if (this.getShadowVisibility() == 0f)
			return; // No shadows at night :)
		glCullFace(GL_FRONT);
		glEnable(GL_CULL_FACE);
		//glDisable(GL_CULL_FACE);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);

		int size = (shadow_map_near).getWidth();
		glViewport(0, 0, size, size);

		shadow_map_renderer_near.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		renderingContext.setCurrentShader(shadowsPassShader);
		//shadowsPassShader.use(true);
		Vector3f lightPosition = sky.getSunPos();
		lightPosition.normalise(normSunPosition);
		int fun = 10;// / hdPass ? 3 : 8;
		if (size > 1024)
			fun = 15;
		else if (size > 2048)
			fun = 20;

		int fun2 = 200;// hdPass ? 100 : 200;
		Matrix4f depthProjectionMatrix = MatrixHelper.getOrthographicMatrix(-fun * 10, fun * 10, -fun * 10, fun * 10, -fun2, fun2);
		Matrix4f depthViewMatrix = MatrixHelper.getLookAtMatrix(normSunPosition, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

		Matrix4f.mul(depthProjectionMatrix, depthViewMatrix, depthMatrix);
		Matrix4f shadowMVP = new Matrix4f(depthMatrix);

		//depthMatrix.translate(new Vector3f((float) Math.floor(camera.camPosX), (float) Math.floor(camera.camPosY), (float) Math.floor(camera.camPosZ)));
		//shadowMVP.translate(new Vector3f((float) Math.floor(camera.camPosX), (float) Math.floor(camera.camPosY), (float) Math.floor(camera.camPosZ)));
		shadowMVP.translate(new Vector3f((float)camera.camPosX, (float)camera.camPosY, (float)camera.camPosZ));

		shadowsPassShader.setUniformMatrix4f("depthMVP", shadowMVP);
		shadowsPassShader.setUniformMatrix4f("localTransform", new Matrix4f());
		shadowsPassShader.setUniformFloat("entity", 0);
		renderWorld(true, -1);
		glViewport(0, 0, scrW, scrH);
	}

	private boolean checkChunkOcclusion(Chunk chunk, int correctedCX, int correctedCY, int correctedCZ, Vector3f viewerPosition, Vector3f viewerDirection)
	{
		Vector3f centerSphere = new Vector3f(correctedCX * 32 + 16, correctedCY * 32 + 15, correctedCZ * 32 + 16);
		return camera.isBoxInFrustrum(centerSphere, new Vector3f(32, 32, 32));
	}

	Texture glowTexture = TexturesHandler.getTexture("environement/glow.png");
	Texture skyTexture = TexturesHandler.getTexture("environement/sky.png");
	Texture lightmapTexture = TexturesHandler.getTexture("environement/light.png");
	Texture waterNormalTexture = TexturesHandler.getTexture("normal.png");

	Texture blocksDiffuseTexture = TexturesHandler.getTexture("tiles_merged_diffuse.png");
	Texture blocksNormalTexture = TexturesHandler.getTexture("tiles_merged_normal.png");
	Texture blocksMaterialTexture = TexturesHandler.getTexture("tiles_merged_material.png");

	float averageBrightness = 1f;
	float apertureModifier = 1f;

	public void renderWorld(boolean shadowPass, int chunksToRenderLimit)
	{
		long t;
		animationTimer = (float) (((System.currentTimeMillis() % 100000) / 200f) % 100.0);

		int chunksViewDistance = (int) (FastConfig.viewDistance / 32);

		skyTexture = TexturesHandler.getTexture(world.isRaining() ? "environement/sky_rain.png" : "environement/sky.png");

		Vector3f sunPos = sky.getSunPos();
		float shadowVisiblity = getShadowVisibility();
		chunksViewDistance = sizeInChunks / 2;
		
		//TODO move
		Texture vegetationTexture = TexturesHandler.getTexture(world.folder.getAbsolutePath() + "/grassColor.png");
		if (vegetationTexture == null || vegetationTexture.getID() == -1)
			vegetationTexture = TexturesHandler.getTexture("./res/textures/environement/grassColor.png");
		vegetationTexture.setMipMapping(true);
		vegetationTexture.setLinearFiltering(true);
		
		if (!shadowPass)
		{
			this.composite_pass_shaded.bind();
			
			Client.profiler.startSection("blocks");
			this.composite_pass_gbuffers.setEnabledRenderTargets();

			renderingContext.setCurrentShader(opaqueBlocksShader);
			//opaqueBlocksShader.use(true);

			//Set materials
			opaqueBlocksShader.setUniformSampler(0, "diffuseTexture", blocksDiffuseTexture);
			opaqueBlocksShader.setUniformSampler(1, "normalTexture", blocksNormalTexture);
			opaqueBlocksShader.setUniformSampler(2, "materialTexture", blocksMaterialTexture);
			opaqueBlocksShader.setUniformSampler(3, "lightColors", lightmapTexture);
			opaqueBlocksShader.setUniformSampler(4, "vegetationColorTexture", vegetationTexture);

			//Set texturing arguments
			blocksDiffuseTexture.setTextureWrapping(false);
			blocksDiffuseTexture.setLinearFiltering(false);
			blocksDiffuseTexture.setMipMapping(false);
			blocksDiffuseTexture.setMipmapLevelsRange(0, 4);

			blocksNormalTexture.setTextureWrapping(false);
			blocksNormalTexture.setLinearFiltering(false);
			blocksNormalTexture.setMipMapping(false);
			blocksNormalTexture.setMipmapLevelsRange(0, 4);

			blocksMaterialTexture.setTextureWrapping(false);
			blocksMaterialTexture.setLinearFiltering(false);
			blocksMaterialTexture.setMipMapping(false);
			blocksMaterialTexture.setMipmapLevelsRange(0, 4);

			//Shadows parameters
			opaqueBlocksShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
			opaqueBlocksShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());

			//Camera-related stuff
			opaqueBlocksShader.setUniformFloat2("screenSize", scrW, scrH);
			opaqueBlocksShader.setUniformFloat3("camPos", viewX, viewY, viewZ);

			//World stuff
			opaqueBlocksShader.setUniformFloat("mapSize", sizeInChunks * 32);
			opaqueBlocksShader.setUniformFloat("time", animationTimer);
			opaqueBlocksShader.setUniformFloat("wetness", world.isRaining() ? 0.5f : 0.0f);

			camera.setupShader(opaqueBlocksShader);

			// Prepare for gbuffer pass
			glEnable(GL_CULL_FACE);
			glCullFace(GL_FRONT);
			glDisable(GL_BLEND);
		}
		else
		{
			renderingContext.setCurrentShader(shadowsPassShader);
			//shadowsPassShader.use(true);
			shadowsPassShader.setUniformFloat("time", animationTimer);
			opaqueBlocksShader.setUniformSampler(0, "albedoTexture", blocksDiffuseTexture);
		}

		// Alpha blending is disabled because certain G-Buffer rendertargets can output a 0 for alpha
		glAlphaFunc(GL_GREATER, 0.0f);
		glDisable(GL_ALPHA_TEST);
		int vertexIn = 0, texCoordIn = 0, colorIn = 0, normalIn = 0;
		// Init vertex attribute locations
		if (!shadowPass)
		{
			vertexIn = opaqueBlocksShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = opaqueBlocksShader.getVertexAttributeLocation("texCoordIn");
			colorIn = opaqueBlocksShader.getVertexAttributeLocation("colorIn");
			normalIn = opaqueBlocksShader.getVertexAttributeLocation("normalIn");
			//glEnablezVertexAttribArray(colorIn);
			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, colorIn, normalIn);
			renderingContext.setCurrentShader(opaqueBlocksShader);
			renderingContext.enableVertexAttribute(colorIn);
		}
		else
		{
			vertexIn = shadowsPassShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = shadowsPassShader.getVertexAttributeLocation("texCoordIn");
			normalIn = shadowsPassShader.getVertexAttributeLocation("normalIn");
			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, colorIn, normalIn);
			renderingContext.setCurrentShader(shadowsPassShader);
		}
		renderingContext.setIsShadowPass(shadowPass);

		renderingContext.enableVertexAttribute(normalIn);
		renderingContext.enableVertexAttribute(vertexIn);
		renderingContext.enableVertexAttribute(texCoordIn);

		// Culling vectors
		viewerPosVector = new Vector3f((float)viewX, (float)viewY, (float)viewZ);
		float transformedViewH = (float) ((viewRotH) / 180 * Math.PI);
		// if(FastConfig.debugGBuffers ) System.out.println(Math.sin(transformedViewV)+"f");
		viewerCamDirVector = new Vector3f((float) (Math.sin((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)), (float) (Math.cos((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)));

		if (FastConfig.debugGBuffers)
			glFinish();
		t = System.nanoTime();

		// renderList.clear();
		glDisable(GL_BLEND);
		int chunksRendered = 0;
		for (CubicChunk chunk : renderList)
		{
			ChunkRenderData chunkRenderData = chunk.chunkRenderData;
			chunksRendered++;
			if (chunksToRenderLimit != -1 && chunksRendered > chunksToRenderLimit)
				break;
			int vboDekalX = 0;
			int vboDekalZ = 0;
			// Adjustements so border chunks show at the correct place.
			vboDekalX = chunk.chunkX * 32;
			vboDekalZ = chunk.chunkZ * 32;
			if (chunk.chunkX - pCX > chunksViewDistance)
				vboDekalX += -sizeInChunks * 32;
			if (chunk.chunkX - pCX < -chunksViewDistance)
				vboDekalX += sizeInChunks * 32;
			if (chunk.chunkZ - pCZ > chunksViewDistance)
				vboDekalZ += -sizeInChunks * 32;
			if (chunk.chunkZ - pCZ < -chunksViewDistance)
				vboDekalZ += sizeInChunks * 32;
			// Update if chunk was modified
			if (chunk.need_render.get() && chunk.requestable.get() && chunk.dataPointer != -1)
				chunksRenderer.requestChunkRender(chunk);
			// Don't bother if it don't render anything
			if (chunkRenderData == null || chunkRenderData.vboSizeFullBlocks + chunkRenderData.vboSizeCustomBlocks == 0)
				continue;
			// If we're doing shadows
			if (shadowPass)
			{
				// TODO : make proper orthogonal view checks etc
				float distanceX = LoopingMathHelper.moduloDistance(pCX, chunk.chunkX, sizeInChunks);
				float distanceZ = LoopingMathHelper.moduloDistance(pCZ, chunk.chunkZ, sizeInChunks);

				int maxShadowDistance = 4;
				if (smr >= 2048)
					maxShadowDistance = 5;
				if (smr >= 4096)
					maxShadowDistance = 6;

				if (distanceX > maxShadowDistance || distanceZ > maxShadowDistance)
					continue;
			}
			else
			{
				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.chunkY;
				int correctedCZ = vboDekalZ / 32;
				//Always show the chunk we're standing in no matter what
				boolean shouldShowChunk = ((int) (viewX / 32) == chunk.chunkX) && ((int) (viewY / 32) == correctedCY) && ((int) (viewZ / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ, viewerPosVector, viewerCamDirVector);
				if (!shouldShowChunk)
					continue;
			}
			if (!shadowPass)
			{
				int camIntPartX = (int) Math.floor(camera.camPosX);
				int camIntPartY = (int) Math.floor(camera.camPosY);
				int camIntPartZ = (int) Math.floor(camera.camPosZ);
				double fractPartX = camera.camPosX - Math.floor(camera.camPosX);
				double fractPartY = camera.camPosY - Math.floor(camera.camPosY);
				double fractPartZ = camera.camPosZ - Math.floor(camera.camPosZ);
				double diffChunkX = (double)(vboDekalX + camIntPartX);
				double diffChunkY = (double)(chunk.chunkY * 32 + camIntPartY);
				double diffChunkZ = (double)(vboDekalZ + camIntPartZ);
				opaqueBlocksShader.setUniformFloat3("borderShift", vboDekalX + camera.camPosX, chunk.chunkY * 32f + camera.camPosY, vboDekalZ + camera.camPosZ);
				opaqueBlocksShader.setUniformFloat3("borderShift", diffChunkX + fractPartX, diffChunkY + fractPartY, diffChunkZ + fractPartZ);
			}
			else
				shadowsPassShader.setUniformFloat3("borderShift", vboDekalX, chunk.chunkY * 32f, vboDekalZ);

			glBindBuffer(GL_ARRAY_BUFFER, chunkRenderData.vboId);
			int geometrySize = chunkRenderData.vboSizeFullBlocks;

			// We're going back to interlaced format
			// Raw blocks ( integer faces ) alignment :
			// Vertex data : [VERTEX_POS(4b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 16 bits
			glVertexAttribPointer(vertexIn, 4, GL_UNSIGNED_BYTE, false, 16, 0);
			//glVertexAttribPointer(vertexIn, 4, GL_INT_2_10_10_10_REV, false, 16, 0);
			glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 16, 4);
			if (!shadowPass)
				glVertexAttribPointer(colorIn, 4, GL_UNSIGNED_BYTE, true, 16, 8);
			glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 16, 12);

			if (geometrySize > 0)
			{
				if (shadowPass)
					renderedVerticesShadow += geometrySize;
				else
				{
					renderedChunks++;
					renderedVertices += geometrySize;
				}
				glDrawArrays(GL_TRIANGLES, 0, geometrySize);
				// Memory usage be geometrySize ( nb vertex ) x 11 ( nb data per
				// vertice (xyz pos, ts texcoord, rgb color and lmn normal) x 3
			}

			if (chunkRenderData.vboSizeCustomBlocks > 0)
			{
				geometrySize = chunkRenderData.vboSizeCustomBlocks;
				int dekal = chunkRenderData.vboSizeFullBlocks * 16 + chunkRenderData.vboSizeWaterBlocks * 24;
				// We're going back to interlaced format
				// Complex blocks ( integer faces ) alignment :
				// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
				glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 24, dekal + 0);
				glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 24, dekal + 12);
				if (!shadowPass)
					glVertexAttribPointer(colorIn, 4, GL_UNSIGNED_BYTE, true, 24, dekal + 16);
				glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 24, dekal + 20);

				if (shadowPass)
					renderedVerticesShadow += geometrySize;
				else
				{
					renderedChunks++;
					renderedVertices += geometrySize;
				}
				glDrawArrays(GL_TRIANGLES, 0, geometrySize);
			}
		}
		// Done looping chunks, now entities
		if (!shadowPass)
		{
			if (FastConfig.debugGBuffers)
				glFinish();
			if (FastConfig.debugGBuffers)
				System.out.println("blocks took " + (System.nanoTime() - t) / 1000000.0 + "ms");

			renderingContext.disableVertexAttribute(vertexIn);
			renderingContext.disableVertexAttribute(texCoordIn);
			renderingContext.disableVertexAttribute(colorIn);
			renderingContext.disableVertexAttribute(normalIn);

			// Select shader
			renderingContext.setCurrentShader(entitiesShader);
			//entitiesShader.use(true);

			vertexIn = entitiesShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = entitiesShader.getVertexAttributeLocation("texCoordIn");
			normalIn = entitiesShader.getVertexAttributeLocation("normalIn");

			renderingContext.enableVertexAttribute(vertexIn);
			renderingContext.enableVertexAttribute(texCoordIn);
			renderingContext.enableVertexAttribute(normalIn);

			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, -1, normalIn);
			
			entitiesShader.setUniformMatrix4f("localTansform", new Matrix4f());
			entitiesShader.setUniformMatrix3f("localTransformNormal", new Matrix3f());

			entitiesShader.setUniformFloat("viewDistance", FastConfig.viewDistance);
			entitiesShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
			entitiesShader.setUniformSampler(4, "lightColors", lightmapTexture);
			lightmapTexture.setTextureWrapping(false);
			entitiesShader.setUniformFloat2("screenSize", scrW, scrH);
			entitiesShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
			entitiesShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
			entitiesShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);
			entitiesShader.setUniformFloat3("blockColor", 1f, 1f, 1f);
			entitiesShader.setUniformFloat("time", animationTimer);

			entitiesShader.setUniformFloat("wetness", world.isRaining() ? 0.5f : 0.0f);

			camera.setupShader(entitiesShader);

			TexturesHandler.bindTexture("res/textures/normal.png");
		}
		else
			shadowsPassShader.setUniformFloat("entity", 1);

		glDisable(GL_CULL_FACE);
		// Render entities
		DefferedLight[] el;
		Iterator<Entity> ie = world.getAllLoadedEntities();
		Entity e;
		while (ie.hasNext())
		//for (Entity e : getAllLoadedEntities())
		{
			e = ie.next();
			//Reset transformations
			entitiesShader.setUniformMatrix4f("localTansform", new Matrix4f());
			entitiesShader.setUniformMatrix3f("localTransformNormal", new Matrix3f());
			if(e!= null)
			{
				e.render(renderingContext);
				// Also populate lights buffer
				el = e.getLights();
				if (el != null)
					for (DefferedLight l : el)
						lights.add(l);
			}
		}
		// Particles rendering
		Client.world.particlesHolder.render(renderingContext);
		glEnable(GL_CULL_FACE);

		renderingContext.disableVertexAttribute(normalIn);
		renderingContext.disableVertexAttribute(vertexIn);
		renderingContext.disableVertexAttribute(texCoordIn);

		if (shadowPass)
			return;

		// Solid blocks done, now render water & lights
		glDisable(GL_CULL_FACE);
		glDisable(GL_ALPHA_TEST);

		// We do water in two passes : one for computing the refracted color and putting it in shaded buffer, and another one
		// to read it back and blend it
		glDepthFunc(GL_LEQUAL);
		for (int pass = 1; pass < 3; pass++)
		{
			liquidBlocksShader = ShadersLibrary.getShaderProgram("blocks_liquid_pass" + (pass));
			renderingContext.setCurrentShader(liquidBlocksShader);
			//liquidBlocksShader.use(true);

			liquidBlocksShader.setUniformFloat("viewDistance", FastConfig.viewDistance);

			liquidBlocksShader.setUniformFloat("yAngle", (float) (viewRotV * Math.PI / 180f));
			liquidBlocksShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
			// liquidBlocksShader.setUniformSamplerCube(3, "skybox",
			// TexturesHandler.idCubemap("textures/skybox"));
			liquidBlocksShader.setUniformSampler(1, "normalTexture", waterNormalTexture);
			liquidBlocksShader.setUniformSampler(3, "lightColors", lightmapTexture);
			liquidBlocksShader.setUniformSampler(0, "diffuseTexture", blocksDiffuseTexture);
			liquidBlocksShader.setUniformFloat2("screenSize", scrW, scrH);
			liquidBlocksShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
			liquidBlocksShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
			liquidBlocksShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);
			liquidBlocksShader.setUniformFloat("time", animationTimer);

			camera.setupShader(liquidBlocksShader);

			// Vertex attributes setup
			vertexIn = liquidBlocksShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = liquidBlocksShader.getVertexAttributeLocation("texCoordIn");
			colorIn = liquidBlocksShader.getVertexAttributeLocation("colorIn");
			normalIn = liquidBlocksShader.getVertexAttributeLocation("normalIn");

			renderingContext.setCurrentShader(liquidBlocksShader);
			renderingContext.enableVertexAttribute(vertexIn);
			if (texCoordIn != -1)
				renderingContext.enableVertexAttribute(texCoordIn);
			if (colorIn != -1)
				renderingContext.enableVertexAttribute(colorIn);
			if (normalIn != -1)
				renderingContext.enableVertexAttribute(normalIn);

			// Set rendering context.
			//renderingContext.setupVertexInputs(vertexIn, texCoordIn, colorIn, normalIn);

			Voxel vox = VoxelTypes.get(world.getDataAt((int) viewX, (int) (viewY + 1.3), (int) viewZ, false));
			liquidBlocksShader.setUniformFloat("underwater", vox.isVoxelLiquid() ? 1 : 0);

			//liquidBlocksShader.setUniformInt("pass", pass-1);
			if (pass == 1)
			{
				composite_pass_gbuffers_waterfp.bind();
				composite_pass_gbuffers_waterfp.setEnabledRenderTargets(true);
				liquidBlocksShader.setUniformSampler(4, "readbackAlbedoBufferTemp", this.composite_albedo);
				liquidBlocksShader.setUniformSampler(5, "readbackMetaBufferTemp", this.composite_meta);
				liquidBlocksShader.setUniformSampler(6, "readbackDepthBufferTemp", this.composite_zbuffer);
				//glEnable(GL_ALPHA_TEST);
				glDisable(GL_ALPHA_TEST);
				glDepthMask(false);
			}
			else if (pass == 2)
			{
				//composite_pass_gbuffers_waterfp.unbind();
				composite_pass_gbuffers.bind();
				composite_pass_gbuffers.setEnabledRenderTargets();
				//System.out.println("Race (tm)");
				liquidBlocksShader.setUniformSampler(4, "readbackShadedBufferTemp", this.composite_shaded);
				liquidBlocksShader.setUniformSampler(6, "readbackDepthBufferTemp", this.composite_zbuffer);
				glDepthMask(true);
			}
			for (CubicChunk chunk : renderList)
			{
				ChunkRenderData chunkRenderData = chunk.chunkRenderData;
				if (chunkRenderData == null || chunkRenderData.vboSizeWaterBlocks == 0)
					continue;

				int vboDekalX = chunk.chunkX * 32;
				int vboDekalZ = chunk.chunkZ * 32;

				if (chunk.chunkX - pCX > chunksViewDistance)
					vboDekalX += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.chunkX - pCX < -chunksViewDistance)
					vboDekalX += sizeInChunks * 32;
				if (chunk.chunkZ - pCZ > chunksViewDistance)
					vboDekalZ += -sizeInChunks * 32;// (int) (Math.random()*50);//-sizeInChunks;
				if (chunk.chunkZ - pCZ < -chunksViewDistance)
					vboDekalZ += sizeInChunks * 32;

				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.chunkY;
				int correctedCZ = vboDekalZ / 32;

				boolean shouldShowChunk = ((int) (viewX / 32) == chunk.chunkX) && ((int) (viewY / 32) == correctedCY) && ((int) (viewZ / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ, viewerPosVector, viewerCamDirVector);
				if (!shouldShowChunk)
					continue;

				liquidBlocksShader.setUniformFloat3("borderShift", vboDekalX, chunk.chunkY * 32, vboDekalZ);

				glBindBuffer(GL_ARRAY_BUFFER, chunkRenderData.vboId);
				int geometrySize = chunkRenderData.vboSizeWaterBlocks;

				int dekal = chunkRenderData.vboSizeFullBlocks * 16;
				// We're going back to interlaced format
				// Complex blocks ( integer faces ) alignment :
				// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
				glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 24, dekal + 0);
				if (texCoordIn != -1)
					glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 24, dekal + 12);
				if (colorIn != -1)
					glVertexAttribPointer(colorIn, 3, GL_UNSIGNED_BYTE, true, 24, dekal + 16);
				if (normalIn != -1)
					glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 24, dekal + 20);

				glDrawArrays(GL_TRIANGLES, 0, geometrySize);
				
				renderedVertices += geometrySize;
			}

			// Disable vertex attributes
			renderingContext.disableVertexAttribute(vertexIn);
			if (texCoordIn != -1)
				renderingContext.disableVertexAttribute(texCoordIn);
			if (colorIn != -1)
				renderingContext.disableVertexAttribute(colorIn);
			if (normalIn != -1)
				renderingContext.disableVertexAttribute(normalIn);
			//renderingContext.doneWithVertexInputs();
		}

		// Draw world shaded with sunlight and vertex light
		glDepthMask(false);
		addShadows();
		glDepthMask(true);

		// Compute SSAO
		if (FastConfig.ssaoQuality > 0)
			this.SSAO(FastConfig.ssaoQuality);

		Client.profiler.startSection("lights");
		this.composite_pass_shaded.bind();

		// Deffered lightning
		// Disable depth read/write
		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		renderingContext.setCurrentShader(lightShader);
		//lightShader.use(true);

		//Required info
		lightShader.setUniformSampler(0, "albedoBuffer", this.composite_albedo);
		lightShader.setUniformSampler(1, "metaBuffer", this.composite_meta);
		lightShader.setUniformSampler(2, "comp_depth", this.composite_zbuffer);
		lightShader.setUniformSampler(3, "comp_normal", this.composite_normal);

		//Parameters
		lightShader.setUniformFloat("powFactor", 5f);
		lightShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
		camera.setupShader(lightShader);
		//Blend parameters
		glEnable(GL_BLEND);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_DEPTH_TEST);
		glBlendFunc(GL_ONE, GL_ONE);

		lightsBuffer = 0;
		//Render entities lights
		for (DefferedLight light : lights)
			renderDefferedLight(light);
		//Render particles's lights
		Client.world.particlesHolder.renderLights(this);
		// Render remaining lights
		if (lightsBuffer > 0)
		{
			lightShader.setUniformInt("lightsToRender", lightsBuffer);
			ObjectRenderer.drawFSQuad(lightShader.getVertexAttributeLocation("vertexIn"));
		}
		//Cleanup
		glDepthMask(true);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
		renderingContext.setCurrentShader(lightShader);
		lights.clear();
		
		// Terrain
		Client.profiler.startSection("terrain");
		glDisable(GL_BLEND);
		
		renderingContext.setCurrentShader(terrainShader);
		//terrainShader.use(true);
		camera.setupShader(terrainShader);
		//terrainShader.setUniformFloat3("vegetationColor", vegetationColor[0] / 255f, vegetationColor[1] / 255f, vegetationColor[2] / 255f);
		terrainShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);
		terrainShader.setUniformFloat("time", animationTimer);
		terrainShader.setUniformFloat("terrainHeight", world.chunkSummaries.getHeightAt((int) viewX, (int) viewZ));
		terrainShader.setUniformFloat("viewDistance", FastConfig.viewDistance);
		terrainShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
		waterNormalTexture.setLinearFiltering(true);
		waterNormalTexture.setMipMapping(true);
		terrainShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
		terrainShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
		terrainShader.setUniformSampler(8, "glowSampler", glowTexture);
		terrainShader.setUniformSampler(7, "colorSampler", skyTexture);
		terrainShader.setUniformSampler(6, "lightColors", lightmapTexture);
		terrainShader.setUniformSampler(5, "normalTexture", waterNormalTexture);
		terrainShader.setUniformSamplerCubemap(9, "environmentCubemap", environmentMap);
		setupShadowColors(terrainShader);
		terrainShader.setUniformFloat("time", sky.time);
		terrainShader.setUniformFloat("isRaining", world.isRaining() ? 1f : 0f);

		terrainShader.setUniformSampler(3, "vegetationColorTexture", vegetationTexture);
		terrainShader.setUniformFloat("mapSize", sizeInChunks * 32);

		if (FastConfig.debugGBuffers)
			glFinish();
		t = System.nanoTime();
		if (!InputAbstractor.isKeyDown(org.lwjgl.input.Keyboard.KEY_F9))
			renderedVertices += terrain.draw(renderingContext, terrainShader);

		if (FastConfig.debugGBuffers)
			glFinish();
		if (FastConfig.debugGBuffers)
			System.out.println("terrain took " + (System.nanoTime() - t) / 1000000.0 + "ms");
	}

	private void setupShadowColors(ShaderProgram shader)
	{
		if (world.isRaining())
		{
			shader.setUniformFloat("shadowStrength", 0.75f);
			shader.setUniformFloat3("sunColor", 1.0f, 1.0f, 1.0f);
			shader.setUniformFloat3("shadowColor", 0.20f, 0.20f, 0.20f);
		}
		else
		{
			shader.setUniformFloat("shadowStrength", 1.0f);
			float x = 1f;
			shader.setUniformFloat3("sunColor", x * 255f / 255f, x * 240f / 255f, x * 222 / 255f);
			shader.setUniformFloat3("shadowColor", 104 / 255f, 110 / 255f, 122 / 255f);
		}
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

	public void modified()
	{
		chunksChanged = true;
	}

	public void setupRenderSize(int w, int h)
	{
		scrW = w;
		scrH = h;
		this.composite_pass_gbuffers.resizeFBO(w, h);
		this.composite_pass_shaded.resizeFBO(w, h);
		this.composite_pass_gbuffers_waterfp.resizeFBO(w, h);
		// Resize bloom components
		blurFBO.resizeFBO(w / 2, h / 2);
		composite_pass_bloom.resizeFBO(w / 2, h / 2);
		composite_pass_ssao.resizeFBO(w, h);
	}

	//SSAO stuff
	Vector3f ssao_kernel[];
	int ssao_kernel_size;
	int ssao_noiseTex = -1;

	float lerp(float a, float b, float f)
	{
		return (a * (1.0f - f)) + (b * f);
	}

	public void SSAO(int quality)
	{
		composite_pass_ssao.bind();
		composite_pass_ssao.setEnabledRenderTargets();

		renderingContext.setCurrentShader(ssaoShader);
		//ssaoShader.use(true);

		ssaoShader.setUniformSampler(1, "normalTexture", this.composite_normal);
		ssaoShader.setUniformSampler(0, "depthTexture", this.composite_zbuffer);

		ssaoShader.setUniformFloat("viewWidth", scrW);
		ssaoShader.setUniformFloat("viewHeight", scrH);

		ssaoShader.setUniformInt("kernelsPerFragment", quality * 8);

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
				scale = lerp(0.1f, 1.0f, scale * scale);
				vec.scale(scale);
				ssao_kernel[i] = vec;
				if (FastConfig.debugGBuffers)
					System.out.println("lerp " + scale + "x " + vec.x);
			}
		}

		for (int i = 0; i < ssao_kernel_size; i++)
		{
			ssaoShader.setUniformFloat3("ssaoKernel[" + i + "]", ssao_kernel[i].x, ssao_kernel[i].y, ssao_kernel[i].z);
		}

		ObjectRenderer.drawFSQuad(ssaoShader.getVertexAttributeLocation("vertexIn"));

		// Blur the thing

		// Vertical pass
		blurFBO.bind();
		renderingContext.setCurrentShader(blurV);
		//blurV.use(true);
		blurV.setUniformFloat2("screenSize", scrW * 2, scrH * 2);
		blurV.setUniformFloat("lookupScale", 2);
		blurV.setUniformSampler(0, "inputTexture", this.composite_ssao);
		ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

		// Horizontal pass
		this.composite_pass_ssao.bind();
		renderingContext.setCurrentShader(blurH);
		//blurH.use(true);
		blurH.setUniformFloat2("screenSize", scrW * 2, scrH * 2);
		blurH.setUniformSampler(0, "inputTexture", blurTemp);
		ObjectRenderer.drawFSQuad(blurH.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

	}

	public void addShadows()
	{
		//if(true)
		//	return;
		if (FastConfig.debugGBuffers)

			if (FastConfig.debugGBuffers)
				glFinish();
		long t = System.nanoTime();

		renderingContext.setCurrentShader(applyShadowsShader);
		//applyShadowsShader.use(true);
		setupShadowColors(applyShadowsShader);
		applyShadowsShader.setUniformFloat("isRaining", world.isRaining() ? 1f : 0f);
		// Sun position
		// if(FastConfig.debugGBuffers ) glFinish();
		// glFlush();

		//glEnable(GL_BLEND);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_DEPTH_TEST);
		//glBlendFunc(GL_DST_COLOR, GL_ZERO);
		//glBlendFunc(GL_ONE, GL_ONE);

		Vector3f sunPos = sky.getSunPos();

		composite_pass_shaded.bind();

		//composite_pass_gbuffers.bind();
		//composite_pass_gbuffers.setEnabledRenderTargets(false, false, true);

		float lightMultiplier = 1.0f;
		// int Bdata = world.getDataAt((int)viewX, (int)viewY, (int)viewZ);
		// int llWherePlayerIs = VoxelFormat.sunlight(Bdata);
		// lightMultiplier += (15-llWherePlayerIs)/15.0f/2f;

		applyShadowsShader.setUniformFloat("brightnessMultiplier", lightMultiplier);

		applyShadowsShader.setUniformSampler(0, "albedoBuffer", composite_albedo);
		applyShadowsShader.setUniformSampler(1, "depthBuffer", composite_zbuffer);
		applyShadowsShader.setUniformSampler(2, "normalBuffer", composite_normal);
		applyShadowsShader.setUniformSampler(3, "metaBuffer", composite_meta);
		applyShadowsShader.setUniformSampler(4, "blockLightmap", lightmapTexture);
		applyShadowsShader.setUniformSampler(5, "shadowMap", shadow_map_near);
		applyShadowsShader.setUniformSampler(6, "glowSampler", glowTexture);
		applyShadowsShader.setUniformSampler(7, "colorSampler", skyTexture);
		Texture lightColors = TexturesHandler.getTexture("./res/textures/environement/lightcolors.png");
		applyShadowsShader.setUniformSampler(8, "lightColors", lightColors);
		//TODO if SSAO
		applyShadowsShader.setUniformSampler(9, "ssaoBuffer", composite_ssao);
		applyShadowsShader.setUniformSamplerCubemap(10, "environmentCubemap", environmentMap);

		//applyShadowsShader.setUniformSampler(4, "comp_spec", this.composite_specular);

		applyShadowsShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
		applyShadowsShader.setUniformFloat("time", sky.time);

		// Sky color etc
		int[] skyColor = sky.getSkyColor();

		applyShadowsShader.setUniformFloat3("skyColor", skyColor[0] / 255f, skyColor[1] / 255f, skyColor[2] / 255f);
		applyShadowsShader.setUniformFloat3("camPos", camera.camPosX, camera.camPosY, camera.camPosZ);

		//Matrix4f.mul(depthMatrix, camera.modelViewMatrix4fInverted, depthMatrix);

		applyShadowsShader.setUniformFloat("shadowMapResolution", smr);
		applyShadowsShader.setUniformFloat("shadowVisiblity", getShadowVisibility());
		applyShadowsShader.setUniformMatrix4f("shadowMatrix", depthMatrix);
		applyShadowsShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);

		// Matrices for screen-space transformations
		camera.setupShader(applyShadowsShader);

		ObjectRenderer.drawFSQuad(applyShadowsShader.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

		if (FastConfig.debugGBuffers)
			glFinish();

		if (FastConfig.debugGBuffers)
			System.out.println("shadows pass took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
	}

	public void postProcess()
	{
		if (FastConfig.debugGBuffers)
			glFinish();
		long t = System.nanoTime();

		// We render to the screen.
		FBO.unbind();
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		glDisable(GL_CULL_FACE);
		renderingContext.setCurrentShader(postProcess);
		//postProcess.use(true);

		postProcess.setUniformSampler(0, "shadedBuffer", this.composite_shaded);
		postProcess.setUniformSampler(1, "albedoBuffer", this.composite_albedo);
		postProcess.setUniformSampler(2, "depthBuffer", this.composite_zbuffer);
		postProcess.setUniformSampler(3, "normalBuffer", this.composite_normal);
		postProcess.setUniformSampler(4, "metaBuffer", this.composite_meta);
		postProcess.setUniformSampler(5, "shadowMap", this.shadow_map_near);
		postProcess.setUniformSampler(6, "bloomBuffer", this.composite_bloom);
		postProcess.setUniformSampler(7, "ssaoBuffer", this.composite_ssao);
		postProcess.setUniformSampler(8, "debugBuffer", this.blocksNormalTexture);

		Voxel vox = VoxelTypes.get(world.getDataAt((int) viewX, (int) (viewY + 1.3), (int) viewZ, false));
		postProcess.setUniformFloat("underwater", vox.isVoxelLiquid() ? 1 : 0);
		postProcess.setUniformFloat("time", animationTimer);

		Vector3f sunPos = sky.getSunPos();
		postProcess.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);

		camera.setupShader(postProcess);

		postProcess.setUniformFloat("viewWidth", scrW);
		postProcess.setUniformFloat("viewHeight", scrH);

		postProcess.setUniformFloat("apertureModifier", apertureModifier);

		ObjectRenderer.drawFSQuad(postProcess.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

		if (FastConfig.debugGBuffers)
			glFinish();

		if (FastConfig.debugGBuffers)
			System.out.println("final blit took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		if (FastConfig.doBloom)
		{
			glBindTexture(GL_TEXTURE_2D, composite_shaded.getID());
			composite_shaded.setMipMapping(true);
			try
			{
				int max_mipmap = (int) (Math.floor(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
				//System.out.println(fBuffer + " " + max_mipmap);
				shadedMipmapZeroLevelColor.rewind();
				if (Math.random() > 0.9)
				{
					glGetTexImage(GL_TEXTURE_2D, max_mipmap, GL_RGB, GL_FLOAT, shadedMipmapZeroLevelColor);
					this.composite_shaded.computeMipmaps();
				}
				//System.out.println(fBuffer);
				float luma = shadedMipmapZeroLevelColor.getFloat() * 0.2125f + shadedMipmapZeroLevelColor.getFloat() * 0.7154f + shadedMipmapZeroLevelColor.getFloat() * 0.0721f;
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
				((EntityHUD) e).drawHUD(camera);
		}
	}

	ByteBuffer shadedMipmapZeroLevelColor = BufferUtils.createByteBuffer(4 * 3);

	public int getQueueSize()
	{
		return this.renderList.size();
	}

	int lightsBuffer = 0;

	public void renderDefferedLight(DefferedLight light)
	{
		// Light culling
		if (!lightInFrustrum(light))
			return;

		lightShader.setUniformFloat("lightDecay[" + lightsBuffer + "]", light.decay);
		lightShader.setUniformFloat3("lightPos[" + lightsBuffer + "]", light.position.x, light.position.y, light.position.z);
		lightShader.setUniformFloat3("lightColor[" + lightsBuffer + "]", light.color.x, light.color.y, light.color.z);
		if (light.direction != null)
		{
			lightShader.setUniformFloat3("lightDir[" + lightsBuffer + "]", light.direction.x, light.direction.y, light.direction.z);
			// if(FastConfig.debugGBuffers ) System.out.println("setup lightdir "+light.direction.toString());
		}
		lightShader.setUniformFloat("lightAngle[" + lightsBuffer + "]", (float) (light.angle / 180 * Math.PI));

		//TexturesHandler.nowrap("res/textures/flashlight.png");

		lightsBuffer++;
		if (lightsBuffer == 64)
		{
			// if(FastConfig.debugGBuffers ) System.out.println("drawing fs quad");
			lightShader.setUniformInt("lightsToRender", lightsBuffer);
			ObjectRenderer.drawFSQuad(lightShader.getVertexAttributeLocation("vertexIn"));
			//drawFSQuad();
			lightsBuffer = 0;
		}
	}

	public boolean lightInFrustrum(DefferedLight light)
	{
		Vector3f centerSphere = new Vector3f(light.position.x, light.position.y, light.position.z);
		double coneAngle = (camera.fov) * (scrW / (scrH * 1f));
		coneAngle = coneAngle / 180d * Math.PI;
		Vector3f v = new Vector3f();
		Vector3f.sub(centerSphere, this.viewerPosVector, v);
		viewerCamDirVector.normalise(viewerCamDirVector);
		float a = Vector3f.dot(v, viewerCamDirVector);
		double b = a * Math.tan(coneAngle);
		double c = Math.sqrt(Vector3f.dot(v, v) - a * a);
		double d = c - b;
		double e = d * Math.cos(coneAngle);
		if (e >= Math.sqrt(light.decay * light.decay * 3)) // R
			return false;
		return true;
	}

	public void destroy()
	{
		// sky.destroy();
		chunksRenderer.die();
		terrain.destroy();
	}

	/**
	 * Renders the whole scene into either a cubemap or saved on disk
	 * 
	 * @param resolution
	 * @param cubemap
	 *            the cubemap to render to, or null to save to disk
	 */
	public void screenCubeMap(int resolution, Cubemap cubemap)
	{
		lastEnvmapRender = System.currentTimeMillis();
		// Save state
		boolean oldBloom = FastConfig.doBloom;
		float oldViewDistance = FastConfig.viewDistance;
		//System.out.println(this.view);
		//FastConfig.viewDistance = 0;
		FastConfig.doBloom = false;
		int oldW = scrW;
		int oldH = scrH;
		float camX = camera.view_rotx;
		float camY = camera.view_roty;
		float camZ = camera.view_rotz;
		float fov = camera.fov;
		camera.fov = 45;
		// Setup cubemap resolution
		this.setupRenderSize(resolution, resolution);
		String[] names = { "front", "back", "top", "bottom", "right", "left" };
		// Minecraft names
		// String[] names = {"right", "left", "top", "bottom", "front", "back"};
		// String[]{"panorama_0","panorama_2","panorama_4","panorama_5","panorama_1","panorama_3"};
		// old 0 0 0 180 -90 0 90 0 0 90 0 270
		//
		// names = new
		// String[]{"panorama_1","panorama_3","panorama_4","panorama_5","panorama_0","panorama_2"};
		// PX NX PY NY PZ NZ

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());

		// byte[] buf = new byte[1024 * 1024 * 4];
		ByteBuffer bbuf = ByteBuffer.allocateDirect(resolution * resolution * 4).order(ByteOrder.nativeOrder());

		for (int z = 0; z < 6; z++)
		{
			// Camera location
			switch (z)
			{
			case 0:
				camera.view_rotx = 0.0f;
				camera.view_roty = 0f;
				break;
			case 1:
				camera.view_rotx = 0;
				camera.view_roty = 180;
				break;
			case 2:
				camera.view_rotx = -90;
				camera.view_roty = 0;
				break;
			case 3:
				camera.view_rotx = 90;
				camera.view_roty = 0;
				break;
			case 4:
				camera.view_rotx = 0;
				camera.view_roty = 90;
				break;
			case 5:
				camera.view_rotx = 0;
				camera.view_roty = 270;
				break;
			}
			this.viewRotH = camera.view_rotx;
			this.viewRotV = camera.view_roty;

			float transformedViewH = (float) ((viewRotH) / 180 * Math.PI);
			viewerCamDirVector = new Vector3f((float) (Math.sin((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)), (float) (Math.cos((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)));

			this.composite_pass_shaded.bind();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Scene rendering
			this.renderWorldAtCameraInternal(camera, cubemap == null ? -1 : 128);

			// GL access
			glBindTexture(GL_TEXTURE_2D, composite_shaded.getID());
			glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

			if (cubemap != null)
			{
				glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap.getID());

				//String[] names = { "front", "back", "top", "bottom", "right", "left" };
				//String[] names = { "right", "left", "top", "bottom", "front", "back" };
				int t[] = new int[] { 4, 5, 3, 2, 0, 1 };
				int f = t[z];
				glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + f, 0, GL_RGBA, resolution, resolution, 0, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				// Anti seam
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				//glFinish();
			}
			else
			{
				// File access
				File image = new File(GameDirectory.getGameFolderPath() + "/skyboxscreens/" + time + "/" + names[z] + ".png");
				image.mkdirs();

				// Saving
				// bbuf.get(buf);
				BufferedImage pixels = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < resolution; x++)
					for (int y = 0; y < resolution; y++)
					{
						int i = 4 * (x + resolution * y);
						int r = (int) (Math.pow((bbuf.get(i) & 0xFF) / 255d, 1d / 2.2d) * 255d);
						int g = (int) (Math.pow((bbuf.get(i + 1) & 0xFF) / 255d, 1d / 2.2d) * 255d);
						int b = (int) (Math.pow((bbuf.get(i + 2) & 0xFF) / 255d, 1d / 2.2d) * 255d);
						pixels.setRGB(x, resolution - 1 - y, (0xFF << 24) | (r << 16) | (g << 8) | b);
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
		// Revert correct data

		FastConfig.viewDistance = oldViewDistance;
		FastConfig.doBloom = oldBloom;
		camera.view_rotx = camX;
		camera.view_roty = camY;
		camera.view_rotz = camZ;
		camera.fov = fov;
		camera.justSetup(oldW, oldH);
		this.setupRenderSize(oldW, oldH);
	}

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
}
