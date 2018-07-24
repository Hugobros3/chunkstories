//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.workers.Tasks;

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
	public Tasks tasks() {
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
