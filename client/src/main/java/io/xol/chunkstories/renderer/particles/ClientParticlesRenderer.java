//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.particles;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticleTypeHandler.ParticleData;
import io.xol.chunkstories.api.particles.ParticleTypeHandler.ParticleTypeRenderer;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.engine.graphics.geometry.VertexBufferGL;



public class ClientParticlesRenderer implements ParticlesRenderer
{
	final WorldClient world;
	final Content.ParticlesTypes store;

	final Map<ParticleTypeHandler, Collection<ParticleData>> particles = new ConcurrentHashMap<ParticleTypeHandler, Collection<ParticleData>>();
	final Map<ParticleTypeHandler, ParticleTypeRenderer> renderers = new HashMap<ParticleTypeHandler, ParticleTypeRenderer>();
	
	VertexBuffer particlesPositions, texCoords;
	FloatBuffer particlesPositionsBuffer, textureCoordinatesBuffer;

	public ClientParticlesRenderer(WorldClient world)
	{
		this.world = world;
		this.store = world.getGameContext().getContent().particles();

		particlesPositions = new VertexBufferGL();
		texCoords = new VertexBufferGL();
		
		//480kb
		particlesPositionsBuffer = BufferUtils.createFloatBuffer(6 * 3 * 10000);
		//320kb
		textureCoordinatesBuffer = BufferUtils.createFloatBuffer(6 * 2 * 10000);
	}

	public int count()
	{
		int a = 0;
		for (Collection<ParticleData> l : particles.values())
		{
			a += l.size();
		}
		return a;
	}

	public void spawnParticleAtPosition(String particleTypeName, Vector3dc location)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, location, null);
	}

	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc location, Vector3dc velocity)
	{
		ParticleTypeHandler particleType = store.getParticleTypeHandlerByName(particleTypeName);
		if (particleType == null || location == null)
			return;

		ParticleData particleData = particleType.createNew(world, (float)(double) location.x(), (float)(double) location.y(), (float)(double) location.z());
		if (velocity != null && particleData instanceof ParticleDataWithVelocity)
			((ParticleDataWithVelocity) particleData).setVelocity(new Vector3f((float)velocity.x(), (float)velocity.y(), (float)velocity.z()));

		addParticle(particleType, particleData);
	}

	void addParticle(ParticleTypeHandler particleType, ParticleData particleData)
	{
		//Add this to the data sets
		if (!particles.keySet().contains(particleType))
		{
			particles.put(particleType, new ConcurrentLinkedQueue<ParticleData>());//ConcurrentHashMap.newKeySet());//Collections.synchronizedList(new ArrayList<ParticleData>()));
		}
		particles.get(particleType).add(particleData);
	}

	public void updatePhysics()
	{
		for (ParticleTypeHandler particleType : particles.keySet())
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

	public void renderParticles(RenderingInterface renderingInterface)
	{
		int totalDrawn = 0;
		
		//For all present particles types
		for (ParticleTypeHandler particleTypeHandler : particles.keySet())
		{
			/*RenderTime renderTime = particleTypeHandler.getType().getRenderTime();
			
			//Skip forward stuff when doing gbuf
			if(isThisGBufferPass && renderTime == RenderTime.FORWARD)
				continue;
			
			//Skip gbuf stuff when doing forward
			else if(!isThisGBufferPass && renderTime == RenderTime.NEVER)
				continue;
			else if(!isThisGBufferPass && renderTime == RenderTime.GBUFFER)
				continue;*/
			String renderPass = particleTypeHandler.getType().getRenderPass();
			if(!renderingInterface.getWorldRenderer().getRenderingPipeline().getCurrentPass().name.equals(renderPass))
				continue;
				
			//Don't bother rendering empty sets
			if (particles.get(particleTypeHandler).size() > 0)
			{
				Iterator<ParticleData> iterator = particles.get(particleTypeHandler).iterator();
				boolean haveTextureCoordinates = false;
				
				ParticleTypeRenderer renderer = getRendererForType(particleTypeHandler);
				
				renderer.beginRenderingForType(renderingInterface);
				textureCoordinatesBuffer.clear();
				particlesPositionsBuffer.clear();
				
				//Some stuff don't wanna be rendered, so don't
				boolean actuallyRenderThatStuff = !renderPass.equals("lights");
				
				int elementsInDrawBuffer = 0;
				while (iterator.hasNext())
				{
					//Iterate over dem particles
					ParticleData p = iterator.next();

					//Check we don't have a null particle
					if (p != null)
					{
						renderer.forEach_Rendering(renderingInterface, p);
						if (p.isDed())
							iterator.remove();
					}
					else
					{
						iterator.remove();
						continue;
					}
					
					if(!actuallyRenderThatStuff)
						continue;
					
					//If > 60k elements, buffer is full, draw it
					if (elementsInDrawBuffer >= 60000)
					{
						drawBuffers(renderingInterface, elementsInDrawBuffer, haveTextureCoordinates);
						totalDrawn += elementsInDrawBuffer;
						elementsInDrawBuffer = 0;
					}

					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					//Second triangle
					
					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					particlesPositionsBuffer.put((float) p.x());
					particlesPositionsBuffer.put((float) p.y());
					particlesPositionsBuffer.put((float) p.z());

					//TODO No this sucks. Delet dis.
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

					elementsInDrawBuffer += 6;
				}
				
				//Draw the stuff
				if (elementsInDrawBuffer > 0)
				{
					drawBuffers(renderingInterface, elementsInDrawBuffer, haveTextureCoordinates);
					totalDrawn += elementsInDrawBuffer;
					elementsInDrawBuffer = 0;
				}
			}
		}
		
		// We done here
		renderingInterface.getRenderTargetManager().setDepthMask(true);
		renderingInterface.setBlendMode(BlendMode.DISABLED);
		
		//return totalDrawn;
	}

	private ParticleTypeRenderer getRendererForType(ParticleTypeHandler particleTypeHandler) {
		
		ParticleTypeRenderer r = this.renderers.get(particleTypeHandler);
		if(r == null) {
			r = particleTypeHandler.getRenderer(this);
			this.renderers.put(particleTypeHandler, r);
		}
		
		return r;
	}

	private int drawBuffers(RenderingInterface renderingInterface, int elements, boolean haveTextureCoordinates)
	{
		renderingInterface.currentShader().setUniform1f("areTextureCoordinatesSupplied", haveTextureCoordinates ? 1f : 0f);

		// Render it now
		particlesPositionsBuffer.flip();

		particlesPositions.uploadData(particlesPositionsBuffer);
		
		renderingInterface.bindAttribute("particlesPositionIn", particlesPositions.asAttributeSource(VertexFormat.FLOAT, 3, 12, 0));
		
		if (haveTextureCoordinates)
		{
			textureCoordinatesBuffer.flip();
			texCoords.uploadData(textureCoordinatesBuffer);

			renderingInterface.bindAttribute("textureCoordinatesIn", texCoords.asAttributeSource(VertexFormat.FLOAT, 2, 8, 0));
		}
		
		renderingInterface.draw(Primitive.TRIANGLE, 0, elements);
		renderingInterface.unbindAttributes();

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
		
		for(ParticleTypeRenderer type : renderers.values())
		{
			type.destroy();
		}
	}
	

	@Override
	public ClientInterface getClient() {
		return world.getClient();
	}

	@Override
	public ClientContent getContent() {
		return world.getClient().getContent();
	}
}
