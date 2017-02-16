package io.xol.engine.graphics.textures;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Texture2DRenderTarget extends Texture2D
{
	public Texture2DRenderTarget(TextureFormat type, int w, int h)
	{
		super(type);
		
		resize(w, h);
	}
}
