package io.xol.chunkstories.api.gui;

import io.xol.chunkstories.gui.OverlayableScene;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Overlay
{
	public OverlayableScene mainScene;
	public Overlay parent;

	public Overlay(OverlayableScene scene, Overlay parent)
	{
		mainScene = scene;
		this.parent = parent;
	}

	public abstract void drawToScreen(int positionStartX, int positionStartY, int width, int height);

	public boolean handleKeypress(int k)
	{
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		return false;
	}
	
	public boolean onScroll(int dy)
	{
		return false;
	}
}