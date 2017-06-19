package io.xol.chunkstories.api.util;

import java.io.PrintWriter;

public interface ChunkStoriesLogger {

	public void log(String text);

	public void log(String text, LogLevel level);

	public void log(String text, LogType type, LogLevel level);

	/** What are we logging about */
	public enum LogType
	{
		UNSPECIFIED, 
		INTERNAL, 
		RENDERING /** Stuff that has to do with putting pixels on the screen */,
		CLIENT /** Has to do with client logic */,
		SERVER /** Has to do with server logic */,
		@Deprecated
		GAMEMODE /** Deprecated */, 
		CONTENT_LOADING /** Has to do with loading content ( ie: downloading mods, loading item definitions etc ) */,
		PLUGIN /** Inside a plugin or foreign code in general */,
		WORLD_TICKING /** On the main world logic loop */, 
		WORLD_GENERATION /** While generating map data */,
		WORLD_STREAMING /** Streaming, both local and remote */,
		NETWORK /** Other networking */
		;
	}

	public enum LogLevel
	{
		ALL, INFO, DEBUG, WARN, ERROR, CRITICAL, NONE;
	}

	public void info(String string);

	public void warning(String string);

	public void error(String string);

	public PrintWriter getPrintWriter();
}
