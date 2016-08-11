package io.xol.chunkstories.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Implementation of common methods to WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient
{
	public WorldClientCommon(WorldInfo info)
	{
		super(info);
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}

	@Override
	public void linkWorldRenderer(WorldRenderer renderer)
	{
		this.renderer = renderer;
		setParticlesManager(new ParticlesRenderer(this));
	}

	@Override
	public WorldRenderer getWorldRenderer()
	{
		return renderer;
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return renderer.getDecalsRenderer();
	}
}
