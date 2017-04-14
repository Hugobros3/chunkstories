package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.world.World;
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
	
	//When should we render those particle types
	public enum RenderTime {
		NEVER, GBUFFER, FORWARD;
	}
	
	public abstract RenderTime getRenderTime();
	
	/**
	 * Particle data is at least a vector3f
	 */
	public class ParticleData extends Vector3fm
	{
		boolean ded = false;
		
		public ParticleData(float x, float y, float z)
		{
			super(x, y, z);
		}
		
		public void destroy()
		{
			ded = true;
		}
		
		public boolean isDed()
		{
			return ded;
		}
	}
	
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new ParticleData(x, y, z);
	}
	
	public void beginRenderingForType(RenderingInterface renderingInterface)
	{
		renderingInterface.setCullingMode(CullingMode.DISABLED);
		renderingInterface.setBlendMode(BlendMode.MIX);
		ShaderInterface particlesShader = renderingInterface.useShader(getShaderName());
		particlesShader.setUniform2f("screenSize", renderingInterface.getWindow().getWidth(), renderingInterface.getWindow().getHeight());
		renderingInterface.getCamera().setupShader(particlesShader);
		renderingInterface.bindTexture2D("lightColors", TexturesHandler.getTexture("./textures/environement/light.png"));
		
		renderingInterface.bindAlbedoTexture(getAlbedoTexture());
		renderingInterface.currentShader().setUniform1f("billboardSize", getBillboardSize());
		//TODO refactor this crappy class
		renderingInterface.bindNormalTexture(TexturesHandler.getTexture("./textures/normalnormal.png"));
	}
	
	protected String getShaderName()
	{
		return "particles";
	}

	public abstract void forEach_Rendering(RenderingInterface renderingInterface, ParticleData data);
	
	public abstract void forEach_Physics(World world, ParticleData data);
	
	public abstract Texture2D getAlbedoTexture();
	
	public abstract float getBillboardSize();
}
