package io.xol.chunkstories.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static io.xol.engine.textures.Texture.TextureType.*;
import static org.lwjgl.opengl.ARBVertexType2_10_10_10_REV.GL_INT_2_10_10_10_REV;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
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
import io.xol.engine.textures.GBufferTexture;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;
import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.renderer.ChunksRenderer.VBOData;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;

public class WorldRenderer
{

	// World pointer
	World world;

	// Worker thread
	public ChunksRenderer chunksRenderer;

	// Current position
	public float viewX, viewY, viewZ, viewRotH, viewRotV;
	public int pCX, pCY, pCZ;

	int sizeInChunks; // cache from world

	// Chunks to render
	public List<CubicChunk> renderList = new ArrayList<CubicChunk>();

	// Wheter to update the renderlist or not.
	private boolean chunksChanged = true;

	// Screen width & height
	int scrW, scrH;

	// camera object ( we need it so much)
	Camera camera;

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
	// G-Buffers
	private GBufferTexture composite_albedo = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_zbuffer = new GBufferTexture(DEPTH_RENDERBUFFER, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_normal = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture composite_meta = new GBufferTexture(RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);

	//private GBufferTexture composite_light = new GBufferTexture(3, XolioWindow.frameW, XolioWindow.frameH);

	// Rendertarget
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
	// private FBO composite_pass_bloom4 = new FBO(null, composite_bloom4);
	// private FBO composite_pass_bloom8 = new FBO(null, composite_bloom8);
	// private FBO composite_pass_bloom16 = new FBO(null, composite_bloom16);

	// Shadowing !
	Vector3f normSunPosition = new Vector3f();

	// Shadow matrices to shaders
	private FloatBuffer matrix44Buffer;
	// private FloatBuffer matrix44Buffer2;

	// Shadow maps
	private int smr = 0;
	private GBufferTexture shadow_map_near = new GBufferTexture(DEPTH_SHADOWMAP, 256, 256);
	// private GBufferTexture shadow_map_far = new GBufferTexture(1, 256, 256);
	private FBO shadow_map_renderer_near = new FBO(shadow_map_near);
	// private FBO shadow_map_renderer_far = new FBO(shadow_map_far);

	// Shadow temp matrixes
	private Matrix4f depthMatrix = new Matrix4f();

	// Sky
	public SkyDome sky;

	// Debug
	DebugProfiler updateProfiler = new DebugProfiler();

	// Light
	public List<DefferedLight> lights = new ArrayList<DefferedLight>();

	// Terrain at distance
	public TerrainSummarizer terrain;

	public WeatherEffectsRenderer wer;

	public WorldRenderer(World w)
	{
		// Link world
		world = w;
		world.linkWorldRenderer(this);
		terrain = new TerrainSummarizer(world);
		wer = new WeatherEffectsRenderer(world, this);
		sky = new SkyDome(world, this);
		sizeInChunks = world.getSizeInChunks();
		resizeShadowMaps();

		if (FastConfig.debugGBuffers)
			System.out.println("Loading shaders");

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

		matrix44Buffer = BufferUtils.createFloatBuffer(16);

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
		// shadow_map_far.resize(smr, smr);
	}

	public void renderWorldAtCamera(Camera camera)
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

		// Shadows pre-pass
		if (FastConfig.doShadows)
		{
			Client.profiler.startSection("shadows");
			shadowPass();
		}
		// Prepare matrices
		camera.justSetup();

		// Clear G-Buffers and bind shaded HDR rendertarget
		composite_pass_gbuffers.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		composite_pass_shaded.bind();

		// Draw sky
		if (FastConfig.debugGBuffers)
			glFinish();
		long t = System.nanoTime();
		sky.time = (world.worldTime % 10000) / 10000f;
		sky.skyShader.use(true);
		sky.render(camera);
		if (FastConfig.debugGBuffers)
			glFinish();
		if (FastConfig.debugGBuffers)
			System.out.println("sky took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		// Move camera to relevant position
		camera.translate();
		composite_pass_gbuffers.setEnabledRenderTargets();

		// Render world
		glViewport(0, 0, scrW, scrH);
		renderWorld(false);
		// Render weather
		composite_pass_shaded.bind();
		composite_pass_shaded.setEnabledRenderTargets();
		wer.renderEffects(camera);

		// Debug
		if (FastConfig.debugGBuffers)
			glFinish();
		if (FastConfig.debugGBuffers)
			System.out.println("total took " + (System.nanoTime() - t) / 1000000.0 + "ms ( " + 1 / ((System.nanoTime() - t) / 1000000000.0) + " fps)");

		// Do bloom
		if (FastConfig.doBloom)
		{
			glDisable(GL_DEPTH_TEST);

			bloomShader.use(true);
			bloomShader.setUniformSampler(0, "shadedBuffer", this.composite_shaded);

			this.composite_pass_bloom.bind();
			this.composite_pass_bloom.setEnabledRenderTargets();
			glViewport(0, 0, scrW / 2, scrH / 2);
			ObjectRenderer.drawFSQuad(bloomShader.getVertexAttributeLocation("vertexIn"));

			// Blur bloom

			// Vertical pass
			blurFBO.bind();
			blurV.use(true);
			blurV.setUniformFloat2("screenSize", XolioWindow.frameW / 2f, XolioWindow.frameH / 2f);
			blurV.setUniformFloat("lookupScale", 1);
			blurV.setUniformSampler(0, "inputTexture", this.composite_bloom);
			//drawFSQuad();
			ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));

			// Horizontal pass
			this.composite_pass_bloom.bind();
			blurH.use(true);
			blurH.setUniformFloat2("screenSize", XolioWindow.frameW / 2f, XolioWindow.frameH / 2f);
			blurH.setUniformSampler(0, "inputTexture", blurTemp);
			//drawFSQuad();
			ObjectRenderer.drawFSQuad(blurH.getVertexAttributeLocation("vertexIn"));

			opaqueBlocksShader.use(false);

			// Done blooming
			glViewport(0, 0, scrW, scrH);
		}
		composite_pass_shaded.bind();
		composite_pass_shaded.setEnabledRenderTargets();

