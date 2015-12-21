package io.xol.engine.scene;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SubScene
{
	protected Scene parent;

	public SubScene(Scene p)
	{
		parent = p;
	}

	public void update()
	{

	}

	public boolean onKeyPress(int k)
	{
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		return false;
	}

	public boolean onWheel(int dx)
	{
		return false;
	}
}
