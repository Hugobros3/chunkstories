package io.xol.chunkstories.renderer.debug;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import io.xol.engine.math.lalgb.Vector4f;

import static org.lwjgl.opengl.GL11.glDrawArrays;

import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OverlayRenderer
{
	//Emulates legacy OpenGL 1.x pipeline for debug functions

	public static int GL_TEXTURE_2D, GL_BLEND, GL_CULL_FACE;

	static Camera camera;

	/**
	 * This class requires knowledge of the camera object
	 * @param camera
	 */
	public static void setCamera(Camera camera)
	{
		OverlayRenderer.camera = camera;
	}

	public static void glEnable(int cap)
	{

	}

	public static void glDisable(int cap)
	{

	}

	public static void glLineWidth(float lineWidth)
	{
		GL11.glLineWidth(lineWidth);
	}

	public static void glColor4f(float r, float g, float b, float a)
	{
		color = new Vector4f(r, g, b, a);
	}

	public static int GL_LINES = GL11.GL_LINES;
	public static int GL_TRIANGLES = GL11.GL_TRIANGLES;

	public static void glBegin(int mode)
	{
		OverlayRenderer.mode = mode;
	}

	public static void glVertex3d(double x, double y, double z)
	{
		glVertex3f((float) x, (float) y, (float) z);
	}

	public static void glVertex3f(float x, float y, float z)
	{
		//if(data.position() == data.limit())
		//	return;
		data.put(x);
		data.put(y);
		data.put(z);
		size++;
	}

	static Vector4f color = new Vector4f(1f, 1f, 1f, 1f);
	static FloatBuffer data = BufferUtils.createFloatBuffer(3 * 1000);
	static int size = 0;
	static int mode = 0;

	public static void glEnd()
	{
		//System.out.println("ntm");
		glDisable(GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		ShaderProgram overlayProgram = ShadersLibrary.getShaderProgram("overlay");
		XolioWindow.getInstance().getRenderingContext().setCurrentShader(overlayProgram);
		//overlayProgram.use(true);
		camera.setupShader(overlayProgram);
		int vertexIn = overlayProgram.getVertexAttributeLocation("vertexIn");
		XolioWindow.getInstance().renderingContext.enableVertexAttribute(vertexIn);
		overlayProgram.setUniformFloat4("colorIn", color);
		//glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0);
		data.flip();
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glVertexAttribPointer(vertexIn, 3, false, 0, data);
		glDrawArrays(mode, 0, size);
		XolioWindow.getInstance().renderingContext.disableVertexAttribute(vertexIn);
		GL11.glDisable(GL11.GL_BLEND);
		data.clear();
		size = 0;
	}
}
