//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.debug;

import java.nio.FloatBuffer;

import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexBufferGL;

public class FakeImmediateModeDebugRenderer
{
	//Emulates legacy OpenGL 1.x pipeline for debug functions

	public static int GL_TEXTURE_2D, GL_BLEND, GL_CULL_FACE;

	static CameraInterface camera;

	/**
	 * This class requires knowledge of the camera object
	 * @param camera
	 */
	public static void setCamera(CameraInterface camera)
	{
		FakeImmediateModeDebugRenderer.camera = camera;
	}

	public static void glEnable(int cap)
	{

	}

	public static void glDisable(int cap)
	{

	}

	public static void glColor4f(float r, float g, float b, float a)
	{
		color = new Vector4f(r, g, b, a);
	}

	public static int GL_LINES = GL11.GL_LINES;
	public static int GL_TRIANGLES = GL11.GL_TRIANGLES;

	public static void glBegin(int mode)
	{
		FakeImmediateModeDebugRenderer.mode = mode;
	}

	public static void glVertex3d(double x, double y, double z)
	{
		glVertex3f((float) x, (float) y, (float) z);
	}

	public static void glVertex3f(float x, float y, float z)
	{
		if(data.position() == data.limit())
			glEnd();
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

	static VertexBufferGL vertexBuffer = new VertexBufferGL();
	
	public static void glEnd()
	{
		RenderingContext renderingContext = Client.getInstance().getGameWindow().getRenderingContext();
		
		renderingContext.setCullingMode(CullingMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		Shader overlayProgram = renderingContext.useShader("overlay");//ShadersLibrary.getShaderProgram("overlay");
		camera.setupShader(overlayProgram);
		overlayProgram.setUniform4f("colorIn", color);
		
		data.flip();
		
		vertexBuffer.uploadData(data);
		
		renderingContext.bindAttribute("vertexIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 3));
		//renderingContext.bindAttribute("vertexIn", new FloatBufferAttributeSource(data, 3));
		
		renderingContext.draw(mode == GL_TRIANGLES ? Primitive.TRIANGLE : Primitive.LINE, 0, size);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		data.clear();
		size = 0;
	}
	
	static VertexBuffer cube = null;
	public static VertexBuffer getCube()
	{
		if(cube == null)
		{
			cube = new VertexBufferGL();
			float[] cubeData = new float[]{
					//Base face
					0.0f, 0.0f, 0.0f,
					0.0f, 1.0f, 0.0f,
					0.0f, 0.0f, 0.0f,
					0.0f, 0.0f, 1.0f,
					0.0f, 1.0f, 0.0f,
					0.0f, 1.0f, 1.0f,
					0.0f, 0.0f, 1.0f,
					0.0f, 1.0f, 1.0f,
					//Top face
					1.0f, 0.0f, 0.0f,
					1.0f, 1.0f, 0.0f,
					1.0f, 0.0f, 0.0f,
					1.0f, 0.0f, 1.0f,
					1.0f, 1.0f, 0.0f,
					1.0f, 1.0f, 1.0f,
					1.0f, 0.0f, 1.0f,
					1.0f, 1.0f, 1.0f,
					//Vertical segments
					0.0f, 0.0f, 0.0f,
					1.0f, 0.0f, 0.0f,
					0.0f, 0.0f, 1.0f,
					1.0f, 0.0f, 1.0f,
					0.0f, 1.0f, 0.0f,
					1.0f, 1.0f, 0.0f,
					0.0f, 1.0f, 1.0f,
					1.0f, 1.0f, 1.0f,
			};
			FloatBuffer dataToUpload = BufferUtils.createFloatBuffer(cubeData.length);
			dataToUpload.put(cubeData);
			dataToUpload.flip();
			cube.uploadData(dataToUpload);
		}
		return cube;
	}

	private static final Vector4f defaultColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
	
	public static void renderCollisionBox(CollisionBox box, Vector4f color) {
		if(color == null)
			color = defaultColor;
		
		glColor4f(color.x(), color.y(), color.z(), color.w());
		glDisable(GL_CULL_FACE);
		
		glBegin(GL_LINES);
		glVertex3d(box.xpos , box.ypos, box.zpos);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos);
		glVertex3d(box.xpos , box.ypos, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos);
		glVertex3d(box.xpos , box.ypos, box.zpos);
		glVertex3d(box.xpos , box.ypos, box.zpos + box.zw);

		glVertex3d(box.xpos , box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos , box.ypos + box.h, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos , box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos , box.ypos + box.h, box.zpos + box.zw);

		glVertex3d(box.xpos , box.ypos, box.zpos);
		glVertex3d(box.xpos , box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos , box.ypos, box.zpos + box.zw);
		glVertex3d(box.xpos , box.ypos + box.h, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos);
		glVertex3d(box.xpos + box.xw, box.ypos, box.zpos + box.zw);
		glVertex3d(box.xpos + box.xw, box.ypos + box.h, box.zpos + box.zw);
		glEnd();
	}
}
