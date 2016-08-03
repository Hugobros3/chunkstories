package io.xol.engine.graphics;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import io.xol.engine.base.GameWindowOpenGL;

public class GLCalls
{
	static long verticesDrawn;
	static long drawCalls;
	
	public static void nextFrame()
	{
		verticesDrawn = 0;
		drawCalls = 0;
	}
	
	public static String getStatistics()
	{
		return "Drawn "+formatBigAssNumber(verticesDrawn+"")+" verts, in "+drawCalls+" draw calls.";
	}
	
	public static void drawArrays(int mode, int first, int count)
	{
		GameWindowOpenGL.getInstance().getRenderingContext().disableUnusedVertexAttributes();
		glDrawArrays(mode, first, count);
		verticesDrawn += count;
		drawCalls++;
	}

	public static String formatBigAssNumber(String in)
	{
		String formatted = "";
		for (int i = 0; i < in.length(); i++)
		{
			if (i > 0 && i % 3 == 0)
				formatted = "." + formatted;
			formatted = in.charAt(in.length() - i - 1) + formatted;
		}
		return formatted;
	}
}
