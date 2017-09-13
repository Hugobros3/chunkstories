package io.xol.chunkstories.core.particles;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleBlood extends ParticleTypeHandler
{
	public ParticleBlood(ParticleType type) {
		super(type);
	}

	public class BloodData extends ParticleData implements ParticleDataWithVelocity {
		
		int timer = 60 * 5; // 5s
		Vector3d vel = new Vector3d();
		
		public BloodData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
		@Override
		public void setVelocity(Vector3dc vel) {
			this.vel.set(vel);
		}

		@Override
		public void setVelocity(Vector3fc vel) {
			this.vel.set(vel);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new BloodData(x, y, z);
	}
	
	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		BloodData b = (BloodData) data;
		
		b.timer--;
		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));
		
		if(!b.isCollidingAgainst(world))
			//if (!((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
			b.vel.y = (b.vel.y() + -0.89/60.0);
		else
			b.vel.set(0d, 0d, 0d);
		
		// 60th square of 0.5
		b.vel.mul(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.timer < 0)
			b.destroy();
	}
	
	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				
			}
			
			@Override
			public void destroy() {
				
			}
			
		};
	}
}
