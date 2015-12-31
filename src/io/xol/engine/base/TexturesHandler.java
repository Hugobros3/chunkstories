package io.xol.engine.base;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;

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

	static Map<String, Integer> loadedTextures = new HashMap<String, Integer>();

	static String alreadyBound = "";

	public static int loadTexture(String name)
	{
		int textureID = glGenTextures();
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_CUBE_MAP);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureID);

		try
		{
			File file = new File(name);
			PNGDecoder decoder = new PNGDecoder(new FileInputStream(file));
			int width = decoder.getWidth();
			int height = decoder.getHeight();
			ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(temp, width * 4, Format.RGBA);
			//ChunkStoriesLogger.getInstance().log("decoded " + width + " by " + height + " pixels (" + name + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.DEBUG);
			temp.flip();
			glBindTexture(GL_TEXTURE_2D, textureID);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, temp);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return textureID;

	}

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
		if (alreadyBound.equals(name))
			return;
		if (loadedTextures.containsKey(name))
		{
			glBindTexture(GL_TEXTURE_CUBE_MAP, loadedTextures.get(name));
		}
		else
		{
			int cubeMapID = loadCubeMap(name);
			if (cubeMapID != -1)
			{
				loadedTextures.put(name, cubeMapID);
				glBindTexture(GL_TEXTURE_CUBE_MAP, loadedTextures.get(name));
			}
		}
	}

	public static void bindTexture(String name)
	{
		/*
		 * if(alreadyBound.equals(name)) return;
		 */
		// glEnable(GL_BLEND);
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, idTexture(name));

		// alreadyBound = name;
	}

	public static void nowrap(String string)
	{
		int texID = idTexture(string);
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

	}

	public static int idCubemap(String name)
	{
		if (loadedTextures.containsKey(name))
		{
			return loadedTextures.get(name);
		}
		else
		{
			int cubeMapID = loadCubeMap(name);
			if (cubeMapID != -1)
			{
				loadedTextures.put(name, cubeMapID);
				// glBindTexture(GL_TEXTURE_CUBE_MAP, loadedTextures.get(name));
			}
			return cubeMapID;
		}
	}

	public static int idTexture(String name)
	{
		if (loadedTextures.containsKey(name))
		{
			return loadedTextures.get(name);
		}
		else
		{
			int textureID = loadTexture(name);
			if (textureID != -1)
			{
				loadedTextures.put(name, textureID);
				glBindTexture(GL_TEXTURE_2D, loadedTextures.get(name));
			}
			return textureID;
			/*
			 * File file = new File(name); if (file.exists()) { try { Texture
			 * tex = TextureLoader.getTexture("PNG", new FileInputStream(new
			 * File(name))); loadedTextures.put(name, tex.getTextureID());
			 * 
			 * return tex.getTextureID(); } catch (Exception e) {
			 * e.printStackTrace(); } } else {
			 * System.out.println("Something went wrong ! File " + name +
			 * " does not exist !"); }
			 */
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
		for (Entry<String, Integer> entry : loadedTextures.entrySet())
		{
			glDeleteTextures(entry.getValue());
		}
		loadedTextures.clear();
	}

	public static void freeTexture(String name)
	{
		name = "./res/textures/" + name + ".png";
		// System.out.println("Asking for deletion of "+name);
		if (loadedTextures.containsKey(name))
		{
			glDeleteTextures(loadedTextures.get(name));
			loadedTextures.remove(name);
			// System.out.println(name+"deleted");
		}
	}
}
