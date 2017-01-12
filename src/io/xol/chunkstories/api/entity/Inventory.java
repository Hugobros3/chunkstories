package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.item.ItemPile;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Inventory extends Iterable<ItemPile>
{
	/**
	 * How many slots wide is this inventory
	 */
	public int getWidth();
	
	/**
	 * How many slots high is this inventory
	 */
	public int getHeight();
	
	/**
	 * Returns the ItemPile in that position. This functions considers the fact that some items are wider than others, thus checking different positions can
	 * return the same items.
	 */
	public ItemPile getItemPileAt(int x, int y);

	/**
	 * Checks if a spot in the inventory is eligible for placement of an ItemPile.
	 * Takes into account the size of the items, as well as item stacking.
	 */
	public boolean canPlaceItemAt(int x, int y, ItemPile pile);

	/**
	 * Tries to place an item at that location, it returns the argument 'pile' if it can't place it.
	 */
	public ItemPile placeItemPileAt(int x, int y, ItemPile pile);

	/**
	 * Tries to replace the pile in the inventory with another pile
	 * The failure condition is that if replacing the pile would cause it to 'overlap' neightbours and to prevent
	 * that the game will not let you do so.
	 * @return true if it succeeds, false else
	 */
	public boolean setItemPileAt(int x, int y, ItemPile pile);

	/**
	 * Try to add a pile to this inventory.
	 * @param pile
	 * @return Null if it succeeds or the input pile if it fails
	 */
	public ItemPile addItemPile(ItemPile pile);

	/**
	 * Iterates over every ItemPile
	 */
	public IterableIterator<ItemPile> iterator();
	
	public EntityWithInventory getHolder();

	/**
	 * Removes all ItemPiles in the inventory.
	 */
	public void clear();

	/**
	 * Counts the amount of stuff this inventory contains.
	 */
	public int size();

	public String getHolderName();

	/** Marks said slot as updated */
	public void refreshItemSlot(int x, int y);

}