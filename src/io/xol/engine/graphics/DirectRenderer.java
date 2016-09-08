package io.xol.engine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The direct renderer is the simplest way to draw stuff on screen, provided that a shader is properly bound and setup you just have to provide whatever buffers you have
 * and the rest is figured out for you
 */
public class DirectRenderer
{
	private RenderingContext renderingContext;

	//4Megs scratch buffer
	FloatBuffer tempBuffer = BufferUtils.createFloatBuffer(1024 * 1024);
	
	// 4 Temporary VBOs for streamed rendering
	private int tempVBO[] = new int[4];

	public DirectRenderer(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
		for (int i = 0; i < tempVBO.length; i++)
			tempVBO[i] = glGenBuffers();
	}

	/**
	 * Renders some vertices using the currently bound shader and data provided by the main memory ( kinda slow, not proper for big things ) Only accepts 32bit SP floats
	 * 
	 * @param vertexCoords
	 *            An array of floats, <b>it must be of a length multiple of 9</b> ( Triangles made up of points made up of 3 coordinates ) <b>or else it silent-fails</b>
	 * @param texCoords
	 *            Must provides texturing info for *all* points given or nothing (null)
	 * @param colors
	 *            Must provides coloring info for *all* points given or nothing (null)
	 * @param normals
	 *            Must provides normal info for *all* points given or nothing (null)
	 */
	public void renderDirectFromFloatBuffers(int verticesToDraw, FloatBuffer vertexCoords, FloatBuffer texCoords, FloatBuffer colors, FloatBuffer normals)
	{
		int vertexIn = renderingContext.currentShader().getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingContext.currentShader().getVertexAttributeLocation("texCoordIn");
		int colorIn = renderingContext.currentShader().getVertexAttributeLocation("colorIn");
		int normalIn = renderingContext.currentShader().getVertexAttributeLocation("normalIn");

		renderingContext.enableVertexAttribute(vertexIn);
		if (texCoordIn != -1)
			renderingContext.enableVertexAttribute(vertexIn);
		else
			renderingContext.disableVertexAttribute(vertexIn);

		if (colorIn != -1)
			renderingContext.enableVertexAttribute(colorIn);
		else
			renderingContext.disableVertexAttribute(colorIn);

		if (normalIn != -1)
			renderingContext.enableVertexAttribute(normalIn);
		else
			renderingContext.disableVertexAttribute(normalIn);

		//Enable vertex arrays (to be moved in setupVertexInputs)

		//Upload data
		glBindBuffer(GL_ARRAY_BUFFER, tempVBO[0]);
		glBufferData(GL_ARRAY_BUFFER, vertexCoords, GL_STREAM_DRAW);
		renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_FLOAT, false, 12, 0);
		if (texCoordIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[1]);
			glBufferData(GL_ARRAY_BUFFER, texCoords, GL_STREAM_DRAW);
			int dimensions = texCoords.capacity() / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(texCoordIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if (colorIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[2]);
			glBufferData(GL_ARRAY_BUFFER, colors, GL_STREAM_DRAW);
			int dimensions = colors.capacity() / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(colorIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if (normalIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[3]);
			glBufferData(GL_ARRAY_BUFFER, normals, GL_STREAM_DRAW);
			int dimensions = normals.capacity() / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(normalIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}

		GLCalls.drawArrays(GL_TRIANGLES, 0, verticesToDraw);
	}

	/**
	 * Renders some vertices using the currently bound shader and data provided by the main memory ( kinda slow, not proper for big things ) Only accepts 32bit SP floats The amount of points drawn is equal to vertexCoords.length / 3 The maximal size of any of the arguments arrays is 1 mebi floats (1024^2), that is 4mebioctels of raw bytes data <b>(silent fails else)</b>
	 * 
	 * @param vertexCoords
	 *            An array of floats, <b>it must be of a length multiple of 9</b> ( Triangles made up of points made up of 3 coordinates ) <b>or else it silent-fails</b>
	 * @param texCoords
	 *            Must provides texturing info for *all* points given or nothing (null)
	 * @param colors
	 *            Must provides coloring info for *all* points given or nothing (null)
	 * @param normals
	 *            Must provides normal info for *all* points given or nothing (null)
	 */
	public void renderDirect(float[] vertexCoords, float[] texCoords, float[] colors, float[] normals)
	{
		//Sanity check
		if (vertexCoords.length % 9 != 0 || vertexCoords.length == 0)
			return;

		//Parse inputs, grab vertex attribute locations
		int verticesToDraw = vertexCoords.length / 3;

		int vertexIn = renderingContext.currentShader().getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingContext.currentShader().getVertexAttributeLocation("texCoordIn");
		int colorIn = renderingContext.currentShader().getVertexAttributeLocation("colorIn");
		int normalIn = renderingContext.currentShader().getVertexAttributeLocation("normalIn");

		renderingContext.enableVertexAttribute(vertexIn);
		if (texCoordIn != -1)
			renderingContext.enableVertexAttribute(vertexIn);
		else
			renderingContext.disableVertexAttribute(vertexIn);

		if(colors != null)
		{
			if (colorIn != -1)
				renderingContext.enableVertexAttribute(colorIn);
			else
				renderingContext.disableVertexAttribute(colorIn);
		}

		if (normalIn != -1)
			renderingContext.enableVertexAttribute(normalIn);
		else
			renderingContext.disableVertexAttribute(normalIn);

		//Upload data
		glBindBuffer(GL_ARRAY_BUFFER, tempVBO[0]);
		tempBuffer.clear();
		tempBuffer.put(vertexCoords);
		tempBuffer.flip();
		glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
		renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_FLOAT, false, 12, 0);
		if (texCoordIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[1]);
			tempBuffer.clear();
			tempBuffer.put(texCoords);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = texCoords.length / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(texCoordIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if (colorIn != -1 && colors != null)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[2]);
			tempBuffer.clear();
			tempBuffer.put(colors);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = colors.length / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(colorIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if (normalIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[3]);
			tempBuffer.clear();
			tempBuffer.put(normals);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = normals.length / verticesToDraw;
			renderingContext.setVertexAttributePointerLocation(normalIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}

		GLCalls.drawArrays(GL_TRIANGLES, 0, verticesToDraw);
	}
}
