//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.textures;

import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;



public class Texture2DRenderTargetGL extends Texture2DGL
{
	protected boolean scheduledForLoad = false;
	
	int scheduledW, scheduledH;
	
	public Texture2DRenderTargetGL(TextureFormat type, int w, int h)
	{
		super(type);
		
		if (!Client.getInstance().getGameWindow().isMainGLWindow())
		{
			//System.out.println("isn't main thread, scheduling texture creation");
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
			resize(scheduledW, scheduledH);
		}
	}
}
