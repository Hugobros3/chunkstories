package io.xol.chunkstories.item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.entity.core.components.EntityComponentInventory;
import io.xol.chunkstories.item.inventory.CSFSerializable;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemPile implements CSFSerializable
{
	public int amount = 1;
	public Item item;

	public EntityComponentInventory inventory;
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

	public ItemPile(String itemName, String[] info)
	{
		this(ItemsList.getItemByName(itemName), info);
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
		item.onCreate(this, null);
	}

	public ItemPile(Item item, int amount)
	{
		this(item);
		this.amount = amount;
	}

	public ItemPile(Item item, String[] info)
	{
		this.item = item;
		this.data = item.getItemData();
		item.onCreate(this, info);
	}

	/**
	 * For items that require special arguments, you can call setInfo on them to apply onCreate once more with proper arguments
	 * 
	 * @param info
	 * @return
	 */
	public ItemPile setInfo(String[] info)
	{
		item.onCreate(this, info);
		return this;
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
		loadCSF(stream);
	}

	public String getTextureName()
	{
		return item.getTextureName(this);
	}

	public Item getItem()
	{
		return item;
	}

	@Override
	public void loadCSF(DataInputStream stream) throws IOException
	{
		this.amount = stream.readInt();
		item.load(this, stream);
	}

	@Override
	public void saveCSF(DataOutputStream stream) throws IOException
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
	public ItemPile moveTo(EntityInventory inventory2, int x2, int y2)
	{
		//Remove it from where we are removing it from
		if (inventory != null)
			inventory.setItemPileAt(this.x, this.y, null);
		//Moving an item to a null inventory destroys it
		if (inventory2 == null)
			return null;
		if (inventory2.canPlaceItemAt(x2, y2, this))
		{
			ItemPile nextSelection = inventory2.getItem(x2, y2);
			inventory2.placeItemPileAt(x2, y2, this);
			//Successfull item move, then notify controller
			
			//if (inventory != null)
			//	if (this.inventory.holder != null && this.inventory.holder instanceof Entity && this.inventory.holder instanceof EntityControllable && ((EntityControllable) this.inventory.holder).getController() != null)
			//		((EntityControllable) this.inventory.holder).getController().notifyInventoryChange((CE) this.inventory.holder);

			return nextSelection;
		}
		//Put it back if we can't move it
		else if (inventory != null)
			inventory.placeItemPileAt(this.x, this.y, this);
		return this;
	}

	public ItemPile setAmount(int amount)
	{
		this.amount = amount;
		return this;
	}

	public ItemData getData()
	{
		return data;
	}

	/**
	 * Returns an exact copy of this pile
	 * 
	 * @return
	 */
	public ItemPile duplicate()
	{
		ItemPile pile = new ItemPile(this.item, this.amount);
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		try
		{
			this.saveCSF(new DataOutputStream(data));
			ByteArrayInputStream stream = new ByteArrayInputStream(data.toByteArray());
			pile.loadCSF(new DataInputStream(stream));
		}
		catch (IOException e)
		{
		}
		return pile;
	}
}
