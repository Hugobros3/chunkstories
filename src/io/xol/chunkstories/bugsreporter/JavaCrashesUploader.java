package io.xol.chunkstories.bugsreporter;

import java.io.File;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A straightforward class that look for hs_id_err files, uploads them and then moves the files to logs/ */
public class JavaCrashesUploader extends Thread
{
	@Override
	public void run()
	{
		File folder = new File(GameDirectory.getGameFolderPath());

		if (!folder.exists() || !folder.isDirectory())
		{
			ChunkStoriesLogger.getInstance().log("JavaCrashesUploader: .chunkstories unfit", LogLevel.CRITICAL);
		}
		else
		{
			//Carry on
			ChunkStoriesLogger.getInstance().log("JavaCrashesUploader: Looking for java crashes dumps", LogLevel.INFO);
			for (File file : folder.listFiles())
			{
				if (!file.isDirectory() && file.getName().startsWith("hs_err_pid"))
				{
					System.out.println("We've got ourselves a client !");

					try
					{
						ChunkStoriesLogger.getInstance().log("JavaCrashesUploader: Found crashfile " + file.getName() + ", uploading (30s max)", LogLevel.INFO);
						ReportThread reportThread = new ReportThread("crash-report-found-" + Client.username, file);
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
