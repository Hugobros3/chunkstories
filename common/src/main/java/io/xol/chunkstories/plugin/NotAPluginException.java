//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.plugin;

import java.io.File;

import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException;

public class NotAPluginException extends PluginLoadException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 67219582600393761L;

	public NotAPluginException(File file)
	{
	}

}
