package io.xol.engine.base;

import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector4f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ObjectRenderer
{
	static FloatBuffer buffer = null;
	
	public static void drawFSQuad(int vertexAttribLocation)
	{
		if(vertexAttribLocation < 0)
			return;
		if(buffer == null)
		{
			buffer = BufferUtils.createFloatBuffer(6 * 2);
			buffer.put(new float[] {
				1f, 1f,
				1f, -1f,
				-1f, -1f,
				1f, 1f,
				-1f, 1f,
				-1f, -1f
			});
		}
		buffer.flip();
		glEnableVertexAttribArray(vertexAttribLocation);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glVertexAttribPointer(vertexAttribLocation, 2, false, 0, buffer);
        glDrawArrays(GL_TRIANGLES, 0, 6);
		glDisableVertexAttribArray(vertexAttribLocation);
        //buffer = null;
	}
	
	public static void renderTexturedRect(float xpos, float ypos, float w, float h, String tex)
	{
		renderTexturedRotatedRect(xpos, ypos, w, h, 0f, 0f, 0f, 1f, 1f, tex);
	}

	public static void renderTexturedRectAlpha(float xpos, float ypos, float w, float h, String tex, float a)
	{
		renderTexturedRotatedRectAlpha(xpos, ypos, w, h, 0f, 0f, 0f, 1f, 1f, tex, a);
	}

	public static void renderTexturedRect(float xpos, float ypos, float w, float h, float tcsx, float tcsy, float tcex, float tcey, float texSize, String tex)
	{
		renderTexturedRotatedRect(xpos, ypos, w, h, 0f, tcsx / texSize, tcsy / texSize, tcex / texSize, tcey / texSize, tex);
	}

	public static void renderTexturedRotatedRect(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String tex)
	{
		renderTexturedRotatedRectAlpha(xpos, ypos, w, h, rot, tcsx, tcsy, tcex, tcey, tex, 1f);
	}

	public static void renderTexturedRotatedRectAlpha(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String tex, float a)
	{
		renderTexturedRotatedRectRVBA(xpos, ypos, w, h, rot, tcsx, tcsy, tcex, tcey, tex, 1f, 1f, 1f, a);
	}

	public static void renderTexturedRotatedRectRVBA(float xpos, float ypos, float w, float h, float rot, float tcsx, float tcsy, float tcex, float tcey, String textureName, float r, float v, float b, float a)
	{

		if (textureName.startsWith("internal://"))
			textureName = textureName.substring("internal://".length());
		else if (textureName.startsWith("gameDir://"))
			textureName = textureName.substring("gameDir://".length());//GameDirectory.getGameFolderPath() + "/" + tex.substring("gameDir://".length());
		else if (textureName.contains("../"))
			textureName = ("./" + textureName.replace("../", "") + ".png");
		else
			textureName = ("./res/textures/" + textureName + ".png");

		Texture texture = TexturesHandler.getTexture(textureName);
		
		texture.setLinearFiltering(false);
		//TexturesHandler.mipmapLevel(texture, -1);

		GuiDrawer.drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, tcsx, tcsy, tcex, tcey, texture.getID(), false, true, new Vector4f(r, v, b, a));
	}

	public static void renderColoredRect(float xpos, float ypos, float w, float h, float rot, String hex, float a)
	{
		int rgb[] = ColorsTools.hexToRGB(hex);
		renderColoredRect(xpos, ypos, w, h, rot, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, a);
	}

	public static void renderColoredRect(float xpos, float ypos, float w, float h, float rot, float r, float v, float b, float a)
	{
		GuiDrawer.drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, 0, 0, 0, 0, 0, false, true, new Vector4f(r, v, b, a));

	}
}
