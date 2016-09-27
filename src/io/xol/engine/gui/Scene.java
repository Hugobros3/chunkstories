package io.xol.engine.gui;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Scene
{
	protected GuiElementsHandler guiHandler = new GuiElementsHandler();
	
	public GameWindowOpenGL gameWindow;

	public Scene(GameWindowOpenGL gameWindow)
	{
		this.gameWindow = gameWindow;
	}

	public abstract void update(RenderingContext renderingContext);

	public void onResize()
	{

	}

	public boolean onKeyDown(int keyCode)
	{
		return false;
	}
	
	public boolean onKeyRepeatEvent(int keyCode)
	{
		return false;
	}

	public boolean onKeyUp(int keyCode)
	{
		return false;
	}
	
	public boolean onMouseButtonDown(int posx, int posy, int button)
	{
		return false;
	}
	
	public boolean onMouseButtonUp(int posx, int posy, int button)
	{
		return false;
	}

	public boolean onScroll(int scrollAmount)
	{
		return false;
	}

	public void destroy()
	{

	}

}
