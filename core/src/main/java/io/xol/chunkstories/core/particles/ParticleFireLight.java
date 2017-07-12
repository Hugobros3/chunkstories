package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.math.Math2;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleFireLight extends ParticleTypeHandler
{
	public ParticleFireLight(ParticleType type) {
		super(type);
	}

	public class ParticleFireData extends ParticleData implements ParticleDataWithVelocity{

		public int timer = 60 * 60;
		public float temp = 7000;
		Vector3d vel = new Vector3d();
		int decay;
		
		public ParticleFireData(float x, float y, float z)
		{
			super(x, y, z);
			
			decay = 15+5;
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
		return new ParticleFireData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		ParticleFireData b = (ParticleFireData) data;
		
		b.timer--;
		
		/*b.setX((float) (b.x() + b.vel.x()));
		b.setY((float) (b.y() + b.vel.y()));
		b.setZ((float) (b.z() + b.vel.z()));
		*/
		
		/*if (!((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
			b.vel.setY(b.vel.y() + 0.02/60.0);
		else
			b.vel.set(0d, 0d, 0d);*/
		
		// 60th square of 0.5
		//b.vel.scale(0.98581402);
		
		//b.vel.scale(0.95129708668990249416970880243486);
		
		b.vel.mul(0.93);
		
		if(b.vel.length() < 0.01/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.temp > 3000)
			b.temp -= 10 + b.decay;
		else if(b.temp > 0)
			b.temp -= b.decay;
		else if(b.temp <= 0)
		{
			b.destroy();
			b.temp = 1;	
		}
		
		//if(((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
		//	b.destroy();
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
				
				String temp_scale = "./textures/particles/fire_temp_scale_expl.png";
				renderingContext.bindTexture2D("colorTempSampler", particlesRenderer.getContent().textures().getTexture(temp_scale));
				particlesRenderer.getContent().textures().getTexture(temp_scale).setLinearFiltering(true);
				particlesRenderer.getContent().textures().getTexture(temp_scale).setTextureWrapping(false);
				
				getAlbedoTexture().setMipMapping(true);
				getAlbedoTexture().setLinearFiltering(true);
				getAlbedoTexture().setTextureWrapping(false);
			}

			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				ParticleFireData b = (ParticleFireData) data;
				renderingContext.getLightsRenderer().queueLight(new Light(new Vector3f(1.0f, 252f/255f, 1/255f),
						new Vector3f((float) data.x(), (float) data.y(), (float) data.z()),
						25f * Math2.clamp((float)(double)b.vel.x() * 5000, 0.0, 1.0)));
				
				//System.out.println("k");
				
				/*data.setY((float) (data.y() + (Math.random() - 0.1) * 0.0015));
				data.setX((float) (data.x() + (Math.random() - 0.5) * 0.0015));
				data.setZ((float) (data.z() + (Math.random() - 0.5) * 0.0015));
				*/
				((ParticleFireData)data).timer--;
				if (((ParticleFireData)data).timer < 0)
					data.destroy();
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
}
