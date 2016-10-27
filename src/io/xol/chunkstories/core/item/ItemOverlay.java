package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An interface for items that draw on top of the 2d screen of the user ( but before actual GUI elements are)
 */
public interface ItemOverlay
{
	public void drawItemOverlay(RenderingInterface renderingInterface);
}
