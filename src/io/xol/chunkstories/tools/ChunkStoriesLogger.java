package io.xol.chunkstories.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import io.xol.chunkstories.client.Client;

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
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run()
			{
				System.out.println("lel");
				ChunkStoriesLogger.getInstance().close();
			}
		});
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

	class SendReportThread extends Thread {

		File logFile;
		
		public SendReportThread(File logFile)
		{
			this.logFile = logFile;
		}

		public void run()
		{
			String url = "http://chunkstories.xyz/debug/upload.php";
			String charset = "UTF-8";
			try
			{
				URLConnection la_vie = new URL(url).openConnection();

				//Thx stackoverflow *lenny face*
				String bound = Long.toHexString(System.currentTimeMillis());
				la_vie.setDoOutput(true);
				la_vie.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + bound);
				
				OutputStream os = la_vie.getOutputStream();
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, charset));
				
				//Parameter
				pw.append("--" + bound + "\r\n");
				pw.append("Content-Disposition: form-data; name=\"username\"" + "\r\n");
				pw.append("Content-Type: text/plain; charset=" + charset + "\r\n");
				pw.append("\r\n" + Client.username + "\r\n");
				pw.flush();
				
				//Binary file upload
				pw.append("--" + bound + "\r\n");
				pw.append("Content-Disposition: form-data; name=\"userfile\"; filename=\"" + logFile.getName() + "\"" + "\r\n");
				pw.append("Content-Type: " + URLConnection.guessContentTypeFromName(logFile.getName()) + "\r\n");
				pw.append("Content-Transfer-Encoding: binary" + "\r\n" + "\r\n");
				pw.flush();
				System.out.println("Sending log report : " +Files.copy(logFile.toPath(), os) + "bytes uploaded");
				os.flush();
				pw.append("\r\n");
				//End
				pw.append("--" + bound + "--\r\n");
				pw.flush();
				
				System.out.println(((HttpURLConnection) la_vie).getResponseCode()+"code"+((HttpURLConnection) la_vie).getResponseMessage());
			}
			catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Once it's done reporting
		}
	}
	
	SendReportThread logReportThread;
	
	public void close()
	{
		try
		{
			info("Successfully written log");
			fileWriter.close();
			// Report whatever happened
			logReportThread = new SendReportThread(logFile);
			logReportThread.run();
			//System.exit(0);
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
