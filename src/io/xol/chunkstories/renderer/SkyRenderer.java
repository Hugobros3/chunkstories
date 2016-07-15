package io.xol.chunkstories.renderer;

import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.model.RenderingContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.FastConfig;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import io.xol.engine.math.Math2;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SkyRenderer
{
	public float time = 0;
	float distance = 1500;
	float height = -500;

	World world;
	WorldRenderer worldRenderer;
	CloudsRenderer cloudsRenderer;
	
	public SkyRenderer(World world, WorldRenderer worldRenderer)
	{
		this.world = world;
		this.worldRenderer = worldRenderer;
		this.cloudsRenderer = new CloudsRenderer(world, this);
	}

	public Vector3f getSunPosition()
	{
		float sunloc = (float) (time * Math.PI * 2 / 1.6 - 0.5);
		float sunangle = 0;
		float sundistance = 1000;
		return new Vector3f((float) (400 + sundistance * Math.sin(rad(sunangle)) * Math.cos(sunloc)), (float) (height + sundistance * Math.sin(sunloc)), (float) (sundistance * Math.cos(rad(sunangle)) * Math.cos(sunloc))).normalise();
	}
	
	public void render(RenderingContext renderingContext)
	{
		//setupFog();

		glDisable(GL_ALPHA_TEST);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		glDepthMask(false);

		Vector3f sunPosVector = getSunPosition();
		double[] sunpos = { sunPosVector.x, sunPosVector.y, sunPosVector.z };

		ShaderProgram skyShader = ShadersLibrary.getShaderProgram("sky");
		renderingContext.setCurrentShader(skyShader);
		
		// TexturesHandler.bindTexture("res/textures/environement/sky.png");
		GameWindowOpenGL.getInstance().getRenderingContext().setCurrentShader(skyShader);
		//skyShader.use(true);
		skyShader.setUniformSampler(9, "cloudsNoise", TexturesHandler.getTexture("environement/cloudsStatic.png"));
		
		Texture2D glowTexture = TexturesHandler.getTexture("environement/glow.png");
		skyShader.setUniformSampler(2, "glowSampler", glowTexture);
		
		glowTexture.setLinearFiltering(true);
		glowTexture.setTextureWrapping(false);
		glowTexture.setTextureWrapping(false);

		Texture2D skyTextureSunny = TexturesHandler.getTexture("environement/sky.png");
		Texture2D skyTextureRaining = TexturesHandler.getTexture("environement/sky_rain.png");
		
		skyShader.setUniformSampler(0, "skyTextureSunny", skyTextureSunny);
		skyShader.setUniformSampler(1, "skyTextureRaining", skyTextureRaining);
		
		skyShader.setUniformFloat("overcastFactor", world.getWeather());
		
		skyTextureSunny.setLinearFiltering(true);
		skyTextureSunny.setMipMapping(false);
		skyTextureSunny.setTextureWrapping(false);
		
		skyTextureRaining.setLinearFiltering(true);
		skyTextureRaining.setMipMapping(false);
		skyTextureRaining.setTextureWrapping(false);

		//skyShader.setUniformSamplerCube(2, "skybox", TexturesHandler.idCubemap("res/textures/skybox"));
		skyShader.setUniformFloat3("camPos", renderingContext.getCamera().pos.castToSP());
		skyShader.setUniformFloat3("sunPos", (float) sunpos[0], (float) sunpos[1], (float) sunpos[2]);
		skyShader.setUniformFloat("time", time);
		renderingContext.getCamera().setupShader(skyShader);

		ObjectRenderer.drawFSQuad(skyShader.getVertexAttributeLocation("vertexIn"));

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glPointSize(1f);

		ShaderProgram starsShader = skyShader = ShadersLibrary.getShaderProgram("stars");
		
		renderingContext.setCurrentShader(starsShader);
		//starsShader.use(true);
		starsShader.setUniformFloat3("sunPos", (float) sunpos[0], (float) sunpos[1], (float) sunpos[2]);
		starsShader.setUniformFloat3("color", 1f, 1f, 1f);
		renderingContext.getCamera().setupShader(starsShader);
		int NB_STARS = 500;
		if (stars == null)
		{
			stars = BufferUtils.createFloatBuffer(NB_STARS * 3);
			for (int i = 0; i < NB_STARS; i++)
			{
				Vector3f star = new Vector3f((float) Math.random() * 2f - 1f, (float) Math.random(), (float) Math.random() * 2f - 1f);
				star.normalise();
				star.scale(100f);
				stars.put(new float[] { star.x, star.y, star.z });
			}
		}
		stars.rewind();
		int vertexIn = starsShader.getVertexAttributeLocation("vertexIn");
		if (vertexIn >= 0)
		{
			renderingContext.enableVertexAttribute(vertexIn);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glVertexAttribPointer(vertexIn, 3, false, 0, stars);
			glDrawArrays(GL_POINTS, 0, NB_STARS);
			renderingContext.disableVertexAttribute(vertexIn);
			//starsShader.use(false);
		}
		glDisable(GL_BLEND);
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		
		cloudsRenderer.renderClouds(renderingContext);
	}

	private FloatBuffer stars = null;

	private double rad(float h)
	{
		return h / 180 * Math.PI;
	}

	public void setupShader(ShaderProgram shader)
	{
		float fogFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.4f) / 0.1f, 1.0f);
		
		shader.setUniformFloat("fogStartDistance", Math2.mix(FastConfig.viewDistance, 32, fogFactor));
		shader.setUniformFloat("fogEndDistance", Math2.mix(1024, 384, fogFactor));
		shader.setUniformFloat("overcastFactor", world.getWeather());
	}
	
	public void destroy()
	{
		cloudsRenderer.destroy();
	}
}
