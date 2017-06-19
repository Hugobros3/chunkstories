package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Only the client actually renders particles */
public interface ParticlesRenderer extends ParticlesManager
{
	public ClientInterface getClient();
	
	public ClientContent getContent();
	
	/** Internal to the engine */
	public int render(RenderingInterface renderingInterface, boolean isThisGBufferPass);
}
