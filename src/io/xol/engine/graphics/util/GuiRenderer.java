package io.xol.engine.graphics.util;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.geometry.VerticesObject.UploadRegime;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.lalgb.Vector4f;
import io.xol.engine.misc.ColorsTools;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GuiRenderer
{
	private RenderingContext renderingContext;
	
	public int MAX_ELEMENTS = 1024;
	public ByteBuffer buf;
	public int elementsToDraw = 0;
	public Texture2D currentTexture;
	public boolean alphaBlending = false;
	public boolean useTexture = true;
	public Vector4f currentColor = new Vector4f(1f, 1f, 1f, 1f);

	// GL stuff
	VerticesObject guiDrawData = new VerticesObject(UploadRegime.FAST);
	
	public GuiRenderer(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
		// Buffer contains MAX_ELEMENTS of 2 triangles, each defined by 3
		// vertices, themselves defined by 4 floats and 4 bytes : 'xy' positions, and
		// textures coords 'ts'.
		buf = BufferUtils.createByteBuffer((4 * 1 + (2 + 2) * 4) * 3 * 2 * MAX_ELEMENTS);
	}

	public void drawBoxWindowsSpace(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4f color)
	{
		drawBox((startX / GameWindowOpenGL.windowWidth) * 2 - 1, (startY / GameWindowOpenGL.windowHeight) * 2 - 1, (endX / GameWindowOpenGL.windowWidth) * 2 - 1, (endY / GameWindowOpenGL.windowHeight) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, texture, alpha, textured, color);
	}
	
	public void drawBoxWindowsSpaceWithSize(float startX, float startY, float width, float height, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4f color)
	{
		float endX = startX + width;
		float endY = startY + height;
		drawBox((startX / GameWindowOpenGL.windowWidth) * 2 - 1, (startY / GameWindowOpenGL.windowHeight) * 2 - 1, (endX / GameWindowOpenGL.windowWidth) * 2 - 1, (endY / GameWindowOpenGL.windowHeight) * 2 - 1, textureStartX, textureStartY, textureEndX, textureEndY, texture, alpha, textured, color);
	}

	public void drawBox(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4f color)
	{
		//Maximum buffer size was reached, in clear the number of vertices in the buffer = 6 * max elements, max elements being the max amount of drawBox calls until drawBuffer()
		if (elementsToDraw >= 6 * MAX_ELEMENTS)
			drawBuffer();
		
		if (color != null && color.w < 1)
		{
			alpha = true; // Force blending if alpha < 1
		}

		setState(texture, alpha, texture != null,  color);

		addVertice(startX, startY, textureStartX, textureStartY );
		addVertice(startX, endY, textureStartX, textureEndY );
		addVertice(endX, endY , textureEndX, textureEndY );

		addVertice(startX, startY , textureStartX, textureStartY );
		addVertice(endX, startY, textureEndX, textureStartY );
		addVertice(endX, endY , textureEndX, textureEndY );

	}

	protected void addVertice(float vx, float vy, float t, float s)
	{
		// 2x4 bytes of float vertex position
		buf.putFloat(vx);
		buf.putFloat(vy);
		// 2x4 bytes of float texture coords
		buf.putFloat(t);
		buf.putFloat(s);
		// 1x4 bytes of ubyte norm color data
		buf.put((byte)(int)(currentColor.x * 255));
		buf.put((byte)(int)(currentColor.y * 255));
		buf.put((byte)(int)(currentColor.z * 255));
		buf.put((byte)(int)(currentColor.w * 255));
		elementsToDraw++;
	}
	
	/**
	 * Called before adding anything to the drawBuffer, if it's the same kind as
	 * before we keep filling it, if not we empty it first by drawing the
	 * current buffer.
	 */
	public void setState(Texture2D texture, boolean alpha, boolean textureEnabled, Vector4f color)
	{
		if(color == null)
			color = new Vector4f(1.0F);
		
		//Only texture changes trigger a buffer flush now
		if (texture != currentTexture || 
				useTexture != textureEnabled )
		{
			drawBuffer();
		}
		
		currentTexture = texture;
		alphaBlending = alpha;
		currentColor = color;
		useTexture = textureEnabled;
	}
	
	/**
	 * Draw the data in the buffer.
	 */
	public void drawBuffer()
	{
		if (elementsToDraw == 0)
			return;

		// Upload data and draw it.
		buf.flip();
		this.guiDrawData.uploadData(buf);
		buf.clear();
		
		renderingContext.useShader("gui");
		renderingContext.currentShader().setUniform1f("useTexture", useTexture ? 1f : 0f);
		
		renderingContext.bindTexture2D("sampler", currentTexture);
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		//TODO depreacated alpha_test mode
		if (alphaBlending || true)
		{
			renderingContext.setBlendMode(BlendMode.MIX);
		}
		else
		{
			renderingContext.setBlendMode(BlendMode.ALPHA_TEST);
		}
		
		renderingContext.setCullingMode(CullingMode.DISABLED);
		renderingContext.bindAttribute("vertexIn", guiDrawData.asAttributeSource(VertexFormat.FLOAT, 2, 20, 0));
		renderingContext.bindAttribute("texCoordIn", guiDrawData.asAttributeSource(VertexFormat.FLOAT, 2, 20, 8));
		renderingContext.bindAttribute("colorIn", guiDrawData.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 20, 16));
		
		renderingContext.draw(Primitive.TRIANGLE, 0, elementsToDraw);
		renderingContext.flush();

		elementsToDraw = 0;
	}

	public void free()
	{
		guiDrawData.destroy();
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
			textureName = ("./textures/" + textureName + ".png");

		Texture2D texture = TexturesHandler.getTexture(textureName);
		
		texture.setLinearFiltering(false);
		//TexturesHandler.mipmapLevel(texture, -1);

		drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, tcsx, tcsy, tcex, tcey, texture, false, true, new Vector4f(r, v, b, a));
	}

	public void renderColoredRect(float xpos, float ypos, float w, float h, float rot, String hex, float a)
	{
		int rgb[] = ColorsTools.hexToRGB(hex);
		renderColoredRect(xpos, ypos, w, h, rot, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, a);
	}

	public void renderColoredRect(float xpos, float ypos, float w, float h, float rot, float r, float v, float b, float a)
	{
		drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, 0, 0, 0, 0, null, false, true, new Vector4f(r, v, b, a));
	}
}
