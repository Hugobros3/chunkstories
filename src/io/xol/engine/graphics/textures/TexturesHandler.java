package io.xol.engine.graphics.textures;

import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.mods.Asset;

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
			Asset asset = Mods.getAsset(name);
			if(asset == null)
				return nullTexture();
			
			Texture2D texture = new Texture2D(asset);
			loadedTextures.put(name, texture);
			return texture;
		}
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
	
	public static void reloadAll()
	{
		for(Texture2D texture : loadedTextures.values())
		{
			texture.destroy();
			//texture.loadTextureFromDisk();
		}
		loadedTextures.clear();
		
		for(Cubemap cubemap : loadedCubemaps.values())
		{
			cubemap.destroy();
			//cubemap.loadCubemapFromDisk();
		}
		loadedCubemaps.clear();
	}

	public static Texture2D nullTexture()
	{
		if(nullTexture == null)
			nullTexture = TexturesHandler.getTexture("./textures/notex.png");
		return nullTexture;
	}
}
