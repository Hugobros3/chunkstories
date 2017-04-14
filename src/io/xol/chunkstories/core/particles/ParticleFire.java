package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleFire extends ParticleType
{
	public ParticleFire(int id, String name)
	{
		super(id, name);
	}

	public class ParticleFireData extends ParticleData implements ParticleDataWithVelocity, ParticleDataWithTextureCoordinates{

		public int timer = 60 * 60;
		public float temp = 7000;
		Vector3dm vel = new Vector3dm();
		int decay;
		
		public ParticleFireData(float x, float y, float z)
		{
			super(x, y, z);
			
			decay = 15+(int)(Math.random()*5);
		}
		
		public void setVelocity(Vector3dm vel)
		{
			this.vel = vel;
		}

		@Override
		public float getTextureCoordinateXTopLeft()
		{
			// TODO Auto-generated method stub
			return temp;
		}

		@Override
		public float getTextureCoordinateXTopRight()
		{
			// TODO Auto-generated method stub
			return temp;
		}

		@Override
		public float getTextureCoordinateXBottomLeft()
		{
			// TODO Auto-generated method stub
			return temp;
		}

		@Override
		public float getTextureCoordinateXBottomRight()
		{
			// TODO Auto-generated method stub
			return temp;
		}

		@Override
		public float getTextureCoordinateYTopLeft()
		{
			return 0;
		}

		@Override
		public float getTextureCoordinateYTopRight()
		{
			return 0;
		}

		@Override
		public float getTextureCoordinateYBottomLeft()
		{
			return 0;
		}

		@Override
		public float getTextureCoordinateYBottomRight()
		{
			return 0;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleFireData(x, y, z);
	}
	
	public RenderTime getRenderTime() {
		return RenderTime.FORWARD;
	}

	@Override
	public  Texture2D getAlbedoTexture()
	{
		return TexturesHandler.getTexture("./textures/particles/white_smoke.png");
	}
	
	@Override
	public void beginRenderingForType(RenderingInterface renderingContext)
	{
		super.beginRenderingForType(renderingContext);
		
		renderingContext.setBlendMode(BlendMode.PREMULT_ALPHA);
		
		renderingContext.getRenderTargetManager().setDepthMask(false);
		//renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		//System.out.println("k");
		
		String temp_scale = "./textures/particles/fire_temp_scale_expl.png";
		renderingContext.bindTexture2D("colorTempSampler", TexturesHandler.getTexture(temp_scale));
		TexturesHandler.getTexture(temp_scale).setLinearFiltering(true);
		TexturesHandler.getTexture(temp_scale).setTextureWrapping(false);
		
		getAlbedoTexture().setMipMapping(true);
		getAlbedoTexture().setLinearFiltering(true);
		getAlbedoTexture().setTextureWrapping(false);
	}
	
	@Override
	protected String getShaderName()
	{
		return "particles_fire";
	}

	@Override
	public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
	{
		data.setY((float) (data.getY() + (Math.random() - 0.1) * 0.0015));
		data.setX((float) (data.getX() + (Math.random() - 0.5) * 0.0015));
		data.setZ((float) (data.getZ() + (Math.random() - 0.5) * 0.0015));
		
		((ParticleFireData)data).timer--;
		if (((ParticleFireData)data).timer < 0)
			data.destroy();
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		ParticleFireData b = (ParticleFireData) data;
		
		b.timer--;
		b.setX((float) (b.getX() + b.vel.getX()));
		b.setY((float) (b.getY() + b.vel.getY()));
		b.setZ((float) (b.getZ() + b.vel.getZ()));
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.vel.setY(b.vel.getY() + 0.02/60.0);
		else
			b.vel.set(0d, 0d, 0d);
		
		// 60th square of 0.5
		//b.vel.scale(0.98581402);
		
		//b.vel.scale(0.95129708668990249416970880243486);
		
		b.vel.scale(0.93);
		
		if(b.vel.length() < 0.1/60.0)
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
		
		if(((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY(), b.getZ()))
			b.destroy();
	}

	@Override
	public float getBillboardSize()
	{
		return 2f;
	}
}
