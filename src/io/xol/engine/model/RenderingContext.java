package io.xol.engine.model;

import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext
{
	XolioWindow engine;

	public ShaderProgram renderingShader = null;
	
	private Camera camera;

	public boolean verticesAttribMode = false;

	public int vertexIn, texCoordIn, colorIn, normalIn;
	public boolean shadow;

	// 4 Temporary VBOs for streamed rendering
	public int tempVBO[] = new int[4];
	
	public RenderingContext(XolioWindow w)
	{
		engine = w;
		for(int i = 0; i < tempVBO.length; i++)
			tempVBO[i] = glGenBuffers();
	}

	public void setCamera(Camera camera)
	{
		this.camera = camera;
	}
	
	public Camera getCamera()
	{
		return camera;
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

	//4Megs scratch buffer
	FloatBuffer tempBuffer = BufferUtils.createFloatBuffer(1024 * 1024);
	
	/**
	 * Renders some vertices using the currently bound shader and data provided by the main memory ( kinda slow, not proper for big things )
	 * Only accepts 32bit SP floats
	 * The amount of points drawn is equal to vertexCoords.length / 3 
	 * The maximal size of any of the arguments arrays is 1 mebi floats (1024^2), that is 4mebioctels of raw bytes data <b>(silent fails else)</b>
	 * @param vertexCoords An array of floats, <b>it must be of a length multiple of 9</b> ( Triangles made up of points made up of 3 coordinates ) <b>or else it silent-fails</b>
	 * @param texCoords Must provides texturing info for *all* points given or nothing (null)
	 * @param colors Must provides coloring info for *all* points given or nothing (null)
	 * @param normals Must provides normal info for *all* points given or nothing (null) 
	 */
	public void renderDirect(float[] vertexCoords, float[] texCoords, float[] colors, float[] normals)
	{
		//Sanity check
		if(vertexCoords.length % 9 != 0)
			return;
		
		//glDisable(GL_CULL_FACE);
		//glDisable(GL_DEPTH_TEST)
		
		//Parse inputs, grab vertex attribute locations
		int pointsToDraw = vertexCoords.length / 3;
		int vIn = renderingShader.getVertexAttributeLocation("vertexIn");
		int tIn = renderingShader.getVertexAttributeLocation("texCoordIn");
		if(texCoords == null)
			tIn = -1;
		int cIn = renderingShader.getVertexAttributeLocation("colorIn");
		if(colors == null)
			cIn = -1;
		int nIn = renderingShader.getVertexAttributeLocation("normalIn");
		if(normals == null)
			nIn = -1;
		setupVertexInputs(vIn, tIn, cIn, nIn);
		
		//Enable vertex arrays (to be moved in setupVertexInputs)
		glEnableVertexAttribArray(vertexIn);
		if(texCoordIn != -1)
			glEnableVertexAttribArray(texCoordIn);
		if(colorIn != -1)
			glEnableVertexAttribArray(colorIn);
		if(normalIn != -1)
			glEnableVertexAttribArray(normalIn);
		
		//Upload data
		glBindBuffer(GL_ARRAY_BUFFER, tempVBO[0]);
		tempBuffer.clear();
		tempBuffer.put(vertexCoords);
		tempBuffer.flip();
		glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
		glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 12, 0);
		if(texCoordIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[1]);
			tempBuffer.clear();
			tempBuffer.put(texCoords);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = texCoords.length / pointsToDraw;
			glVertexAttribPointer(texCoordIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(colorIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[2]);
			tempBuffer.clear();
			tempBuffer.put(colors);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = colors.length / pointsToDraw;
			glVertexAttribPointer(colorIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(normalIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[3]);
			tempBuffer.clear();
			tempBuffer.put(normals);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = normals.length / pointsToDraw;
			glVertexAttribPointer(normalIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		
		glDrawArrays(GL_TRIANGLES, 0, pointsToDraw);

		//Disable vertex arrays (to be moved in doneWithVertexInputs)
		glDisableVertexAttribArray(vertexIn);
		if(texCoordIn != -1)
			glDisableVertexAttribArray(texCoordIn);
		if(colorIn != -1)
			glDisableVertexAttribArray(colorIn);
		if(normalIn != -1)
			glDisableVertexAttribArray(normalIn);
		doneWithVertexInputs();
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