		Client.profiler.startSection("done");
		opaqueBlocksShader.use(false);
	}

	private int fastfloor(double x)
	{
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	int maxYChunkLoaded = 0;
	int minYChunkLoaded = 0;

	int maxXChunkLoaded = 0;
	int minXChunkLoaded = 0;

	int maxZChunkLoaded = 0;
	int minZChunkLoaded = 0;

	public void updateRender(float x, float y, float z, float view_rotx, float view_roty)
	{
		// Called every frame, this method takes care of updating the world :
		// It will keep up to date the camera position, as well as a list of
		// to-render chunks in order to fill empty VBO space

		// Upload generated chunks data to GPU
		updateProfiler.reset("vbo upload");
		VBOData toload = chunksRenderer.doneChunk();
		int loadLimit = 16;
		while (toload != null)
		{
			CubicChunk c = world.getChunk(toload.x, toload.y, toload.z, false);
			if (c != null)
			{
				if (c.vbo_id == -1)
					c.vbo_id = glGenBuffers();
				glBindBuffer(GL_ARRAY_BUFFER, c.vbo_id);
				glBufferData(GL_ARRAY_BUFFER, toload.buf, GL_STATIC_DRAW);
				//if (c.vbo_size_normal + c.vbo_size_complex + c.vbo_size_water <= 0)
				//	c.fadeTicks = 25;

				c.vbo_size_normal = toload.s_normal;
				c.vbo_size_complex = toload.s_complex;
				c.vbo_size_water = toload.s_water;

				chunksChanged = true;
			}
			else
			{
				if (FastConfig.debugGBuffers)
					System.out.println("ChunkRenderer outputted a chunk render for a not loaded chunk : ");
				if (FastConfig.debugGBuffers)
					System.out.println("Chunks coordinates : X=" + toload.x + " Y=" + toload.y + " Z=" + toload.z);
				if (FastConfig.debugGBuffers)
					System.out.println("Render information : vbo size =" + toload.s_normal + " and water size =" + toload.s_water);
			}
			loadLimit--;
			if (loadLimit > 0)
				toload = chunksRenderer.doneChunk();
			else
				toload = null;
		}
		// if(FastConfig.debugGBuffers ) glFinish();

		// Update view
		viewX = x;
		viewY = y;
		viewZ = z;
		viewRotH = view_rotx;
		viewRotV = view_roty;
		int npCX = fastfloor((x - 16) / 32);
		int npCY = fastfloor((y) / 32);
		int npCZ = fastfloor((z - 16) / 32);
		// Fill the VBO array with chunks VBO ids if the player changed chunk
		if (pCX != npCX || pCY != npCY || pCZ != npCZ || chunksChanged || true)
		{
			// Update far terrain
			if (pCX != npCX || pCZ != npCZ)
				terrain.generateArround(x, z);

			//if(FastConfig.debugGBuffers ) System.out.println("chunk changed");

			terrain.updateData();

			int chunksViewDistance = (int) (FastConfig.viewDistance / 32);

			// Reset transition variables
			maxYChunkLoaded = 2;
			minYChunkLoaded = -2;

			maxXChunkLoaded = chunksViewDistance - 1;
			minXChunkLoaded = -chunksViewDistance + 1;

			maxZChunkLoaded = chunksViewDistance - 1;
			minZChunkLoaded = -chunksViewDistance + 1;

			// Unload too far chunks
			updateProfiler.startSection("unloadFar");
			List<CubicChunk> allChunks = world.getAllLoadedChunks();
			for (CubicChunk c : allChunks)
			{
				if ((LoopingMathHelper.moduloDistance(c.chunkX, pCX, sizeInChunks) > chunksViewDistance + 1) || (LoopingMathHelper.moduloDistance(c.chunkZ, pCZ, sizeInChunks) > chunksViewDistance + 1) || (Math.abs(c.chunkY - pCY) > 4))
				{
					glDeleteBuffers(c.vbo_id);
					world.removeChunk(c, false);
				}
			}

			// Now delete from the worker threads what we won't need anymore
			chunksRenderer.purgeUselessWork(pCX, pCY, pCZ, sizeInChunks, chunksViewDistance);
			world.ioHandler.requestChunksUnload(pCX, pCY, pCZ, sizeInChunks, chunksViewDistance);
			// Also clean the chunk summaries
			world.chunkSummaries.removeFurther(pCX, pCZ, 32);

			// Raytrace a cone of needed chunks
			renderList.clear();
			CubicChunk chunk;
			for (int d = 0; d < chunksViewDistance; d++)
			{
				int a = 0;
				int c = 0;
				for (int b = pCY - 3; b < pCY + 2; b++)
				{
					for (int i = 0; i < d * 2 + 1; i++)
					{
						a = pCX - d + i;
						c = pCZ - d;
						chunk = world.getChunk(a, b, c, true);
						//if(a == 12 && b == 1 && c == 8)
						//	if(FastConfig.debugGBuffers ) System.out.println("our client" + chunk + " nr ="+chunk.need_render + "ra = " + chunk.requestable);
						if (chunk != null)
						{
							if (chunk.need_render.get() && chunk.dataPointer != -1)
							{
								chunksRenderer.requestChunkRender(chunk);
								//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
							}
							renderList.add(chunk);
						}
						if (d < chunksViewDistance)
						{
							a = pCX + d + 1 - i;
							c = pCZ + d + 1;
							chunk = world.getChunk(a, b, c, true);
							if (chunk != null)
							{
								if (chunk.need_render.get() && chunk.dataPointer != -1)
								{
									chunksRenderer.requestChunkRender(chunk);
									//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
								}
								renderList.add(chunk);
							}
							//
							a = pCX + d + 1;
							c = pCZ - d + i;
							chunk = world.getChunk(a, b, c, true);
							if (chunk != null)
							{
								if (chunk.need_render.get() && chunk.dataPointer != -1)
								{
									chunksRenderer.requestChunkRender(chunk);
									//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
								}
								renderList.add(chunk);
							}
						}
						a = pCX - d;
						c = pCZ + d + 1 - i;
						chunk = world.getChunk(a, b, c, true);
						if (chunk != null)
						{
							if (chunk.need_render.get() && chunk.dataPointer != -1)
							{
								chunksRenderer.requestChunkRender(chunk);
								//chunksRenderer.addTask(a, b, c, chunk.need_render_fast);
							}
							renderList.add(chunk);
						}
					}
				}
			}

			chunksChanged = false;
			// Load nearby chunks
			for (int d = 0; d < chunksViewDistance + 1; d++)
			{
				int a = 0;
				int c = 0;
				for (int b = pCY - 3; b < pCY + 2; b++)
				{
					for (int i = 0; i < d * 2 + 1; i++)
					{
						a = pCX - d + i;
						c = pCZ - d;
						chunk = world.getChunk(a, b, c, true);
						a = pCX + d + 1 - i;
						c = pCZ + d + 1;
						chunk = world.getChunk(a, b, c, true);
						a = pCX + d + 1;
						c = pCZ - d + i;
						chunk = world.getChunk(a, b, c, true);
						a = pCX - d;
						c = pCZ + d + 1 - i;
						chunk = world.getChunk(a, b, c, true);
					}
				}
			}
			boolean valid_chunk;
			// Visibility check
			for (int a = pCX - chunksViewDistance + 1; a < pCX + chunksViewDistance - 1; a++)
				for (int b = pCZ - chunksViewDistance + 1; b < pCZ + chunksViewDistance - 1; b++)
					for (int c = pCY - 2; c < pCY + 2; c++)
					{
						chunk = world.getChunk(a, c, b, false);
						if (chunk == null)
							valid_chunk = false;
						else if (chunk.dataPointer == -1 || chunk.vbo_id != -1)
							valid_chunk = true;
						else
							valid_chunk = false;

						if (!valid_chunk)
						{
							//
							if (c > minYChunkLoaded)
								minYChunkLoaded = c;
						}
					}

		}
		pCX = npCX;
		pCY = npCY;
		pCZ = npCZ;
		// It will send chunks to be rendered in the ChunksRenderer thread and
		// then re-insert the data in the done chunk
	}

	//float a = 0.2f;
	float animationTimer = 0.0f;

	public int renderedVertices = 0;
	public int renderedVerticesShadow = 0;
	public int renderedChunks = 0;

	private Vector3f viewerPosVector;

	private Vector3f viewerCamDirVector;

	public void shadowPass()
	{
		// float worldTime = (world.worldTime%1000+1000)%1000;
		if (this.getShadowVisibility() == 0f)
			return; // No shadows at night :)
		glCullFace(GL_FRONT);
		glDisable(GL_CULL_FACE);
		glDisable(GL_ALPHA_TEST);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);

		int size = (shadow_map_near).getWidth();
		glViewport(0, 0, size, size);

		shadow_map_renderer_near.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		shadowsPassShader.use(true);
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
		depthMatrix.translate(new Vector3f((float) Math.floor(camera.camPosX), (float) Math.floor(camera.camPosY), (float) Math.floor(camera.camPosZ)));
		depthMatrix.store(matrix44Buffer);
		matrix44Buffer.flip();
		shadowsPassShader.setUniformMatrix4f("depthMVP", matrix44Buffer);
		shadowsPassShader.setUniformMatrix4f("localTransform", new Matrix4f());

		renderWorld(true);
		shadowsPassShader.use(false);
		glViewport(0, 0, scrW, scrH);
	}

	private boolean checkChunkOcclusion(CubicChunk chunk, int correctedCX, int correctedCY, int correctedCZ, Vector3f viewerPosition, Vector3f viewerDirection)
	{
		Vector3f centerSphere = new Vector3f(correctedCX * 32 + 16, correctedCY * 32 + 16, correctedCZ * 32 + 16);
		if (!InputAbstractor.isKeyDown(org.lwjgl.input.Keyboard.KEY_F10))
			return camera.isBoxInFrustrum(centerSphere, new Vector3f(32, 32, 32));

		double coneAngle = (camera.fov) * (scrW / (scrH * 1f));
		if (scrW == scrH)
			coneAngle = camera.fov;

		// coneAngle/=2;

		coneAngle = coneAngle / 180d * Math.PI;
		Vector3f v = new Vector3f();
		Vector3f.sub(centerSphere, viewerPosition, v);
		viewerDirection.normalise(viewerDirection);
		float a = Vector3f.dot(v, viewerDirection);
		double b = a * Math.tan(coneAngle);
		double c = Math.sqrt(Vector3f.dot(v, v) - a * a);
		double d = c - b;
		double e = d * Math.cos(coneAngle);
		if (e >= 27.71281292d) // R
			return false;
		return true;
	}

	Texture glowTexture = TexturesHandler.getTexture("environement/glow.png");
	Texture skyTexture = TexturesHandler.getTexture("environement/sky.png");
	Texture lightmapTexture = TexturesHandler.getTexture("environement/light.png");
	Texture waterNormalTexture = TexturesHandler.getTexture("normal.png");

	Texture blocksDiffuseTexture = TexturesHandler.getTexture("tiles_merged_diffuse.png");
	Texture blocksNormalTexture = TexturesHandler.getTexture("tiles_merged_normal.png");

	public void renderWorld(boolean shadowPass)
	{
		long t;
		animationTimer = (float) (((System.currentTimeMillis() % 100000) / 200f) % 100.0);

		int chunksViewDistance = (int) (FastConfig.viewDistance / 32);

		skyTexture = TexturesHandler.getTexture(world.isRaining() ? "environement/sky_rain.png" : "environement/sky.png");
		
		int[] vegetationColor = { 72, 128, 45 };
		//int[] vegetationColor = { 35, 35, 35 };
		//int[] vegetationColor = { (int) (Math.sin((System.currentTimeMillis() % 1000 ) / 1000.0 * Math.PI) * 255 * 0.5 + 255/2), (int) (Math.sin((System.currentTimeMillis() % 2000 ) / 2000.0 * Math.PI) * 255 * 0.5 + 255/2),	(int) (Math.cos((System.currentTimeMillis() % 5000 ) / 5000.0 * Math.PI) * 255 * 0.5 + 255/2)};
		//int[] vegetationColor = { (int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255) };

		Vector3f sunPos = sky.getSunPos();
		float shadowVisiblity = getShadowVisibility();
		chunksViewDistance = sizeInChunks / 2;
		if (!shadowPass)
		{
			this.composite_pass_shaded.bind();

			Client.profiler.startSection("terrain");
			glDisable(GL_BLEND);

			terrainShader.use(true);
			camera.setupShader(terrainShader);
			terrainShader.setUniformFloat3("vegetationColor", vegetationColor[0] / 255f, vegetationColor[1] / 255f, vegetationColor[2] / 255f);
			terrainShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);
			terrainShader.setUniformFloat("time", animationTimer);
			terrainShader.setUniformFloat("terrainHeight", world.chunkSummaries.getHeightAt((int) viewX, (int) viewZ));
			terrainShader.setUniformFloat("viewDistance", FastConfig.viewDistance);
			terrainShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
			terrainShader.setUniformSampler(6, "lightColors", lightmapTexture);
			terrainShader.setUniformSampler(5, "normalTexture", waterNormalTexture);
			waterNormalTexture.setLinearFiltering(true);
			waterNormalTexture.setMipMapping(true);
			terrainShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
			terrainShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
			terrainShader.setUniformSampler(8, "glowSampler", glowTexture);
			terrainShader.setUniformSampler(7, "colorSampler", skyTexture);
			setupShadowColors(terrainShader);
			terrainShader.setUniformFloat("time", sky.time);
			terrainShader.setUniformFloat("isRaining", world.isRaining() ? 1f : 0f);

			if (Client.world.name.contains("cherna"))
				terrainShader.setUniformFloat("waterLevel", 4f);

			if (FastConfig.debugGBuffers)
				glFinish();
			t = System.nanoTime();
			if (!InputAbstractor.isKeyDown(org.lwjgl.input.Keyboard.KEY_F9))
				renderedVertices += terrain.draw(camera, terrainShader);

			if (FastConfig.debugGBuffers)
				glFinish();
			if (FastConfig.debugGBuffers)
				System.out.println("terrain took " + (System.nanoTime() - t) / 1000000.0 + "ms");

			Client.profiler.startSection("blocks");
			this.composite_pass_gbuffers.setEnabledRenderTargets();

			opaqueBlocksShader.use(true);
			opaqueBlocksShader.setUniformSampler(0, "diffuseTexture", blocksDiffuseTexture);
			opaqueBlocksShader.setUniformSampler(1, "normalTexture", blocksNormalTexture);
			opaqueBlocksShader.setUniformSampler(4, "lightColors", lightmapTexture);
			blocksDiffuseTexture.setTextureWrapping(false);
			blocksDiffuseTexture.setLinearFiltering(false);
			blocksDiffuseTexture.setMipMapping(false);
			blocksDiffuseTexture.setMipmapLevelsRange(0, 4);

			blocksNormalTexture.setTextureWrapping(false);
			blocksNormalTexture.setLinearFiltering(false);
			blocksNormalTexture.setMipMapping(true);
			blocksNormalTexture.setMipmapLevelsRange(0, 4);

			opaqueBlocksShader.setUniformFloat3("vegetationColor", vegetationColor[0] / 255f, vegetationColor[1] / 255f, vegetationColor[2] / 255f);
			//opaqueBlocksShader.setUniformFloat("viewDistance", FastConfig.viewDistance);
			opaqueBlocksShader.setUniformFloat("shadowVisiblity", shadowVisiblity);
			opaqueBlocksShader.setUniformFloat2("screenSize", scrW, scrH);
			opaqueBlocksShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
			opaqueBlocksShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
			//opaqueBlocksShader.setUniformFloat3("sunPos", sunPos.x, sunPos.y, sunPos.z);
			//opaqueBlocksShader.setUniformFloat3("blockColor", 1f, 1f, 1f);
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
			shadowsPassShader.use(true);
			shadowsPassShader.setUniformFloat("time", animationTimer);
			depthMatrix.store(matrix44Buffer);
			matrix44Buffer.flip();
			shadowsPassShader.setUniformMatrix4f("depthMVP", matrix44Buffer);
			opaqueBlocksShader.setUniformSampler(0, "albedoTexture", blocksDiffuseTexture);
		}

		//if(true)
		//	return;
		
		// We just roll our loaded vbos
		//TexturesHandler.bindTexture("tiles_merged_diffuse.png");

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
			glEnableVertexAttribArray(colorIn);
			RenderingContext.enableVAMode(vertexIn, texCoordIn, colorIn, normalIn, false);
			RenderingContext.setCurrentShader(opaqueBlocksShader);
		}
		else
		{
			vertexIn = shadowsPassShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = shadowsPassShader.getVertexAttributeLocation("texCoordIn");
			normalIn = shadowsPassShader.getVertexAttributeLocation("normalIn");
			RenderingContext.enableVAMode(vertexIn, texCoordIn, colorIn, normalIn, true);
			RenderingContext.setCurrentShader(shadowsPassShader);
		}

		glEnableVertexAttribArray(normalIn);
		glEnableVertexAttribArray(vertexIn);
		glEnableVertexAttribArray(texCoordIn);

		// Culling vectors
		viewerPosVector = new Vector3f(viewX, viewY, viewZ);
		float transformedViewH = (float) ((viewRotH) / 180 * Math.PI);
		// if(FastConfig.debugGBuffers ) System.out.println(Math.sin(transformedViewV)+"f");
		viewerCamDirVector = new Vector3f((float) (Math.sin((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)), (float) (Math.cos((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)));

		if (FastConfig.debugGBuffers)
			glFinish();
		t = System.nanoTime();

		// renderList.clear();
		glDisable(GL_BLEND);
		for (CubicChunk chunk : renderList)
		{
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

			//if(FastConfig.debugGBuffers ) System.out.println("TOPKEK");

			// Update if chunk was modified
			if (chunk.need_render.get() && chunk.requestable.get() && chunk.dataPointer != -1)
			{
				// Launch task
				chunksRenderer.requestChunkRender(chunk);
				//if(FastConfig.debugGBuffers ) System.out.println("TOPKEK");
			}
			// Don't bother if it don't render anything
			if (chunk.vbo_size_normal + chunk.vbo_size_complex == 0)
				continue;
			// If we're doing shadows
			if (shadowPass)
			{
				// TODO : make proper orthogonal view checks etc
				float distanceX = LoopingMathHelper.moduloDistance(pCX, chunk.chunkX, sizeInChunks);
				float distanceZ = LoopingMathHelper.moduloDistance(pCZ, chunk.chunkZ, sizeInChunks);

				if (distanceX > 4 || distanceZ > 4)
					continue;
			}
			else
			{
				// Cone occlusion checking !
				int correctedCX = vboDekalX / 32;
				int correctedCY = chunk.chunkY;
				int correctedCZ = vboDekalZ / 32;

				boolean shouldShowChunk = ((int) (viewX / 32) == chunk.chunkX) && ((int) (viewY / 32) == correctedCY) && ((int) (viewZ / 32) == correctedCZ);
				if (!shouldShowChunk)
					shouldShowChunk = checkChunkOcclusion(chunk, correctedCX, correctedCY, correctedCZ, viewerPosVector, viewerCamDirVector);
				if (!shouldShowChunk)
					continue; // Again, don't bother if there isn't anthing to
								// render
			}
			if (!shadowPass)
				opaqueBlocksShader.setUniformFloat3("borderShift", vboDekalX, chunk.chunkY * 32f, vboDekalZ);
			else
				shadowsPassShader.setUniformFloat3("borderShift", vboDekalX, chunk.chunkY * 32f, vboDekalZ);

			glBindBuffer(GL_ARRAY_BUFFER, chunk.vbo_id);
			int geometrySize = chunk.vbo_size_normal;

			// Texture data is offset by the vertex data as
			// 64 x 64 x 3 vertices x 2 triangles x 3 coordinates x 4 bytes per float
			// So it's like 64x64x3x2 is the geometry size, we do 3x4 geometry for textcoords
			glVertexAttribPointer(vertexIn, 4, GL_INT_2_10_10_10_REV, false, 4, 0);
			int vertexSize = 4;
			glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 4, (geometrySize) * vertexSize);
			if (!shadowPass)
				glVertexAttribPointer(colorIn, 4, GL_UNSIGNED_BYTE, true, 4, (geometrySize) * (vertexSize + 4));
			glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 0, (geometrySize) * (vertexSize + 8));

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

			if (chunk.vbo_size_complex > 0)
			{
				geometrySize = chunk.vbo_size_complex;
				int dekal = (chunk.vbo_size_normal + chunk.vbo_size_water) * (4) * 4;
				vertexSize = 12;
				glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, vertexSize, dekal + 0);

				glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 4, dekal + (geometrySize) * vertexSize);
				if (!shadowPass)
					glVertexAttribPointer(colorIn, 4, GL_UNSIGNED_BYTE, true, 4, dekal + (geometrySize) * (vertexSize + 4));
				glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 0, dekal + (geometrySize) * (vertexSize + 8));

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

			glDisableVertexAttribArray(vertexIn);
			glDisableVertexAttribArray(texCoordIn);
			glDisableVertexAttribArray(colorIn);
			glDisableVertexAttribArray(normalIn);

			// Select shader
			entitiesShader.use(true);

			vertexIn = entitiesShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = entitiesShader.getVertexAttributeLocation("texCoordIn");
			normalIn = entitiesShader.getVertexAttributeLocation("normalIn");

			glEnableVertexAttribArray(vertexIn);
			glEnableVertexAttribArray(texCoordIn);
			glEnableVertexAttribArray(normalIn);

			RenderingContext.enableVAMode(vertexIn, texCoordIn, 0, normalIn, true);
			RenderingContext.setCurrentShader(entitiesShader);
			entitiesShader.setUniformMatrix4f("localTransformNormal", new Matrix4f());

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
		for (Entity e : world.getAllLoadedEntities())
		{
			e.render();
			// Also populate lights buffer
			el = e.getLights();
			if (el != null)
				for (DefferedLight l : el)
					lights.add(l);
		}
		// Particles zzz
		Client.world.particlesHolder.render(camera);
		glEnable(GL_CULL_FACE);

		glDisableVertexAttribArray(normalIn);
		glDisableVertexAttribArray(vertexIn);
		glDisableVertexAttribArray(texCoordIn);

		if (shadowPass)
			return;

		// Solid blocks done, now render water & lights
		glDisable(GL_CULL_FACE);
		//glEnable(GL_BLEND);
		glDisable(GL_ALPHA_TEST);

		// We do water in two passes : one for computing the refracted color and putting it in shaded buffer, and another one
		// to read it back and blend it
		glDepthFunc(GL_LEQUAL);
		for (int pass = 1; pass < 3; pass++)
		{
			liquidBlocksShader = ShadersLibrary.getShaderProgram("blocks_liquid_pass" + (pass));

			liquidBlocksShader.use(true);

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

			liquidBlocksShader.setUniformMatrix4f("shadowMatrix", matrix44Buffer);
			// liquidBlocksShader.setUniformMatrix4f("shadowMatrix2",
			// matrix44Buffer2);

			// Vertex attributes setup
			vertexIn = liquidBlocksShader.getVertexAttributeLocation("vertexIn");
			texCoordIn = liquidBlocksShader.getVertexAttributeLocation("texCoordIn");
			colorIn = liquidBlocksShader.getVertexAttributeLocation("colorIn");
			normalIn = liquidBlocksShader.getVertexAttributeLocation("normalIn");

			glEnableVertexAttribArray(vertexIn);
			glEnableVertexAttribArray(texCoordIn);
			glEnableVertexAttribArray(colorIn);
			glEnableVertexAttribArray(normalIn);

			// Set rendering context.
			RenderingContext.enableVAMode(vertexIn, texCoordIn, colorIn, normalIn, false);
			RenderingContext.setCurrentShader(liquidBlocksShader);

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
				if (chunk.vbo_size_water == 0)
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

				glBindBuffer(GL_ARRAY_BUFFER, chunk.vbo_id);
				int geometrySize = chunk.vbo_size_water;
				int dekal = (chunk.vbo_size_normal) * (4) * 4;
				// Texture data is offset by the vertex data as
				// 64 x 64 x 3 vertices x 2 triangles x 3 coordinates x 4 bytes
				// per float
				// So it's like 64x64x3x2 is the geometry size, we do
				// 3x4xgeometry for textcoords

				// glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 12, 0);
				glVertexAttribPointer(vertexIn, 4, GL_INT_2_10_10_10_REV, false, 4, dekal + 0);
				int vertexSize = 4;
				glVertexAttribPointer(texCoordIn, 2, GL_UNSIGNED_SHORT, false, 4, dekal + (geometrySize) * vertexSize);
				glVertexAttribPointer(colorIn, 3, GL_UNSIGNED_BYTE, true, 4, dekal + (geometrySize) * (vertexSize + 4));
				glVertexAttribPointer(normalIn, 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 0, dekal + (geometrySize) * (vertexSize + 8));

				if (geometrySize > 0)
				{
					glDrawArrays(GL_TRIANGLES, 0, geometrySize);
					// if(FastConfig.debugGBuffers ) System.out.println(geometrySize+":"+dekal);
					renderedVertices += geometrySize;
				}
			}

			// Disable vertex attributes
			glDisableVertexAttribArray(vertexIn);
			glDisableVertexAttribArray(texCoordIn);
			glDisableVertexAttribArray(colorIn);
			glDisableVertexAttribArray(normalIn);
			RenderingContext.disableVAMode();
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
		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		lightShader.use(true);

		lightShader.setUniformSampler(1, "albedoBuffer", this.composite_albedo);
		lightShader.setUniformSampler(0, "metaBuffer", this.composite_meta);
		//lightShader.setUniformSampler(1, "lightBuffer", this.composite_light);

		lightShader.setUniformSampler(2, "comp_depth", this.composite_zbuffer);
		lightShader.setUniformSampler(3, "comp_normal", this.composite_normal);

		//lightShader.setUniformSampler(4, "comp_spec", this.composite_specular);
		lightShader.setUniformFloat("powFactor", 5f);
		camera.setupShader(lightShader);
		lightShader.setUniformFloat3("camPos", viewX, viewY, viewZ);
		glEnable(GL_BLEND);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_DEPTH_TEST);
		glBlendFunc(GL_ONE, GL_ONE);
		lightsBuffer = 0;
		for (DefferedLight light : lights)
		{
			renderDefferedLight(light);
			// if(FastConfig.debugGBuffers ) System.out.println("rendering deffered light");
		}
		Client.world.particlesHolder.renderLights(this);
		// Render remaining lights
		if (lightsBuffer > 0)
		{
			// if(FastConfig.debugGBuffers ) System.out.println("one light remaing");
			lightShader.setUniformInt("lightsToRender", lightsBuffer);
			ObjectRenderer.drawFSQuad(lightShader.getVertexAttributeLocation("vertexIn"));
			//drawFSQuad();
		}

		glDepthMask(true);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
		lightShader.use(false);
		this.composite_pass_gbuffers.setEnabledRenderTargets();
		lights.clear();

	}

	private void setupShadowColors(ShaderProgram shader)
	{
		if(world.isRaining())
		{
			shader.setUniformFloat("shadowStrength", 0.75f);
			shader.setUniformFloat3("shadowColor", 0.20f, 0.20f, 0.20f);
		}
		else
		{
			shader.setUniformFloat("shadowStrength", 0.90f);
			shader.setUniformFloat3("shadowColor", 0.20f, 0.20f, 0.31f);
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
		if (FastConfig.debugGBuffers)
			System.out.println("setup render size");
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

		ssaoShader.use(true);

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
		//drawFSQuad();
		ssaoShader.use(false);

		// Blur bloom

		// Vertical pass
		blurFBO.bind();
		blurV.use(true);
		blurV.setUniformFloat2("screenSize", XolioWindow.frameW * 2, XolioWindow.frameH * 2);
		blurV.setUniformFloat("lookupScale", 2);
		blurV.setUniformSampler(0, "inputTexture", this.composite_ssao);
		ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

		// Horizontal pass
		this.composite_pass_ssao.bind();
		blurH.use(true);
		blurH.setUniformFloat2("screenSize", XolioWindow.frameW * 2, XolioWindow.frameH * 2);
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

		applyShadowsShader.use(true);
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
		//TODO if SSAO
		applyShadowsShader.setUniformSampler(8, "ssaoBuffer", composite_ssao);

		//applyShadowsShader.setUniformSampler(4, "comp_spec", this.composite_specular);

		applyShadowsShader.setUniformFloat("sunIntensity", sky.getShadowIntensity());
		applyShadowsShader.setUniformFloat("time", sky.time);

		// Sky color etc
		int[] skyColor = sky.getSkyColor();

		applyShadowsShader.setUniformFloat3("skyColor", skyColor[0] / 255f, skyColor[1] / 255f, skyColor[2] / 255f);
		applyShadowsShader.setUniformFloat3("camPos", camera.camPosX, camera.camPosY, camera.camPosZ);

		depthMatrix.store(matrix44Buffer);
		matrix44Buffer.flip();

		applyShadowsShader.setUniformFloat("shadowMapResolution", smr);
		applyShadowsShader.setUniformFloat("shadowVisiblity", getShadowVisibility());
		applyShadowsShader.setUniformMatrix4f("shadowMatrix", matrix44Buffer);
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
		postProcess.use(true);
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

		ObjectRenderer.drawFSQuad(postProcess.getVertexAttributeLocation("vertexIn"));
		//drawFSQuad();

		if (FastConfig.debugGBuffers)
			glFinish();

		if (FastConfig.debugGBuffers)
			System.out.println("final blit took " + (System.nanoTime() - t) / 1000000.0 + "ms");

		postProcess.use(false);
		/*for (Entity e : world.getAllLoadedEntities())
		{
			if (e instanceof EntityHUD)
				((EntityHUD) e).drawHUD(camera);
		}*/
	}

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

	public void screenCubeMap(int resolution)
	{
		// Save state
		int oldW = scrW;
		int oldH = scrH;
		XolioWindow.frameH = resolution;
		XolioWindow.frameW = resolution;
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
				camera.view_rotx = 0;
				camera.view_roty = 0;
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
			// if(FastConfig.debugGBuffers ) System.out.println(Math.sin(transformedViewV)+"f");
			viewerCamDirVector = new Vector3f((float) (Math.sin((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)), (float) (Math.cos((180 + viewRotV) / 180 * Math.PI) * Math.cos(transformedViewH)));

			this.composite_pass_shaded.bind();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Scene rendering
			this.renderWorldAtCamera(camera);

			// GL access
			glBindTexture(GL_TEXTURE_2D, composite_shaded.getID());
			glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

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
					int r = bbuf.get(i) & 0xFF;
					int g = bbuf.get(i + 1) & 0xFF;
					int b = bbuf.get(i + 2) & 0xFF;
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
		// Revert correct data
		camera.view_rotx = camX;
		camera.view_roty = camY;
		camera.view_rotz = camZ;
		camera.fov = fov;
		XolioWindow.frameH = oldH;
		XolioWindow.frameW = oldW;
		camera.justSetup();
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
