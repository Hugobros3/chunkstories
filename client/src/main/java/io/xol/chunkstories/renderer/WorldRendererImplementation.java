//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.chunks.ChunkMeshesRenderer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRendererImplementation;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.renderer.passes.RenderingGraph;
import io.xol.chunkstories.renderer.terrain.FarTerrainGSMeshRenderer;
import io.xol.chunkstories.renderer.terrain.FarTerrainMeshRenderer;
//import io.xol.chunkstories.renderer.terrain.FarTerrainNoMeshRenderer;
import io.xol.chunkstories.renderer.terrain.SummariesArrayTexture;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;

/** A tragically huge behemoth, held responsible of actually displaying all that mess */
public class WorldRendererImplementation implements WorldRenderer
{
	protected final Logger logger = LoggerFactory.getLogger("renderer.world");
	
	private final GameWindow gameWindow;
	private final WorldClientCommon world;
	
	private ChunkMeshesRenderer chunksRenderer;
	
	private RenderingGraph renderingGraph;
	
	CulledEntitiesRenderer entitiesRenderer;
	SkyRenderer skyRenderer;
	DecalsRendererImplementation decalsRenderer;
	ClientParticlesRenderer particlesRenderer;
	FarTerrainRenderer farTerrainRenderer;
	SummariesTexturesHolder summariesTexturesHolder;
	WorldEffectsRenderer weatherEffectsRenderer;
	//CubemapRenderer cubemapRenderer;

	AverageLuma averageLuma;
	
	float animationTimer = 0.0f;
	float apertureModifier = 1.0f;

	public WorldRendererImplementation(WorldClientCommon world, ClientInterface client)
	{
		this.world = world;
		this.gameWindow = client.getGameWindow();

		//Creates subsystems
		this.chunksRenderer = new ChunkMeshesRenderer(this);
		
		this.entitiesRenderer = new CulledEntitiesRenderer(world);
		this.particlesRenderer = new ClientParticlesRenderer(world);
		this.farTerrainRenderer = new FarTerrainGSMeshRenderer(this);
		this.summariesTexturesHolder = new SummariesArrayTexture(client, world);
		this.weatherEffectsRenderer = new DefaultWeatherEffectsRenderer(world, this);
		this.decalsRenderer = new DecalsRendererImplementation(this);
		
		this.averageLuma = new AverageLuma();
		//this.cubemapRenderer = new CubemapRenderer(this);
		
		this.renderingGraph = new RenderingGraph(this.getRenderingInterface());
	}

	@Override
	public WorldClient getWorld()
	{
		return world;
	}

	@Override
	public SkyRenderer getSkyRenderer()
	{
		return skyRenderer;
	}

	@Override
	public void setSkyRenderer(SkyRenderer skyRenderer) {
		this.skyRenderer = skyRenderer;
	}
	
	public void renderWorld(RenderingInterface renderingInterface)
	{
		((SummariesArrayTexture) summariesTexturesHolder).update();
		
		//if(RenderingConfig.doDynamicCubemaps)
		//	cubemapRenderer.renderWorldCubemap(renderingInterface, renderBuffers.rbEnvironmentMap, 128, true);
		
		//Step one, set the camera to the proper spot
		CameraInterface mainCamera = renderingInterface.getCamera();
		EntityControllable entity = world.getClient().getPlayer().getControlledEntity();
		
		if(entity != null)
			entity.setupCamera(renderingInterface);
		
		animationTimer = ((float)(System.currentTimeMillis() & 0x7FFF)) / 100.0f;
		
		//TODO remove entirely
		FakeImmediateModeDebugRenderer.setCamera(mainCamera);
		chunksRenderer.updatePVSSet(mainCamera);
		
		// Prepare matrices
		mainCamera.setupUsingScreenSize(gameWindow.getWidth(), gameWindow.getHeight());
		
		this.renderWorldInternal(renderingInterface);
	}
	
	protected void renderWorldInternal(RenderingInterface renderingInterface) {
		this.renderingGraph.render(renderingInterface);
		
		Texture finalBuffer = this.renderingGraph.getRenderPass("forward").resolvedOutputs.get("shadedBuffer");
		if(finalBuffer != null && finalBuffer instanceof Texture2DRenderTargetGL) {
			this.averageLuma.computeAverageLuma((Texture2DRenderTargetGL) finalBuffer);
		}
	}
	
	public void blitFinalImage(RenderingInterface renderingContext, boolean hideGui)
	{
		Texture finalBuffer = this.renderingGraph.getRenderPass("final").resolvedOutputs.get("finalBuffer");
		
		if(finalBuffer != null && finalBuffer instanceof Texture2D) {
			final Texture2D finalTexture = (Texture2D)(finalBuffer);
			
			// We render to the screen.
			renderingContext.getRenderTargetManager().setConfiguration(null);

			renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
			renderingContext.setBlendMode(BlendMode.DISABLED);

			renderingContext.useShader("blit");
			
			renderingContext.bindTexture2D("diffuseTexture", finalTexture);

			renderingContext.drawFSQuad();
			
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
	}
	
	public void destroy()
	{
		particlesRenderer.destroy();
		farTerrainRenderer.destroy();
		summariesTexturesHolder.destroy();
		entitiesRenderer.clearLoadedEntitiesRenderers();
		chunksRenderer.destroy();
		
		averageLuma.destroy();
	}
	
	@Override
	public void flagChunksModified()
	{
		//Nothing. We do our PVS every frame anyways
	}

	@Override
	public void setupRenderSize()
	{
		int width = gameWindow.getWidth();
		int height = gameWindow.getHeight();
		this.setupRenderSize(width, height);
	}
	
	public void setupRenderSize(int width, int height) {
		this.renderingGraph.resize(width, height);
	}

	/** Debug-related, usually not called in gameplay */
	public void reloadContentSpecificStuff()
	{
		//TODO REMOVE REMOVE REMOVE
		if(farTerrainRenderer instanceof FarTerrainMeshRenderer)
			((FarTerrainMeshRenderer) farTerrainRenderer).markVoxelTexturesSummaryDirty();
		
		entitiesRenderer.clearLoadedEntitiesRenderers();
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

	@Override
	public SummariesTexturesHolder getSummariesTexturesHolder() {
		return summariesTexturesHolder;
	}

	@Override
	public RenderingInterface getRenderingInterface() {
		return Client.getInstance().getGameWindow().getRenderingContext();
	}

	@Override
	public GameWindow getWindow() {
		return gameWindow;
	}

	@Override
	public RenderPasses renderPasses() {
		return renderingGraph;
	}
	
	@Override
	public float getAnimationTimer() {
		return this.animationTimer;
	}
	

	@Override
	public ChunkMeshesRenderer getChunksRenderer() {
		return this.chunksRenderer;
	}

	@Override
	public EntitiesRenderer getEntitiesRenderer() {
		return this.entitiesRenderer;
	}
}
