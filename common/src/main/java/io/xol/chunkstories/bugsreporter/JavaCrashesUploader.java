//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.bugsreporter;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.content.GameDirectory;

/** A straightforward class that look for hs_id_err files, uploads them and then moves the files to logs/ */
public class JavaCrashesUploader extends Thread
{
	private final GameContext context;
	
	public JavaCrashesUploader(GameContext context) {
		this.context = context;
	}
	
	private static final Logger logger = LoggerFactory.getLogger("crash_uploader");
	public Logger logger() {
		return logger;
	}
	
	@Override
	public void run()
	{
		File folder = new File(GameDirectory.getGameFolderPath());

		if (!folder.exists() || !folder.isDirectory())
		{
			logger().error("JavaCrashesUploader: .chunkstories unfit");
		}
		else
		{
			//Carry on
			logger().debug("JavaCrashesUploader: Looking for java crashes dumps");
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
						
						logger().info("JavaCrashesUploader: Found crashfile " + file.getName() + ", uploading (30s max)");
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
