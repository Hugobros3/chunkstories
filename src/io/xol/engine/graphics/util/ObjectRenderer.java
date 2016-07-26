package io.xol.engine.graphics.util;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.misc.ColorsTools;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ObjectRenderer
{
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

		Texture2D texture = TexturesHandler.getTexture(textureName);
		
		texture.setLinearFiltering(false);
		//TexturesHandler.mipmapLevel(texture, -1);

		GameWindowOpenGL.getInstance().getRenderingContext().getGuiRenderer().drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, tcsx, tcsy, tcex, tcey, texture.getId(), false, true, new Vector4f(r, v, b, a));
	}

	public static void renderColoredRect(float xpos, float ypos, float w, float h, float rot, String hex, float a)
	{
		int rgb[] = ColorsTools.hexToRGB(hex);
		renderColoredRect(xpos, ypos, w, h, rot, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, a);
	}

	public static void renderColoredRect(float xpos, float ypos, float w, float h, float rot, float r, float v, float b, float a)
	{
		GameWindowOpenGL.getInstance().getRenderingContext().getGuiRenderer().drawBoxWindowsSpace(xpos - w / 2, ypos + h / 2, xpos + w / 2, ypos - h / 2, 0, 0, 0, 0, 0, false, true, new Vector4f(r, v, b, a));
	}
}
