package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.gui.Layer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GameWindow
{
	public ClientInterface getClient();
	
	//TODO float
	public int getWidth();
	
	public int getHeight();
	
	public Layer getLayer();
	
	public void setLayer(Layer layer);

	public void close();

	public ClientInputsManager getInputsManager();

	public boolean hasFocus();

	public String takeScreenshot();
}
