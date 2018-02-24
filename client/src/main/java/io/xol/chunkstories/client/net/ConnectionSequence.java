//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.vanillasockets.TCPServerConnection;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.engine.misc.ConnectionStep;
import io.xol.engine.net.SimplePostRequest;

/** The job of the ConnectionSequence is to execute the required steps to login in a server, while monitoring back progress to the main thread */
public class ConnectionSequence extends Thread
{
	ServerConnection connection;
	boolean isDone = false;
	
	ConnectionStep status;
	
	private Semaphore authSemaphore = new Semaphore(0);
	
	private String modsString;
	private Semaphore modsSemaphore = new Semaphore(0);

	private Semaphore translatorSemaphore = new Semaphore(0);
	
	private Semaphore worldSemaphore = new Semaphore(0);
	
	private String aborted;
	
	private static final Logger logger = LoggerFactory.getLogger("net");
	
	public ConnectionSequence(String ip, int port)
	{
		this.connection = new TCPServerConnection(Client.getInstance(), ip, port) {

			@Override
			public boolean handleSystemRequest(String msg) {
				if(msg.startsWith("info/mods:")) {
					modsString = msg.substring(10, msg.length());
					modsSemaphore.release();
					return true;
				} else if (msg.equals("login/ok")) {
					authSemaphore.release();
					return true;
				} else if (msg.equals("world/ok")) {
					worldSemaphore.release();
					return true;
				} else if (msg.equals("world/translator_ok")) {
					translatorSemaphore.release();
					return true;
				}
				return super.handleSystemRequest(msg);
			}
			
		};
		
		this.status = new ConnectionStep("Establishing connection to "+ip+":"+port);
		this.start();
	}
	
	@Override
	public void run()
	{
		logger.info("Connection sequence initialized.");
		try {
			if(!connection.connect())
				abort("Failed to establish connection");
			
			if (Client.offline) {
				connection.sendTextMessage("login/start");
				connection.sendTextMessage("login/username:" + Client.username);
				connection.sendTextMessage("login/logintoken:nopenopenopenopenope");
				connection.sendTextMessage("login/version:"+VersionInfo.networkProtocolVersion);
				connection.sendTextMessage("login/confirm");
				
				status = new ConnectionStep("Offline-mode enabled, skipping login token phase");
			} else {
				status = new ConnectionStep("Requesting a login token...");
				SimplePostRequest spr = new SimplePostRequest("https://chunkstories.xyz/api/serverTokenObtainer.php", "username=" + Client.username + "&sessid=" + Client.session_key);
				String reply = spr.result();
				if(reply != null && reply.startsWith("ok")) {
					String loginToken = reply.split(":")[1];
					
					connection.sendTextMessage("login/start");
					connection.sendTextMessage("login/username:" + Client.username);
					connection.sendTextMessage("login/logintoken:" + loginToken);
					connection.sendTextMessage("login/version:"+VersionInfo.networkProtocolVersion);
					connection.sendTextMessage("login/confirm");
					
					status = new ConnectionStep("Token obtained, logging in...");
				} else {
					abort("Failed to obtain a login token from the servers");
				}
			}
			if(!authSemaphore.tryAcquire(5, TimeUnit.SECONDS))
				abort("Server login timed out");
			
			//Obtain the mods list, check if we have them enabled
			this.status = new ConnectionStep("Asking server required mods...");
			connection.sendTextMessage("mods");
			if(!modsSemaphore.tryAcquire(5, TimeUnit.SECONDS))
				abort("Failed to obtain mods list from server");
			
			Set<String> requiredMd5s = new HashSet<String>();
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
					status = connection.obtainModFile(modMd5Hash, cached);
					status.waitForEnd();
				}
				
				//Check their size and signature
				if(cached.length() != modSizeInBytes) {
					Client.getInstance().logger().info("Invalid filesize for downloaded mod "+modInternalName + " (hash: " + modMd5Hash + ")" + " expected filesize = " + modSizeInBytes + " != actual filesize = "+cached.length());
					cached.delete(); //Delete suspicious file
					abort("Failed to download " + modInternalName + ", wrong file size. You can try again.");
				}
	
				//Test if the mod loads
				ModZip testHash = null;
				try {
					testHash = new ModZip(cached);
				} catch (ModLoadFailureException e) {
					e.printStackTrace();
					
					Client.getInstance().logger().info("Could not load downloaded mod "+modInternalName + " (hash: " + modMd5Hash + "), see stack trace");
					cached.delete(); //Delete suspicious file
					abort("Failed to load " + modInternalName + ", check error log.");
				}
				
				//Test the md5 hash wasn't tampered with
				String actualMd5Hash = testHash.getMD5Hash();
				if(!actualMd5Hash.equals(modMd5Hash)) {
					Client.getInstance().logger().info("Invalid md5 hash for mod "+modInternalName + " expected md5 hash = " + modMd5Hash + " != actual md5 hash = "+actualMd5Hash);
					cached.delete(); //Delete suspicious file
					abort("Mod " + modInternalName + " hash did not match.");
				}
			}
			
			// Build the string to pass to the modsManager as to ask it to enable said mods
			String[] requiredMods = new String[requiredMd5s.size()];
			int i = 0;
			for (String m : requiredMd5s) {
				requiredMods[i++] = "md5:" + m;
			}
			Client.getInstance().getContent().modsManager().setEnabledMods(requiredMods);
			
			status = new ConnectionStep("Reloading mods...");
			Client.getInstance().reloadAssets();

			status = new ConnectionStep("Loading ContentTranslator...");
			connection.sendTextMessage("world/translator");
			if(!translatorSemaphore.tryAcquire(5, TimeUnit.SECONDS))
				abort("Timed out waiting for content translator");
			
			//Ask the server world info and if allowed where to spawn and preload chunks
			connection.sendTextMessage("world/enter");
			if(!worldSemaphore.tryAcquire(15, TimeUnit.SECONDS))
				abort("Timed out waiting for world");
			
			status = new ConnectionStep("Loading world...");
			
			//Ask the server to eventually spawn the player entity
			//TODO
			synchronized (this) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//We are good.
			isDone = true;
		} catch (AbortException e) {
			logger.info("Connection sequence aborted.");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private void abort(String string) throws AbortException {
		this.connection.close();
		this.status = new ConnectionStep(string);
		this.aborted = string;
		throw new AbortException();
	}
	
	@SuppressWarnings("serial")
	class AbortException extends Exception {
		
	}
	
	public void authCallback() {
		this.authSemaphore.release();
	}
	
	public void modsCallback(String string) {
		this.modsString = string;
		this.modsSemaphore.release();
	}
	
	public void worldCallback() {
		this.worldSemaphore.release();
	}
	
	public boolean isDone() {
		return isDone;
	}

	public ConnectionStep getStatus() {
		return status;
	}

	public String wasAborted() {
		return this.aborted;
	}
}
