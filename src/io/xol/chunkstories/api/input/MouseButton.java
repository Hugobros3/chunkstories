package io.xol.chunkstories.api.input;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MouseButton implements Input
{
	String name;
	int button;
	
	private MouseButton(String name, int button)
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
	
	public static MouseButton LEFT = new MouseButton("mouse.left", 0);
	public static MouseButton RIGHT = new MouseButton("mouse.right", 1);
	public static MouseButton MIDDLE = new MouseButton("mouse.middle", 2);
	
	public long getHash()
	{
		return button;
	}

}
