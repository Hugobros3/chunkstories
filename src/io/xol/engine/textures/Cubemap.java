package io.xol.engine.textures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Cubemap
{
	String name;
	CubemapType type;
	int size;
	int glId = -1;
	
	public Cubemap(CubemapType type)
	{
		this.type = type;
		
		glId = glGenTextures();
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		// Anti seam
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}
	
	public Cubemap(String name)
	{
		this(CubemapType.RGBA_8BPP);
		this.name = name;
		loadCubemapFromDisk();
	}
	
	public int loadCubemapFromDisk()
	{
		if(glId == -1)
			glId = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, glId);
		
		ByteBuffer temp;
		String[] names = { "right", "left", "top", "bottom", "front", "back" };
		if (GameData.getFileLocation((name + "/front.png")) == null)
		{
			ChunkStoriesLogger.getInstance().log("Can't find front.png from CS-format skybox, trying MC format.", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
			names = new String[] { "panorama_1", "panorama_3", "panorama_4", "panorama_5", "panorama_0", "panorama_2" };
		}
		try
		{
			for (int i = 0; i < 6; i++)
			{
				File pngFile = GameData.getFileLocation(name + "/" + names[i] + ".png");
				if(pngFile == null)
					throw new FileNotFoundException(name + "/" + names[i] + ".png");
				PNGDecoder decoder = new PNGDecoder(new FileInputStream(pngFile));
				temp = ByteBuffer.allocateDirect(4 * decoder.getWidth() * decoder.getHeight());
				decoder.decode(temp, decoder.getWidth() * 4, Format.RGBA);
				temp.flip();
				glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, temp);
				// Anti alias
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				// Anti seam
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			}
		}
		catch(FileNotFoundException e)
		{
			ChunkStoriesLogger.getInstance().info("Clouldn't find file : "+e.getMessage());
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().log("Failed to load properly cubemap : " + name, ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
		}
		return glId;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public int getID()
	{
		if(glId == -1)
			glId = glGenTextures();
		return glId;
	}
	
	public void free()
	{
		glDeleteTextures(glId);
		glId = -1;
	}
	
	public enum CubemapType {
		RGBA_8BPP;
	}
}
