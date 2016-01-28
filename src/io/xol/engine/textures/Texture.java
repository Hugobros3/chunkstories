package io.xol.engine.textures;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.GameData;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Texture
{
	String name;
	TextureType type;
	int glId = -1;
	int width, height;
	boolean wrapping = true;
	boolean mipmapping = false;
	boolean mipmapsUpToDate = false;
	boolean linearFiltering = true;
	int baseMipmapLevel = 0;
	int maxMipmapLevel = 1000;
	
	public Texture(TextureType type)
	{
		this.type = type;
	}
	
	public Texture(String name)
	{
		this(TextureType.RGBA_8BPP);
		this.name = name;
		loadTextureFromDisk();
	}
	
	public int loadTextureFromDisk()
	{
		File textureFile = GameData.getTextureFileLocation(name);
		if(textureFile == null)
		{
			ChunkStoriesLogger.getInstance().warning("Couldn't load texture "+name+", no file found on disk matching this name.");
			return -1;
		}
		if(glId == -1)
			glId = glGenTextures();
		glEnable(GL_TEXTURE_2D);
		//TODO we probably don't need half this shit
		glDisable(GL_TEXTURE_CUBE_MAP);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, glId);
		try
		{
			PNGDecoder decoder = new PNGDecoder(new FileInputStream(textureFile));
			width = decoder.getWidth();
			height = decoder.getHeight();
			ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(temp, width * 4, Format.RGBA);
			//ChunkStoriesLogger.getInstance().log("decoded " + width + " by " + height + " pixels (" + name + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.DEBUG);
			temp.flip();
			glBindTexture(GL_TEXTURE_2D, glId);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, temp);
			
			if(mipmapping)
			{
				//Regenerate the mipmaps only when necessary
				if (FastConfig.openGL3Capable)
					GL30.glGenerateMipmap(GL_TEXTURE_2D);
				else if (FastConfig.fbExtCapable)
					ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
			}
			if(!wrapping)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, baseMipmapLevel);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxMipmapLevel);
			setFiltering();
			
		}
		catch(FileNotFoundException e)
		{
			ChunkStoriesLogger.getInstance().info("Clouldn't find file : "+e.getMessage());
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().warning("Error loading file : "+e.getMessage());
			e.printStackTrace();
		}
		mipmapsUpToDate = false;
		return glId;
	}

	/**
	 * Returns the OpenGL GL_TEXTURE id of this object
	 * @return
	 */
	public int getID()
	{
		return glId;
	}
	
	public void free()
	{
		glDeleteTextures(glId);
		glId = -1;
	}
	
	// Texture modifications
	
	/**
	 * Determines if a texture will loop arround itself or clamp to it's edges
	 * @param on
	 */
	public void setTextureWrapping(boolean on)
	{
		if(glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
		
		if(wrapping != on) // We changed something so we redo them
			applyParameters = true;
		
		wrapping = on;

		if(!applyParameters)
			return;
		glBindTexture(GL_TEXTURE_2D, glId);
		if(!on)
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
	}
	
	public void setMipMapping(boolean on)
	{
		if(glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
		
		if(mipmapping != on) // We changed something so we redo them
			applyParameters = true;
		
		mipmapping = on;
		
		if(!applyParameters)
			return;
		glBindTexture(GL_TEXTURE_2D, glId);
		setFiltering();
		if(mipmapping && !mipmapsUpToDate)
		{
			//Regenerate the mipmaps only when necessary
			if (FastConfig.openGL3Capable)
				GL30.glGenerateMipmap(GL_TEXTURE_2D);
			else if (FastConfig.fbExtCapable)
				ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
		}
	}
	
	public void setLinearFiltering(boolean on)
	{
		if(glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
		
		if(linearFiltering != on) // We changed something so we redo them
			applyParameters = true;
		
		linearFiltering = on;
		
		if(!applyParameters)
			return;
		glBindTexture(GL_TEXTURE_2D, glId);
		setFiltering();
	}
	
	// Private function that sets both filering scheme and mipmap usage.
	private void setFiltering()
	{
		//System.out.println("Set filtering called for "+name+" "+linearFiltering);
		if(mipmapping)
		{
			if(linearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,  GL_NEAREST_MIPMAP_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,  GL_NEAREST);
			}
		}
		else
		{
			if(linearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}
		}
	}

	public void setMipmapLevelsRange(int baseLevel, int maxLevel)
	{
		if(glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
		
		if(this.baseMipmapLevel != baseLevel || this.maxMipmapLevel != maxLevel) // We changed something so we redo them
			applyParameters = true;
		
		baseMipmapLevel = baseLevel;
		maxMipmapLevel = maxLevel;
		
		if(!applyParameters)
			return;
		glBindTexture(GL_TEXTURE_2D, glId);
	}
	
	public void uploadExplicitMipmapLevel(int level, ByteBuffer data)
	{
		//TODO do
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}

	public enum TextureType {
		RGBA_8BPP,
		RGB_HDR,
		DEPTH_SHADOWMAP,
		DEPTH_RENDERBUFFER;
	}
}
