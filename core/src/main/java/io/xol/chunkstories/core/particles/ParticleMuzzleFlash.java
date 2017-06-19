package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleMuzzleFlash extends ParticleTypeHandler
{
	public ParticleMuzzleFlash(ParticleType type) {
		super(type);
	}

	public class MuzzleData extends ParticleData {

		public int timer = 2;
		
		public MuzzleData(float x, float y, float z)
		{
			super(x, y, z);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new MuzzleData(x, y, z);
	}

	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				renderingContext.getLightsRenderer().queueLight(new Light(new Vector3fm(1.0f, 181f/255f, 79/255f),
						new Vector3fm((float) data.getX(), (float) data.getY(), (float) data.getZ()),
						15f + (float) Math.random() * 5f));
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
	
	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		((MuzzleData)data).timer--;
		if(((MuzzleData)data).timer < 0)
			data.destroy();
	}
}
