package io.xol.engine.math;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.renderer.Camera;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

public class GeometryHelper
{

	// CREDIT: teletubo on java-gaming.org for original code
	// Cleaned it up and added some code to get a normalized vector

	static IntBuffer viewport = BufferUtils.createIntBuffer(16);
	static FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
	static FloatBuffer projection = BufferUtils.createFloatBuffer(16);
	static FloatBuffer winZ = BufferUtils.createFloatBuffer(20);
	static FloatBuffer position = BufferUtils.createFloatBuffer(3);

	static public Vector3f getMousePositionIn3dCoords(int mouseX, int mouseY)
	{
		viewport.clear();
		modelview.clear();
		projection.clear();
		winZ.clear();
		position.clear();
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
		float winX = (float) mouseX;
		float winY = (float) mouseY;
		GL11.glReadPixels(mouseX, (int) winY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, winZ);
		float zz = winZ.get();
		GLU.gluUnProject(winX, winY, zz, modelview, projection, viewport, position);
		Vector3f v = new Vector3f(position.get(0), position.get(1), position.get(2));
		return v;
	}

	// my part
	static public Vector3f getVectorMouseIn3d(int mx, int my, Camera cam)
	{
		Vector3f position = getMousePositionIn3dCoords(mx, my);
		// System.out.println("position : "+position.x+":"+position.y+":"+position.z);
		Vector3f camera = new Vector3f((float) -cam.camPosX, (float) -cam.camPosY, (float) -cam.camPosZ);
		// System.out.println("camera : "+camera.x+":"+camera.y+":"+camera.z);
		// position.normalise();
		// camera.normalise();
		Vector3f v = new Vector3f(0, 0, 0);
		Vector3f.sub(position, camera, v);
		v.normalise();
		// System.out.println("vec : "+v.x+":"+v.y+":"+v.z);
		return v;
	}

}
