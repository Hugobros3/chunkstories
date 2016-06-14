package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.renderer.Camera;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * An entity that draws 2d stuff on screen upon rendering
 */
public interface EntityHUD
{
	public abstract void drawHUD(Camera camera);
}
