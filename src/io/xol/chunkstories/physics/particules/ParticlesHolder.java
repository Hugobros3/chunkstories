package io.xol.chunkstories.physics.particules;

import io.xol.chunkstories.physics.particules.Particle.Type;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.TexturesHandler;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticlesHolder
{
	final Map<Particle.Type, List<Particle>> particles = new HashMap<Particle.Type, List<Particle>>();
	ShaderProgram particlesShader = ShadersLibrary.getShaderProgram("particles");
	int planesVBO, particlesVBO;
	FloatBuffer renderBuffer;

	public ParticlesHolder()
	{
		planesVBO = glGenBuffers();
		FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 2 * 10000); // 320Kb
																		// buffer
																		// holding
																		// 80.000
																		// floats
		for (int i = 0; i < 10000; i++)
		{
			fb.put(new float[] { 1, 1 });
			fb.put(new float[] { 1, -1 });
			fb.put(new float[] { -1, -1 });
			fb.put(new float[] { -1, 1 });
		}
		fb.flip();
		glBindBuffer(GL_ARRAY_BUFFER, planesVBO);
		glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

		particlesVBO = glGenBuffers();
		renderBuffer = BufferUtils.createFloatBuffer(4 * 3 * 10000); // 480Kb
																		// buffer
																		// holding
																		// 120.000
																		// floats
	}

	public int count()
	{
		int a = 0;
		for (List<Particle> l : particles.values())
		{
			a += l.size();
		}
		return a;
	}

	public void addParticle(Particle particle)
	{
		Type particleType = particle.getType();
		if (!particles.keySet().contains(particleType))
		{
			particles.put(particleType, Collections.synchronizedList(new ArrayList<Particle>()));
		}
		particles.get(particleType).add(particle);
	}

	public synchronized void updatePhysics()
	{

		for (List<Particle> list : particles.values())
		{
			synchronized (list)
			{
				Iterator<Particle> iterator = list.iterator();
				Particle p;
				while (iterator.hasNext())
				{
					p = iterator.next();
					if (p != null)
					{
						p.update();
						if (p.dead)
							iterator.remove();
					}
					else
						iterator.remove();
				}
			}
		}

	}

	public int renderLights(WorldRenderer worldRenderer)
	{
		int i = 0;
		for (List<Particle> list : particles.values())
		{
			synchronized (list)
			{
				if (list.size() > 0)
				{
					if (list.get(0).emitsLights())
					{
						for (Particle p : list)
						{
							worldRenderer.renderDefferedLight(p.getLightEmited());
						}
					}
				}
			}
		}
		return i;
	}

	public int render(RenderingContext renderingContext)
	{
		int totalDrawn = 0;
		XolioWindow.getInstance().getRenderingContext().setCurrentShader(particlesShader);
		//particlesShader.use(true);
		// TexturesHandler.bindTexture("./res/textures/smoke.png");
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquation(GL_FUNC_ADD);
		glDisable(GL_CULL_FACE);
		 glEnable(GL_ALPHA_TEST);
		 glAlphaFunc(GL_GREATER, 0.0f);

		particlesShader.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);

		renderingContext.getCamera().setupShader(particlesShader);
		// glDisable(GL_DEPTH_TEST);

		// glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		// glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		// glDepthMask(false);

		int planeVAL = particlesShader.getVertexAttributeLocation("planeCoord");
		int billCoordVAL = particlesShader.getVertexAttributeLocation("billboardCoord");
		//int texcoordVAL = particlesShader.getVertexAttributeLocation("textureCoord");

		particlesShader.setUniformSampler(1, "lightColors", TexturesHandler.getTexture("environement/light.png"));

		renderingContext.enableVertexAttribute(planeVAL);
		renderingContext.enableVertexAttribute(billCoordVAL);
		//glEnablezVertexAttribArray(texcoordVAL);

		// glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

		for (List<Particle> list : particles.values())
		{
			synchronized (list)
			{
				if (list.size() > 0)
				{
					particlesShader.setUniformSampler(0, "diffuseTexture", TexturesHandler.getTexture(list.get(0).getTextureName()));
					particlesShader.setUniformFloat("billboardSize", list.get(0).getSize());

					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

					renderBuffer.clear();
					int elements = 0;
					for (Particle p : list)
					{
						if (elements >= 40000) // If elements exceed 40K
												// vertices ( and thus 120K
												// floats and the buffer size )
						{
							// Render it now
							renderBuffer.flip();

							glBindBuffer(GL_ARRAY_BUFFER, particlesVBO);
							glBufferData(GL_ARRAY_BUFFER, renderBuffer, GL_STATIC_DRAW);
							glVertexAttribPointer(billCoordVAL, 3, GL_FLOAT, false, 12, 0);

							glBindBuffer(GL_ARRAY_BUFFER, planesVBO);
							glVertexAttribPointer(planeVAL, 2, GL_FLOAT, false, 8, 0);
							// glDrawElements(GL_POINTS, elements,
							// GL_UNSIGNED_BYTE, 0);
							glPointSize(4f);
							glDrawArrays(GL_QUADS, 0, elements);
							totalDrawn += elements;

							// And then clear the buffer to start over
							elements = 0;
							renderBuffer.clear();
						}
						renderBuffer.put((float) p.posX);
						renderBuffer.put((float) p.posY);
						renderBuffer.put((float) p.posZ);
						elements++;
						renderBuffer.put((float) p.posX);
						renderBuffer.put((float) p.posY);
						renderBuffer.put((float) p.posZ);
						elements++;
						renderBuffer.put((float) p.posX);
						renderBuffer.put((float) p.posY);
						renderBuffer.put((float) p.posZ);
						elements++;
						renderBuffer.put((float) p.posX);
						renderBuffer.put((float) p.posY);
						renderBuffer.put((float) p.posZ);
						elements++;
					}
					if (elements > 0)
					{
						renderBuffer.flip();

						glBindBuffer(GL_ARRAY_BUFFER, particlesVBO);
						glBufferData(GL_ARRAY_BUFFER, renderBuffer, GL_STATIC_DRAW);
						glVertexAttribPointer(billCoordVAL, 3, GL_FLOAT, false, 12, 0);

						glBindBuffer(GL_ARRAY_BUFFER, planesVBO);
						glVertexAttribPointer(planeVAL, 2, GL_FLOAT, false, 8, 0);
						// glDrawElements(GL_POINTS, elements, GL_UNSIGNED_BYTE,
						// 0);
						glPointSize(4f);
						glDrawArrays(GL_QUADS, 0, elements);
						totalDrawn += elements;
					}
					// System.out.println("drawing "+elements+" elements." +
					// renderBuffer.toString());
				}
			}
		}
		// We done here
		renderingContext.disableVertexAttribute(planeVAL);
		renderingContext.disableVertexAttribute(billCoordVAL);
		//glDisablezVertexAttribArray(texcoordVAL);

		glDepthMask(true);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBlendEquation(GL_FUNC_ADD);
		glDisable(GL_BLEND);

		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

		return totalDrawn;
	}

	public void cleanAllParticles()
	{
		particles.clear();
	}

	/*public void reloadShaders()
	{
		particlesShader.reload(FastConfig.getShaderConfig());
	}*/
}
