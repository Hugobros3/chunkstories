package io.xol.chunkstories.api.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.world.WorldAuthority;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Item
{
	private final ItemType type;
	
	public Item(ItemType type)
	{
		this.type = type;
	}
	
	public ItemType getType()
	{
		return type;
	}
	
	public String getName()
	{
		return type.getInternalName();
	}
	
	/** Returns null by default, you can have custom Item renderers just by returning an Item renderer here. */
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer)
	{
		// return new MyFancyCustomRenderer(fallbackRenderer);
		return null;
	}
	
	/**
	 * Should be called when the owner has this item selected
	 * @param owner
	 */
	public void tickInHand(WorldAuthority authority, Entity owner, ItemPile itemPile)
	{
		
	}
	
	/**
	 * Handles some input from the user
	 * @param user
	 * @param pile
	 * @param input
	 * @return false if the item doesn't handle the input, true if it does
	 */
	public boolean handleInteraction(Entity owner, ItemPile itemPile, Input input, Controller controller)
	{
		return false;
	}
	
	/**
	 * Use : determine if two stacks can be merged together, should be overriden when items have extra info.
	 * @return Returns true if the two items are similar and can share a stack without loosing information.
	 */
	public boolean canMergeWith(Item item)
	{
		return type.equals(item.getType());
	}
	
	/**
	 * For Items not implementing a custom renderer, it just shows a dull icon and thus require an icon texture.
	 * @return The full path to the image file.
	 */
	public String getTextureName(ItemPile pile)
	{
		return "./items/icons/"+getInternalName()+".png";
	}
	
	/** Returns the assignated ID for this item. */
	public final int getID()
	{
		return type.getID();
	}
	
	public String getInternalName()
	{
		return type.getInternalName();
	}

	/** Unsafe, called upon loading this item from a stream. If you do use it, PLEASE ensure you remember how many bytes you read/write and be consistent, else you break the savefile */
	public void load(DataInputStream stream) throws IOException
	{ }
	
	/** See load(). */
	public void save(DataOutputStream stream) throws IOException
	{ }
}
