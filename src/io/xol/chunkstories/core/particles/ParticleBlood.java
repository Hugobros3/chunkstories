package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.RenderingContext;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleBlood extends ParticleType
{
	public ParticleBlood(int id, String name)
	{
		super(id, name);
	}

	public class BloodData extends ParticleData {
		
		int timer = 60 * 30; // 30s
		Vector3d vel = new Vector3d();
		
		public BloodData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
		public void setVelocity(Vector3d vel)
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
	public Texture2D getTexture()
	{
		return TexturesHandler.getTexture("./res/textures/particles/blood.png");
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
		b.x += b.vel.x;
		b.y += b.vel.y;
		b.z += b.vel.z;
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.x, b.y, b.z))
			b.vel.y += -0.89/60.0;
		else
			b.vel.zero();
		
		// 60th square of 0.5
		b.vel.scale(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.zero();
		
		if(b.timer < 0)
			b.destroy();
	}
}
