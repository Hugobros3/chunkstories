//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.plugin.PluginManager;
import xyz.chunkstories.api.workers.Tasks;

public class TestGameContext implements GameContext {

	private Logger logger;
	private GameContentStore content;

	public TestGameContext(String mods) {
		logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		String coreContentLocation = System.getProperty("coreContentLocation", "../chunkstories-core/res/");

		content = new GameContentStore(this, new File(coreContentLocation), mods);
		content.reload();
	}

	@Override
	public Content getContent() {
		return content;
	}

	@Override
	public PluginManager getPluginManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Tasks getTasks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void print(String message) {
		logger.info(message);
	}

	@Override
	public Logger logger() {
		return logger;
	}

}
