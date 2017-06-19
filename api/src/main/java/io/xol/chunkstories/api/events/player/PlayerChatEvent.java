package io.xol.chunkstories.api.events.player;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.player.Player;

public class PlayerChatEvent extends CancellableEvent
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(PlayerChatEvent.class);
	
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
	final String message;
	String formattedMessage;

	public PlayerChatEvent(Player player, String message)
	{
		this.player = player;
		this.message = message;
		
		this.formattedMessage = player.getDisplayName() + " > " + message;
	}
	
	public String getFormattedMessage()
	{
		return formattedMessage;
	}

	public void setFormattedMessage(String formattedMessage)
	{
		this.formattedMessage = formattedMessage;
	}

	public String getMessage()
	{
		return message;
	}

	public Player getPlayer()
	{
		return player;
	}
}
