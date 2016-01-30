package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.gui.OverlayableScene;

public class MenuOverlay
{
	public OverlayableScene mainScene;
	public MenuOverlay parent;

	public MenuOverlay(OverlayableScene scene, MenuOverlay parent)
	{
		mainScene = scene;
		this.parent = parent;
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		
	}

	public boolean handleKeypress(int k)
	{
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		return false;
	}
}