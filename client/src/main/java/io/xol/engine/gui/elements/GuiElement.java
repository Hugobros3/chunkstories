package io.xol.engine.gui.elements;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class GuiElement
{
	protected int posx;
	protected int posy;
	
	protected int scale = 1;
	private boolean focus = false;

	public boolean hasFocus()
	{
		return focus;
	}

	public void setFocus(boolean b)
	{
		focus = b;
	}
	
	public void setScale(int s)
	{
		scale = s;
	}

	public void setPosition(float x, float y)
	{
		posx = (int) x;
		posy = (int) y;
	}
}
