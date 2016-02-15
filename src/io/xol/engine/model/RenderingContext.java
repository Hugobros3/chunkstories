package io.xol.engine.model;

import io.xol.engine.shaders.ShaderProgram;

import static org.lwjgl.opengl.GL11.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext
{
	public static ShaderProgram renderingShader = null;

	public static boolean verticesAttribMode = false;

	public static int vertexIn, texCoordIn, colorIn, normalIn;
	public static boolean shadow;

	public static void disableVAMode()
	{
		verticesAttribMode = false;
	}

	public static void enableVAMode(int vertexIn, int texCoordIn, int colorIn, int normalIn, boolean shadow)
	{
		RenderingContext.vertexIn = vertexIn;
		RenderingContext.texCoordIn = texCoordIn;
		RenderingContext.colorIn = colorIn;
		RenderingContext.normalIn = normalIn;
		RenderingContext.shadow = shadow;
		verticesAttribMode = true;
	}

	public static void setCurrentShader(ShaderProgram s)
	{
		renderingShader = s;
	}

	public static void setDiffuseTexture(int id)
	{
		if (renderingShader != null)
			renderingShader.setUniformSampler(0, "diffuseTexture", id);
		glBindTexture(GL_TEXTURE_2D, id);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	}

	public static void setNormalTexture(int id)
	{
		if (renderingShader != null)
			renderingShader.setUniformSampler(1, "normalTexture", id);
	}

}