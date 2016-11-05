package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NICEST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glHint;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP_HINT;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;

import io.xol.chunkstories.client.RenderingConfig;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.geometry.IllegalRenderingThreadException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Texture2D extends Texture
{
	protected int width;
	protected int height;
	boolean wrapping = true;
	boolean mipmapping = false;
	protected boolean mipmapsUpToDate = false;
	boolean linearFiltering = true;
	int baseMipmapLevel = 0;
	int maxMipmapLevel = 1000;
	protected boolean scheduledForLoad = false;
	static int currentlyBoundId = 0;

	protected void applyTextureParameters()
	{
		//Generate mipmaps
		if (mipmapping)
		{
			if (RenderingConfig.gl_openGL3Capable)
				GL30.glGenerateMipmap(GL_TEXTURE_2D);
			else if (RenderingConfig.gl_fbExtCapable)
				ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
	
			mipmapsUpToDate = true;
		}
		if (!wrapping)
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
		setFiltering();
	}

	public boolean uploadTextureData(int width, int height, ByteBuffer data)
	{
		return uploadTextureData(width, height, 0, data);
	}

	public boolean uploadTextureData(int width, int height, int level, ByteBuffer data)
	{
		int k = currentlyBoundId;
	
		glActiveTexture(GL_TEXTURE0 + 15);
		bind();
		this.width = width;
		this.height = height;
	
		glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), width, height, 0, type.getFormat(), type.getType(), (ByteBuffer) data);
	
		applyTextureParameters();
		
		if(k > 0)
			glBindTexture(GL_TEXTURE_2D, currentlyBoundId);
		
		return true;
	}

	/**
	 * Returns the OpenGL GL_TEXTURE id of this object
	 * 
	 * @return
	 */
	public int getId()
	{
		return glId;
	}

	public Texture2D(TextureFormat type)
	{
		super(type);
	}

	public void bind()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
			throw new IllegalRenderingThreadException();
		//Allow creation only in intial state
		if (glId == -1)
		{
			aquireID();
		}
	
		glBindTexture(GL_TEXTURE_2D, glId);
		currentlyBoundId = glId;
	}

	/**
	 * Determines if a texture will loop arround itself or clamp to it's edges
	 */
	public void setTextureWrapping(boolean on)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		if (wrapping != on) // We changed something so we redo them
			applyParameters = true;
	
		wrapping = on;
	
		if (!applyParameters)
			return;
		bind();
		if (!on)
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
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		if (mipmapping != on) // We changed something so we redo them
			applyParameters = true;
	
		mipmapping = on;
	
		if (!applyParameters)
			return;
		bind();
		setFiltering();
		if (mipmapping && !mipmapsUpToDate)
		{
			computeMipmaps();
		}
	}

	public void computeMipmaps()
	{
		//System.out.println("Computing mipmap for "+glId);
		bind();
		
		//Regenerate the mipmaps only when necessary
		if (RenderingConfig.gl_openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (RenderingConfig.gl_fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
	
		mipmapsUpToDate = true;
	}

	public void setLinearFiltering(boolean on)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		if (linearFiltering != on) // We changed something so we redo them
			applyParameters = true;
	
		linearFiltering = on;
	
		if (!applyParameters)
			return;
		bind();
		setFiltering();
	}

	private void setFiltering()
	{
		//System.out.println("Set filtering called for "+name+" "+linearFiltering);
		if (mipmapping)
		{
			if (linearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			}
	
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, baseMipmapLevel);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxMipmapLevel);
		}
		else
		{
			if (linearFiltering)
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
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;
	
		if (this.baseMipmapLevel != baseLevel || this.maxMipmapLevel != maxLevel) // We changed something so we redo them
			applyParameters = true;
	
		baseMipmapLevel = baseLevel;
		maxMipmapLevel = maxLevel;
	
		if (!applyParameters)
			return;
		bind();
		setFiltering();
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public long getVramUsage()
	{
		int surface = getWidth() * getHeight();
		return surface * type.getBytesPerTexel();
	}

	public abstract String getName();

}
