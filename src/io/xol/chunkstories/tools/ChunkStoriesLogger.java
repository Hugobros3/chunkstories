package io.xol.chunkstories.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static io.xol.chunkstories.tools.ChunkStoriesLogger.LogType.*;
import static io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkStoriesLogger
{

	static ChunkStoriesLogger instance;

	public static void init(ChunkStoriesLogger log)
	{
		instance = log;
	}

	public static ChunkStoriesLogger getInstance()
	{
		return instance;
	}

	public ChunkStoriesLogger(LogLevel logConsole, LogLevel logFile, File file)
	{
		logToConsole = logConsole;
		logToFile = logFile;
		if (file != null)
		{
			this.logFile = file;
			try
			{
				file.getParentFile().mkdirs();
				if (!file.exists())
					file.createNewFile();
				fileWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(file), "UTF-8"));
				log("Successfully started logFile : " + file.getAbsolutePath(),
						INTERNAL, INFO);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void close()
	{
		try
		{
			fileWriter.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	LogLevel logToConsole;
	LogLevel logToFile;
	File logFile;
	Writer fileWriter;

	public void log(String text)
	{
		log(text, UNSPECIFIED, INFO);
	}

	public void log(String text, LogLevel level)
	{
		log(text, UNSPECIFIED, level);
	}

	public void log(String text, LogType type, LogLevel level)
	{
		String line = "[" + level.name() + "]";
		if (type != UNSPECIFIED)
			line += "[" + type.name() + "]";
		line += text;

		// ie NONE CRITICAL
		// CRITICAL > NONE
		if (logToConsole.compareTo(level) <= 0)
		{
			System.out.println(line);
		}
		if (logFile != null && logToFile.compareTo(level) <= 0)
		{
			try
			{
				fileWriter.append(line + "\n");
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public enum LogType
	{
		UNSPECIFIED, INTERNAL, RENDERING, GAMEMODE;
	}

	public enum LogLevel
	{
		ALL, INFO, DEBUG, WARN, ERROR, CRITICAL, NONE;
	}

	public void save()
	{
		try
		{
			fileWriter.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void info(String string)
	{
		log(string, UNSPECIFIED, INFO);
	}

	public void warning(String string)
	{
		this.log(string, WARN);
	}
	
	public void error(String string)
	{
		this.log(string, ERROR);
	}
}
