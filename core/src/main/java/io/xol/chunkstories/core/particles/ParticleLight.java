package io.xol.chunkstories.core.particles;

import org.joml.Vector3f;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.particles.ParticleMuzzleFlash.MuzzleData;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleLight extends ParticleTypeHandler
{
	public ParticleLight(ParticleType type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	public class ParticleLightData extends ParticleData {

		public int timer = 2;
		public Vector3f c;
		
		public ParticleLightData(float x, float y, float z)
		{
			super(x, y, z);
			c = new Vector3f((float)Math.random(), (float)Math.random(), (float)Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleLightData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		((MuzzleData)data).timer--;
		if(((MuzzleData)data).timer < 0)
			data.destroy();
	}
	
	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data2)
			{
				ParticleLightData data = (ParticleLightData)data2;
				renderingContext.getLightsRenderer().queueLight(new Light(new Vector3f(1.0f, 181f/255f, 79/255f),
						new Vector3f((float) data.c.x(), (float) data.c.y(), (float) data.c.z()),
						15f + (float) Math.random() * 5f));
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
}
