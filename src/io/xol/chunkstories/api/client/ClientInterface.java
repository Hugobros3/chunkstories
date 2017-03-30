package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.entity.PlayerClient;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldClient;

import io.xol.chunkstories.world.WorldClientCommon;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInterface extends GameContext
{
	public PlayerClient getPlayer();
	
	public GameWindow getGameWindow();

	/** Prints some text into the client chat */
	public void printChat(String textToPrint);
	
	/** Returns the currently played world, if such exist or null */
	public WorldClient getWorld();
	
	/** Changes the game to a new world */
	public void changeWorld(WorldClientCommon world);
	
	/** Closes current world and exits to main menu */
	public void exitToMainMenu();
	
	/** Closes current world and exits to main menu with an error message*/
	public void exitToMainMenu(String errorMessage);
	
	/**
	 * Opens the inventory GUI with all the specified inventories opened
	 */
	public void openInventories(Inventory... inventories);
	
	/**
	 * @return Is the game GUI in focus or obstructed by other things ?
	 */
	public boolean hasFocus();
	
	/**
	 * Reloads all assets, shaders, sounds and whatnot from the mods and the main game.
	 */
	public void reloadAssets();
	
	public SoundManager getSoundManager();

	public ClientInputsManager getInputsManager();

	public ParticlesManager getParticlesManager();

	public DecalsManager getDecalsManager();
}
