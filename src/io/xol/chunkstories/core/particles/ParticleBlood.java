package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleBlood extends ParticleType
{
	public ParticleBlood(int id, String name)
	{
		super(id, name);
	}

	public class BloodData extends ParticleData implements ParticleDataWithVelocity {
		
		int timer = 60 * 5; // 5s
		Vector3dm vel = new Vector3dm();
		
		public BloodData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
		public void setVelocity(Vector3dm vel)
		{
			this.vel = vel;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new BloodData(x, y, z);
	}

	@Override
	public Texture2D getAlbedoTexture()
	{
		return TexturesHandler.getTexture("./textures/particles/blood.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.1f;
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		BloodData b = (BloodData) data;
		
		b.timer--;
		b.setX((float) (b.getX() + b.vel.getX()));
		b.setY((float) (b.getY() + b.vel.getY()));
		b.setZ((float) (b.getZ() + b.vel.getZ()));
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.vel.setY(b.vel.getY() + -0.89/60.0);
		else
			b.vel.set(0d, 0d, 0d);
		
		// 60th square of 0.5
		b.vel.scale(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.timer < 0)
			b.destroy();
	}
}
