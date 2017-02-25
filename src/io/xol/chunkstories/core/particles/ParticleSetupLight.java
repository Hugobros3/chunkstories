package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSetupLight extends ParticleType
{
	public ParticleSetupLight(int id, String name)
	{
		super(id, name);
	}

	public class ParticleSetupLightData extends ParticleData {

		public int timer = 4800;
		public Vector3fm c;
		public Light light;
		
		public ParticleSetupLightData(float x, float y, float z)
		{
			super(x, y, z);
			c = new Vector3fm(Math.random(), Math.random(), Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleSetupLightData(x, y, z);
	}
	
	@Override
	public Texture2D getAlbedoTexture()
	{
		return TexturesHandler.getTexture("./textures/light.png");
	}
	@Override
	public float getBillboardSize()
	{
		return 3f;
	}

	@Override
	public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
	{
		if(((ParticleSetupLightData)data).light != null)
			renderingContext.getLightsRenderer().queueLight(((ParticleSetupLightData)data).light);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		// TODO Auto-generated method stub
		
	}
}
