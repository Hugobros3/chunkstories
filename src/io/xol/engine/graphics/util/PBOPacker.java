package io.xol.engine.graphics.util;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;

import java.nio.ByteBuffer;

import io.xol.engine.graphics.textures.Texture2D;

public class PBOPacker
{
	int bufferId;
	
	public PBOPacker()
	{
		bufferId = glGenBuffers();
	}
	
	public void copyTexure(Texture2D texture)
	{
		copyTexure(texture, 0);
	}
	
	public void copyTexure(Texture2D texture, int level)
	{
		//glFinish();
		glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
		glBindTexture(GL_TEXTURE_2D, texture.getId());
		
		int width = texture.getWidth();
		int height = texture.getHeight();
		
		double pow = Math.pow(2, level);
		width =  (int)Math.ceil(width / pow);
		height = (int)Math.ceil(height / pow);
		glBufferData(GL_PIXEL_PACK_BUFFER, width * height * 4 * 3 , GL_STREAM_COPY);
		
		//glReadPixels(0,0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		glGetTexImage(GL_TEXTURE_2D, level, GL_RGB, GL_FLOAT, 0);

		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
	}
	
	public ByteBuffer readPBO()
	{
		glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
		ByteBuffer buf = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, null);
	    //glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
		return buf;
	}
	
	public void doneWithReading()
	{

		glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
	    glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
	}
	
	public void destroy()
	{
		glDeleteBuffers(bufferId);
	}
}
