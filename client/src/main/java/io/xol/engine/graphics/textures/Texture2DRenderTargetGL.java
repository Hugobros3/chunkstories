package io.xol.engine.graphics.textures;

import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Texture2DRenderTargetGL extends Texture2DGL
{
	protected boolean scheduledForLoad = false;
	
	int scheduledW, scheduledH;
	
	public Texture2DRenderTargetGL(TextureFormat type, int w, int h)
	{
		super(type);
		
		if (!Client.getInstance().getGameWindow().isMainGLWindow())
		{
			System.out.println("isn't main thread, scheduling texture creation");
			scheduledForLoad = true;
			scheduledW = w;
			scheduledH = h;
			return;
		}
		
		resize(w, h);
	}
	
	public void bind()
	{
		super.bind();
		
		if (scheduledForLoad)
		{
			scheduledForLoad = false;
			//TODO defer to asynch thread
			long ms = System.currentTimeMillis();
			System.out.print("main thread called, actually creating the texture ... ");
			resize(scheduledW, scheduledH);
			System.out.print(" took "+(System.currentTimeMillis()-ms) + "ms \n");
		}
	}
}
