package io.xol.engine.graphics.textures;

import io.xol.chunkstories.api.rendering.textures.TextureFormat;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Texture2DRenderTargetGL extends Texture2DGL
{
	public Texture2DRenderTargetGL(TextureFormat type, int w, int h)
	{
		super(type);
		
		resize(w, h);
	}
}
