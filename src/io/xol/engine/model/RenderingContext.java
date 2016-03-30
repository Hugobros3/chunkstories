package io.xol.engine.model;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext
{
	XolioWindow engine;
	private ShaderProgram renderingShader = null;
	private Camera camera;

	//private boolean verticesAttribMode = false;
	//public int vertexIn, texCoordIn, colorIn, normalIn;
	
	public boolean shadow;

	Set<Integer> enabledAttributes = new HashSet<Integer>();
	// 4 Temporary VBOs for streamed rendering
	public int tempVBO[] = new int[4];

	public Set<Light> lights = new HashSet<Light>();
	
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

	/**
	 * Enables if not already the vertex attribute at the said location.
	 * Reset uppon shader switch.
	 * @param vertexAttributeLocation
	 */
	public void enableVertexAttribute(int vertexAttributeLocation)
	{
		if(!enabledAttributes.contains(vertexAttributeLocation))
		{
			glEnableVertexAttribArray(vertexAttributeLocation);
			enabledAttributes.add(vertexAttributeLocation);
		}
	}
	
	/**
	 * Disables if not already the vertex attribute at the said location.
	 * Reset uppon shader switch.
	 * @param vertexAttributeLocation
	 */
	public void disableVertexAttribute(int vertexAttributeLocation)
	{
		if(enabledAttributes.contains(vertexAttributeLocation))
		{
			glDisableVertexAttribArray(vertexAttributeLocation);
			enabledAttributes.remove(vertexAttributeLocation);
		}
	}

	public void enableVertexAttribute(String string)
	{
		enableVertexAttribute(this.getCurrentShader().getVertexAttributeLocation(string));
	}
	
	public void disableVertexAttribute(String string)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Resets the vertex attributes enabled (disables all)
	 */
	public void clearVertexAttributes()
	{
		Iterator<Integer> i = enabledAttributes.iterator();
		while(i.hasNext())
		{
			int vertexAttributeLocation = i.next();
			glDisableVertexAttribArray(vertexAttributeLocation);
			i.remove();
		}
	}

	//4Megs scratch buffer
	FloatBuffer tempBuffer = BufferUtils.createFloatBuffer(1024 * 1024);
	
	/**
	 * Renders some vertices using the currently bound shader and data provided by the main memory ( kinda slow, not proper for big things )
	 * Only accepts 32bit SP floats
	 * @param vertexCoords An array of floats, <b>it must be of a length multiple of 9</b> ( Triangles made up of points made up of 3 coordinates ) <b>or else it silent-fails</b>
	 * @param texCoords Must provides texturing info for *all* points given or nothing (null)
	 * @param colors Must provides coloring info for *all* points given or nothing (null)
	 * @param normals Must provides normal info for *all* points given or nothing (null) 
	 */
	public void renderDirectFromFloatBuffers(int verticesToDraw, FloatBuffer vertexCoords, FloatBuffer texCoords, FloatBuffer colors, FloatBuffer normals)
	{
		int vertexIn = renderingShader.getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingShader.getVertexAttributeLocation("texCoordIn");
		int colorIn = renderingShader.getVertexAttributeLocation("colorIn");
		int normalIn = renderingShader.getVertexAttributeLocation("normalIn");
		
		enableVertexAttribute(vertexIn);
		if(texCoordIn != -1)
			enableVertexAttribute(vertexIn);
		else
			disableVertexAttribute(vertexIn);
		
		if(colorIn != -1)
			enableVertexAttribute(colorIn);
		else
			disableVertexAttribute(colorIn);
		
		if(normalIn != -1)
			enableVertexAttribute(normalIn);
		else
			disableVertexAttribute(normalIn);
		
		//Enable vertex arrays (to be moved in setupVertexInputs)
		
		//Upload data
		glBindBuffer(GL_ARRAY_BUFFER, tempVBO[0]);
		glBufferData(GL_ARRAY_BUFFER, vertexCoords, GL_STREAM_DRAW);
		glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 12, 0);
		if(texCoordIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[1]);
			glBufferData(GL_ARRAY_BUFFER, texCoords, GL_STREAM_DRAW);
			int dimensions = texCoords.capacity() / verticesToDraw;
			glVertexAttribPointer(texCoordIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(colorIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[2]);
			glBufferData(GL_ARRAY_BUFFER, colors, GL_STREAM_DRAW);
			int dimensions = colors.capacity() / verticesToDraw;
			glVertexAttribPointer(colorIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(normalIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[3]);
			glBufferData(GL_ARRAY_BUFFER, normals, GL_STREAM_DRAW);
			int dimensions = normals.capacity() / verticesToDraw;
			glVertexAttribPointer(normalIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		
		glDrawArrays(GL_TRIANGLES, 0, verticesToDraw);
	}
	
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
		int verticesToDraw = vertexCoords.length / 3;
		
		int vertexIn = renderingShader.getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingShader.getVertexAttributeLocation("texCoordIn");
		int colorIn = renderingShader.getVertexAttributeLocation("colorIn");
		int normalIn = renderingShader.getVertexAttributeLocation("normalIn");
		
		enableVertexAttribute(vertexIn);
		if(texCoordIn != -1)
			enableVertexAttribute(vertexIn);
		else
			disableVertexAttribute(vertexIn);
		
		if(colorIn != -1)
			enableVertexAttribute(colorIn);
		else
			disableVertexAttribute(colorIn);
		
		if(normalIn != -1)
			enableVertexAttribute(normalIn);
		else
			disableVertexAttribute(normalIn);
		
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
			int dimensions = texCoords.length / verticesToDraw;
			glVertexAttribPointer(texCoordIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(colorIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[2]);
			tempBuffer.clear();
			tempBuffer.put(colors);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = colors.length / verticesToDraw;
			glVertexAttribPointer(colorIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		if(normalIn != -1)
		{
			glBindBuffer(GL_ARRAY_BUFFER, tempVBO[3]);
			tempBuffer.clear();
			tempBuffer.put(normals);
			tempBuffer.flip();
			glBufferData(GL_ARRAY_BUFFER, tempBuffer, GL_STREAM_DRAW);
			int dimensions = normals.length / verticesToDraw;
			glVertexAttribPointer(normalIn, dimensions, GL_FLOAT, false, 4 * dimensions, 0);
		}
		
		glDrawArrays(GL_TRIANGLES, 0, verticesToDraw);
	}

	public void setCurrentShader(ShaderProgram shaderProgram)
	{
		if (shaderProgram != renderingShader)
		{
			//When changing shaders, we make sure we disable whatever was enabled
			clearVertexAttributes();
			shaderProgram.use();
		}
		//else
		//	System.out.println("Prevented useless shader switch : "+s);
		renderingShader = shaderProgram;
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

	Matrix4f temp = new Matrix4f();
	Matrix3f normal = new Matrix3f();
	/**
	 * Sets the current local matrix transformation and normal 3x3 counterpart
	 * @param matrix
	 */
	public void sendTransformationMatrix(Matrix4f matrix)
	{
		if(matrix == null)
			matrix = new Matrix4f();
		this.renderingShader.setUniformMatrix4f("localTransform", matrix);
		Matrix4f.invert(matrix, temp);
		Matrix4f.transpose(temp, temp);
		normal.m00 = temp.m00;
		normal.m01 = temp.m01;
		normal.m02 = temp.m02;

		normal.m10 = temp.m10;
		normal.m11 = temp.m11;
		normal.m12 = temp.m12;

		normal.m20 = temp.m20;
		normal.m21 = temp.m21;
		normal.m22 = temp.m22;
		this.renderingShader.setUniformMatrix3f("localTransformNormal", normal);
	}
	
	/**
	 * Sets the current bone matrix transformation and normal 3x3 counterpart
	 * @param matrix
	 */
	public void sendBoneTransformationMatrix(Matrix4f matrix)
	{
		if(matrix == null)
			matrix = new Matrix4f();
		this.renderingShader.setUniformMatrix4f("boneTransform", matrix);
		Matrix4f.invert(matrix, temp);
		Matrix4f.transpose(temp, temp);
		normal.m00 = temp.m00;
		normal.m01 = temp.m01;
		normal.m02 = temp.m02;

		normal.m10 = temp.m10;
		normal.m11 = temp.m11;
		normal.m12 = temp.m12;

		normal.m20 = temp.m20;
		normal.m21 = temp.m21;
		normal.m22 = temp.m22;
		this.renderingShader.setUniformMatrix3f("boneTransformNormal", normal);
	}

	public ShaderProgram getCurrentShader()
	{
		return renderingShader;
	}
}