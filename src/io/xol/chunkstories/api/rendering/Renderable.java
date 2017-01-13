package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A self-contained object that can render itself in a single method, taking care of setting up uniforms etc itself.
 */
public interface Renderable
{
	/**
	 * Setups the RenderingInterface as it needs and registers RenderingCommands
	 * @throws RenderingException 
	 */
	public void render(RenderingInterface renderingInterface) throws RenderingException;
}
