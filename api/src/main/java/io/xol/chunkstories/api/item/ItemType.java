package io.xol.chunkstories.api.item;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.content.NamedWithProperties;
import io.xol.chunkstories.api.item.renderer.ItemRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Immutable, describes an item type and is a common reference in all items of that type
 * It gets loaded from the .items file
 */
public interface ItemType extends NamedWithProperties
{
	/** @return Returns the associated ID in the .items files */
	public int getID();
	
	/** @return The name this item is declared by */
	public String getInternalName();
	
	public Content.ItemsTypes store();

	/** Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, horizontally */
	public int getSlotsWidth();

	/** Items in chunk stories can take up more than one slot.
	 * @return How many slots this items use, vertically */
	public int getSlotsHeight();

	/** Defines the maximal 'amount' an ItemPile can have of this item. */
	public int getMaxStackSize();

	/** Instanciates a new item */
	public Item newItem();

	/** Returns a suitable ItemRenderer for this ItemType. Will return null if called on anything else than a Client. */
	public ItemRenderer getRenderer();
}