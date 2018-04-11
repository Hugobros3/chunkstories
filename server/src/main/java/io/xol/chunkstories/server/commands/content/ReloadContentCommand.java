//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.content;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;

public class ReloadContentCommand implements CommandHandler{

	final GameContext gameContext;
	
	public ReloadContentCommand(GameContext gameContext) {
		this.gameContext = gameContext;
		gameContext.getPluginManager().registerCommand("reload").setHandler(this);
	}
	
	//final String[] serverHotReloadableStuff = {};
	//final String[] clientHotReloadableStuff = {"shaders", "textures"};

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("reload") && emitter.hasPermission("content.reload"))
		{
			if(arguments.length == 0) {
				emitter.sendMessage("#00FFD0" + "Usage for /reload: ");
				emitter.sendMessage("#00FFD0" + "/reload all #FFFFD0| Reloads everything (might break stuff!)");
				emitter.sendMessage("#00FFD0" + "/reload assets #FFFFD0| Reloads non-code assets without rebuilding the filesystem ( for iteration )");
				emitter.sendMessage("#00FFD0" + "/reload plugins #FFFFD0| Reloads only the plugins ( disables them, loads them anew then re-enables them )");
				emitter.sendMessage("#00FFD0" + "/reload <subsystem> #FFFFD0| Reloads one of the specific hot-reloadable subsystems shown below");
				//TODO show those subsystems

			} else {
				if(arguments[0].equals("all")) {
					gameContext.getContent().reload();
					emitter.sendMessage("#00FFD0" + "Reloaded everything.");
				}
				else if(arguments[0].equals("shaders")) {
					reloadShaders(emitter);
				}
				else if(arguments[0].equals("textures")) {
					reloadTextures(emitter);
				}
				else if(arguments[0].equals("meshes")) {
					reloadMeshes(emitter);
				}
				else if(arguments[0].equals("animations")) {
					reloadAnimations(emitter);
				}
				else if(arguments[0].equals("assets")) {
					reloadShaders(emitter);
					reloadTextures(emitter);
					reloadMeshes(emitter);
					reloadAnimations(emitter);
				}
				//TODO plugins
			}
			return true;
		}
		return false;
	}

	private void reloadShaders(CommandEmitter emitter) {
		if(this.gameContext instanceof ClientInterface) {
			ClientInterface client = (ClientInterface)gameContext;
			client.getContent().shaders().reloadAll();
			emitter.sendMessage("#00FFD0" + "Reloaded shaders.");
		}
	}
	
	private void reloadTextures(CommandEmitter emitter) {
		if(this.gameContext instanceof ClientInterface) {
			ClientInterface client = (ClientInterface)gameContext;
			client.getContent().textures().reloadAll();
			emitter.sendMessage("#00FFD0" + "Reloaded textures.");
		}
	}
	
	private void reloadMeshes(CommandEmitter emitter) {
		if(this.gameContext instanceof ClientInterface) {
			ClientInterface client = (ClientInterface)gameContext;
			client.getContent().meshes().reloadAll();
			emitter.sendMessage("#00FFD0" + "Reloaded meshes.");
		}
	}
	
	private void reloadAnimations(CommandEmitter emitter) {
		if(this.gameContext instanceof ClientInterface) {
			ClientInterface client = (ClientInterface)gameContext;
			client.getContent().getAnimationsLibrary().reloadAll();
			emitter.sendMessage("#00FFD0" + "Reloaded animations.");
		}
	}
}
