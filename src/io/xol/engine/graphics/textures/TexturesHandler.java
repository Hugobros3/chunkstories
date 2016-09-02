package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.util.concurrent.ConcurrentHashMap;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class TexturesHandler
{
	static Texture2D nullTexture;
	
	static ConcurrentHashMap<String, Cubemap> loadedCubemaps = new ConcurrentHashMap<String, Cubemap>();
	static Cubemap currentCubemap;
	
	static ConcurrentHashMap<String, Texture2D> loadedTextures = new ConcurrentHashMap<String, Texture2D>();
	static Texture2D currentTexture;

	public static Texture2D getTexture(String name)
	{
		if(loadedTextures.containsKey(name))
		{
			return loadedTextures.get(name);
		}
		else
		{
			Texture2D texture = new Texture2D(name);
			loadedTextures.put(name, texture);
			return texture;
		}
	}
	
	public static void bindTexture(String name)
	{
		int id = getTextureID(name);
		if(id < 0)
		{
			ChunkStoriesLogger.getInstance().info("Failed to bind texture "+name+", not loaded properly on disk.");
			return;
		}
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, id);
	}
	
	public static int getTextureID(String name)
	{
		return getTexture(name).getId();
	}

	public static Cubemap getCubemap(String name)
	{
		if(loadedCubemaps.containsKey(name))
		{
			return loadedCubemaps.get(name);
		}
		else
		{
			Cubemap cubemap = new Cubemap(name);
			loadedCubemaps.put(name, cubemap);
			return cubemap;
		}
	}
	
	public static void bindCubemap(String name)
	{
		int id = getCubemapID(name);
		if(id < 0)
		{
			ChunkStoriesLogger.getInstance().info("Failed to bind Cubemap "+name+", not loaded properly on disk.");
			return;
		}
		glEnable(GL_TEXTURE_CUBE_MAP);
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);
	}
	
	public static int getCubemapID(String name)
	{
		return getCubemap(name).getID();
	}
	
	public static void reloadAll()
	{
		for(Texture2D texture : loadedTextures.values())
		{
			texture.loadTextureFromDisk();
		}
		
		for(Cubemap cubemap : loadedCubemaps.values())
		{
			cubemap.loadCubemapFromDisk();
		}
	}

	public static Texture2D nullTexture()
	{
		if(nullTexture == null)
			nullTexture = TexturesHandler.getTexture("./textures/notex.png");
		return nullTexture;
	}
}
