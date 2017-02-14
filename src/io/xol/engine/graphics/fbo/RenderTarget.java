package io.xol.engine.graphics.fbo;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Different kinds of stuff qualify as a render target, mostly textures
 */
public interface RenderTarget
{
	public void attacAsDepth();
	
	public void attachAsColor(int colorAttachement);

	public void resize(int width, int height);
	
	public int getWidth();
	
	public int getHeight();

	public boolean destroy();
}
