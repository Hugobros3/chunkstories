package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * An entity that draws 2d stuff on screen upon rendering
 */
public interface EntityOverlay
{
	public abstract void drawEntityOverlay(RenderingInterface renderingInterface);
}
