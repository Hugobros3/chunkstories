package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;

import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.content.PluginsManager;

import io.xol.chunkstories.world.WorldClientCommon;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInterface
{
	public ClientSideController getClientSideController();
	
	public SoundManager getSoundManager();
	
	public PluginsManager getPluginsManager();

	public InputsManager getInputsManager();

	/** Prints some text into the client chat */
	public void printChat(String textToPrint);
	
	public void changeWorld(WorldClientCommon world);
	
	public void exitToMainMenu();
	
	/**
	 * Opens the inventory GUI with the controlled entity's inventory, if applicable
	 * @param otherInventory If not null, opens this other inventory as well
	 */
	public void openInventory(Inventory otherInventory);

	/**
	 * @return Is the game GUI in focus or obstructed by other things ?
	 */
	public boolean hasFocus();
	
	/**
	 * Reloads all assets, shaders, sounds and whatnot from the mods and the main game.
	 */
	public void reloadAssets();
	
	public ClientToServerConnection getServerConnection();

	ParticlesManager getParticlesManager();

	DecalsManager getDecalsManager();
}
