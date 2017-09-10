package io.xol.chunkstories.world;

import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.renderer.debug.WorldLogicTimeRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRendererImplementation;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Implementation of common methods to WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient
{
	protected WorldRendererImplementation renderer;
	
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
	
	public Client getGameContext()
	{
		return getClient();
	}

	@Override
	public WorldRendererImplementation getWorldRenderer()
	{
		return renderer;
	}

	@Override
	public DecalsRendererImplementation getDecalsManager()
	{
		return renderer.getDecalsRenderer();
	}

	@Override
	public ClientParticlesRenderer getParticlesManager()
	{
		return renderer.getParticlesRenderer();
	}

	@Override
	public void tick()
	{
		super.tick();

		WorldLogicTimeRenderer.tickWorld();
		
		this.getWorldRenderer().getWorldEffectsRenderer().tick();

		//Update particles subsystem if it exists
		if (getParticlesManager() != null && getParticlesManager() instanceof ClientParticlesRenderer)
			((ClientParticlesRenderer) getParticlesManager()).updatePhysics();
	}
}
