package io.xol.engine.gui.elements;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class GuiElement
{
	protected int posx;
	protected int posy;
	
	private boolean focus = false;

	public boolean hasFocus()
	{
		return focus;
	}

	public void setFocus(boolean b)
	{
		focus = b;
	}

	public void setPosition(float x, float y)
	{
		posx = (int) x;
		posy = (int) y;
	}
}
