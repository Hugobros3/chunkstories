//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.bugsreporter;

import java.io.File;

import xyz.chunkstories.api.Engine;
import xyz.chunkstories.api.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A straightforward class that look for hs_id_err files, uploads them and then
 * moves the files to logs/
 */
public class JavaCrashesUploader extends Thread {
	private final Engine context;

	public JavaCrashesUploader(Engine context) {
		this.context = context;
	}

	private static final Logger logger = LoggerFactory.getLogger("crash_uploader");

	public Logger logger() {
		return logger;
	}

	@Override
	public void run() {
		File folder = new File(".");

		if (!folder.exists() || !folder.isDirectory()) {
			logger().error("JavaCrashesUploader: .chunkstories unfit");
		} else {
			// Carry on
			logger().debug("JavaCrashesUploader: Looking for java crashes dumps");
			for (File file : folder.listFiles()) {
				if (!file.isDirectory() && file.getName().startsWith("hs_err_pid")) {
					System.out.println("We've got ourselves a client !");

					try {
						String str = "not-client";
						if (context instanceof Client)
							str = ((Client) context).getUser().getName();

						logger().info(
								"JavaCrashesUploader: Found crashfile " + file.getName() + ", uploading (30s max)");
						ReportThread reportThread = new ReportThread("crash-report-found-" + str, file);
						reportThread.start();

						synchronized (reportThread) {
							reportThread.join(30 * 1000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					file.renameTo(new File( "logs/sent_" + file.getName()));
				}
			}
		}
	}
}
