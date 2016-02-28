package io.xol.engine.gui;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.TexturesHandler;

import java.nio.FloatBuffer;



import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class GuiDrawer
{
	public static int MAX_ELEMENTS = 1024;
	public static FloatBuffer buf;
	public static int elementsToDraw = 0;
	public static int currentTexture = -1;
	public static boolean alphaBlending = false;
	public static boolean useTexture = true;
	public static Vector4f currentColor = new Vector4f(1f, 1f, 1f, 1f);

	// GL stuff
	static int glVBO;
	static ShaderProgram shader;

	public static void initGL()
	{
		// Buffer contains MAX_ELEMENTS of 2 triangles, each defined by 3
		// vertices, themselves defined by 4 floats : 'xy' positions, and
		// textures coords 'ts'.
		buf = BufferUtils.createFloatBuffer((2 + 2) * 3 * 2 * MAX_ELEMENTS);
		glVBO = glGenBuffers();
		shader = ShadersLibrary.getShaderProgram("gui");
	}

	public static void drawBoxWindowsSpace(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		drawBox((startX / XolioWindow.frameW) * 2 - 1, (startY / XolioWindow.frameH) * 2 - 1, (endX / XolioWindow.frameW) * 2 - 1, (endY / XolioWindow.frameH) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, textureID, alpha, textured, color);
	}
	
	public static void drawBoxWindowsSpaceWithSize(float startX, float startY, float width, float height, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		float endX = startX + width;
		float endY = startY + height;
		drawBox((startX / XolioWindow.frameW) * 2 - 1, (startY / XolioWindow.frameH) * 2 - 1, (endX / XolioWindow.frameW) * 2 - 1, (endY / XolioWindow.frameH) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, textureID, alpha, textured, color);
	}

	public static void drawBox(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		//if (color == null)
		//	color = new Vector4f(1f, 1f, 1f, 1f);

		if (elementsToDraw >= 6 * 1024)
		{
			
			//System.out.println("Elements out of bounds : "+elementsToDraw);
			drawBuffer();
		}

		if (color != null && color.w < 1)
			alpha = true; // Force blending if alpha < 1

		if(textureID != -1)
			setState(textureID, alpha, true,  color);

		addVertice(startX, startY, textureStartX, textureStartY );
		addVertice(startX, endY, textureStartX, textureEndY );
		addVertice(endX, endY , textureEndX, textureEndY );

		addVertice(startX, startY , textureStartX, textureStartY );
		addVertice(endX, startY, textureEndX, textureStartY );
		addVertice(endX, endY , textureEndX, textureEndY );

	}

	public static void debugDraw()
	{
		setState(TexturesHandler.getTextureID("res/textures/logo.png"), false, true, new Vector4f(1f, 1f, 1f, 1f));

		addVertice(new float[] { -1, -1 }, new float[] { 0, 1 });
		addVertice(new float[] { -1, 1 }, new float[] { 0, 0 });
		addVertice(new float[] { 1, 1 }, new float[] { 1, 0 });

		addVertice(new float[] { -1, -1 }, new float[] { 0, 1 });
		addVertice(new float[] { 1, -1 }, new float[] { 1, 1 });
		addVertice(new float[] { 1, 1 }, new float[] { 1, 0 });
	}

	protected static void addVertice(float vx, float vy, float t, float s)
	{
		buf.put(vx);
		buf.put(vy);
		buf.put(t);
		buf.put(s);
		elementsToDraw++;
	}
	
	protected static void addVertice(float[] vertexIn, float[] texCoordIn)
	{
		buf.put(vertexIn);
		buf.put(texCoordIn);
		elementsToDraw++;
	}

	/**
	 * Called before adding anything to the drawBuffer, if it's the same kind as
	 * before we keep filling it, if not we empty it first by drawing the
	 * current buffer.
	 */
	public static void setState(int textureID, boolean alpha, boolean texture, Vector4f color)
	{
		if (textureID != currentTexture || alpha != alphaBlending || useTexture != texture || color == null || !color.equals(currentColor))
			drawBuffer();
		currentTexture = textureID;
		alphaBlending = alpha;
		currentColor = color;
		useTexture = texture;
	}

	/**
	 * Draw the data in the buffer.
	 */
	public static void drawBuffer()
	{
		if (elementsToDraw == 0)
			return;

		// Upload data and draw it.
		buf.limit((2 + 2) * 3 * 2 * MAX_ELEMENTS);

		buf.flip();
		// System.out.println(buf.get(0)+" : "+buf.limit());

		glBindBuffer(GL_ARRAY_BUFFER, glVBO);
		// glBufferData(GL_ARRAY_BUFFER, (2 + 2) * 3 * 2 * MAX_ELEMENTS,
		// GL_STREAM_DRAW);
		// glBufferSubData(GL_ARRAY_BUFFER, 0, buf);

		glBufferData(GL_ARRAY_BUFFER, buf, GL_STREAM_DRAW);

		buf.clear();
		XolioWindow.getInstance().getRenderingContext().setCurrentShader(shader);
		//shader.use(true);
		// Get attributes locations
		int vertexIn = shader.getVertexAttributeLocation("vertexIn");
		int texCoordIn = shader.getVertexAttributeLocation("texCoordIn");
		glEnableVertexAttribArray(vertexIn);
		glEnableVertexAttribArray(texCoordIn);
		shader.setUniformFloat("useTexture", useTexture ? 1f : 0f);
		if(currentColor != null)
			shader.setUniformFloat4("color", currentColor);
		else
			shader.setUniformFloat4("color", 1f, 1f, 1f, 1f);
		shader.setUniformSampler(0, "sampler", currentTexture);
		glDisable(GL_DEPTH_TEST);
		if (alphaBlending)
		{
			glEnable(GL_BLEND);
		}
		else
		{
			glDisable(GL_BLEND);
			glEnable(GL_ALPHA_TEST);
			glAlphaFunc(GL_GREATER, 0.1f);
		}
		glEnable(GL_BLEND);
		glDisable(GL_CULL_FACE);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		//glBlendEquation(GL_FUNC_ADD);

		glVertexAttribPointer(vertexIn, 2, GL_FLOAT, false, 16, 0);
		glVertexAttribPointer(texCoordIn, 2, GL_FLOAT, false, 16, 8);
		// System.out.println("Drawing"+elementsToDraw
		// +" vertexIn "+vertexIn+" texCoordIn "+texCoordIn+" vbo: "+glVBO);
		glDrawArrays(GL_TRIANGLES, 0, elementsToDraw);

		// Clean up
		glDisableVertexAttribArray(vertexIn);
		glDisableVertexAttribArray(texCoordIn);
		//shader.use(false);

		elementsToDraw = 0;
	}

	public static void free()
	{
		glDeleteBuffers(glVBO);
		//shader.free();
	}
}
