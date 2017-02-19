package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
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
	
	public void beginRenderingForType(RenderingInterface renderingInterface)
	{
		renderingInterface.bindAlbedoTexture(getAlbedoTexture());
		renderingInterface.currentShader().setUniform1f("billboardSize", getBillboardSize());
		//TODO refactor this crappy class
		renderingInterface.bindNormalTexture(TexturesHandler.getTexture("./textures/normalnormal.png"));
	}
	
	public abstract void forEach_Rendering(RenderingInterface renderingInterface, ParticleData data);
	
	public abstract void forEach_Physics(World world, ParticleData data);
	
	public abstract Texture2D getAlbedoTexture();
	
	public abstract float getBillboardSize();
}
