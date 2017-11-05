package io.xol.engine.graphics.textures;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Texture2DAsset extends Texture2DGL
{
	//String name;
	Asset asset;
	String assetName;
	
	//TODO redo
	protected boolean scheduledForLoad = false;

	public Texture2DAsset(Asset asset)
	{
		super(TextureFormat.RGBA_8BPP);
		this.assetName = asset.getName();
		this.asset = asset;
		loadTextureFromAsset();
	}

	public void bind()
	{
		super.bind();
		
		if (scheduledForLoad && asset != null)
		{
			//TODO defer to asynch thread
			long ms = System.currentTimeMillis();
			System.out.print("main thread called, actually loading the texture ... ");
			this.loadTextureFromAsset();
			System.out.print(" took "+(System.currentTimeMillis()-ms) + "ms \n");
		}
	}
	
	public int loadTextureFromAsset()
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
			InputStream is = asset.read();
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
	
	// Texture modifications

	public void setAsset(Asset newAsset)
	{
		if(newAsset == null)
			throw new NullPointerException();
		
		this.asset = newAsset;
	}
	
	public Asset getAsset()
	{
		return this.asset;
	}

	public String getName()
	{
		if(assetName != null)
			return assetName;
		
		//TODO split loaded textures from vanilla ones
		throw new UnsupportedOperationException();
	}
}
