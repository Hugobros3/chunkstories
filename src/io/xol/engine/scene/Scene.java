package io.xol.engine.scene;

import java.util.ArrayList;
import java.util.List;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.Button;
import io.xol.engine.gui.GuiDrawer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Scene
{
	public XolioWindow eng;
	public List<Button> buttons = new ArrayList<Button>();
	public boolean resized = false;

	public Scene(XolioWindow XolioWindow)
	{
		eng = XolioWindow;
	}

	public void update()
	{
		if (resized)
		{
			resized = false;
		}
		for (Button b : buttons)
		{
			b.render();
			b.update();
		}

		GuiDrawer.drawBuffer();
		XolioWindow.tick();
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
