package io.xol.engine.gui;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.util.GuiDrawer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Scene
{
	public GameWindowOpenGL eng;
	public boolean resized = false;

	public Scene(GameWindowOpenGL XolioWindow)
	{
		eng = XolioWindow;
	}

	public void update()
	{
		if (resized)
		{
			resized = false;
		}

		GuiDrawer.drawBuffer();
		GameWindowOpenGL.tick();
	}

	public void onResize()
	{

	}

	public boolean onClick(int posx, int posy, int button)
	{
		return false;
	}

	public boolean onKeyPress(int k)
	{
		return false;
	}

	public boolean onKeyRelease(int k)
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
