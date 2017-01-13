package io.xol.chunkstories.api.rendering.entity;

import io.xol.chunkstories.api.utils.IterableIterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderingIterator<E extends EntityRenderable> extends IterableIterator<E>
{
	/**
	 * __The specification of Iterator__
	 * 
	 * If the next element is within the view frustrum it will also prepare the renderer for this object
	 */
	public E next();
	
	/**
	 * Checks the current element is in view frustrum
	 * Requires you have already called next() and it returned a non-null value, otherwise it will throw a RuntimeException
	 */
	public boolean isCurrentElementInViewFrustrum();
	
	/**
	 * Returns a filtered iterator that only returns elements in-frustrum
	 */
	public RenderingIterator<E> getElementsInFrustrumOnly();
}
