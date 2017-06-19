package io.xol.chunkstories.api.rendering.mesh;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.Renderable;
import io.xol.chunkstories.api.rendering.RenderingInterface;

public interface RenderableMesh extends Renderable {
	
	/**
	 * Renders the mesh using the current pipeline state
	 * @throws RenderingException 
	 */
	public void render(RenderingInterface renderingInterface);// throws RenderingException;
}
