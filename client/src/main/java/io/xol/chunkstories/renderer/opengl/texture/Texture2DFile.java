//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.texture;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glTexImage2D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

public class Texture2DFile extends Texture2DGL
{
	private File file;

	protected boolean scheduledForLoad = false;

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
		if (!Client.getInstance().getGameWindow().isMainGLWindow())
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
		
			this.applyTextureWrapping();
			this.applyFiltering();
			this.computeMipmaps();
		}
		catch (FileNotFoundException e)
		{
			logger().warn("Couldn't find file : " + e.getMessage());
		}
		catch (IOException e)
		{
			logger().error("Error loading file : " + e.getMessage());
			e.printStackTrace();
		}
		mipmapsUpToDate = false;
		return glId;
	}

	public String getName()
	{
		return file.getAbsolutePath();
	}

}
