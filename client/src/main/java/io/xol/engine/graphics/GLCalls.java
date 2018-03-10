//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBDrawInstanced.*;

public class GLCalls
{
	static long verticesDrawn;
	static long drawCalls;
	
	public static void nextFrame()
	{
		verticesDrawn = 0;
		drawCalls = 0;
	}
	
	public static void DrawArrays(int mode, int first, int verticesCount)
	{
		glDrawArrays(mode, first, verticesCount);
		verticesDrawn += verticesCount;
		drawCalls++;
	}

	public static void DrawArraysInstanced(int mode, int first, int verticesCount, int instancesCount)
	{
		glDrawArraysInstancedARB(mode, first, verticesCount, instancesCount);
		verticesDrawn += instancesCount * verticesCount;
		drawCalls++;
	}

	public static void MultiDrawArrays(int mode, IntBuffer starts, IntBuffer counts)
	{
		glMultiDrawArrays(mode, starts, counts);
		while(counts.hasRemaining())
			verticesDrawn += counts.get();
		drawCalls++;
	}
	
	public static String getStatistics()
	{
		return "Drew "+formatBigAssNumber(verticesDrawn+"")+" verts, in "+drawCalls+" draw calls.";
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
