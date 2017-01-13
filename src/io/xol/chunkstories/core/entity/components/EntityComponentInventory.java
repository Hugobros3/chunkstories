package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.exceptions.NullItemException;
import io.xol.chunkstories.api.exceptions.UndefinedItemTypeException;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.net.packets.PacketInventoryPartialUpdate;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityComponentInventory extends EntityComponent implements Inventory
{
	public int width;
	public int height;

	//What does this inventory belong to ?
	public EntityWithInventory holder;

	protected ItemPile[][] contents;

	public EntityComponentInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, holder == null ? null : holder.getComponents().getLastComponent());
		this.holder = holder;
		this.width = width;
		this.height = height;
		contents = new ItemPile[width][height];
	}

	public ItemPile[][] getContents()
	{
		return contents;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#getItem(int, int)
	 */
	@Override
	public ItemPile getItemPileAt(int x, int y)
	{
		if (contents[x % width][y % height] != null)
			return contents[x % width][y % height];
		else
		{
			ItemPile p;
			for (int i = 0; i < x + (1); i++)
			{
				// If overflow in width
				if (i >= width)
					break;
				for (int j = 0; j < y + (1); j++)
				{
					// If overflow in height
					if (j >= height)
						break;
					p = contents[i % width][j % height];
					if (p != null)
					{
						if (i + p.getItem().getSlotsWidth() - 1 >= x && j + p.getItem().getSlotsHeight() - 1 >= y)
							return p;
					}
				}
			}
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#canPlaceItemAt(int, int, io.xol.chunkstories.item.ItemPile)
	 */
	@Override
	public boolean canPlaceItemAt(int x, int y, ItemPile itemPile)
	{
		if (contents[x % width][y % height] != null)
		{
			return false;
		}
		else
		{
			ItemPile p;
			//Iterate the inventory up to the new pile x end ( position + width - 1 )
			for (int i = 0; i < x + (itemPile.getItem().getSlotsWidth()); i++)
			{
				// If the item width would overflow the limits of the inventory
				if (i >= width)
					return false;
				for (int j = 0; j < y + (itemPile.getItem().getSlotsHeight()); j++)
				{
					// If overflow in height
					if (j >= height)
						return false;
					// Check nearby items don't overlap our pile
					p = contents[i % width][j % height];
					if (p != null)
					{
						if (i + p.getItem().getSlotsWidth() - 1 >= x && j + p.getItem().getSlotsHeight() - 1 >= y)
							return false;
					}
				}
			}
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#placeItemPileAt(int, int, io.xol.chunkstories.item.ItemPile)
	 */
	@Override
	public ItemPile placeItemPileAt(int x, int y, ItemPile itemPile)
	{
		ItemPile currentPileAtLocation = this.getItemPileAt(x, y);
		//If empty and has space, put it in.
		if (currentPileAtLocation == null && canPlaceItemAt(x, y, itemPile))
		{
			itemPile.setInventory(this);
			itemPile.setX(x);
			itemPile.setY(y);
			contents[x % width][y % height] = itemPile;

			//Push changes
			if (this.holder != null)
				this.refreshItemSlot(x, y, contents[x % width][y % height]);

			//There is nothing left
			return null;
		}
		//If the two piles are similar we can try to merge them
		if (currentPileAtLocation != null && currentPileAtLocation.canMergeWith(itemPile) && !currentPileAtLocation.equals(itemPile))
		{
			Item item = currentPileAtLocation.getItem();
			int currentAmount = currentPileAtLocation.getAmount();
			int wouldBeAddedAmount = itemPile.getAmount();

			//The existing pile is not already full
			if (currentAmount < item.getMaxStackSize())
			{
				int totalAmount = currentAmount + wouldBeAddedAmount;
				//How much can we add ?
				int addableAmmount = Math.min(totalAmount, item.getMaxStackSize()) - currentAmount;

				currentPileAtLocation.setAmount(currentAmount + addableAmmount);
				//If we could add all to the first stack, discard the second pile
				if (addableAmmount == wouldBeAddedAmount)
				{
					//Push changes
					if (this.holder != null)
						this.refreshItemSlot(x, y, contents[x % width][y % height]);

					return null;
				}
				//If we couldn't, reduce it's size
				else
				{
					itemPile.setAmount(wouldBeAddedAmount - addableAmmount);

					//Push changes
					if (this.holder != null)
						this.refreshItemSlot(x, y, contents[x % width][y % height]);

					return itemPile;
				}
			}
		}
		//If none of the above can be done, return the original pile.
		return itemPile;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#setItemPileAt(int, int, io.xol.chunkstories.item.ItemPile)
	 */
	@Override
	public boolean setItemPileAt(int x, int y, ItemPile pile)
	{
		if (pile == null)
		{
			contents[x % width][y % height] = null;

			if (this.holder != null)
				this.refreshItemSlot(x, y, contents[x % width][y % height]);

			return true;
		}
		ItemPile temp = null;
		if (contents[x % width][y % height] != null)
		{
			temp = contents[x % width][y % height];
			contents[x % width][y % height] = null;
		}

		if (canPlaceItemAt(x, y, pile))
		{
			pile.setInventory(this);
			pile.setX(x);
			pile.setY(y);
			contents[x % width][y % height] = pile;
		}
		else
		{
			contents[x % width][y % height] = temp;
			return false;
		}

		if (this.holder != null)
			this.refreshItemSlot(x, y, contents[x % width][y % height]);
		return true;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#addItemPile(io.xol.chunkstories.item.ItemPile)
	 */
	@Override
	public ItemPile addItemPile(ItemPile pile)
	{
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++)
				if (placeItemPileAt(i, j, pile) == null)
					return null;
		return pile;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#iterator()
	 */
	@Override
	/**
	 * Iterates over every ItemPile
	 */
	public IterableIterator<ItemPile> iterator()
	{
		IterableIterator<ItemPile> it = new IterableIterator<ItemPile>()
		{
			public int i = 0;
			public int j = 0;

			ItemPile current = contents[0][0];

			@Override
			public boolean hasNext()
			{
				while (current == null && !reachedEnd())
				{
					i++;
					if (i >= width)
					{
						i = 0;
						j++;
					}
					if (reachedEnd())
						return false;
					current = contents[i][j];
				}
				return current != null;
			}

			private boolean reachedEnd()
			{
				return j >= height;
			}

			@Override
			public ItemPile next()
			{
				if (reachedEnd())
					return null;

				if (current == null)
					hasNext();

				ItemPile r = current;
				current = null;
				return r;
			}

			@Override
			public void remove()
			{
				contents[i][j] = null;
			}
		};
		return it;

	}

	/**
	 * Copy the contents of another Inventory.
	 * 
	 * @param inventory
	 */
	public void load(EntityComponentInventory inventory)
	{
		this.width = inventory.width;
		this.height = inventory.height;
		contents = inventory.contents;
		//Update inventory references
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (contents[i][j] != null)
					contents[i][j].setInventory(this);
	}

	protected enum UpdateMode
	{
		//MOVE_ITEM, 
		//CHANGE_ITEM, 
		REFRESH;
	}

	public void refreshCompleteInventory()
	{
		pushComponentController();
	}

	public void refreshItemSlot(int x, int y)
	{
		refreshItemSlot(x, y, this.contents[x][y]);
	}
	
	public void refreshItemSlot(int x, int y, ItemPile pileChanged)
	{
		//System.out.println("Updating slot: "+x+", "+y+" to "+pileChanged);

		Packet packetItemUpdate = new PacketInventoryPartialUpdate(this, x, y, pileChanged);
		Controller controller = null;
		if (entity instanceof EntityControllable)
			controller = ((EntityControllable) entity).getControllerComponent().getController();

		if (controller != null)
			controller.pushPacket(packetItemUpdate);
	}

	public void pushItemMove(int xFrom, int yFrom, int xTo, int yTo)
	{
		throw new UnsupportedOperationException();
	}

	protected void pushWholeInventoryRefresh(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
		stream.writeByte(UpdateMode.REFRESH.ordinal());

		stream.writeInt(width);
		stream.writeInt(height);

		ItemPile pile;
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
			{
				pile = contents[i][j];
				if (pile == null)
					stream.writeInt(0);
				else
				{
					pile.saveItemIntoStream(stream);
				}
			}
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
		pushWholeInventoryRefresh(destinator, stream);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream stream) throws IOException
	{
		//Unused
		stream.readByte();

		pullWholeInventoryRefresh(from, stream);

		/*UpdateMode mode = UpdateMode.values()[stream.readByte()];
		
		//System.out.println("Received " + mode + " inventory update");
		
		switch (mode)
		{
		case REFRESH:
			pullWholeInventoryRefresh(from, stream);
			break;
		case CHANGE_ITEM:
			pullItemChange(from, stream);
			break;
		case MOVE_ITEM:
			pullItemMove(from, stream);
			break;
		}*/
	}

	protected void pullWholeInventoryRefresh(StreamSource from, DataInputStream stream) throws IOException
	{
		this.width = stream.readInt();
		this.height = stream.readInt();

		contents = new ItemPile[width][height];
		//int id;
		//Item item;
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
			{
				ItemPile itemPile;
				try
				{
					itemPile = ItemPile.obtainItemPileFromStream(entity.getWorld().getGameContext().getContent().items(), stream);
					//Then add the thing
					contents[i][j] = itemPile;
					contents[i][j].setInventory(this);
					contents[i][j].setX(i);
					contents[i][j].setY(j);
				}
				catch (NullItemException e)
				{
					//Don't do anything about it, no big deal
				}
				catch (UndefinedItemTypeException e)
				{
					//This is slightly more problematic
					ChunkStoriesLogger.getInstance().log(e.getMessage(), LogLevel.WARN);
					e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				}

				/*id = stream.readInt() & 0x00FFFFFF;
				ItemType itemType = ItemTypes.getItemTypeById(id);
				if (itemType != null)
				{
					item = itemType.newItem();
					contents[i][j] = new ItemPile(item, stream);
					contents[i][j].setInventory(this);
					contents[i][j].setX(i);
					contents[i][j].setY(j);
				}*/
			}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#clear()
	 */
	@Override
	public void clear()
	{
		contents = new ItemPile[width][height];

		if (this.holder != null)
			this.refreshCompleteInventory();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#size()
	 */
	@Override
	public int size()
	{
		int size = 0;
		Iterator<ItemPile> i = this.iterator();
		while (i.hasNext())
		{
			i.next();
			size++;
		}
		return size;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#getHolderName()
	 */
	@Override
	public String getHolderName()
	{
		if (holder != null)
		{
			if (holder instanceof EntityNameable)
				return ((EntityNameable) holder).getName();
			return holder.getClass().getSimpleName();
		}
		return "/dev/null";
	}

	@Override
	public EntityWithInventory getHolder()
	{
		return holder;
	}

	@Override
	public int getWidth()
	{
		return width;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

}
