package io.xol.chunkstories.api.item;

import io.xol.chunkstories.api.Content;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Immutable, describes an item type and is a common reference in all items of that type
 * It gets loaded from the .items file
 */
public interface ItemType
{
	/**
	 * @return Returns the associated ID in the .items files
	 */
	public int getID();
	
	/**
	 * @return The name this item is declared by
	 */
	public String getInternalName();
	
	public Content.ItemsTypes store();

	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, horizontally
	 */
	public int getSlotsWidth();

	/**
	 * Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, vertically
	 */
	public int getSlotsHeight();

	/**
	 * Defines the maximal 'amount' an ItemPile can have of this item.
	 * @return
	 */
	public int getMaxStackSize();
	
	public String resolveProperty(String propertyName);
	
	/**
	 * @param propertyName Name of the property to look for
	 * @param defaultValue The value to return if the above isn't present
	 * @return Arbitrary properties defined in .items files
	 */
	public String resolveProperty(String propertyName, String defaultValue);

	public Item newItem();
}