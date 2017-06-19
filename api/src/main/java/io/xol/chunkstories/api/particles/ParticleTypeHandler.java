package io.xol.chunkstories.api.particles;

import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Determines how a specific particle type should be handled, what type of metadata to keep for each particle, how to render them etc. */
public abstract class ParticleTypeHandler {

	private final ParticleType type;
	
	public ParticleTypeHandler(ParticleType type)
	{
		this.type = type;
	}
	
	public ParticleType getType()
	{
		return type;
	}
	
	public String getName()
	{
		return type.getName();
	}
	
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
	
	public abstract void forEach_Physics(World world, ParticleData data);
	
	public abstract class ParticleTypeRenderer {
		protected final ParticlesRenderer particlesRenderer;
		private final Texture2D albedoTexture;
		private final Texture2D normalTexture;
		private final Texture2D materialTexture;
		
		public ParticleTypeRenderer(ParticlesRenderer particlesRenderer) {
			this.particlesRenderer = particlesRenderer;
			
			//Get those at initialization of de renderer
			albedoTexture   = type.getAlbedoTexture() != null   ? particlesRenderer.getContent().textures().getTexture(type.getAlbedoTexture())   :  particlesRenderer.getContent().textures().nullTexture();
			normalTexture   = type.getNormalTexture() != null   ? particlesRenderer.getContent().textures().getTexture(type.getNormalTexture())   :  particlesRenderer.getContent().textures().nullTexture();
			materialTexture = type.getMaterialTexture() != null ? particlesRenderer.getContent().textures().getTexture(type.getMaterialTexture()) :  particlesRenderer.getContent().textures().nullTexture();
		}
		
		public Texture2D getAlbedoTexture() 
		{
			return albedoTexture;
		}
		
		public Texture2D getNormalTexture()
		{
			return normalTexture;
		}

		public Texture2D getMaterialTexture()
		{
			return materialTexture;
		}
		
		public void beginRenderingForType(RenderingInterface renderingInterface)
		{
			renderingInterface.setCullingMode(CullingMode.DISABLED);
			renderingInterface.setBlendMode(BlendMode.MIX);
			ShaderInterface particlesShader = renderingInterface.useShader(type.getShaderName());
			particlesShader.setUniform2f("screenSize", renderingInterface.getWindow().getWidth(), renderingInterface.getWindow().getHeight());
			renderingInterface.getCamera().setupShader(particlesShader);
			renderingInterface.bindTexture2D("lightColors", particlesRenderer.getContent().textures().getTexture("./textures/environement/light.png"));
			
			renderingInterface.bindAlbedoTexture(getAlbedoTexture());
			renderingInterface.currentShader().setUniform1f("billboardSize", type.getBillboardSize());
			//TODO refactor this crappy class
			renderingInterface.bindNormalTexture(particlesRenderer.getContent().textures().getTexture("./textures/normalnormal.png"));
		}

		/** Called at each iteration, on the rendering thread. */
		public abstract void forEach_Rendering(RenderingInterface renderingInterface, ParticleData data);

		/** You must free any non-auto destructing graphics objects here. Freeing up textures and models is a nice touch. */
		public abstract void destroy();
	}
	
	public abstract ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer);
}
