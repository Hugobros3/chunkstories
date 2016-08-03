package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.util.GuiRenderer;
import io.xol.engine.graphics.util.TrueTypeFontRenderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext
{
	private GameWindowOpenGL mainWindows;
	private ShaderProgram currentlyBoundShader = null;

	private Camera camera;
	private boolean isThisAShadowPass;

	private Set<Integer> enabledAttributes = new HashSet<Integer>();

	private Set<Light> lights = new HashSet<Light>();

	private DirectRenderer directRenderer;
	private GuiRenderer guiRenderer;
	private TrueTypeFontRenderer trueTypeFontRenderer;

	public RenderingContext(GameWindowOpenGL windows)
	{
		mainWindows = windows;
		directRenderer = new DirectRenderer(this);
		guiRenderer = new GuiRenderer(this);
		trueTypeFontRenderer = new TrueTypeFontRenderer(this);
	}

	public String toString()
	{
		String attributes = "";
		for(int i : enabledAttributes)
		{
			attributes += i;
		}
		attributes += " ("+enabledAttributes.size()+")";
		return "[RenderingContext shadow:"+isThisAShadowPass+" enabledAttributes: "+attributes+" lights: "+lights.size()+" shader:"+getCurrentShader()+" ]";
	}

	public void setCamera(Camera camera)
	{
		this.camera = camera;
	}

	public Camera getCamera()
	{
		return camera;
	}

	public boolean isThisAShadowPass()
	{
		return isThisAShadowPass;
	}

	public void setIsShadowPass(boolean isShadowPass)
	{
		isThisAShadowPass = isShadowPass;
	}

	public ShaderProgram getCurrentShader()
	{
		return currentlyBoundShader;
	}

	public void addLight(Light light)
	{
		if (!this.isThisAShadowPass)
			lights.add(light);
	}

	public Iterator<Light> getAllLights()
	{
		return lights.iterator();
	}

	public DirectRenderer getDirectRenderer()
	{
		return directRenderer;
	}

	public GuiRenderer getGuiRenderer()
	{
		return guiRenderer;
	}
	
	public TrueTypeFontRenderer getTrueTypeFontRenderer()
	{
		return trueTypeFontRenderer;
	}

	/**
	 * Enables if not already the vertex attribute at the said location. Reset uppon shader switch.
	 * 
	 * @param vertexAttributeLocation
	 */
	public void enableVertexAttribute(int vertexAttributeLocation)
	{
		if (vertexAttributeLocation < 0)
			return;
		if (!enabledAttributes.contains(vertexAttributeLocation))
		{
			glEnableVertexAttribArray(vertexAttributeLocation);
			enabledAttributes.add(vertexAttributeLocation);
		}
	}

	/**
	 * Disables if not already the vertex attribute at the said location. Reset uppon shader switch.
	 * 
	 * @param vertexAttributeLocation
	 */
	public void disableVertexAttribute(int vertexAttributeLocation)
	{
		if (vertexAttributeLocation < 0)
			return;
		if (enabledAttributes.contains(vertexAttributeLocation))
		{
			glDisableVertexAttribArray(vertexAttributeLocation);
			enabledAttributes.remove(vertexAttributeLocation);
		}
	}

	public void enableVertexAttribute(String vertexAttributeName)
	{
		enableVertexAttribute(this.getCurrentShader().getVertexAttributeLocation(vertexAttributeName));
	}

	public void disableVertexAttribute(String vertexAttributeName)
	{
		disableVertexAttribute(this.getCurrentShader().getVertexAttributeLocation(vertexAttributeName));
	}

	public void setVertexAttributePointer(String vertexAttributeName, int dimensions, int vertexType, boolean normalized, int stride, int offset)
	{
		setVertexAttributePointer(this.getCurrentShader().getVertexAttributeLocation(vertexAttributeName), dimensions, vertexType, normalized, stride, offset);
	}

	/**
	 * If the said attribute is enabled, tells openGL where to lookup data for it within the bind buffer
	 * 
	 * @param vertexAttributeLocation
	 * @param dimensions
	 * @param vertexType
	 * @param normalized
	 * @param stride
	 * @param offset
	 */
	public void setVertexAttributePointer(int vertexAttributeLocation, int dimensions, int vertexType, boolean normalized, int stride, int offset)
	{
		if (enabledAttributes.contains(vertexAttributeLocation))
		{
			glVertexAttribPointer(vertexAttributeLocation, dimensions, vertexType, normalized, stride, offset);
		}
	}

	/**
	 * Resets the vertex attributes enabled (disables all)
	 */
	public void clearVertexAttributes()
	{
		Iterator<Integer> i = enabledAttributes.iterator();
		while (i.hasNext())
		{
			int vertexAttributeLocation = i.next();
			glDisableVertexAttribArray(vertexAttributeLocation);
			i.remove();
		}
	}

	public void setCurrentShader(ShaderProgram shaderProgram)
	{
		//Save calls
		if (shaderProgram != currentlyBoundShader)
		{
			//When changing shaders, we make sure we disable whatever was enabled
			clearVertexAttributes();
			shaderProgram.use();
		}
		currentlyBoundShader = shaderProgram;
	}

	public void setDiffuseTexture(int id)
	{
		if (currentlyBoundShader != null)
			currentlyBoundShader.setUniformSampler(0, "diffuseTexture", id);
	}

	public void setNormalTexture(int id)
	{
		if (currentlyBoundShader != null)
			currentlyBoundShader.setUniformSampler(1, "normalTexture", id);
	}

	public void setMaterialTexture(int id)
	{
		if (currentlyBoundShader != null)
			currentlyBoundShader.setUniformSampler(2, "materialTexture", id);
	}

	Matrix4f temp = new Matrix4f();
	Matrix3f normal = new Matrix3f();

	/**
	 * Sets the current local matrix transformation and normal 3x3 counterpart
	 * 
	 * @param matrix
	 */
	public void sendTransformationMatrix(Matrix4f matrix)
	{
		if (matrix == null)
			matrix = new Matrix4f();
		this.currentlyBoundShader.setUniformMatrix4f("localTransform", matrix);
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
		this.currentlyBoundShader.setUniformMatrix3f("localTransformNormal", normal);
	}

	/**
	 * Sets the current bone matrix transformation and normal 3x3 counterpart
	 * 
	 * @param matrix
	 */
	public void sendBoneTransformationMatrix(Matrix4f matrix)
	{
		if (matrix == null)
			matrix = new Matrix4f();
		this.currentlyBoundShader.setUniformMatrix4f("boneTransform", matrix);

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
		this.currentlyBoundShader.setUniformMatrix3f("boneTransformNormal", normal);
	}
	
	static FloatBuffer fsQuadBuffer = null;

	public void drawFSQuad(int vertexAttribLocation)
	{
		if (vertexAttribLocation < 0)
			return;
		fsQuadBuffer = null;
		if (fsQuadBuffer == null)
		{
			fsQuadBuffer = BufferUtils.createFloatBuffer(6 * 2);
			fsQuadBuffer.put(new float[] { 1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f });
		}
		fsQuadBuffer.flip();
		enableVertexAttribute(vertexAttribLocation);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glVertexAttribPointer(vertexAttribLocation, 2, false, 0, fsQuadBuffer);
		GLCalls.drawArrays(GL_TRIANGLES, 0, 6);

		disableVertexAttribute(vertexAttribLocation);
		//buffer = null;
	}
}