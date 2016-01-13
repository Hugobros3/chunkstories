package io.xol.chunkstories.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.ARBTextureFloat.GL_RGBA16F_ARB;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.ByteBuffer;

import io.xol.chunkstories.client.FastConfig;

import static org.lwjgl.opengl.GL30.*;
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GBufferTexture
{

	int id;
	GBufferType type;
	int w, h;
	boolean isShadowMap = false;

	public enum GBufferType {
		RGBA_8BPP,
		RGB_HDR,
		DEPTH_SHADOWMAP,
		DEPTH_RENDERBUFFER;
	}
	
	public GBufferTexture(GBufferType type, int w, int h)
	{
		id = glGenTextures();
		this.type = type;
		resize(w, h);
	}

	public void resize(int w, int h)
	{
		glBindTexture(GL_TEXTURE_2D, id);
		
		if(this.w == w && this.h == h)
			return;
		
		this.w = w;
		this.h = h;
		if (type == GBufferType.RGBA_8BPP)
		{
			//ChunkStoriesLogger.getInstance().log("Created " + w + "by" + h + " RGBA texture", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
		}
		else if (type == GBufferType.RGB_HDR)
		{
			//ChunkStoriesLogger.getInstance().log("Created " + w + "by" + h + " D16 texture", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			// Optimization for OpenGL 3 cards
			if(FastConfig.openGL3Capable)
				glTexImage2D(GL_TEXTURE_2D, 0, GL_R11F_G11F_B10F, w, h, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
			else
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F_ARB, w, h, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
				
			// GL_RGBA16F_ARB for GL3
		}
		else if (type == GBufferType.DEPTH_SHADOWMAP)
		{
			//ChunkStoriesLogger.getInstance().log("Created " + w + "by" + h + " D16 texture", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
			// D16 Textures are for shadow maps
			isShadowMap = true;
		}
		else if (type == GBufferType.DEPTH_RENDERBUFFER)
		{
			//ChunkStoriesLogger.getInstance().log("Created " + w + "by" + h + " D32 texture", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.INFO);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
		}
		if (!isShadowMap)
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		}
		else
		{
			// For proper hardware linear filtering of textures :o
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	public int getID()
	{
		return id;
	}

	public void free()
	{
		glDeleteTextures(id);
	}

	public int getWidth()
	{
		return w;
	}

	public int getHeight()
	{
		return h;
	}
}
