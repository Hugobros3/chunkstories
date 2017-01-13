package io.xol.chunkstories.api.rendering;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Handles weather and other whole-world effects
 */
public interface WorldEffectsRenderer
{
	/**
	 * Called when rendering a frame
	 */
	public void renderEffects(RenderingInterface renderingContext);

	/**
	 * Called each tick (default is 60 tps)
	 */
	public void tick();

	/**
	 * Called when cleaning up ( deletes vbos, frees textures )
	 */
	public void destroy();
}