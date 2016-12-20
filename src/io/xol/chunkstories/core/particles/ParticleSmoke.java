package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.particles.ParticleBlood.BloodData;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSmoke extends ParticleType
{
	public ParticleSmoke(int id, String name)
	{
		super(id, name);
	}

	public class ParticleSmokeData extends ParticleData {

		public int timer = 60 * 60;
		
		public ParticleSmokeData(float x, float y, float z)
		{
			super(x, y, z);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleSmokeData(x, y, z);
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		data.setY(data.getY() + (Math.random() - 0.1) * 0.015);
		data.setX(data.getX() + (Math.random() - 0.5) * 0.015);
		data.setZ(data.getZ() + (Math.random() - 0.5) * 0.015);
		
		((ParticleSmokeData)data).timer--;
		if (((ParticleSmokeData)data).timer < 0)
			data.destroy();
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		BloodData b = (BloodData) data;
		
		b.timer--;
		b.setX(b.getX() + b.vel.getX());
		b.setY(b.getY() + b.vel.getY());
		b.setZ(b.getZ() + b.vel.getZ());
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.vel.setY(b.vel.getY() + -0.89/60.0);
		else
			b.vel.zero();
		
		// 60th square of 0.5
		b.vel.scale(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.zero();
		
		if(b.timer < 0)
			b.destroy();
	}

	@Override
	public  Texture2D getTexture()
	{
		return TexturesHandler.getTexture("./textures/smoke2.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.05f;
	}
}
