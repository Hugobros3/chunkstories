package io.xol.chunkstories.physics.particules;

import io.xol.chunkstories.world.WorldImplementation;
import static io.xol.chunkstories.physics.particules.Particle.Type.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleRainfall extends Particle
{

	@Override
	public Type getType()
	{
		return RAINFALL;
	}

	@Override
	public void update()
	{
		if (!((WorldImplementation) world).checkCollisionPoint(posX, posY, posZ))
		{
			posY -= 0.15;
		} else
			kill();

		if (posY < 0)
			kill();
		/*
		 * posY+=(Math.random()-0.1)*0.005; posX+=(Math.random()-0.5)*0.005;
		 * posZ+=(Math.random()-0.5)*0.005;
		 */
	}

	public ParticleRainfall(WorldImplementation world, double posX, double posY, double posZ)
	{
		super(world, posX, posY, posZ);
	}

	@Override
	public String getTextureName()
	{
		return "./res/textures/environement/rain.png";
	}

	@Override
	public Float getSize()
	{
		return 0.5f;
	}
}
