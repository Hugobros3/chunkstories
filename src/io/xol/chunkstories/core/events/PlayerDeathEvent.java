package io.xol.chunkstories.core.events;

import java.io.File;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.server.Server;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerDeathEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners();
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code
	
	final Player player;
	String deathMessage;

	public PlayerDeathEvent(Player player)
	{
		this.player = player;
	}
	
	public String getDeathMessage()
	{
		return deathMessage;
	}

	public void setDeathMessage(String deathMessage)
	{
		this.deathMessage = deathMessage;
	}

	@Override
	public void defaultBehaviour()
	{
		//When a player dies, delete his save
		File playerSavefile = new File("./players/" + player.getName().toLowerCase() + ".csf");
		if(playerSavefile.exists())
		{
			System.out.println("Removing player file as he died :");
			playerSavefile.delete();
		}
		
		//TODO have a proper clean way of doing this
		if(deathMessage != null)
		{
			Server.getInstance().broadcastMessage(deathMessage);
		}
	}

	public Player getPlayer()
	{
		return player;
	}
}
