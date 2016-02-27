package io.xol.engine.model;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;

import static org.lwjgl.opengl.GL11.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext
{
	XolioWindow engine;

	public ShaderProgram renderingShader = null;

	public boolean verticesAttribMode = false;

	public int vertexIn, texCoordIn, colorIn, normalIn;
	public boolean shadow;

	public RenderingContext(XolioWindow w)
	{
		engine = w;
	}

	public void setIsShadowPass(boolean isShadowPass)
	{
		shadow = isShadowPass;
	}

	public void setupVertexInputs(int vertexIn, int texCoordIn, int colorIn, int normalIn)
	{
		this.vertexIn = vertexIn;
		this.texCoordIn = texCoordIn;
		this.colorIn = colorIn;
		this.normalIn = normalIn;
		verticesAttribMode = true;
	}

	public void doneWithVertexInputs()
	{
		verticesAttribMode = false;
	}

	public void renderDirect(float[] vertexCoords, float[] texCoords, float[] colors, float[] normals)
	{

	}

	public void setCurrentShader(ShaderProgram s)
	{
		if (s != renderingShader)
			s.use();
		//else
		//	System.out.println("Prevented useless shader switch : "+s);
		renderingShader = s;
	}

	public void setDiffuseTexture(int id)
	{
		if (renderingShader != null)
			renderingShader.setUniformSampler(0, "diffuseTexture", id);
		//glBindTexture(GL_TEXTURE_2D, id);
		//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		//glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	}

	public void setNormalTexture(int id)
	{
		if (renderingShader != null)
			renderingShader.setUniformSampler(1, "normalTexture", id);
	}

	public void setMaterialTexture(int id)
	{
		if (renderingShader != null)
			renderingShader.setUniformSampler(2, "materialTexture", id);
	}
}