package io.xol.chunkstories.api.rendering.target;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Different kinds of stuff qualify as a render target, mostly textures
 */
public interface RenderTarget
{
	public void resize(int width, int height);
	
	public int getWidth();
	
	public int getHeight();

	public boolean destroy();
	
	//Internal.
	public void attachAsDepth();
	
	public void attachAsColor(int colorAttachement);
}
