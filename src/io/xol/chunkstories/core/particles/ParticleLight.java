package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.particles.ParticleMuzzleFlash.MuzzleData;

//(c) 2015-2017 XolioWare Interactive
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
		public Vector3fm c;
		
		public ParticleLightData(float x, float y, float z)
		{
			super(x, y, z);
			c = new Vector3fm(Math.random(), Math.random(), Math.random());
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
		renderingContext.addLight(new Light(new Vector3fm(1.0f, 181f/255f, 79/255f),
				new Vector3fm((float) data.c.getX(), (float) data.c.getY(), (float) data.c.getZ()),
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
