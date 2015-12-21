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

	public SubScene subscene = null;
	boolean shouldDestroySubscene = false;

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
		if (shouldDestroySubscene)
		{
			subscene = null;
			shouldDestroySubscene = false;
		}
		if (subscene != null)
			subscene.update();

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

	public void setSubscene(SubScene s)
	{
		subscene = s;
	}

	public void destroySubscene()
	{
		shouldDestroySubscene = true;
	}

	public void destroy()
	{

	}

}
