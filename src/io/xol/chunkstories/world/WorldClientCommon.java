package io.xol.chunkstories.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.world.WorldClient;
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

	@Override
	public void tick()
	{
		super.tick();
		
		//Spawn some snow arround
		Entity e = Client.getInstance().getClientSideController().getControlledEntity();
		if(e != null)
		{
			Location loc = e.getLocation();
			
			for(int i = 0; i < 10; i++)
				this.getParticlesManager()
				.spawnParticleAtPosition("snow", 
						loc.add(Math.random() * 20 - 10, Math.random() * 20, Math.random() * 20 - 10));
		}
	}
}
