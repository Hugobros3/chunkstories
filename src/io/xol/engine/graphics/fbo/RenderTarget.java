package io.xol.engine.graphics.fbo;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderTarget
{
	public void attacAshDepth();
	
	public void attachAsColor(int colorAttachement);

	public void resize(int width, int height);
	
	public int getWidth();
	
	public int getHeight();

	public boolean destroy();
}
