package io.xol.chunkstories.world;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.particles.ClientParticleManager;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.renderer.decals.DecalsRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Implementation of common methods to WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient
{
	public WorldClientCommon(Client client, WorldInfoImplementation info)
	{
		super(client, info);
		
		this.renderer = new WorldRendererImplementation(this, client);
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
	public WorldRendererImplementation getWorldRenderer()
	{
		return renderer;
	}

	@Override
	public DecalsRenderer getDecalsManager()
	{
		return renderer.getDecalsRenderer();
	}

	@Override
	public ClientParticleManager getParticlesManager()
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
