package io.xol.chunkstories.client.net;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.Mods;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientSideConnectionSequence extends Thread implements HttpRequester
{
	ClientToServerConnection connection;
	boolean isDone = false;
	
	String status = "Establishing connection";
	
	public ClientSideConnectionSequence(String ip, int port)
	{
		this.connection = new ClientToServerConnection(this, ip, port);
		this.start();
	}
	
	@Override
	public void run()
	{
		//Obtain a login token and send it to the server
		login();
		connection.getAuthentificationFence().traverse();
		//Obtain the mods list, check if we have them enabled
		
		status = "Asking server required mods...";
		String modsString = connection.obtainModsString();
		Set<String> requiredMd5s = new HashSet<String>();
		Set<String> toDownload = new HashSet<String>();
		
		for(String requiredMod : modsString.split(";"))
		{
			if(requiredMod.startsWith("md5:"))
			{
				requiredMod = requiredMod.substring(4, requiredMod.length());
				String md5Required = requiredMod.contains(":") ? requiredMod.split(":")[0] : requiredMod;
				System.out.println("Mod with md5="+md5Required+" required.");
				
				requiredMd5s.add(md5Required);
				
				File cached = new File(GameDirectory.getGameFolderPath()+"/servermods/"+md5Required+".zip");
				if(!cached.exists())
					//Gib me
					toDownload.add(md5Required);
			}
			//TODO handle ? or define it in spec
			
		}
		//if not check we have them downloaded
		for(String md5Required : toDownload)
		{
			status = "Downloading mod "+md5Required;
			connection.obtainModFile(md5Required, new File(GameDirectory.getGameFolderPath()+"/servermods/"+md5Required+".zip"));
		}
		//Now enable all this
		String[] requiredMods = new String[requiredMd5s.size()];
		int i = 0;
		for(String m : requiredMd5s)
		{
			requiredMods[i++] = "md5:"+m;
		}
		Mods.setEnabledMods(requiredMods);
		status = "Reloading mods...";
		try
		{
			Thread.sleep(150);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Client.getInstance().reloadAssets();
		
		//Ask the server world info and if allowed where to spawn and preload chunks
		connection.sendTextMessage("world/info");
		connection.flush();
		status = "Loading world...";
		
		//Ask the server to eventually spawn the player entity
		//TODO
		
		synchronized(this)
		{
			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//We are good.
		isDone = true;
	}

	// auth
	private void login()
	{
		if (Client.offline)
		{
			// If online-mode, send dummy info
			connection.sendTextMessage("login/start");

			connection.sendTextMessage("login/username:" + Client.username);
			connection.sendTextMessage("login/logintoken:nopenopenopenopenope");
			connection.sendTextMessage("login/version:"+VersionInfo.networkProtocolVersion);
			connection.sendTextMessage("login/confirm");
			connection.flush();
			
			status = "Offline-mode enabled, skipping login token phase";
		}
		else
		{
			// Before sending the server the info it needs for authentification
			// we need a valid login token so
			// we fire up a http request to grab one
			status = "Requesting a login token...";
			HttpRequestThread httpRequest = new HttpRequestThread(this, "token", "http://chunkstories.xyz/api/serverTokenObtainer.php", "username=" + Client.username + "&sessid=" + Client.session_key);
			httpRequest.waitUntilTermination();
		}
	}

	@Override
	public void handleHttpRequest(String info, String result)
	{
		// System.out.println("Request "+info+" got answered: "+result);
		if (info.equals("token"))
		{
			if (result.startsWith("ok"))
			{
				String token = result.split(":")[1];

				connection.sendTextMessage("login/start");

				connection.sendTextMessage("login/username:" + Client.username);
				connection.sendTextMessage("login/logintoken:" + token);
				connection.sendTextMessage("login/version:"+VersionInfo.networkProtocolVersion);
				connection.sendTextMessage("login/confirm");
				
				status = "Token obtained, logging in...";
			}
			else
			{
				status = "Could not obtain token from XolioWare Interactive servers ( " + result + " ).";
				connection.close();
			}
		}
	}
	
	public boolean isDone()
	{
		return isDone;
	}
	
	public String hasFailed()
	{
		if(this.connection.hasFailed() || !this.connection.isAlive())
			return this.connection.getLatestErrorMessage();
		return null;
	}

	public String getStatus()
	{
		return status;
	}
}
