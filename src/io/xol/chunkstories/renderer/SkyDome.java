package io.xol.chunkstories.renderer;

import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.TexturesHandler;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.chunkstories.client.FastConfig;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SkyDome
{
	public float time = 0;
	float distance = 1500;
	float height = -500;

	BufferedImage colors;
	BufferedImage light;

	ShaderProgram skyShader;
	ShaderProgram starsShader;
	ShaderProgram cloudsShader;

	public SkyDome()
	{
		try
		{
			colors = ImageIO.read(new File("res/textures/environement/sky.png"));
			light = ImageIO.read(new File("res/textures/environement/lightcolors.png"));

			skyShader = ShadersLibrary.getShaderProgram("sky");
			cloudsShader = ShadersLibrary.getShaderProgram("clouds");
			starsShader = ShadersLibrary.getShaderProgram("stars");

		}
		catch (IOException e)
		{
			System.out.println("couldn't properly load colors file :(");
			e.printStackTrace();
		}
	}

	public Vector3f getSunPos()
	{
		float sunloc = (float) (time * Math.PI * 2 / 1.6 - 0.5);
		float sunangle = 0;
		float sundistance = 1000;
		// double[] sunpos =
		// {sundistance*Math.sin(rad(sunangle))*Math.cos(sunloc),
		// height+sundistance*Math.sin(sunloc),
		// sundistance*Math.cos(rad(sunangle))*Math.cos(sunloc)};
		return new Vector3f((float) (400 + sundistance * Math.sin(rad(sunangle)) * Math.cos(sunloc)), (float) (height + sundistance * Math.sin(sunloc)), (float) (sundistance * Math.cos(rad(sunangle)) * Math.cos(sunloc)));
	}

	public void render(Camera camera)
	{
		setupFog();

		glDisable(GL_ALPHA_TEST);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		glDepthMask(false);

		Vector3f sunPosVector = getSunPos();
		double[] sunpos = { sunPosVector.x, sunPosVector.y, sunPosVector.z };

		// TexturesHandler.bindTexture("res/textures/environement/sky.png");
		skyShader.use(true);
		skyShader.setUniformSampler(9, "cloudsNoise", "res/textures/environement/cloudsStatic.png");
		skyShader.setUniformSampler(1, "glowSampler", "res/textures/environement/glow.png");
		glBindTexture(GL_TEXTURE_2D, TexturesHandler.idTexture("res/textures/environement/glow.png"));
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		skyShader.setUniformSampler(0, "colorSampler", "res/textures/environement/sky.png");
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		//skyShader.setUniformSamplerCube(2, "skybox", TexturesHandler.idCubemap("res/textures/skybox"));
		skyShader.setUniformFloat3("camPos", camera.camPosX, camera.camPosY, camera.camPosZ);
		skyShader.setUniformFloat3("sunPos", (float) sunpos[0], (float) sunpos[1], (float) sunpos[2]);
		skyShader.setUniformFloat("time", time);
		camera.setupShader(skyShader);

		// glTexParameteri(GL_TEXTURE_2D, pname, param);
		// glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		TexturesHandler.nowrap("res/textures/environement/sky.png");

		ObjectRenderer.drawFSQuad(skyShader.getVertexAttributeLocation("vertexIn"));

		skyShader.use(false);

		/*TexturesHandler.bindTexture("res/textures/environement/moon.png");
		double moonloc = (0.2 + time) * Math.PI * 2;
		float moonangle = 0;
		double moondistance = 1350;
		double[] moonpos = { moondistance * Math.sin(rad(moonangle)) * Math.cos(moonloc), height + moondistance * Math.sin(moonloc), moondistance * Math.cos(rad(moonangle)) * Math.cos(moonloc) };
		 */

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glPointSize(1f);
		starsShader.use(true);
		starsShader.setUniformFloat3("sunPos", (float) sunpos[0], (float) sunpos[1], (float) sunpos[2]);
		starsShader.setUniformFloat3("color", 1f, 1f, 1f);
		camera.setupShader(starsShader);
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
			glEnableVertexAttribArray(vertexIn);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glVertexAttribPointer(vertexIn, 3, false, 0, stars);
			glDrawArrays(GL_POINTS, 0, NB_STARS);
			glDisableVertexAttribArray(vertexIn);
			starsShader.use(false);
		}
		glDisable(GL_BLEND);
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		//stars = null;

	}

	private FloatBuffer stars = null;

	private double rad(float h)
	{
		return h / 180 * Math.PI;
	}

	public void reloadShader()
	{
		TexturesHandler.freeTexture("res/textures/environement/sky.png");
		TexturesHandler.freeTexture("res/textures/environement/glow.png");
		//skyShader.reload(FastConfig.getShaderConfig());
		//cloudsShader.reload(FastConfig.getShaderConfig());
		try
		{
			colors = ImageIO.read(new File("res/textures/environement/sky.png"));
			light = ImageIO.read(new File("res/textures/environement/lightcolors.png"));

		}
		catch (IOException e)
		{
			System.out.println("couldn't properly load colors file :(");
			e.printStackTrace();
		}
	}

	public float getShadowIntensity()
	{
		Color color = new Color(light.getRGB((int) (time * 255), 0));
		return color.getRed() / 255f;
	}

	public int[] getSkyColor()
	{
		Color color = new Color(colors.getRGB((int) (time * 255), 255));
		return new int[] { color.getRed(), color.getGreen(), color.getBlue() };
	}

	private void setupFog()
	{
		FloatBuffer fogColor = BufferUtils.createFloatBuffer(4);
		Color color = new Color(colors.getRGB((int) (time * 256), 255));
		fogColor.put(color.getRed() / 255f).put(color.getGreen() / 255f).put(color.getBlue() / 255f).put(1f).flip();
		int fogMode;
		fogMode = GL_EXP2;
		glFogi(GL_FOG_MODE, fogMode);
		glFog(GL_FOG_COLOR, fogColor);
		/*
		 * float fogFactor = exp2( -gl_Fog.density * gl_Fog.density * dist *
		 * dist * LOG2 );
		 * 
		 * 1 = exp2(-x*x *distance*distance*log2) 1 = 2^(-x²*dist²*log2)
		 * (-x²*dist²*log2) = log(2,1) = 0
		 * 
		 * let's say -0.005 constant LOG2 = 1.442695
		 * 
		 * (-x²*dist²*log2) = -0.005 (x²*dist²*log2) = 0.005 x² =
		 * 0.005/(dist²*log2) x =
		 * Math.sqrt(0.005d/(FastConfig.viewDistance*FastConfig
		 * .viewDistance*1.442695*1.442695));
		 * 
		 * 
		 * a = 3² 2 = log(a,3)
		 */
		glFogf(GL_FOG_DENSITY, (float) Math.sqrt(0.015d / (FastConfig.viewDistance * 1.442695)));
		// System.out.println( (float)
		// Math.sqrt(0.005d/(FastConfig.viewDistance*1.442695))+" "+0.0055f);
		// glFogf(GL_FOG_DENSITY, 0.0055f);
		glHint(GL_FOG_HINT, GL_DONT_CARE);
		glFogf(GL_FOG_START, 550.0f);
		glFogf(GL_FOG_END, 650.0f);
	}
}
