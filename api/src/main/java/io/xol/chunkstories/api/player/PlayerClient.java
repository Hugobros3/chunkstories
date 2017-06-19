package io.xol.chunkstories.api.player;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.rendering.GameWindow;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PlayerClient extends Player
{
	/** Returns the client playing */
	public ClientInterface getClient();
	
	/** Gives access to the input subsystem */
	public ClientInputsManager getInputsManager();
	
	/** @return Is the game GUI in focus or obstructed by other things ? */
	public boolean hasFocus();
	
	/** Returns access to the game window */
	public GameWindow getWindow();
}
