//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.textures;

import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

public class Texture2DRenderTargetGL extends Texture2DGL implements Texture2DRenderTarget {
	protected boolean scheduledForCreation = false;

	int scheduledW, scheduledH;

	public Texture2DRenderTargetGL(TextureFormat type, int width, int height) {
		super(type);

		if (!Client.getInstance().getGameWindow().isMainGLWindow()) {
			// System.out.println("isn't main thread, scheduling texture creation");
			scheduledForCreation = true;
			scheduledW = width;
			scheduledH = height;
			return;
		}

		resize(width, height);
	}

	public void bind() {
		super.bind();

		if (scheduledForCreation) {
			scheduledForCreation = false;
			// TODO defer to asynch thread
			resize(scheduledW, scheduledH);
		}
	}

	@Override
	public void computeMipmaps() {
		mipmapsUpToDate = false; 
		super.computeMipmaps();
	}
	
	
}
