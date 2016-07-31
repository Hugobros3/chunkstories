package io.xol.chunkstories.api.world;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.WorldRenderer;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A 'Client' world is one responsible of graphical and input tasks
 * A world can be both client and master.
 */
public interface WorldClient extends World
{
	public Client getClient();

	public void linkWorldRenderer(WorldRenderer renderer);
	
	public WorldRenderer getWorldRenderer();
}
