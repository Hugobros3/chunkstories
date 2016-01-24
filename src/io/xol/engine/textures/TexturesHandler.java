package io.xol.engine.textures;

import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class TexturesHandler
{
	static Map<String, Integer> loadedCubemaps = new HashMap<String, Integer>();
	static String alreadyBoundCubemap = "";
	
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

	/*public static void nowrap(String string)
	{
		int texID = getTextureID(string);
		glBindTexture(GL_TEXTURE_2D, texID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	public static void mipmap(String string)
	{
		int texID = idTexture(string);
		glBindTexture(GL_TEXTURE_2D, texID);
		if (FastConfig.openGL3Capable)
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		else if (FastConfig.fbExtCapable)
			ARBFramebufferObject.glGenerateMipmap(GL_TEXTURE_2D);
	}

	public static void mipmapLevel(String string, int level)
	{
		int texID = idTexture(string);
		glBindTexture(GL_TEXTURE_2D, texID);
		if (level >= 0)
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, level);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, level);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		}
	}*/

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

	public static void reloadAll()
	{
		for(Texture texture : loadedTextures.values())
		{
			if(texture.getID() != -1)
			texture.loadTextureFromDisk();
		}
	}
}
