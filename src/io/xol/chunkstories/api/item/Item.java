package io.xol.chunkstories.api.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Item
{
	private final ItemType type;
	
	protected ItemRenderer itemRenderer;

	public Item(ItemType type)
	{
		this.type = type;
		itemRenderer = new DefaultItemRenderer(this);
	}
	
	public ItemType getType()
	{
		return type;
	}
	
	public ItemRenderer getItemRenderer()
	{
		return itemRenderer;
	}
	
	/**
	 * Called on creation of an itemPile of this object (you should override this if you need something in particular in your item data,
	 * and/or if you want to specify subtypes to your item using the info[] tags
	 * @param pile
	 * @param info When spawning an item it parses everything after itemname: and returns an array of strings separated by ':'
	 * ie : myitem:prout:22 infers an String[] info = {"prout", "22"};
	 */
	public void onCreate(ItemPile pile, String[] info)
	{
		
	}
	
	/**
	 * Handles some input from the user
	 * @param user
	 * @param pile
	 * @param input
	 * @return false if the item doesn't handle the input, true if it does
	 */
	public boolean handleInteraction(Entity user, ItemPile pile, Input input)
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
		return "res/items/icons/"+getInternalName()+".png";
	}
	
	/**
	 * Returns the assignated ID for this item.
	 * @return
	 */
	public final int getID()
	{
		return type.getID();
	}
	
	// ----- Begin get/set hell -----
	
	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, horizontally
	 */
	public int getSlotsWidth()
	{
		return type.getSlotsHeight();
	}

	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, vertically
	 */
	public int getSlotsHeight()
	{
		return type.getSlotsHeight();
	}
	
	public String getInternalName()
	{
		return type.getInternalName();
	}

	/**
	 * Defines the maximal 'amount' an ItemPile can have of this item.
	 * @return
	 */
	public int getMaxStackSize()
	{
		return type.getMaxStackSize();
	}

	/**
	 * Called on loading an ItemPile containing this item, usefull for loading stuff into the itemData of the pile.
	 * @param stream
	 * @throws IOException
	 */
	public void load(DataInputStream stream) throws IOException
	{
		
	}
	
	/**
	 * See load()
	 * @param stream
	 * @throws IOException
	 */
	public void save(DataOutputStream stream) throws IOException
	{
		
	}
	// ----- End get/set hell -----
}
