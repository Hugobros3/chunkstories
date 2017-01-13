package io.xol.chunkstories.particles;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.world.World;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.BufferUtils;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticlesRenderer implements ParticlesManager
{
	final World world;
	final Content.ParticlesTypes store;

	final Map<ParticleType, Set<ParticleData>> particles = new ConcurrentHashMap<ParticleType, Set<ParticleData>>();
	
	VerticesObject particlesPositions, texCoords;
	FloatBuffer particlesPositionsBuffer, textureCoordinatesBuffer;

	public ParticlesRenderer(World world)
	{
		this.world = world;
		this.store = world.getGameContext().getContent().particles();

		particlesPositions = new VerticesObject();
		texCoords = new VerticesObject();
		
		//480kb
		particlesPositionsBuffer = BufferUtils.createFloatBuffer(6 * 3 * 10000);
		//320kb
		textureCoordinatesBuffer = BufferUtils.createFloatBuffer(6 * 2 * 10000);
	}

	public int count()
	{
		int a = 0;
		for (Set<ParticleData> l : particles.values())
		{
			a += l.size();
		}
		return a;
	}

	public void spawnParticleAtPosition(String particleTypeName, Vector3dm location)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, location, null);
	}

	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dm location, Vector3dm velocity)
	{
		ParticleType particleType = store.getParticleTypeByName(particleTypeName);
		if (particleType == null || location == null)
			return;

		ParticleData particleData = particleType.createNew(world, (float)(double) location.getX(), (float)(double) location.getY(), (float)(double) location.getZ());
		if (velocity != null && particleData instanceof ParticleDataWithVelocity)
			((ParticleDataWithVelocity) particleData).setVelocity(velocity);

		addParticle(particleType, particleData);
	}

	void addParticle(ParticleType particleType, ParticleData particleData)
	{
		//Add this to the data sets
		if (!particles.keySet().contains(particleType))
		{
			particles.put(particleType, ConcurrentHashMap.newKeySet());//Collections.synchronizedList(new ArrayList<ParticleData>()));
		}
		particles.get(particleType).add(particleData);
	}

	public void updatePhysics()
	{
		for (ParticleType particleType : particles.keySet())
		{
			Iterator<ParticleData> iterator = particles.get(particleType).iterator();

			ParticleData p;
			while (iterator.hasNext())
			{
				p = iterator.next();
				if (p != null)
				{
					particleType.forEach_Physics(world, p);
					if (p.isDed())
						iterator.remove();
				}
				else
					iterator.remove();
			}
		}

	}

	public void renderLights(RenderingContext renderingContext)
	{
		for (ParticleType particleType : particles.keySet())
		{
			Iterator<ParticleData> iterator = particles.get(particleType).iterator();

			ParticleData p;
			while (iterator.hasNext())
			{
				p = iterator.next();
				if (p != null)
				{
					particleType.forEach_Rendering(renderingContext, p);
					if (p.isDed())
						iterator.remove();
				}
				else
					iterator.remove();
			}
		}
	}

	public int render(RenderingContext renderingContext)
	{
		int totalDrawn = 0;
		
		renderingContext.setCullingMode(CullingMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.MIX);

		ShaderInterface particlesShader = renderingContext.useShader("particles");

		particlesShader.setUniform2f("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		renderingContext.getCamera().setupShader(particlesShader);
		
		renderingContext.bindTexture2D("lightColors", TexturesHandler.getTexture("./textures/environement/light.png"));

		//Vertex attributes
		
		//For all present particles types
		for (ParticleType particleType : particles.keySet())
		{
			//Don't bother rendering empty sets
			if (particles.get(particleType).size() > 0)
			{
				Iterator<ParticleData> iterator = particles.get(particleType).iterator();
				boolean haveTextureCoordinates = false;

				particleType.beginRenderingForType(renderingContext);

				particleType.getTexture().setLinearFiltering(false);

				textureCoordinatesBuffer.clear();
				particlesPositionsBuffer.clear();
				int elements = 0;

				while (iterator.hasNext())
				{
					//Iterate over dem particles
					ParticleData p = iterator.next();

					//If > 60k elements, buffer is full, draw it
					if (elements >= 60000)
					{
						drawBuffers(renderingContext, elements, haveTextureCoordinates);
						totalDrawn += elements;
						elements = 0;
					}

					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					//Second triangle
					
					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					particlesPositionsBuffer.put((float) p.getX());
					particlesPositionsBuffer.put((float) p.getY());
					particlesPositionsBuffer.put((float) p.getZ());

					if (p instanceof ParticleDataWithTextureCoordinates)
					{
						haveTextureCoordinates = true;

						ParticleDataWithTextureCoordinates texCoords = (ParticleDataWithTextureCoordinates) p;
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopRight());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYTopRight());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYTopLeft());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXBottomLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYBottomLeft());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXBottomLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYBottomLeft());
						
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopRight());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYTopRight());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYBottomLeft());
					}

					elements += 6;
				}
				if (elements > 0)
				{
					drawBuffers(renderingContext, elements, haveTextureCoordinates);
					totalDrawn += elements;
					elements = 0;
					
				}
			}
		}
		// We done here

		renderingContext.getRenderTargetManager().setDepthMask(true);
		//glDepthMask(true);
		
		//ObjectRenderer.drawFSQuad(billCoordVAL);

		renderingContext.getRenderTargetManager().setDepthMask(true);
		//glDepthMask(true);

		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderLights(renderingContext);
		return totalDrawn;
	}

	private int drawBuffers(RenderingContext renderingContext, int elements, boolean haveTextureCoordinates)
	{
		renderingContext.currentShader().setUniform1f("areTextureCoordinatesIninatesSupplied", haveTextureCoordinates ? 1f : 0f);

		// Render it now
		particlesPositionsBuffer.flip();

		particlesPositions.uploadData(particlesPositionsBuffer);
		
		renderingContext.bindAttribute("particlesPositionIn", particlesPositions.asAttributeSource(VertexFormat.FLOAT, 3, 12, 0));
		
		if (haveTextureCoordinates)
		{
			textureCoordinatesBuffer.flip();
			texCoords.uploadData(textureCoordinatesBuffer);

			renderingContext.bindAttribute("textureCoordinatesIn", texCoords.asAttributeSource(VertexFormat.FLOAT, 2, 8, 0));
		}
		
		renderingContext.draw(Primitive.TRIANGLE, 0, elements);
		renderingContext.flush();
		renderingContext.unbindAttributes();

		// And then clear the buffer to start over
		particlesPositionsBuffer.clear();

		return elements;
	}

	public void cleanAllParticles()
	{
		particles.clear();
	}

	public void destroy()
	{
		//Cleans up
		this.particlesPositions.destroy();
		this.texCoords.destroy();
	}
}
