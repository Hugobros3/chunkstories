package io.xol.chunkstories.api.rendering.entity;

import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityRenderer<E extends EntityRenderable>
{
	/**
	 * Called when starting to render all entities of this type
	 */
	public void setupRender(RenderingInterface renderingInterface);
	
	/**
	 * Called for each entity
	 */
	public int forEach(RenderingInterface renderingInterface, RenderingIterator<E> renderableEntitiesIterator);
	
	public void freeRessources();
}
