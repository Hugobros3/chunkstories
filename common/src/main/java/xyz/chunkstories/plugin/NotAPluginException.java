//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.plugin;

import java.io.File;

import xyz.chunkstories.api.exceptions.plugins.PluginLoadException;

public class NotAPluginException extends PluginLoadException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 67219582600393761L;

	public NotAPluginException(File file) {
	}

}
