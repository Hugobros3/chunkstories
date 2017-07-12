package io.xol.chunkstories.core.particles;

import org.joml.Vector3f;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSetupLight extends ParticleTypeHandler
{
	public ParticleSetupLight(ParticleType type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	public class ParticleSetupLightData extends ParticleData {

		public int timer = 4800;
		public Vector3f c;
		public Light light;
		
		public ParticleSetupLightData(float x, float y, float z)
		{
			super(x, y, z);
			c = new Vector3f((float)Math.random(), (float)Math.random(), (float)Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleSetupLightData(x, y, z);
	}

	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				if(((ParticleSetupLightData)data).light != null)
					renderingContext.getLightsRenderer().queueLight(((ParticleSetupLightData)data).light);
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
	
	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		// TODO Auto-generated method stub
		
	}
}
