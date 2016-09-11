package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.particles.ParticleMuzzleFlash.MuzzleData;
import io.xol.chunkstories.renderer.lights.DefferedLight;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleLight extends ParticleType
{
	public ParticleLight(int id, String name)
	{
		super(id, name);
	}

	public class ParticleLightData extends ParticleData {

		public int timer = 2;
		public Vector3f c;
		
		public ParticleLightData(float x, float y, float z)
		{
			super(x, y, z);
			c = new Vector3f(Math.random(), Math.random(), Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleLightData(x, y, z);
	}

	@Override
	public Texture2D getTexture()
	{
		return TexturesHandler.getTexture("./res/textures/particle.png");
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data2)
	{
		ParticleLightData data = (ParticleLightData)data2;
		renderingContext.addLight(new DefferedLight(new Vector3f(1.0f, 181f/255f, 79/255f),
				new Vector3f((float) data.c.x, (float) data.c.y, (float) data.c.z),
				15f + (float) Math.random() * 5f));
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		((MuzzleData)data).timer--;
		if(((MuzzleData)data).timer < 0)
			data.destroy();
	}

	@Override
	public float getBillboardSize()
	{
		return 0.0f;
	}
}
