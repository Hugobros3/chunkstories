package io.xol.chunkstories.bugsreporter;

import java.io.File;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A straightforward class that look for hs_id_err files, uploads them and then moves the files to logs/ */
public class JavaCrashesUploader extends Thread
{
	private final GameContext context;
	
	public JavaCrashesUploader(GameContext context) {
		this.context = context;
	}
	
	@Override
	public void run()
	{
		File folder = new File(GameDirectory.getGameFolderPath());

		if (!folder.exists() || !folder.isDirectory())
		{
			ChunkStoriesLoggerImplementation.getInstance().log("JavaCrashesUploader: .chunkstories unfit", LogLevel.CRITICAL);
		}
		else
		{
			//Carry on
			ChunkStoriesLoggerImplementation.getInstance().log("JavaCrashesUploader: Looking for java crashes dumps", LogLevel.INFO);
			for (File file : folder.listFiles())
			{
				if (!file.isDirectory() && file.getName().startsWith("hs_err_pid"))
				{
					System.out.println("We've got ourselves a client !");

					try
					{
						String str = "not-client";
						if(context instanceof ClientInterface)
							str = ((ClientInterface) context).username();
						
						ChunkStoriesLoggerImplementation.getInstance().log("JavaCrashesUploader: Found crashfile " + file.getName() + ", uploading (30s max)", LogLevel.INFO);
						ReportThread reportThread = new ReportThread("crash-report-found-" + str, file);
						reportThread.start();

						synchronized (reportThread)
						{
							reportThread.join(30 * 1000);
						}
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}

					file.renameTo(new File(GameDirectory.getGameFolderPath() + "/logs/sent_" + file.getName()));
				}
			}
		}
	}
}
