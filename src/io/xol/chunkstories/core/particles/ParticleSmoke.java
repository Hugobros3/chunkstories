package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSmoke extends ParticleType
{
	public ParticleSmoke(int id, String name)
	{
		super(id, name);
	}

	public class ParticleSmokeData extends ParticleData implements ParticleDataWithVelocity{

		public int timer = 60 * 60;
		Vector3dm vel = new Vector3dm();
		
		public ParticleSmokeData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
		public void setVelocity(Vector3dm vel)
		{
			this.vel = vel;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleSmokeData(x, y, z);
	}
	
	public void beginRenderingForType(RenderingContext renderingContext)
	{
		super.beginRenderingForType(renderingContext);
		
		TexturesHandler.getTexture("./textures/particles/smoke.png").setMipMapping(false);
		TexturesHandler.getTexture("./textures/particles/smoke.png").setLinearFiltering(false);
		
		TexturesHandler.getTexture("./textures/particles/smoke_normal.png").setMipMapping(true);
		TexturesHandler.getTexture("./textures/particles/smoke_normal.png").setLinearFiltering(false);
		renderingContext.bindNormalTexture(TexturesHandler.getTexture("./textures/particles/smoke_normal.png"));
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		data.setY((float) (data.getY() + (Math.random() - 0.1) * 0.0015));
		data.setX((float) (data.getX() + (Math.random() - 0.5) * 0.0015));
		data.setZ((float) (data.getZ() + (Math.random() - 0.5) * 0.0015));
		
		((ParticleSmokeData)data).timer--;
		if (((ParticleSmokeData)data).timer < 0)
			data.destroy();
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		ParticleSmokeData b = (ParticleSmokeData) data;
		
		b.timer--;
		b.setX((float) (b.getX() + b.vel.getX()));
		b.setY((float) (b.getY() + b.vel.getY()));
		b.setZ((float) (b.getZ() + b.vel.getZ()));
		
		/*if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.vel.setY(b.vel.getY() + -0.89/60.0);
		else
			b.vel.set(0d, 0d, 0d);*/
		
		// 60th square of 0.5
		b.vel.scale(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.timer < 0 || ((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.destroy();
	}

	@Override
	public  Texture2D getAlbedoTexture()
	{
		return TexturesHandler.getTexture("./textures/particles/smoke.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.7f;
	}
}
