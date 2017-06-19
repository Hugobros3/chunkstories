package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
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
		Vector3dm vel = new Vector3dm();
		int decay;
		
		public ParticleFireData(float x, float y, float z)
		{
			super(x, y, z);
			
			decay = 15+5;
		}
		
		public void setVelocity(Vector3dm vel)
		{
			this.vel = vel;
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
		
		/*b.setX((float) (b.getX() + b.vel.getX()));
		b.setY((float) (b.getY() + b.vel.getY()));
		b.setZ((float) (b.getZ() + b.vel.getZ()));
		*/
		
		/*if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.vel.setY(b.vel.getY() + 0.02/60.0);
		else
			b.vel.set(0d, 0d, 0d);*/
		
		// 60th square of 0.5
		//b.vel.scale(0.98581402);
		
		//b.vel.scale(0.95129708668990249416970880243486);
		
		b.vel.scale(0.93);
		
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
		
		//if(((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
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
				renderingContext.getLightsRenderer().queueLight(new Light(new Vector3fm(1.0f, 252f/255f, 1/255f),
						new Vector3fm((float) data.getX(), (float) data.getY(), (float) data.getZ()),
						25f * Math2.clamp((float)(double)b.vel.getX() * 5000, 0.0, 1.0)));
				
				//System.out.println("k");
				
				/*data.setY((float) (data.getY() + (Math.random() - 0.1) * 0.0015));
				data.setX((float) (data.getX() + (Math.random() - 0.5) * 0.0015));
				data.setZ((float) (data.getZ() + (Math.random() - 0.5) * 0.0015));
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
