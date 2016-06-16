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
	private int amount = 1;
	public Item item;

	public EntityComponentInventory inventory;
	public int x, y;

	//public ItemData data = null;

	/**
	 * Creates an item pile of the item type named 'itemName'
	 * 
	 * @param itemName
	 */
	public ItemPile(String itemName)
	{
		this(ItemsList.getItemTypeByName(itemName).newItem());
	}

	public ItemPile(String itemName, String[] info)
	{
		this(ItemsList.getItemTypeByName(itemName).newItem(), info);
	}
	
	public ItemPile(Item item)
	{
		this.item = item;
		//this.data = item.getItemData();
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
		//this.data = item.getItemData();
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
		if (inventory != null)
			this.inventory.pushComponentController();
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
		//this.data = item.getItemData();
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
		item.load(stream);
	}

	@Override
	public void saveCSF(DataOutputStream stream) throws IOException
	{
		stream.writeInt(amount);
		item.save(stream);
	}

	/**
	 * Try to move an item to another slot
	 * 
	 * @param destinationInventory
	 *            new slot's inventory
	 * @param destinationX
	 * @param destinationY
	 * @return null if successfull, this if not.
	 */
	//@SuppressWarnings("unchecked")
	public boolean moveItemPileTo(EntityInventory destinationInventory, int destinationX, int destinationY, int amountToTransfer)
	{
		//We duplicate the pile and limit it's amount
		ItemPile pileToSend = this.duplicate();
		pileToSend.setAmount(amountToTransfer);

		//The amount we're not trying to transfer
		int leftAmountBeforeTransaction = this.getAmount() - amountToTransfer;

		ItemPile leftFromTransaction = null;
		//Moving an item to a null inventory would destroy it so it stays nulls
		if (destinationInventory != null)
			leftFromTransaction = destinationInventory.placeItemPileAt(destinationX, destinationY, pileToSend);

		//If something was left from the transaction ( incomplete )
		if (leftFromTransaction != null)
			this.setAmount(leftAmountBeforeTransaction + leftFromTransaction.getAmount());

		//If nothing was left but we only moved part of the stuff
		else if (leftAmountBeforeTransaction > 0)
			this.setAmount(leftAmountBeforeTransaction);

		//If everything was moved we destroy this pile ... if it ever existed ( /dev/null inventories, creative mode etc )
		else if (inventory != null)
			inventory.setItemPileAt(this.x, this.y, null);

		//Success conditions : either we transfered all or we transfered at least one
		return leftFromTransaction == null || leftFromTransaction.getAmount() < amountToTransfer;
	}

	public ItemPile setAmount(int amount)
	{
		this.amount = amount;

		if (inventory != null)
			this.inventory.pushComponentController();

		return this;
	}

	public int getAmount()
	{
		return amount;
	}

	public boolean canMergeWith(ItemPile itemPile)
	{
		return this.getItem().canMergeWith(itemPile.getItem());
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
	
	public String toString()
	{
		return "[ItemPile t:"+getItem()+" a:"+amount+" i:"+inventory+" x:"+x+" y:"+y+" ]";
	}
}
