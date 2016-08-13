package io.xol.engine.graphics.util;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import io.xol.engine.math.lalgb.Vector4f;
import io.xol.engine.misc.ColorsTools;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GuiRenderer
{
	private RenderingContext renderingContext;
	
	public int MAX_ELEMENTS = 1024;
	public FloatBuffer buf;
	public int elementsToDraw = 0;
	public int currentTexture = -1;
	public boolean alphaBlending = false;
	public boolean useTexture = true;
	public Vector4f currentColor = new Vector4f(1f, 1f, 1f, 1f);

	// GL stuff
	int glVBO;
	ShaderProgram shader;

	public GuiRenderer(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
		// Buffer contains MAX_ELEMENTS of 2 triangles, each defined by 3
		// vertices, themselves defined by 4 floats : 'xy' positions, and
		// textures coords 'ts'.
		buf = BufferUtils.createFloatBuffer((2 + 2) * 3 * 2 * MAX_ELEMENTS);
		glVBO = glGenBuffers();
		shader = ShadersLibrary.getShaderProgram("gui");
	}

	public void drawBoxWindowsSpace(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		drawBox((startX / GameWindowOpenGL.windowWidth) * 2 - 1, (startY / GameWindowOpenGL.windowHeight) * 2 - 1, (endX / GameWindowOpenGL.windowWidth) * 2 - 1, (endY / GameWindowOpenGL.windowHeight) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, textureID, alpha, textured, color);
	}
	
	public void drawBoxWindowsSpaceWithSize(float startX, float startY, float width, float height, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		float endX = startX + width;
		float endY = startY + height;
		drawBox((startX / GameWindowOpenGL.windowWidth) * 2 - 1, (startY / GameWindowOpenGL.windowHeight) * 2 - 1, (endX / GameWindowOpenGL.windowWidth) * 2 - 1, (endY / GameWindowOpenGL.windowHeight) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, textureID, alpha, textured, color);
	}

	public void drawBox(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, int textureID, boolean alpha, boolean textured, Vector4f color)
	{
		//if (color == null)
		//	color = new Vector4f(1f, 1f, 1f, 1f);

		if (elementsToDraw >= 6 * 1024)
			drawBuffer();

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

	public void debugDraw()
	{
		setState(TexturesHandler.getTextureID("res/textures/logo.png"), false, true, new Vector4f(1f, 1f, 1f, 1f));

		addVertice(new float[] { -1, -1 }, new float[] { 0, 1 });
		addVertice(new float[] { -1, 1 }, new float[] { 0, 0 });
		addVertice(new float[] { 1, 1 }, new float[] { 1, 0 });

		addVertice(new float[] { -1, -1 }, new float[] { 0, 1 });
		addVertice(new float[] { 1, -1 }, new float[] { 1, 1 });
		addVertice(new float[] { 1, 1 }, new float[] { 1, 0 });
	}

	protected void addVertice(float vx, float vy, float t, float s)
	{
		buf.put(vx);
		buf.put(vy);
		buf.put(t);
		buf.put(s);
		elementsToDraw++;
	}
	
	protected void addVertice(float[] vertexIn, float[] texCoordIn)
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
	public void setState(int textureID, boolean alpha, boolean texture, Vector4f color)
	{
		//System.out.println("color: "+color + " currentColor: "+currentColor + "");
		
		//if (textureID != currentTexture || alpha != alphaBlending || useTexture != texture || color == null || !color.equals(currentColor))
		if (textureID != currentTexture || alpha != alphaBlending || useTexture != texture || (color == null && currentColor != null && !currentColor.equals(new Vector4f(1f, 1f, 1f, 1f))) || (color != null && !color.equals(currentColor)))
		{
			//System.out.println("color: "+color + " currentColor: "+currentColor + " == ?" + ((color == null && currentColor == null ) && (color != null && currentColor != null && color.equals(currentColor))) );
			drawBuffer();
		}
		currentTexture = textureID;
		alphaBlending = alpha;
		currentColor = color;
		useTexture = texture;
	}

	/**
	 * Draw the data in the buffer.
	 */
	public void drawBuffer()
	{
		if (elementsToDraw == 0)
			return;

		// Upload data and draw it.
		
		//System.out.println(this.currentTexture + " / " + elementsToDraw);
		//System.out.println("b:"+buf+" : "+buf.limit());
		buf.limit((2 + 2) * 3 * 2 * MAX_ELEMENTS);

		buf.flip();
		//System.out.println(buf+" : "+buf.limit());

		glBindBuffer(GL_ARRAY_BUFFER, glVBO);
		// glBufferData(GL_ARRAY_BUFFER, (2 + 2) * 3 * 2 * MAX_ELEMENTS,
		// GL_STREAM_DRAW);
		// glBufferSubData(GL_ARRAY_BUFFER, 0, buf);

		glBufferData(GL_ARRAY_BUFFER, buf, GL_STREAM_DRAW);

		buf.clear();
		renderingContext.setCurrentShader(shader);
		//shader.use(true);
		// Get attributes locations
		int vertexIn = shader.getVertexAttributeLocation("vertexIn");
		int texCoordIn = shader.getVertexAttributeLocation("texCoordIn");
		renderingContext.enableVertexAttribute(vertexIn);
		renderingContext.enableVertexAttribute(texCoordIn);
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

		renderingContext.setVertexAttributePointerLocation(vertexIn, 2, GL_FLOAT, false, 16, 0);
		renderingContext.setVertexAttributePointerLocation(texCoordIn, 2, GL_FLOAT, false, 16, 8);
		// System.out.println("Drawing"+elementsToDraw
		// +" vertexIn "+vertexIn+" texCoordIn "+texCoordIn+" vbo: "+glVBO);
		GLCalls.drawArrays(GL_TRIANGLES, 0, elementsToDraw);

		// Clean up
		renderingContext.disableVertexAttribute(vertexIn);
		renderingContext.disableVertexAttribute(texCoordIn);

		elementsToDraw = 0;
	}

	public void free()
	{
		glDeleteBuffers(glVBO);
		//shader.free();
	}
	
	//TODO remove completely
	
	public void renderTexturedRect(float xpos, float ypos, float w, float h, String tex)
	{
		renderTexturedRotatedRect(xpos, ypos, w, h, 0f, 0f, 0f, 1f, 1f, tex);
	}

	public void renderTexturedRectAlpha(float xpos, float ypos, float w, float h, String tex, float a)
	{
		renderTexturedRotatedRectAlpha(xpos, ypos, w, h, 0f, 0f, 0f, 1f, 1f, tex, a);
	}

	public void renderTexturedRect(float xpos, float ypos, float w, float h, float tcsx, float tcsy, float tcex, float tcey, float texSize, String tex)
	{
		renderTexturedRotatedRect(xpos, ypos, w, h, 0f, tcsx / texSize, tcsy / texSize, tcex / texSize, tcey / texSize, tex);
	}

	public void renderTexturedRotatedRect(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String tex)
	{
		renderTexturedRotatedRectAlpha(xpos, ypos, w, h, rot, tcsx, tcsy, tcex, tcey, tex, 1f);
	}

	public void renderTexturedRotatedRectAlpha(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String tex, float a)
	{
		renderTexturedRotatedRectRVBA(xpos, ypos, w, h, rot, tcsx, tcsy, tcex, tcey, tex, 1f, 1f, 1f, a);
	}

	public void renderTexturedRotatedRectRVBA(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String textureName, float r, float v, float b, float a)
	{

		if (textureName.startsWith("internal://"))
			textureName = textureName.substring("internal://".length());
		else if (textureName.startsWith("gameDir://"))
			textureName = textureName.substring("gameDir://".length());//GameDirectory.getGameFolderPath() + "/" + tex.substring("gameDir://".length());
		else if (textureName.contains("../"))
			textureName = ("./" + textureName.replace("../", "") + ".png");
		else
			textureName = ("./res/textures/" + textureName + ".png");

		Texture2D texture = TexturesHandler.getTexture(textureName);
		
		texture.setLinearFiltering(false);
		//TexturesHandler.mipmapLevel(texture, -1);

		drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, tcsx, tcsy, tcex, tcey, texture.getId(), false, true, new Vector4f(r, v, b, a));
	}

	public void renderColoredRect(float xpos, float ypos, float w, float h, float rot, String hex, float a)
	{
		int rgb[] = ColorsTools.hexToRGB(hex);
		renderColoredRect(xpos, ypos, w, h, rot, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, a);
	}

	public void renderColoredRect(float xpos, float ypos, float w, float h, float rot, float r, float v, float b, float a)
	{
		drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, 0, 0, 0, 0, 0, false, true, new Vector4f(r, v, b, a));
	}
}
