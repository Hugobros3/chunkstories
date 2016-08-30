package io.xol.chunkstories.api.rendering.entity;

import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityRenderer<E extends EntityRenderable>
{
	/**
	 * Called when starting to render all entities of this type
	 */
	public void setupRender(RenderingContext renderingContext);
	
	/**
	 * Called for each entity
	 */
	public void forEach(RenderingContext renderingContext, RenderingIterator<E> renderableEntitiesIterator);
	
	public void freeRessources();
}
