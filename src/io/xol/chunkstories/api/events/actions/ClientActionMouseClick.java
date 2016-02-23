package io.xol.chunkstories.api.events.actions;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientActionMouseClick implements ClientAction
{
	private MouseButton mouseButtonPressed;
	
	public ClientActionMouseClick(MouseButton button)
	{
		mouseButtonPressed = button;
	}
	
	public MouseButton getMouseButtonPressed()
	{
		return mouseButtonPressed;
	}
	
	public enum MouseButton {
		MOUSE_LEFT,
		MOUSE_MIDDLE,
		MOUSE_RIGHT;
	}
}
