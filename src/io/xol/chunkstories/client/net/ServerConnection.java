package io.xol.chunkstories.client.net;

import io.xol.engine.misc.HttpRequestThread;
import io.xol.engine.misc.HttpRequester;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.net.packets.Packet;
import io.xol.chunkstories.net.packets.Packet00Text;
import io.xol.chunkstories.net.packets.Packet01WorldInfo;
import io.xol.chunkstories.net.packets.Packet02ChunkCompressedData;
import io.xol.chunkstories.net.packets.Packet03ChunkSummary;
import io.xol.chunkstories.net.packets.Packet04Entity;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.world.WorldClient;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerConnection extends Thread implements HttpRequester
{
	// Network handling class - takes care of connecting to a server and dealing
	// the info with it.

	public String ip = "";
	public int port = 30410;

	private Socket socket;

	public DataInputStream in;
	//private DataOutputStream out;

	// Utility for ping server
	boolean whoisMode = false;

	// Status check
	boolean connected = false;
	public boolean authentificated = false;
	boolean failed = false;
	String latestErrorMessage = "";
	public String connectionStatus = "Establishing connection...";

	// Receiving buffers
	public List<String> chatReceived = new ArrayList<String>();
	public List<String> techReceived = new ArrayList<String>();

	// Code magic here
	boolean die = false;
	boolean dead = false;

	public SendQueue sendQueue;
	
	public ServerConnection(String i, int p)
	{
		ip = i;
		port = p;
		this.setName("Server Connection thread - " + ip);
		connect();
	}

	// Connect on/off

	public boolean connect()
	{
		System.out.println("Connecting to " + ip + ":" + port + ".");
		try
		{
			socket = new Socket(ip, port);
			in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			sendQueue = new SendQueue(out);
			sendQueue.start();
			connectionStatus = "Established, waiting for login token...";
			this.start();
			sendTextMessage("info");
			auth();
			connected = true;
			return true;
		}
		catch (Exception e)
		{
			failed = true;
			latestErrorMessage = "Failed to connect to " + ip + ":" + port + ". (" + e.getClass().getName() + ")";
			System.out.println(latestErrorMessage);
			// e.printStackTrace();
			return false;
		}
	}

	// @SuppressWarnings("deprecation")
	public void close()
	{
		if (dead)
			return;
		dead = true;
		try
		{
			in.close();
			sendQueue.kill();
			socket.close();
			connected = false;
			die = true;
		}
		catch (Exception e)
		{
			System.out.println("Couldn't close connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")");
			// e.printStackTrace();
		}
	}

	// I/O

	public void handleTextPacket(String msg)
	{
		// System.out.println("m:"+msg); //debug
		if (msg.startsWith("chat/"))
		{
			// System.out.println(msg.substring(5, msg.length()));
			chatReceived.add(msg.substring(5, msg.length()));
		}
		if (msg.startsWith("world/"))
		{
			System.out.println("Received a message about the world, but no remote world exists as of now...\nFaulty message : \n" + msg.substring(6, msg.length()));
			// Client.word.handleWorldMessage(msg.substring(6, msg.length()));
		}
		if (msg.startsWith("disconnect/"))
		{
			latestErrorMessage = msg.replace("disconnect/", "");
			failed = true;
			close();
		}
		if (msg.equals("login/ok"))
		{
			authentificated = true;
		}
		else
			techReceived.add(msg);
	}

	public void sendTextMessage(String msg)
	{
		try
		{
			Packet00Text packet = new Packet00Text(true);
			packet.text = msg;
			sendQueue.queue(packet);
		}
		catch (Exception e)
		{
			// close();
			System.out.println("Fatal error while handling connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")");
			e.printStackTrace();
		}
	}

	public void sendPacket(Packet packet)
	{
		try
		{
			sendQueue.queue(packet);
		}
		catch (Exception e)
		{
			// close();
			System.out.println("Fatal error while handling connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")");
			e.printStackTrace();
		}
	}
	
	// accessor

	public synchronized String getLastChatMessage()
	{
		if (chatReceived.size() > 0)
		{
			String m = chatReceived.get(0);
			chatReceived.remove(0);
			return m;
		}
		return null;
	}

	/*
	 * public synchronized String getLastTechMessage() { if (techReceived.size()
	 * > 0) { String m = techReceived.get(0); techReceived.remove(0); return m;
	 * } return null; }
	 */

	// auth
	private void auth()
	{
		if (Client.offline)
		{
			// If online-mode, send dummy info
			sendTextMessage("login/start");

			sendTextMessage("login/username:" + Client.username);
			sendTextMessage("login/logintoken:nopenopenopenopenope");
			// send("login/version:"+VersionInfo.version);
			sendTextMessage("login/confirm");
		}
		else
		{
			// Before sending the server the info it needs for authentification
			// we need a valid login token so
			// we fire up a http request to grab one
			new HttpRequestThread(this, "token", "http://chunkstories.xyz/api/serverTokenObtainer.php", "username=" + Client.username + "&sessid=" + Client.session_key).start();
		}
	}

	// run
	public void run()
	{
		while (!die)
		{
			// Just wait for the goddamn packets to come !
			try
			{
				byte type = in.readByte();
				handlePacket(type, in);
			}
			catch (Exception e)
			{
				if (!die) // If the thread was killed then there is no point
							// handling the error.
				{
					// close();
					failed = true;
					latestErrorMessage = "Fatal error while handling connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")";
					System.out.println(latestErrorMessage);
					close();
					e.printStackTrace();
				}
			}
		}
		System.out.println("Letting thread die as it finished it's job.");
	}

	boolean infoAvaible = false;
	WorldInfo info;

	public WorldInfo getWorldInfo()
	{
		if (infoAvaible)
		{
			infoAvaible = false;
			return info;
		}
		return null;
	}

	private void handlePacket(byte type, DataInputStream in) throws IOException, UnknowPacketException
	{
			if (type == 0x00)
			{
				// UTF-8 text data
				Packet00Text packet = new Packet00Text(true);
				packet.read(in);
				handleTextPacket(packet.text);
			}
			else if (type == 0x01) // WORLD INFO
			{
				// When receiving this packet the games defines a new world
				Packet01WorldInfo packet = new Packet01WorldInfo(true);
				packet.read(in);
				info = packet.info;
				
				Client.world = new WorldClient(info);
				//message = Client.world.name;
				//System.out.println(info.name);
				//loginOk = true;
			}
			else if (type == 0x02)
			{
				//Chunk bits
				Packet02ChunkCompressedData packet = new Packet02ChunkCompressedData(true);
				packet.read(in);
				((IOTasksMultiplayerClient) Client.world.ioHandler).requestChunkCompressedDataProcess(packet);
			}
			else if (type == 0x03)
			{
				//Chunk bits
				Packet03ChunkSummary packet = new Packet03ChunkSummary(true);
				packet.read(in);
				((IOTasksMultiplayerClient) Client.world.ioHandler).requestChunkSummaryProcess(packet);
			}
			else if (type == 0x04)
			{
				Packet04Entity packet = new Packet04Entity(false);
				packet.read(in);
				Entity entity = Client.world.getEntityByUUID(packet.entityID);
				if(packet.deleteFlag)
				{
					System.out.println("Deleting Entity "+entity);
					Client.world.removeEntity(entity);
				}
				else
				{
					if(entity == null)
					{
						entity = EntitiesList.newEntity(Client.world, packet.entityType);
						entity.entityID = packet.entityID;
						packet.applyToEntity(entity, in);
						Client.world.addEntity(entity);
						System.out.println("Added entity "+entity);
						if(packet.defineControl)
						{
							Client.controller = entity;
							//System.out.println("you should control this entity :"+entity);
						}
					}
					else
					{
						packet.applyToEntity(entity, in);
						//System.out.println("Updated Entity "+entity);
					}
				}
				//System.out.println("mdr ");
				//entity = EntitiesList.newEntity(world, entityType);
			}
			else
			{
				throw new UnknowPacketException(type);
			}
	}

	public boolean hasFailed()
	{
		return failed;
	}

	public String getLatestErrorMessage()
	{
		return this.latestErrorMessage;
	}

	public void handleHttpRequest(String info, String result)
	{
		// System.out.println("Request "+info+" got answered: "+result);
		if (info.equals("token"))
		{
			if (result.startsWith("ok"))
			{
				String token = result.split(":")[1];

				sendTextMessage("login/start");

				sendTextMessage("login/username:" + Client.username);
				sendTextMessage("login/logintoken:" + token);
				// send("login/version:"+VersionInfo.version);
				sendTextMessage("login/confirm");
				connectionStatus = "Token obtained, logging in...";
			}
			else
			{
				close();
				failed = true;
				latestErrorMessage = "Could not obtain token from XolioWare Interactive servers ( " + result + " ).";
				System.out.println("no token");
			}
		}
	}
}
