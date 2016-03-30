package io.xol.chunkstories.physics.particules;

import io.xol.chunkstories.world.World;
import static io.xol.chunkstories.physics.particules.Particle.Type.*;

import io.xol.chunkstories.api.rendering.Light;

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

	public ParticleSetupLight(World world, double posX, double posY,
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
