package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.exceptions.RenderingException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A self-contained object that can render itself in a single method, taking care of setting up uniforms etc itself.
 */
public interface Renderable
{
	/**
	 * Setups the RenderingInterface as it needs and registers a RenderingCommand
	 * @throws RenderingException 
	 */
	public RenderingCommand render(RenderingInterface renderingInterface) throws RenderingException;
}
