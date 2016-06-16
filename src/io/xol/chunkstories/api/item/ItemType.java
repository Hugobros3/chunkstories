package io.xol.chunkstories.api.item;

//(c) 2015-2016 XolioWare Interactive
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
	int getID();
	
	String getInternalName();

	int getSlotsWidth();

	int getSlotsHeight();

	int getMaxStackSize();

	Item newItem();
}