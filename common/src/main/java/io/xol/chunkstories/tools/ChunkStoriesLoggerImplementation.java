package io.xol.chunkstories.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.fusesource.jansi.AnsiConsole;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;

import static io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType.*;
import static io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel.*;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkStoriesLoggerImplementation implements ChunkStoriesLogger
{
	private static ChunkStoriesLoggerImplementation instance;

	final GameContext gameContext;

	public static ChunkStoriesLoggerImplementation getInstance()
	{
		return instance;
	}

	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	boolean logUploadPolicy = false;

	public ChunkStoriesLoggerImplementation(GameContext gameContext, LogLevel logConsole, LogLevel logFile, File file)
	{
		this.gameContext = gameContext;
		
		//Set static reference
		instance = this;
		
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
				fileWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				log("Successfully started logFile : " + file.getAbsolutePath(), INTERNAL, INFO);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		AnsiConsole.systemInstall();
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				ChunkStoriesLoggerImplementation.getInstance().close();
			}
		});
	}

	private void close()
	{
		info("Successfully written log");
		fileWriter.close();

		try
		{
			if(gameContext instanceof ClientInterface)
			{
				ClientInterface client = (ClientInterface)gameContext;

				if (client.configDeprecated().getProp("log-policy", "undefined").equals("send"))
					Runtime.getRuntime().exec("java -jar logs-reporter.jar " + client.username() + " \"" + logFile.getAbsolutePath() + "\"");
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Report whatever happened

		/*logReportThread = new SendReportThread(logFile);
		logReportThread.run();*/

		//System.exit(0);
	}

	LogLevel logToConsole;
	LogLevel logToFile;
	File logFile;
	PrintWriter fileWriter;

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

		Calendar cal = Calendar.getInstance();
		
		String time = sdf.format(cal.getTime());
		line = "[" + time + "]" + line;

		// ie NONE CRITICAL
		// CRITICAL > NONE
		if (logToConsole.compareTo(level) <= 0)
		{
			System.out.println(line);
		}
		if (logFile != null && logToFile.compareTo(level) <= 0)
		{
			fileWriter.append(line + "\n");
		}
	}

	public void save()
	{
		fileWriter.flush();
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

	public PrintWriter getPrintWriter()
	{
		return fileWriter;
	}
}
