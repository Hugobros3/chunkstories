package io.xol.chunkstories.api.entity;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.item.ItemPile;

public interface EntityInventory
{

	ItemPile getItemPileAt(int x, int y);

	boolean canPlaceItemAt(int x, int y, ItemPile pile);

	/**
	 * Tries to place an item at that location, it returns the argument 'pile' if it can't place it.
	 */
	ItemPile placeItemPileAt(int x, int y, ItemPile pile);

	/**
	 * Tries to replace the pile in the inventory with another pile
	 * The failure condition is that if replacing the pile would cause it to 'overlap' neightbours and to prevent
	 * that the game will not let you do so.
	 * @return true if it succeeds, false else
	 */
	boolean setItemPileAt(int x, int y, ItemPile pile);

	/**
	 * Try to add a pile to this inventory.
	 * @param pile
	 * @return Null if it succeeds or the input pile if it fails
	 */
	ItemPile addItemPile(ItemPile pile);

	/**
	 * Iterates over every ItemPile
	 */
	Iterator<ItemPile> iterator();
	
	ItemPile[][] getContents();
	
	EntityWithInventory getHolder();
	
	int getWidth();
	
	int getHeight();

	/**
	 * Removes all itempiles in the inventory.
	 */
	void clear();

	/**
	 * Counts the ammount of stuff this inventory contains.
	 * @return
	 */
	int size();

	String getHolderName();

}