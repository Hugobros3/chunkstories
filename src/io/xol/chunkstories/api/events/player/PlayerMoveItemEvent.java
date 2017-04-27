package io.xol.chunkstories.api.events.player;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Describe a player moving item action on Master */
public class PlayerMoveItemEvent extends CancellableEvent
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(PlayerMoveItemEvent.class);
	
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
	
	private final Player player;
	private final ItemPile pile;
	
	private final Inventory from;
	private final Inventory to;
	
	private final int fromX;
	private final int fromY;
	private final int toX;
	private final int toY;
	
	private final int amount;
	
	public PlayerMoveItemEvent(Player player, ItemPile pile, Inventory from, Inventory to, int fromX, int fromY, int toX, int toY, int amount)
	{
		this.player = player;
		this.pile = pile;
		
		this.from = from;
		this.to = to;
		
		this.fromX = fromX;
		this.fromY = fromY;
		
		this.toX = toX;
		this.toY = toY;
		
		this.amount = amount;
	}

	public ItemPile getPile()
	{
		return pile;
	}

	public Inventory getSourceInventory()
	{
		return from;
	}

	public Inventory getTargetInventory()
	{
		return to;
	}

	public int getFromX()
	{
		return fromX;
	}

	public int getFromY()
	{
		return fromY;
	}

	public int getToX()
	{
		return toX;
	}

	public int getToY()
	{
		return toY;
	}

	public int getAmount()
	{
		return amount;
	}

	public Player getPlayer()
	{
		return player;
	}
	
}
