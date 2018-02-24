//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.announcer;

import java.net.Inet4Address;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.engine.net.HttpRequests;



//TODO Use proper way to http stuff instead of this ugly ass hack
/**
 * Small background thread that is tasked with putting and keeping up to date 
 * the server's entry in the global list
 */
public class ServerAnnouncerThread extends Thread
{
	AtomicBoolean run = new AtomicBoolean(true);

	int lolcode = 0;
	public long updatedelay = 0;

	public String srv_name;
	public String srv_desc;

	DedicatedServer server;
	
	public ServerAnnouncerThread(DedicatedServer server)
	{
		this.server = server;
		
		lolcode = server.getServerConfig().getInteger("lolcode", 0);
		if (lolcode == 0L)
		{
			Random rnd = new Random();
			lolcode = rnd.nextInt(Integer.MAX_VALUE);
			server.getServerConfig().setInteger("lolcode", lolcode);
		}
		updatedelay = server.getServerConfig().getLong("update-delay", 10000L);
		String hostname = HttpRequests.sendPost("https://chunkstories.xyz/api/sayMyName.php?host=1", "");
		srv_name = server.getServerConfig().getString("server-name", "unnamedserver@" + hostname);
		srv_desc = server.getServerConfig().getString("server-desc", "Default description.");
		setName("Multiverse thread");
	}

	public void stopAnnouncer()
	{
		run.set(false);
	}

	@Override
	public void run()
	{
		try
		{
			String internalIp = Inet4Address.getLocalHost().getHostAddress();
			String externalIp = HttpRequests.sendPost("httpss://chunkstories.xyz/api/sayMyName.php?ip=1", "");
			
			while (run.get())
			{
				// System.out.println("Updating server data on Multiverse.");
				if (server.getServerConfig().getString("enable-multiverse", "false").equals("true"))
				{
					HttpRequests.sendPost("https://chunkstories.xyz/api/serverAnnounce.php", "srvname=" + srv_name + "&desc=" + srv_desc + "&ip=" + externalIp + "&iip=" + internalIp + "&mu=" + server.getHandler().getMaxClients() + "&u="
							+ server.getHandler().getPlayersNumber() + "&n=0&w=default&p=1&v=" + VersionInfo.version + "&lolcode=" + lolcode);
					sleep(updatedelay);
				}
				else
					sleep(6000);
			}
		}
		catch (Exception e)
		{
			server.getLogger().error("An unexpected error happened during multiverse stuff. More info below.");
			e.printStackTrace();
		}
	}
}
