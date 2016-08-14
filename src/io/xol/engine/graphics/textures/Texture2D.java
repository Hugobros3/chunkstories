package io.xol.engine.graphics.textures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.geometry.IllegalRenderingThreadException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Texture2D
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

	private boolean scheduledForLoad = false;

	public Texture2D(TextureType type)
	{
		this.type = type;

		totalTextureObjects++;
		allTextureObjects.add(new WeakReference<Texture2D>(this));
	}

	public TextureType getType()
	{
		return type;
	}

	public Texture2D(String name)
	{
		this(TextureType.RGBA_8BPP);
		this.name = name;
		loadTextureFromDisk();
	}

	public int loadTextureFromDisk()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
		{
			System.out.println("isn't main thread, scheduling load");
			scheduledForLoad = true;
			return -1;
		}
		scheduledForLoad = false;

		File textureFile = GameData.getTextureFileLocation(name);
		if (textureFile == null)
		{
			ChunkStoriesLogger.getInstance().warning("Couldn't load texture " + name + ", no file found on disk matching this name.");
			return -1;
		}
		//TODO we probably don't need half this shit
		//glActiveTexture(GL_TEXTURE0);
		bind();
		try
		{
			PNGDecoder decoder = new PNGDecoder(new FileInputStream(textureFile));
			width = decoder.getWidth();
			height = decoder.getHeight();
			ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(temp, width * 4, Format.RGBA);
			//ChunkStoriesLogger.getInstance().log("decoded " + width + " by " + height + " pixels (" + name + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.DEBUG);
			temp.flip();
			bind();
			glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), width, height, 0, type.getFormat(), type.getType(), (ByteBuffer) temp);
			//glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, temp);

			applyTextureParameters();

		}
		catch (FileNotFoundException e)
		{
			ChunkStoriesLogger.getInstance().info("Clouldn't find file : " + e.getMessage());
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().warning("Error loading file : " + e.getMessage());
			e.printStackTrace();
		}
		mipmapsUpToDate = false;
		return glId;
	}

	private void applyTextureParameters()
	{
		//Generate mipmaps
		if (mipmapping)
		{
			if (RenderingConfig.openGL3Capable)
				GL30.glGenerateMipmap(GL_TEXTURE_2D);
			else if (RenderingConfig.fbExtCapable)
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
		bind();
		this.width = width;
		this.height = height;

		//glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, level, GL_RGBA, GL_UNSIGNED_BYTE, data);
		glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), width, height, 0, type.getFormat(), type.getType(), (ByteBuffer) data);

		applyTextureParameters();
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

	public void bind()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
			throw new IllegalRenderingThreadException();
		//Allow creation only in intial state
		if (glId == -1)
		{
			glId = glGenTextures();
		}

		glBindTexture(GL_TEXTURE_2D, glId);

		if (scheduledForLoad)
		{
			System.out.println("main thread called, actually loading");
			this.loadTextureFromDisk();
		}
	}

	public synchronized boolean destroy()
	{
		if (GameWindowOpenGL.isMainGLWindow())
		{

			if (glId >= 0)
			{
				glDeleteTextures(glId);
			}
			//Only register destruction once
			if (glId != -2)
			{
				totalTextureObjects--;
				glId = -2;
			}
			return true;
		}
		else
		{
			synchronized (objectsToDestroy)
			{
				objectsToDestroy.add(this);
			}
			return false;
		}
	}

	// Texture modifications

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
		//System.out.println("Computing mipmap for "+name);
		bind();
		glHint(GL_GENERATE_MIPMAP_HINT, GL_NICEST);
		//Regenerate the mipmaps only when necessary
		if (RenderingConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (RenderingConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);

		mipmapsUpToDate = true;
		//setFiltering();
		//setFiltering();
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

	// Private function that sets both filering scheme and mipmap usage.
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
		if (type == TextureType.RGBA_8BPP)
			return surface * 4;
		if (type == TextureType.RGB_HDR)
			return surface * 4;
		if (type == TextureType.DEPTH_SHADOWMAP)
			return surface * 3;
		if (type == TextureType.DEPTH_RENDERBUFFER)
			return surface * 4;
		return surface;
		
	}
	private static BlockingQueue<Texture2D> objectsToDestroy = new LinkedBlockingQueue<Texture2D>();

	public static int destroyPendingTextureObjects()
	{
		int destroyedVerticesObjects = 0;

		synchronized (objectsToDestroy)
		{
			Iterator<Texture2D> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				Texture2D object = i.next();

				if (object.destroy())
					destroyedVerticesObjects++;

				i.remove();
			}
		}

		return destroyedVerticesObjects;
	}

	public static int getTotalNumberOfTextureObjects()
	{
		return totalTextureObjects;
	}

	public static long getTotalVramUsage()
	{
		long vram = 0;

		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<Texture2D>> i = allTextureObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<Texture2D> reference = i.next();

			Texture2D object = reference.get();
			if (object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}

		return vram;
	}

	private static int totalTextureObjects = 0;
	private static BlockingQueue<WeakReference<Texture2D>> allTextureObjects = new LinkedBlockingQueue<WeakReference<Texture2D>>();
}
