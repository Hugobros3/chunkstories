package io.xol.chunkstories.core.particles;

import static io.xol.chunkstories.particles.Particle.Type.*;

import io.xol.chunkstories.particles.Particle;
import io.xol.chunkstories.particles.Particle.Type;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSmoke extends Particle
{

	int timer = 600;// for 10sec

	@Override
	public Type getType()
	{
		return SMOKE;
	}

	@Override
	public void update()
	{
		/*
		 * if(!world.checkCollisionPoint(posX, posY, posZ)) {
		 * 
		 * }
		 */
		timer--;
		if (timer < 0)
			kill();
		posY += (Math.random() - 0.1) * 0.015;
		posX += (Math.random() - 0.5) * 0.015;
		posZ += (Math.random() - 0.5) * 0.015;
	}

	public ParticleSmoke(WorldImplementation world, double posX, double posY, double posZ)
	{
		super(world, posX, posY, posZ);
	}

	@Override
	public String getTextureName()
	{
		return "./textures/smoke2.png";
	}

	@Override
	public Float getSize()
	{
		return 0.05f;
	}
}
