package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.effects.DecalsRenderer;
import io.xol.chunkstories.api.world.WorldClient;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface WorldRenderer
{
	public WorldClient getWorld();
	
	public SkyboxRenderer getSky();
	
	/** Renders the world into the back buffer */
	public void renderWorld(RenderingInterface renderingInterface);
	
	/** Blits that buffer back */
	public void blitFinalImage(RenderingInterface renderingInterface);

	/** Tells the chunks renderer to rebuilt it's PVS set */
	public void flagChunksModified();

	/** Resizes the shadow maps to fit the user parameters */
	public void resizeShadowMaps();

	/** Resizes the rendering buffers to fit the game window */
	public void setupRenderSize();

	/** Lists passes the 3d engines does */
	public enum RenderingPass
	{
		SHADOW, //Depth-only for sun and light shadows
		NORMAL_OPAQUE, 
		NORMAL_LIQUIDS_PASS_1, 
		NORMAL_LIQUIDS_PASS_2, 
		ALPHA_BLENDED, 
		INTERNAL, //Internal states of the renderer, do not mess with those.
		;
	}

	/** Returns null or a valid element of RenderingPasses */
	public RenderingPass getCurrentRenderingPass();
	
	public FarTerrainMeshRenderer getFarTerrainRenderer();
	
	interface FarTerrainMeshRenderer {
		public void markFarTerrainMeshDirty();
	}

	public DecalsRenderer getDecalsRenderer();

	public ParticlesRenderer getParticlesRenderer();

	public WorldEffectsRenderer getWorldEffectsRenderer();

	public String screenShot();
}
