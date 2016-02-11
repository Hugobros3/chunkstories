package io.xol.chunkstories.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.inventory.CSFSerializable;
import io.xol.chunkstories.entity.inventory.Inventory;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemPile implements CSFSerializable
{
	public int amount = 1;
	public Item item;

	public Inventory inventory;
	public int x, y;

	public ItemData data = null;

	/**
	 * Creates an item pile of the item type named 'itemName'
	 * 
	 * @param itemName
	 */
	public ItemPile(String itemName)
	{
		this(ItemsList.getItemByName(itemName));
	}

	/**
	 * Creates an item pile of this item
	 * 
	 * @param item
	 */
	public ItemPile(Item item)
	{
		this.item = item;
		this.data = item.getItemData();
		item.onCreate(this);
	}

	/**
	 * Loads an item pile based on the data supplied (for amount and external data)
	 * 
	 * @param item
	 * @param stream
	 * @throws IOException
	 */
	public ItemPile(Item item, DataInputStream stream) throws IOException
	{
		this.item = item;
		this.data = item.getItemData();
		load(stream);
	}

	public ItemPile(Item type, int amount)
	{
		this(type);
		this.amount = amount;
	}

	public String getTextureName()
	{
		return item.getTextureName();
	}

	public Item getItem()
	{
		return item;
	}

	@Override
	public void load(DataInputStream stream) throws IOException
	{
		this.amount = stream.readInt();
		item.load(this, stream);
	}

	@Override
	public void save(DataOutputStream stream) throws IOException
	{
		stream.writeInt(amount);
		item.save(this, stream);
	}

	/**
	 * Try to move an item to another slot
	 * 
	 * @param inventory2
	 *            new slot's inventory
	 * @param x2
	 * @param y2
	 * @return null if successfull, this if not.
	 */
	//@SuppressWarnings("unchecked")
	public <CE extends Entity & EntityControllable> ItemPile moveTo(Inventory inventory2, int x2, int y2)
	{
		//Remove it from where we are removing it from
		if (inventory != null)
			inventory.setItemPileAt(this.x, this.y, null);
		//Moving an item to a null inventory destroys it
		if(inventory2 == null)
			return null;
		if (inventory2.canPlaceItemAt(x2, y2, this))
		{
			ItemPile nextSelection = inventory2.getItem(x2, y2);
			inventory2.setItemPileAt(x2, y2, this);
			//Successfull item move, then notify controller
			//if (inventory != null)
			//	if (this.inventory.holder != null && this.inventory.holder instanceof Entity && this.inventory.holder instanceof EntityControllable && ((EntityControllable) this.inventory.holder).getController() != null)
			//		((EntityControllable) this.inventory.holder).getController().notifyInventoryChange((CE) this.inventory.holder);

			return nextSelection;
		}
		//Put it back if we can't move it
		else if (inventory != null)
			inventory.setItemPileAt(this.x, this.y, this);
		return this;
	}
}
