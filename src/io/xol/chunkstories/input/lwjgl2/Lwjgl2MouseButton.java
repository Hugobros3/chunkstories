package io.xol.chunkstories.input.lwjgl2;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.input.Input;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Lwjgl2MouseButton implements Input, LWJGLPollable
{
	private final String name;
	private final int button;
	
	private boolean isDown = false;
	
	public Lwjgl2MouseButton(String name, int button)
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
		return isDown;
	}
	
	public long getHash()
	{
		return button;
	}

	@Override
	public void updateStatus()
	{
		isDown = Mouse.isButtonDown(button);
	}

}
