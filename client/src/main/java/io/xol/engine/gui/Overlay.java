package io.xol.engine.gui;

import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Overlay
{
	public GuiElementsHandler guiHandler = new GuiElementsHandler();
	public OverlayableScene mainScene;
	public Overlay parent;

	public Overlay(OverlayableScene scene, Overlay parent)
	{
		mainScene = scene;
		this.parent = parent;
	}

	public abstract void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height);

	public boolean handleKeypress(int k)
	{
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
	
	public boolean onScroll(int dy)
	{
		return false;
	}
}