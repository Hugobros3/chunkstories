package io.xol.engine.gui;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Focusable
{
	public boolean focus = false;

	public boolean hasFocus()
	{
		return focus;
	}

	public void setFocus(boolean b)
	{
		focus = b;
	}
}
