package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;

public class ParticleSnow extends ParticleType
{

	public ParticleSnow(int id, String name)
	{
		super(id, name);
		// TODO Auto-generated constructor stub
	}

	public class SnowData extends ParticleData implements ParticleDataWithVelocity {
		
		int hp = 60 * 2; // 5s
		Vector3d vel = new Vector3d((Math.random() * 0.5 - 0.25) * 0.5, -Math.random() * 0.15 - 0.10, (Math.random() * 0.5 - 0.25) * 0.5);
		
		public SnowData(float x, float y, float z)
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
		return new SnowData(x, y, z);
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		SnowData b = (SnowData) data;
		
		b.setX(b.getX() + b.vel.getX());
		b.setY(b.getY() + b.vel.getY());
		b.setZ(b.getZ() + b.vel.getZ());
		
		if (((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
		{
			b.hp--;
			b.vel.zero();
		}
		
		// 60th square of 0.5
		//b.vel.scale(0.98581402);
		//if(b.vel.length() < 0.1/60.0)
		//	b.vel.zero();
		
		if(b.hp < 0 || b.getY() < 0)
			b.destroy();
	}

	@Override
	public Texture2D getTexture()
	{
		return TexturesHandler.getTexture("./textures/particles/snow.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.06125f;
	}

}
