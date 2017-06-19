package io.xol.chunkstories.item.inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.NullItemException;
import io.xol.chunkstories.api.exceptions.UndefinedItemTypeException;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.InventoryHolder;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

public class BasicInventory implements Inventory
{
	protected int width;
	protected int height;

	protected ItemPile[][] contents;
	
	public BasicInventory(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		this.contents = new ItemPile[width][height];
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
						if (i + p.getItem().getType().getSlotsWidth() - 1 >= x && j + p.getItem().getType().getSlotsHeight() - 1 >= y)
							return p;
					}
				}
			}
			return null;
		}
	}
	
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
			for (int i = 0; i < x + (itemPile.getItem().getType().getSlotsWidth()); i++)
			{
				// If the item width would overflow the limits of the inventory
				if (i >= width)
					return false;
				for (int j = 0; j < y + (itemPile.getItem().getType().getSlotsHeight()); j++)
				{
					// If overflow in height
					if (j >= height)
						return false;
					// Check nearby items don't overlap our pile
					p = contents[i % width][j % height];
					if (p != null)
					{
						if (i + p.getItem().getType().getSlotsWidth() - 1 >= x && j + p.getItem().getType().getSlotsHeight() - 1 >= y)
							return false;
					}
				}
			}
			return true;
		}
	}
	
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
			if (currentAmount < item.getType().getMaxStackSize())
			{
				int totalAmount = currentAmount + wouldBeAddedAmount;
				//How much can we add ?
				int addableAmmount = Math.min(totalAmount, item.getType().getMaxStackSize()) - currentAmount;

				currentPileAtLocation.setAmount(currentAmount + addableAmmount);
				//If we could add all to the first stack, discard the second pile
				if (addableAmmount == wouldBeAddedAmount)
				{
					//Push changes
					this.refreshItemSlot(x, y, contents[x % width][y % height]);

					return null;
				}
				//If we couldn't, reduce it's size
				else
				{
					itemPile.setAmount(wouldBeAddedAmount - addableAmmount);

					//Push changes
					this.refreshItemSlot(x, y, contents[x % width][y % height]);

					return itemPile;
				}
			}
		}
		//If none of the above can be done, return the original pile.
		return itemPile;
	}
	
	@Override
	public boolean setItemPileAt(int x, int y, ItemPile pile)
	{
		if (pile == null)
		{
			contents[x % width][y % height] = null;

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
			this.refreshItemSlot(x, y, contents[x % width][y % height]);
			return false;
		}

		this.refreshItemSlot(x, y, contents[x % width][y % height]);
		return true;
	}
	
	@Override
	public ItemPile addItemPile(ItemPile pile)
	{
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++)
				if (placeItemPileAt(i, j, pile) == null)
					return null;
		return pile;
	}
	
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

	public void refreshCompleteInventory()
	{
		//Do nothing !
	}

	public void refreshItemSlot(int x, int y)
	{
		refreshItemSlot(x, y, this.contents[x][y]);
	}
	
	public void refreshItemSlot(int x, int y, ItemPile pileChanged)
	{
		//Don't do shit either
	}

	public void pushItemMove(int xFrom, int yFrom, int xTo, int yTo)
	{
		throw new UnsupportedOperationException();
	}

	public void pushInventory(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
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

	public void pullInventory(StreamSource from, DataInputStream stream, Content content) throws IOException
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
					itemPile = ItemPile.obtainItemPileFromStream(content.items(), stream);
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
					ChunkStoriesLoggerImplementation.getInstance().log(e.getMessage(), LogLevel.WARN);
					e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
				}
			}
	}

	@Override
	public void clear()
	{
		contents = new ItemPile[width][height];

		this.refreshCompleteInventory();
	}

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

	@Override
	public String getInventoryName()
	{
		return "UNNAMED INVENTORY PLEASE GIB NAME";
	}

	@Override
	public InventoryHolder getHolder()
	{
		return null;
	}

	@Override
	public boolean isAccessibleTo(Entity entity)
	{
		return true;
	}
}
