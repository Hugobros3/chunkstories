package io.xol.chunkstories.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.world.World;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticlesRenderer implements ParticlesManager
{
	final World world;

	final Map<ParticleType, Set<ParticleData>> particles = new ConcurrentHashMap<ParticleType, Set<ParticleData>>();

	ShaderProgram particlesShader = ShadersLibrary.getShaderProgram("particles");

	//TODO use objects
	//int billboardSquareVBO, particlesPositionsVBO, texCoordsVBO;
	VerticesObject billboardSquare, particlesPositions, texCoords;
	FloatBuffer particlesPositionsBuffer, textureCoordinatesBuffer;

	public ParticlesRenderer(World world)
	{
		this.world = world;

		billboardSquare = new VerticesObject();
		//billboardSquareVBO = glGenBuffers();
		
		//320kb
		FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 2 * 10000);
		for (int i = 0; i < 10000; i++)
		{
			fb.put(new float[] { 1, 1 });
			fb.put(new float[] { 1, -1 });
			fb.put(new float[] { -1, -1 });
			fb.put(new float[] { -1, 1 });
		}
		fb.flip();
		
		//glBindBuffer(GL_ARRAY_BUFFER, billboardSquareVBO);
		//glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
		billboardSquare.uploadData(fb);

		particlesPositions = new VerticesObject();
		texCoords = new VerticesObject();
		//particlesPositionsVBO = glGenBuffers();
		//texCoordsVBO = glGenBuffers();
		
		//480kb
		particlesPositionsBuffer = BufferUtils.createFloatBuffer(4 * 3 * 10000);
		//320kb
		textureCoordinatesBuffer = BufferUtils.createFloatBuffer(4 * 2 * 10000);
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

	public void spawnParticleAtPosition(String particleTypeName, Vector3d location)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, location, null);
	}

	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3d location, Vector3d velocity)
	{
		ParticleType particleType = ParticleTypes.getParticleTypeByName(particleTypeName);
		if (particleType == null || location == null)
			return;

		ParticleData particleData = particleType.createNew(world, (float) location.getX(), (float) location.getY(), (float) location.getZ());
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
		//particlesShader.use(true);
		// TexturesHandler.bindTexture("./res/textures/smoke.png");
		//glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquation(GL_FUNC_ADD);
		glDisable(GL_CULL_FACE);
		glEnable(GL_ALPHA_TEST);
		glAlphaFunc(GL_GREATER, 0.0f);

		// glDisable(GL_DEPTH_TEST);

		// glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		// glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		// glDepthMask(false);

		renderingContext.setCurrentShader(particlesShader);

		particlesShader.setUniformFloat2("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		renderingContext.getCamera().setupShader(particlesShader);
		particlesShader.setUniformSampler(1, "lightColors", TexturesHandler.getTexture("environement/light.png"));

		//Vertex attributes

		int billboardSquareVAL = particlesShader.getVertexAttributeLocation("billboardSquareCoordsIn");
		int particlesPositionsVAL = particlesShader.getVertexAttributeLocation("particlesPositionIn");
		int textureCoordinatesVAL = particlesShader.getVertexAttributeLocation("textureCoordinatesIn");

		renderingContext.enableVertexAttribute(billboardSquareVAL);
		renderingContext.enableVertexAttribute(particlesPositionsVAL);
		//glEnablezVertexAttribArray(texcoordVAL);

		// glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

		//For all present particles types
		for (ParticleType particleType : particles.keySet())
		{

			//synchronized (list)
			//{
			//Don't bother rendering empty sets
			if (particles.get(particleType).size() > 0)
			{
				Iterator<ParticleData> iterator = particles.get(particleType).iterator();
				boolean haveTextureCoordinates = false;

				particleType.beginRenderingForType(renderingContext);
				//particlesShader.setUniformSampler(0, "diffuseTexture", TexturesHandler.getTexture(list.get(0).getTextureName()));
				//particlesShader.setUniformFloat("billboardSize", list.get(0).getSize());

				particleType.getTexture().setLinearFiltering(false);
				//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

				textureCoordinatesBuffer.clear();
				particlesPositionsBuffer.clear();
				int elements = 0;

				while (iterator.hasNext())
				{
					//Iterate over dem particles
					ParticleData p = iterator.next();

					//If > 40k elements, buffer is full, draw it
					if (elements >= 40000)
					{
						drawBuffers(renderingContext, billboardSquareVAL, particlesPositionsVAL, textureCoordinatesVAL, elements, haveTextureCoordinates);
						totalDrawn += elements;
						elements = 0;
					}

					particlesPositionsBuffer.put((float) p.x);
					particlesPositionsBuffer.put((float) p.y);
					particlesPositionsBuffer.put((float) p.z);

					particlesPositionsBuffer.put((float) p.x);
					particlesPositionsBuffer.put((float) p.y);
					particlesPositionsBuffer.put((float) p.z);

					particlesPositionsBuffer.put((float) p.x);
					particlesPositionsBuffer.put((float) p.y);
					particlesPositionsBuffer.put((float) p.z);

					particlesPositionsBuffer.put((float) p.x);
					particlesPositionsBuffer.put((float) p.y);
					particlesPositionsBuffer.put((float) p.z);

					if (p instanceof ParticleDataWithTextureCoordinates)
					{
						/*
						 * 
						fb.put(new float[] { 1, 1 });
						fb.put(new float[] { 1, -1 });
						fb.put(new float[] { -1, -1 });
						fb.put(new float[] { -1, 1 });
						 */

						if (!haveTextureCoordinates)
							renderingContext.enableVertexAttribute(textureCoordinatesVAL);

						haveTextureCoordinates = true;

						ParticleDataWithTextureCoordinates texCoords = (ParticleDataWithTextureCoordinates) p;
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopRight());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYTopRight());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXTopLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYTopLeft());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXBottomLeft());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYBottomLeft());

						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateXBottomRight());
						textureCoordinatesBuffer.put(texCoords.getTextureCoordinateYBottomRight());
					}

					elements += 4;
				}
				if (elements > 0)
				{
					drawBuffers(renderingContext, billboardSquareVAL, particlesPositionsVAL, textureCoordinatesVAL, elements, haveTextureCoordinates);
					totalDrawn += elements;
					elements = 0;

					/*particlesPositionsBuffer.flip();
					
					glBindBuffer(GL_ARRAY_BUFFER, particlesPositionsVBO);
					glBufferData(GL_ARRAY_BUFFER, particlesPositionsBuffer, GL_STATIC_DRAW);
					renderingContext.setVertexAttributePointerLocation(billCoordVAL, 3, GL_FLOAT, false, 12, 0);
					
					glBindBuffer(GL_ARRAY_BUFFER, billboardSquareVBO);
					renderingContext.setVertexAttributePointerLocation(planeVAL, 2, GL_FLOAT, false, 8, 0);
					// glDrawElements(GL_POINTS, elements, GL_UNSIGNED_BYTE,
					// 0);
					//glPointSize(4f);
					GLCalls.drawArrays(GL_QUADS, 0, elements);
					totalDrawn += elements;*/
				}
				// System.out.println("drawing "+elements+" elements." +
				// renderBuffer.toString());

				if (haveTextureCoordinates)
					renderingContext.disableVertexAttribute(textureCoordinatesVAL);
			}
			//}
		}
		// We done here
		renderingContext.disableVertexAttribute(billboardSquareVAL);
		renderingContext.disableVertexAttribute(particlesPositionsVAL);
		//glDisablezVertexAttribArray(texcoordVAL);

		//ObjectRenderer.drawFSQuad(billCoordVAL);

		glDepthMask(true);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquation(GL_FUNC_ADD);
		glDisable(GL_BLEND);

		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

		renderLights(renderingContext);

		return totalDrawn;
	}

	private int drawBuffers(RenderingContext renderingContext, int planeVAL, int billCoordVAL, int texCoordVAL, int elements, boolean haveTextureCoordinates)
	{
		renderingContext.currentShader().setUniformFloat("areTextureCoordinatesIninatesSupplied", haveTextureCoordinates ? 1f : 0f);

		// Render it now
		particlesPositionsBuffer.flip();

		//glBindBuffer(GL_ARRAY_BUFFER, particlesPositionsVBO);
		//glBufferData(GL_ARRAY_BUFFER, particlesPositionsBuffer, GL_STATIC_DRAW);
		particlesPositions.uploadData(particlesPositionsBuffer);
		renderingContext.setVertexAttributePointerLocation("particlesPositionIn", 3, GL_FLOAT, false, 12, 0, particlesPositions);
		//renderingContext.setVertexAttributePointerLocation(billCoordVAL, 3, GL_FLOAT, false, 12, 0);

		if (haveTextureCoordinates)
		{
			textureCoordinatesBuffer.flip();
			//glBindBuffer(GL_ARRAY_BUFFER, texCoordsVBO);
			//glBufferData(GL_ARRAY_BUFFER, textureCoordinatesBuffer, GL_STATIC_DRAW);
			texCoords.uploadData(textureCoordinatesBuffer);
			
			renderingContext.setVertexAttributePointerLocation("textureCoordinatesIn", 2, GL_FLOAT, false, 8, 0, texCoords);
		}

		//glBindBuffer(GL_ARRAY_BUFFER, billboardSquareVBO);
		renderingContext.setVertexAttributePointerLocation("billboardSquareCoordsIn", 2, GL_FLOAT, false, 8, 0, billboardSquare);
		// glDrawElements(GL_POINTS, elements,
		// GL_UNSIGNED_BYTE, 0);
		glPointSize(4f);
		GLCalls.drawArrays(GL_QUADS, 0, elements);

		// And then clear the buffer to start over
		particlesPositionsBuffer.clear();

		return elements;
	}

	public void cleanAllParticles()
	{
		particles.clear();
	}
}
