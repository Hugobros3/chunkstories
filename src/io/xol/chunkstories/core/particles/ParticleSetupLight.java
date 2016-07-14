package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.world.WorldImplementation;

import static io.xol.chunkstories.particles.Particle.Type.*;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.particles.Particle;
import io.xol.chunkstories.particles.Particle.Type;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSetupLight extends Particle
{

	int timer = 4800;// for 40sec

	Light dl;

	@Override
	public Type getType()
	{
		return SLIGHT;
	}

	@Override
	public void update()
	{

	}

	public ParticleSetupLight(WorldImplementation world, double posX, double posY,
			double posZ, Light dl)
	{
		super(world, posX, posY, posZ);
		this.dl = dl;
	}

	@Override
	public String getTextureName()
	{
		return "./res/textures/light.png";
	}

	@Override
	public boolean emitsLights()
	{
		return true;
	}

	@Override
	public Light getLightEmited()
	{
		return dl;
	}

	@Override
	public Float getSize()
	{
		return 3f;
	}
}
