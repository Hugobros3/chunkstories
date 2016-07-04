package io.xol.engine.graphics.textures;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface FBOAttachement
{
	public void attachDepth();
	
	public void attachColor(int colorAttachement);

	public void resize(int w, int h);

	public void free();
}
