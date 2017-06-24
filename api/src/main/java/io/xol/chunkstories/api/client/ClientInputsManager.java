package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.input.Mouse;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInputsManager extends InputsManager
{
	public boolean onInputPressed(Input input);

	public boolean onInputReleased(Input input);
	
	public Mouse getMouse();
	
	/*public int getMouseCursorX();
	
	public int getMouseCursorY();
	
	public void setMouseCursorLocation(int x, int y);*/
}
