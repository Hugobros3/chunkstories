package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.content.PluginsManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInterface
{
	/**
	 * Returns the {@link SoundManager} in use
	 * @return
	 */
	public SoundManager getSoundManager();
	
	public PluginsManager getPluginsManager();

	public InputsManager getInputsManager();

	/**
	 * Prints some text into the client chat
	 * @param textToPrint
	 */
	public void printChat(String textToPrint);
	
	/**
	 * Opens the inventory GUI with the controlled entity's inventory, if applicable
	 * @param otherInventory If not null, opens this other inventory as well
	 */
	public void openInventory(EntityInventory otherInventory);
	
	/**
	 * Reloads all assets, shaders, sounds and whatnot from the mods and the main game.
	 */
	public void reloadAssets();
	
	public ClientToServerConnection getServerConnection();
}
