package io.xol.chunkstories.core.particles;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSmoke extends ParticleTypeHandler
{
	public ParticleSmoke(ParticleType type) {
		super(type);
	}

	public class ParticleSmokeData extends ParticleData implements ParticleDataWithVelocity{

		public int timer = 60 * 60;
		Vector3d vel = new Vector3d();
		
		public ParticleSmokeData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
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
		return new ParticleSmokeData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		ParticleSmokeData b = (ParticleSmokeData) data;
		
		b.timer--;
		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));
		
		/*if (!((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
			b.vel.setY(b.vel.y() + -0.89/60.0);
		else
			b.vel.set(0d, 0d, 0d);*/
		
		// 60th square of 0.5
		b.vel.mul(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.timer < 0 || ((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
			b.destroy();
	}

	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void beginRenderingForType(RenderingInterface renderingContext)
			{
				super.beginRenderingForType(renderingContext);
				
				renderingContext.setBlendMode(BlendMode.PREMULT_ALPHA);
				
				renderingContext.getRenderTargetManager().setDepthMask(false);
				//renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
				//System.out.println("k");
				
				getAlbedoTexture().setMipMapping(true);
				getAlbedoTexture().setLinearFiltering(false);
			}

			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				data.y = ((float) (data.y() + (Math.random() - 0.1) * 0.0015));
				data.x = ((float) (data.x() + (Math.random() - 0.5) * 0.0015));
				data.z = ((float) (data.z() + (Math.random() - 0.5) * 0.0015));
				
				((ParticleSmokeData)data).timer--;
				if (((ParticleSmokeData)data).timer < 0)
					data.destroy();
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
}
