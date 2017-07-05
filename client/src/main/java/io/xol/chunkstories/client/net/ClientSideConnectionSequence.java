package io.xol.chunkstories.client.net;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.engine.misc.ConnectionStep;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientSideConnectionSequence extends Thread implements HttpRequester
{
	ClientConnection connection;
	boolean isDone = false;
	
	ConnectionStep status;
	
	public ClientSideConnectionSequence(String ip, int port)
	{
		this.connection = new ClientConnection(this, ip, port);
		
		this.status = new ConnectionStep("Establishing connection to "+ip+":"+port);
		this.start();
	}
	
	@Override
	public void run()
	{
		//Obtain a login token and send it to the server
		login();
		connection.getAuthentificationFence().traverse();
		//Obtain the mods list, check if we have them enabled
		
		this.status = new ConnectionStep("Asking server required mods...");
		String modsString = connection.obtainModsString();
		Set<String> requiredMd5s = new HashSet<String>();
		//Set<String> toDownload = new HashSet<String>();
		
		for(String requiredMod : modsString.split(";"))
		{
			//if(requiredMod.startsWith("md5:"))
			//requiredMod = requiredMod.substring(4, requiredMod.length());
			
			if(!requiredMod.contains(":"))
				continue;
			
			String[] properties = requiredMod.split(":");
			if(properties.length < 3)
				continue;
			
			String modInternalName = properties[0];
			String modMd5Hash = properties[1];
			long modSizeInBytes = Long.parseLong(properties[2]);
			
			//String md5Required = requiredMod.contains(":") ? requiredMod.split(":")[0] : requiredMod;
			Client.getInstance().logger().info("Server asks for mod "+modInternalName +" ("+modSizeInBytes+" bytes), md5="+modMd5Hash);
			
			requiredMd5s.add(modMd5Hash);
			
			File cached = new File(GameDirectory.getGameFolderPath()+"/servermods/"+modMd5Hash+".zip");
			if(!cached.exists()) {
				//Sequentially download all the mods from the server
				status = connection.obtainModFile(modMd5Hash, cached);//new File(GameDirectory.getGameFolderPath()+"/servermods/"+modMd5Hash+".zip"));
				status.waitForEnd();
			}
			
			//Check their size and signature
			if(cached.length() != modSizeInBytes) {
				Client.getInstance().logger().info("Invalid filesize for downloaded mod "+modInternalName + " (hash: " + modMd5Hash + ")" + " expected filesize = " + modSizeInBytes + " != actual filesize = "+cached.length());
				cached.delete(); //Delete suspicious file
				status = new ConnectionStep("Error loading mod " + modInternalName + " check error log.");
				connection.close();
			}

			//Test if the mod loads
			ModZip testHash = null;
			try {
				testHash = new ModZip(cached);
			} catch (ModLoadFailureException e) {
				e.printStackTrace();
				
				Client.getInstance().logger().info("Could not load downloaded mod "+modInternalName + " (hash: " + modMd5Hash + "), see stack trace");
				cached.delete(); //Delete suspicious file
				status = new ConnectionStep("Error loading mod " + modInternalName + " check error log.");
				connection.close();
			}
			
			//Test the md5 hash wasn't tampered with
			String actualMd5Hash = testHash.getMD5Hash();
			if(!actualMd5Hash.equals(modMd5Hash)) {
				Client.getInstance().logger().info("Invalid md5 hash for mod "+modInternalName + " expected md5 hash = " + modMd5Hash + " != actual md5 hash = "+actualMd5Hash);
				cached.delete(); //Delete suspicious file
				status = new ConnectionStep("Error loading mod " + modInternalName + " check error log.");
				connection.close();
			}
			
		}
		//if not check we have them downloaded
		
		/*for(String md5Required : toDownload)
		{
			//status = "Downloading mod "+md5Required;
			status = connection.obtainModFile(md5Required, new File(GameDirectory.getGameFolderPath()+"/servermods/"+md5Required+".zip"));
			status.waitForEnd();
		}*/
		
		//Now enable all this
		String[] requiredMods = new String[requiredMd5s.size()];
		int i = 0;
		for(String m : requiredMd5s)
		{
			requiredMods[i++] = "md5:"+m;
		}
		Client.getInstance().getContent().modsManager().setEnabledMods(requiredMods);
		/*try
		{
			Thread.sleep(150);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		status = new ConnectionStep("Reloading mods...");
		Client.getInstance().reloadAssets();
		//status = Client.getInstance().reloadAssets();
		//status.waitForEnd();
		
		//Ask the server world info and if allowed where to spawn and preload chunks
		connection.sendTextMessage("world/info");
		connection.flush();
		
		//status = "Loading world...";
		status = new ConnectionStep("Loading world...");
		
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
			
			//status = "Offline-mode enabled, skipping login token phase";
			status = new ConnectionStep("Offline-mode enabled, skipping login token phase");
		}
		else
		{
			// Before sending the server the info it needs for authentification
			// we need a valid login token so
			// we fire up a http request to grab one
			//status = "Requesting a login token...";
			status = new ConnectionStep("Requesting a login token...");
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
				
				//status = "Token obtained, logging in...";
				status = new ConnectionStep("Token obtained, logging in...");
			}
			else
			{
				//status = "Could not obtain token from XolioWare Interactive servers ( " + result + " ).";
				status = new ConnectionStep("Could not obtain token from XolioWare Interactive servers ( " + result + " ).");
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

	public ConnectionStep getStatus()
	{
		return status;
	}
}
