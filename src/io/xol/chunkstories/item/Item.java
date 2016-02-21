package io.xol.chunkstories.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Item
{
	private final int id;
	private String internalName;
	
	private int slotsWidth = 1;
	private int slotsHeight = 1;

	private int maxStackSize = 100;

	public Item(int id)
	{
		this.id = id;
	}
	
	public void onCreate(ItemPile pile, String[] info)
	{
		
	}
	
	/**
	 * Called when the user interacts with the item
	 * @param user Must be of type Entity implements EntityControllable ! The entity who made use of this item
	 * @param pile The itemPile
	 * @param action Additional information on the action the user performed : 0 = Left click, 1 = Right click, other values are left for modding purposes
	 */
	public void onUse(Entity user, ItemPile pile, int action)
	{
		
	}

	public ItemData getItemData()
	{
		return null;
	}
	
	/**
	 * This method is used to determine if two items piles can be stacked together in one.
	 * Default behavior only checks the ids.
	 * @param a The first ItemPile
	 * @param b The second ItemPile
	 * @return Wether they are stackable together without loss of information.
	 */
	public boolean comparePiles(ItemPile a, ItemPile b)
	{
		if(a.getItem().getID() == b.getItem().getID())
			return true;
		return false;
	}
	
	/**
	 * For Items not implementing a custom renderer, it just shows a dull icon and thus require an icon texture.
	 * @return The full path to the image file.
	 */
	public String getTextureName(ItemPile pile)
	{
		return "res/items/icons/"+internalName+".png";
	}
	
	/**
	 * Returns the assignated ID for this item.
	 * @return
	 */
	public final int getID()
	{
		return id;
	}
	
	// ----- Begin get/set hell -----
	
	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, horizontally
	 */
	public int getSlotsWidth()
	{
		return slotsWidth;
	}

	public void setSlotsWidth(int slotsWidth)
	{
		this.slotsWidth = slotsWidth;
	}

	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, vertically
	 */
	public int getSlotsHeight()
	{
		return slotsHeight;
	}

	public void setSlotsHeight(int slotsHeight)
	{
		this.slotsHeight = slotsHeight;
	}

	public String getInternalName()
	{
		return internalName;
	}

	protected void setInternalName(String internalName)
	{
		this.internalName = internalName;
	}

	/**
	 * Defines the maximal 'amount' an ItemPile can have of this item.
	 * @return
	 */
	public int getMaxStackSize()
	{
		return maxStackSize;
	}

	public void setMaxStackSize(int maxStackSize)
	{
		this.maxStackSize = maxStackSize;
	}

	/**
	 * Called on loading an ItemPile containing this item, usefull for loading stuff into the itemData of the pile.
	 * @param itemPile
	 * @param stream
	 * @throws IOException
	 */
	public void load(ItemPile itemPile, DataInputStream stream) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * See load()
	 * @param itemPile
	 * @param stream
	 * @throws IOException
	 */
	public void save(ItemPile itemPile, DataOutputStream stream) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	// ----- End get/set hell -----
}
