package io.xol.chunkstories.physics.particules;

import io.xol.engine.math.lalgb.Vector3d;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.world.World;

import static io.xol.chunkstories.physics.particules.Particle.Type.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleBlood extends Particle
{
	int timer = 60 * 30; // 30s
	Vector3d vel;

	@Override
	public Type getType()
	{
		return BLOOD;
	}

	@Override
	public void update()
	{
		timer--;
		this.posX += vel.x;
		this.posY += vel.y;
		this.posZ += vel.z;
		
		if (!((World) world).checkCollisionPoint(posX, posY, posZ))
			vel.y += -0.89/60.0;
		else
			vel.zero();
		
		// 60th square of 0.5
		vel.scale(0.98581402);
		if(vel.length() < 0.1/60.0)
			vel.zero();
		
		if(timer < 0)
			kill();
	}

	public ParticleBlood(WorldInterface world, Vector3d pos, Vector3d vel)
	{
		this(world, pos.x, pos.y, pos.z);
		this.vel = vel;
	}
	
	private ParticleBlood(WorldInterface world, double posX, double posY, double posZ)
	{
		super(world, posX, posY, posZ);
	}

	@Override
	public String getTextureName()
	{
		return "./res/textures/particles/blood.png";
	}

	@Override
	public boolean emitsLights()
	{
		return false;
	}

	@Override
	public Light getLightEmited()
	{
		return null;
	}

	@Override
	public Float getSize()
	{
		return 0.1f;
	}
}
