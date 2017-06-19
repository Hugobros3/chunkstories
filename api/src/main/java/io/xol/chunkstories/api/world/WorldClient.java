package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A 'Client' world is one responsible of graphical and input tasks
 * A world can be both client and master.
 */
public interface WorldClient extends World
{
	public ClientInterface getClient();
	
	public WorldRenderer getWorldRenderer();
}
