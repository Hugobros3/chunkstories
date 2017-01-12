package io.xol.chunkstories.world;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRenderer;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Implementation of common methods to WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient
{
	public WorldClientCommon(Client client, WorldInfo info)
	{
		super(client, info);
		
		this.renderer = new WorldRenderer(this);
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}
	
	public GameContext getGameContext()
	{
		return getClient();
	}

	@Override
	public WorldRenderer getWorldRenderer()
	{
		return renderer;
	}

	@Override
	public DecalsRenderer getDecalsManager()
	{
		return renderer.getDecalsRenderer();
	}

	@Override
	public ParticlesRenderer getParticlesManager()
	{
		return renderer.getParticlesRenderer();
	}

	@Override
	public void tick()
	{
		super.tick();

		this.getWorldRenderer().getWorldEffectsRenderer().tick();
	}
}
