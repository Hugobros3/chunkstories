package io.xol.engine.graphics.textures;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class TexturesHandler
{
	static Texture2DGL nullTexture;
	
	static ConcurrentHashMap<String, CubemapGL> loadedCubemaps = new ConcurrentHashMap<String, CubemapGL>();
	static Cubemap currentCubemap;
	
	static ConcurrentHashMap<String, Texture2DGL> loadedTextures = new ConcurrentHashMap<String, Texture2DGL>();
	static Texture2D currentTexture;

	public static Texture2DGL getTexture(String name)
	{
		if(loadedTextures.containsKey(name))
		{
			return loadedTextures.get(name);
		}
		else
		{
			if(name.startsWith("./"))
			{
				Asset asset = Client.getInstance().getContent().getAsset(name);
				if(asset == null)
					return nullTexture();
				
				Texture2DGL texture = new Texture2DAsset(asset);
				loadedTextures.put(name, texture);
				return texture;
			}
			else
			{
				//TODO check we are allowed to do this !
				File file = new File(name);
				if(file == null || !file.exists())
					return nullTexture();
				
				Texture2DGL texture = new Texture2DFile(file);
				loadedTextures.put(name, texture);
				return texture;
			}
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
			CubemapGL cubemap = new CubemapGL(name);
			loadedCubemaps.put(name, cubemap);
			return cubemap;
		}
	}
	
	public static void reloadAll()
	{
		for(Texture2DGL texture : loadedTextures.values())
		{
			if(texture instanceof Texture2DAsset)
			{
				Asset newAsset = Client.getInstance().getContent().getAsset(((Texture2DAsset) texture).getName());
				if(newAsset != null)
				{
					((Texture2DAsset) texture).setAsset(newAsset);
					((Texture2DAsset) texture).loadTextureFromAsset();
				}
				//If the asset is no longer avaible, don't update the texture and delete it
				else
				{
					texture.destroy();
					loadedTextures.remove(texture);
				}
			}
			else if(texture instanceof Texture2DFile)
			{
				((Texture2DFile) texture).loadTextureFromFile();
			}
		}

		for(CubemapGL cubemap : loadedCubemaps.values())
		{
			cubemap.loadCubemapFromDisk();
			//Asset newAsset = Mods.getAsset(cubemap.getName());
			//cubemap.setAsset(newAsset);
		}
	}

	public static Texture2DGL nullTexture()
	{
		if(nullTexture == null)
			nullTexture = TexturesHandler.getTexture("./textures/notex.png");
		return nullTexture;
	}
}
