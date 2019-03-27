//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.converter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import xyz.chunkstories.api.world.WorldSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.converter.MinecraftBlocksTranslator;
import xyz.chunkstories.api.plugin.PluginManager;
import xyz.chunkstories.api.world.WorldInfo;
import xyz.chunkstories.api.world.WorldUser;
import xyz.chunkstories.content.GameContentStore;
import xyz.chunkstories.util.FoldersUtils;
import xyz.chunkstories.util.LogbackSetupHelper;
import xyz.chunkstories.world.WorldLoadingException;
import xyz.chunkstories.world.WorldTool;
import io.xol.enklume.MinecraftWorld;

public abstract class OfflineWorldConverter implements GameContext, WorldUser {

	public static void main(String arguments[]) throws IOException {
		// Parse arguments first
		if (arguments.length < 5) {
			String helpText = "Chunk stories Minecraft map importer(converter) cmd line.\n";

			helpText += "Usage : anvil-export anvilWorldDir csWorldDir <size> <x-start> <z-start> [void-fill] [-vr]\n";
			helpText += "anvilWorldDir is the directory containing the Minecraft level ( the one with level.dat inside )\n";
			helpText += "csWorldDir is the export destination.\n";
			helpText += "Target size for chunk stories world, avaible sizes : " + WorldSize.values()
					+ "\n";
			helpText += "<x-start> and <z-start> are the two coordinates (in mc world) from where we will take the data, "
					+ "going up in the coordinates to fill the world size.\n Exemple : anvil-export mc cs TINY -512 -512 will take the"
					+ "minecraft portion between X:-512 and Z:-512 to fill a 1024x1024 cs level.\n";
			helpText += "void-fill designates how you want the void chunks ( : not generated in minecraft) to be filled. default : air\n";

			helpText += "-v : verbose mode\n";
			helpText += "-r : delete and rewrite destination if already present\n";

			helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.";
			// TODO implement these for real
			helpText += "--mods=xxx,yyy | -mods=* Tells the converter to start with those mods enabled\n";
			helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n";

			System.out.println(helpText);
			return;
		}

		// Modifiers
		boolean verboseMode = false;
		boolean deleteAndRewrite = false;

		int threadCount = -1;

		File coreContentLocation = new File("we.zip");
		for (int i = 5; i < arguments.length; i++) {
			if (arguments[i].startsWith("-")) {
				if (arguments[i].contains("v"))
					verboseMode = true;
				if (arguments[i].contains("r"))
					deleteAndRewrite = true;
			}
			if (arguments[i].startsWith("-mt")) {
				if (arguments[i].startsWith("-mt=")) {
					String coreCounts = arguments[i].substring(4);
					threadCount = Integer.parseInt(coreCounts);
				} else
					threadCount = Runtime.getRuntime().availableProcessors();
			} else if (arguments[i].contains("--core")) {
				String coreContentLocationPath = arguments[i].replace("--core=", "");
				coreContentLocation = new File(coreContentLocationPath);
			}
		}

		String mcWorldName = arguments[0];
		File mcWorldDir = new File(mcWorldName);
		if (!mcWorldDir.exists() || !mcWorldDir.isDirectory()) {
			System.out.println(mcWorldDir + " is not a valid directory.");
			return;
		}

		String csWorldName = arguments[1];
		File csWorldDir = new File("worlds/" + csWorldName + "/");
		if (csWorldDir.exists() && !deleteAndRewrite) {
			System.out.println(
					"Destination world " + csWorldName + " already exists in worlds/" + csWorldName + ", aborting.");
			System.out.println(
					"To force deleting and rewriting of this world, at your risk of loosing data, plese use the -r flag.");
			return;
		} else if (csWorldDir.exists() && deleteAndRewrite) {
			System.out.println("Deleting older world " + csWorldDir);
			FoldersUtils.deleteFolder(csWorldDir);
		}

		WorldSize size = WorldSize.valueOf(arguments[2]);
		if (size == null) {
			System.out.println("Invalid world size. Valid world sizes : " + WorldSize.values());
			return;
		}

		int minecraftOffsetX = Integer.parseInt(arguments[3]);
		int minecraftOffsetZ = Integer.parseInt(arguments[4]);
		if (minecraftOffsetX % 32 != 0 || minecraftOffsetZ % 32 != 0) {
			System.out.println("<x-start> and <z-start> offsets need to be multiples of 32.");
			return;
		}

		// Finally start the conversion
		MultithreadedOfflineWorldConverter converter = new MultithreadedOfflineWorldConverter(verboseMode, mcWorldDir,
				csWorldDir, mcWorldName, csWorldName, size, minecraftOffsetX, minecraftOffsetZ, coreContentLocation,
				threadCount);
		converter.run();
	}

	// TODO Does standard vanilla minecraft even support 512 and more worlds now ?
	// No information to be found on the wiki apparently
	public static final int mcWorldHeight = 256;

	protected final boolean verboseMode;
	protected final GameContentStore content;
	protected Logger logger;

	// TODO make these configurable
	protected final int targetChunksToKeepInRam = 4096;

	protected final MinecraftWorld mcWorld;
	protected final WorldTool csWorld;

	protected final int minecraftOffsetX;
	protected final int minecraftOffsetZ;

	protected final String mcWorldName;

	protected final MinecraftBlocksTranslator mappers;

	public OfflineWorldConverter(boolean verboseMode, File mcFolder, File csFolder, String mcWorldName,
			String csWorldName, WorldSize size, int minecraftOffsetX, int minecraftOffsetZ, File coreContentLocation)
			throws IOException {
		this.verboseMode = verboseMode;
		this.minecraftOffsetX = minecraftOffsetX;
		this.minecraftOffsetZ = minecraftOffsetZ;
		this.mcWorldName = mcWorldName;

		// Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());

		logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		String loggingFilename = "./logs/converter_" + time + ".log";
		new LogbackSetupHelper(loggingFilename);

		content = new GameContentStore(this, coreContentLocation, "");
		content.reload();

		verbose("Loading converter_mapping.txt");
		File file = new File("converter_mapping.txt");
		mappers = new MinecraftBlocksTranslator(this, file);
		verbose("Done, took " + (System.nanoTime() - System.nanoTime()) / 1000 + " �s");

		// Loads the Minecraft World
		mcWorld = new MinecraftWorld(mcFolder);

		String worldGenerator = "blank";

		// String csWorldName = "converted_" + mcWorldName;
		String internalName = csWorldName.replaceAll("[^\\w\\s]", "_");

		String description = "Automatic conversion of the Minecraft map '" + mcWorldName + "'";

		Random random = new Random();

		File folder = new File("out/"+internalName);
		WorldInfo info = new WorldInfo(internalName, csWorldName, description,random.nextLong() + "", size, worldGenerator);

		csWorld = WorldTool.Companion.createWorld(this, folder, info);
	}

	protected void verbose(String s) {
		if (verboseMode) {
			System.out.println(s);
		}
	}

	@Override
	public Content getContent() {
		return content;
	}

	@Override
	public PluginManager getPluginManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void print(String message) {
		logger.info("GameContext:" + message);
	}

	@Override
	public Logger logger() {
		return logger;
	}
}
