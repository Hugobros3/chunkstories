package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.world.World;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ParticleType
{
	final int id;
	final String name;
	
	public ParticleType(int id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleData(x, y, z);
	}
	
	public void beginRenderingForType(RenderingContext renderingContext)
	{
		renderingContext.setDiffuseTexture(getTexture().getId());
		renderingContext.getCurrentShader().setUniformFloat("billboardSize", getBillboardSize());
	}
	
	public abstract void forEach_Rendering(RenderingContext renderingContext, ParticleData data);
	
	public abstract void forEach_Physics(World world, ParticleData data);
	
	public abstract Texture2D getTexture();
	
	public abstract float getBillboardSize();
}
