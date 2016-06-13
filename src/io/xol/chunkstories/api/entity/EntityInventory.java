package io.xol.chunkstories.api.entity;

import java.util.Iterator;

import io.xol.chunkstories.item.ItemPile;

public interface EntityInventory
{

	ItemPile getItem(int x, int y);

	boolean canPlaceItemAt(int x, int y, ItemPile pile);

	/**
	 * Returns null if the item was put in this inventory, the item if it wasn't
	 * 
	 * @param x
	 * @param y
	 * @param pile
	 * @return
	 */
	ItemPile placeItemPileAt(int x, int y, ItemPile pile);

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