//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.system;

import java.util.Iterator;

import xyz.chunkstories.api.content.mods.Mod;
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;
import xyz.chunkstories.util.VersionInfo;

/** Handles /uptime, /info commands */
public class InfoCommands extends ServerCommandBasic {

	public InfoCommands(Server serverConsole) {
		super(serverConsole);

		server.getPluginManager().registerCommand("uptime", this);
		server.getPluginManager().registerCommand("info", this);
		server.getPluginManager().registerCommand("help", this);
		server.getPluginManager().registerCommand("plugins", this);
		server.getPluginManager().registerCommand("mods", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command cmd, String[] arguments) {
		if (cmd.getName().equals("uptime")) {
			emitter.sendMessage("#00FFD0The server has been running for " + server.getUptime() + " seconds.");
			return true;
		} else if (cmd.getName().equals("info")) {
			emitter.sendMessage("#00FFD0The server's ip is " + server.getPublicIp());
			emitter.sendMessage("#00FFD0It's running version " + VersionInfo.version + " of the server software.");
			emitter.sendMessage("#00FFD0" + server.getWorld());
			emitter.sendMessage("#00FFD0" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb used out of "
					+ Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb allocated");
			return true;
		} else if (cmd.getName().equals("help")) {
			emitter.sendMessage("#00FFD0Avaible commands :");
			emitter.sendMessage("#00FFD0" + " /plugins");
			emitter.sendMessage("#00FFD0" + " /mods");
			emitter.sendMessage("#00FFD0" + " /list");
			emitter.sendMessage("#00FFD0" + " /info");
			emitter.sendMessage("#00FFD0" + " /uptime");
			for (Command command : server.getPluginManager().commands()) {
				emitter.sendMessage("#00FFD0 /" + command.getName());
			}
			return true;

		} else if (cmd.getName().equals("plugins")) {
			String list = "";

			Iterator<ChunkStoriesPlugin> i = server.getPluginManager().activePlugins().iterator();
			while (i.hasNext()) {
				ChunkStoriesPlugin plugin = i.next();
				list += plugin.getName() + (i.hasNext() ? ", " : "");
			}

			emitter.sendMessage("#00FFD0" + i + " active server plugins : " + list);
			return true;

		} else if (cmd.getName().equals("mods")) {
			String list = "";
			int i = 0;
			for (Mod csp : server.getContent().modsManager().getCurrentlyLoadedMods()) {
				i++;
				list += csp.getModInfo().getName()
						+ (i == server.getContent().modsManager().getCurrentlyLoadedMods().size() ? "" : ", ");
			}
			emitter.sendMessage("#FF0000" + i + " active server mods : " + list);
			return true;

		}

		System.out.println("fuck off");
		return false;
	}

}
