package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PlayerClient extends Controller, Player
{
	/** @return Is the game GUI in focus or obstructed by other things ? */
	public boolean hasFocus();
	
	/** Gives access to the input subsystem */
	public ClientInputsManager getInputsManager();
	
	/** Returns access to the game window */
	public GameWindow getWindow();
}
