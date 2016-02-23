package io.xol.engine.textures;

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
	static ConcurrentHashMap<String, Cubemap> loadedCubemaps = new ConcurrentHashMap<String, Cubemap>();
	static Cubemap currentCubemap;
	
	static ConcurrentHashMap<String, Texture> loadedTextures = new ConcurrentHashMap<String, Texture>();
	static Texture currentTexture;

	public static Texture getTexture(String name)
	{
		if(loadedTextures.containsKey(name))
		{
			return loadedTextures.get(name);
		}
		else
		{
			Texture texture = new Texture(name);
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
		return getTexture(name).getID();
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
	
	/*
	public static int loadCubeMap(String name)
	{
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);
		ByteBuffer temp;
		String[] names = { "right", "left", "top", "bottom", "front", "back" };
		if (!(new File(name + "/front.png")).exists())
		{
			ChunkStoriesLogger.getInstance().log("Can't find front.png from CS-format skybox, trying MC format.", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.WARN);
			names = new String[] { "panorama_1", "panorama_3", "panorama_4", "panorama_5", "panorama_0", "panorama_2" };
		}
		try
		{
			for (int i = 0; i < 6; i++)
			{
				PNGDecoder decoder = new PNGDecoder(new FileInputStream(new File(name + "/" + names[i] + ".png")));
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

			// ChunkStoriesLogger.getInstance().log(,
			// ChunkStoriesLogger.LogType.RENDERING,
			// ChunkStoriesLogger.LogLevel.WARN);
			e.printStackTrace();
			textureID = -1;
		}
		return textureID;
	}

	public static void bindCubeMap(String name)
	{
		if (alreadyBoundCubemap.equals(name))
			return;
		if (loadedCubemaps.containsKey(name))
		{
			glBindTexture(GL_TEXTURE_CUBE_MAP, loadedCubemaps.get(name));
		}
		else
		{
			int cubeMapID = loadCubeMap(name);
			if (cubeMapID != -1)
			{
				loadedCubemaps.put(name, cubeMapID);
				glBindTexture(GL_TEXTURE_CUBE_MAP, loadedCubemaps.get(name));
			}
		}
	}

	public static int idCubemap(String name)
	{
		if (loadedCubemaps.containsKey(name))
		{
			return loadedCubemaps.get(name);
		}
		else
		{
			int cubeMapID = loadCubeMap(name);
			if (cubeMapID != -1)
			{
				loadedCubemaps.put(name, cubeMapID);
				// glBindTexture(GL_TEXTURE_CUBE_MAP, loadedTextures.get(name));
			}
			return cubeMapID;
		}
	}
	
	public static void loadSkybox(int skyboxpart, String path)
	{
		try
		{
			BufferedImage image = ImageIO.read(new FileInputStream(new File(path)));
			int[] pixels = new int[image.getWidth() * image.getHeight()];
			image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
			ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4); // 4
																										// for
																										// RGBA,
																										// 3
																										// for
																										// RGB
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					int pixel = pixels[y * image.getWidth() + x];
					buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red component
					buffer.put((byte) ((pixel >> 8) & 0xFF)); // Green component
					buffer.put((byte) (pixel & 0xFF)); // Blue component
					buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
																// component.
																// Only for RGBA
				}
			}
			buffer.flip();
			glTexImage2D(skyboxpart, 0, GL_RGBA, (int) image.getWidth(), (int) image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

*/
	public static void reloadAll()
	{
		for(Texture texture : loadedTextures.values())
		{
			texture.loadTextureFromDisk();
		}
		
		for(Cubemap cubemap : loadedCubemaps.values())
		{
			cubemap.loadCubemapFromDisk();
		}
	}
}
