package io.xol.chunkstories.client.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.client.Client;
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
		
		//if not check we have them downloaded
		
		//if not check if the server can provide them
		
		//And download them
		
		//Ask the server world info and if allowed where to spawn and preload chunks
		connection.sendTextMessage("world/info");
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
		return isDone || !this.connection.isAlive();
	}

	public String getStatus()
	{
		return status;
	}
}
