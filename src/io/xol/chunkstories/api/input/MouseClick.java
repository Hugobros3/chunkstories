package io.xol.chunkstories.api.input;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MouseClick implements Input
{
	String name;
	int button;
	
	private MouseClick(String name, int button)
	{
		this.name = name;
		this.button = button;
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isPressed()
	{
		return false;
	}
	
	public static MouseClick LEFT = new MouseClick("mouse.left", 0);
	public static MouseClick RIGHT = new MouseClick("mouse.right", 1);
	public static MouseClick MIDDLE = new MouseClick("mouse.middle", 2);

}
