package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glTexImage2D;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.base.GameWindowOpenGL;

public class Texture2DFile extends Texture2D
{
	File file;

	public Texture2DFile(File file)
	{
		super(TextureFormat.RGBA_8BPP);
		this.file = file;

		loadTextureFromFile();
	}

	public void bind()
	{
		super.bind();
		
		if (scheduledForLoad && file != null && file.exists())
		{
			long ms = System.currentTimeMillis();
			System.out.print("main thread called, actually loading the texture ... ");
			this.loadTextureFromFile();
			System.out.print((System.currentTimeMillis()-ms) + "ms \n");
		}
	}

	public int loadTextureFromFile()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
		{
			System.out.println("isn't main thread, scheduling load");
			scheduledForLoad = true;
			return -1;
		}
		scheduledForLoad = false;

		//TODO we probably don't need half this shit
		bind();
		try
		{
			InputStream is = new FileInputStream(file);
			PNGDecoder decoder = new PNGDecoder(is);
			width = decoder.getWidth();
			height = decoder.getHeight();
			ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(temp, width * 4, Format.RGBA);
			is.close();
			
			//ChunkStoriesLogger.getInstance().log("decoded " + width + " by " + height + " pixels (" + name + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.DEBUG);
			temp.flip();
			bind();
			glTexImage2D(GL_TEXTURE_2D, 0, type.getInternalFormat(), width, height, 0, type.getFormat(), type.getType(), (ByteBuffer) temp);
		
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

	@Override
	public String getName()
	{
		return file.getAbsolutePath();
	}

}
