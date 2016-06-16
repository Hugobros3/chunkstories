package io.xol.chunkstories.entity.core.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.net.StreamSource;
import io.xol.chunkstories.api.net.StreamTarget;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemsList;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityComponentInventory extends EntityComponent implements Iterable<ItemPile>, EntityInventory
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
			return contents[x % width][y % height] ;
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
						if(i+p.item.getSlotsWidth()-1 >= x && j+p.item.getSlotsHeight()-1 >= y)
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
			for (int i = 0; i < x + (itemPile.item.getSlotsWidth()); i++)
			{
				// If the item width would overflow the limits of the inventory
				if (i >= width)
					return false;
				for (int j = 0; j < y + (itemPile.item.getSlotsHeight()); j++)
				{
					// If overflow in height
					if (j >= height)
						return false;
					// Check nearby items don't overlap our pile
					p = contents[i % width][j % height];
					if (p != null)
					{
						if(i+p.item.getSlotsWidth()-1 >= x && j+p.item.getSlotsHeight()-1 >= y)
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
		if(currentPileAtLocation == null && canPlaceItemAt(x, y, itemPile))
		{
			itemPile.inventory = this;
			itemPile.x = x;
			itemPile.y = y;
			contents[x % width][y % height] = itemPile;
			
			//Push changes
			if(this.holder != null)
				this.pushComponentController();
			
			//There is nothing left
			return null;
		}
		//If the two piles are similar we can try to merge them
		if(currentPileAtLocation != null && currentPileAtLocation.canMergeWith(itemPile) && !currentPileAtLocation.equals(itemPile))
		{
			Item item = currentPileAtLocation.getItem();
			int currentAmount = currentPileAtLocation.getAmount();
			int wouldBeAddedAmount = itemPile.getAmount();
			
			//The existing pile is not already full
			if(currentAmount < item.getMaxStackSize())
			{
				int totalAmount = currentAmount + wouldBeAddedAmount;
				//How much can we add ?
				int addableAmmount = Math.min(totalAmount, item.getMaxStackSize()) - currentAmount;
				
				currentPileAtLocation.setAmount(currentAmount + addableAmmount);
				//If we could add all to the first stack, discard the second pile
				if(addableAmmount == wouldBeAddedAmount)
				{
					//Push changes
					if(this.holder != null)
						this.pushComponentController();
					
					return null;
				}
				//If we couldn't, reduce it's size
				else
				{
					itemPile.setAmount(wouldBeAddedAmount - addableAmmount);
					
					//Push changes
					if(this.holder != null)
						this.pushComponentController();
					
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
			
			if(this.holder != null)
				this.pushComponentController();
			
			return true;
		}
		ItemPile temp = null;
		if(contents[x % width][y % height] != null)
		{
			temp = contents[x % width][y % height];
			contents[x % width][y % height] = null;
		}
		
		if (canPlaceItemAt(x, y, pile))
		{
			pile.inventory = this;
			pile.x = x;
			pile.y = y;
			contents[x % width][y % height] = pile;
		}
		else
		{
			contents[x % width][y % height] = temp;
			return false;
		}

		if(this.holder != null)
			this.pushComponentController();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#addItemPile(io.xol.chunkstories.item.ItemPile)
	 */
	@Override
	public ItemPile addItemPile(ItemPile pile)
	{
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height; j++)
				if(placeItemPileAt(i, j, pile) == null)
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
	public Iterator<ItemPile> iterator()
	{
		Iterator<ItemPile> it = new Iterator<ItemPile>()
		{
			public int i = 0;
			public int j = 0;
			
			ItemPile current = null;

			@Override
			public boolean hasNext()
			{
				while(current == null && !reachedEnd())
				{
					i++;
					if (i >= width)
					{
						i = 0;
						j++;
					}
					if(reachedEnd())
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
				if(reachedEnd())
					return null;
				do
				{
					current = contents[i][j];
					i++;
					if (i >= width)
					{
						i = 0;
						j++;
					}
				}
				while(current == null && !reachedEnd());
				return current;
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
	 * @param inventory
	 */
	public void load(EntityComponentInventory inventory)
	{
		this.width = inventory.width;
		this.height = inventory.height;
		contents = inventory.contents;
		//Update inventory references
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height ; j++)
				if(contents[i][j] != null)
					contents[i][j].inventory = this;
	}
	
	@Override
	protected void push(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
		stream.writeInt(width);
		stream.writeInt(height);
		/*boolean hasName = name != null;
		stream.writeBoolean(hasName);
		if(hasName)
			stream.writeUTF(name);*/
		ItemPile pile;
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height ; j++)
			{
				pile = contents[i][j];
				if(pile == null)
					stream.writeInt(0);
				else
				{
					stream.writeInt(pile.getItem().getID());
					pile.saveCSF(stream);
				}
			}
	}

	@Override
	protected void pull(StreamSource from, DataInputStream stream) throws IOException
	{
		this.width = stream.readInt();
		this.height = stream.readInt();
		/*boolean hasName = stream.readBoolean();
		if(hasName)
			name = stream.readUTF();*/
		contents = new ItemPile[width][height];
		int id;
		Item item;
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height ; j++)
			{
				id = stream.readInt() & 0x00FFFFFF;
				item = ItemsList.getItemTypeById(id).newItem();
				if(item != null)
				{
					contents[i][j] = new ItemPile(item, stream);
					contents[i][j].inventory = this;
					contents[i][j].x = i;
					contents[i][j].y = j;
				}
			}
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#clear()
	 */
	@Override
	public void clear()
	{
		contents = new ItemPile[width][height];

		if(this.holder != null)
			this.pushComponentController();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.entity.core.components.EntityInventory#size()
	 */
	@Override
	public int size()
	{
		int size = 0;
		Iterator<ItemPile> i = this.iterator();
		while(i.hasNext())
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
		if(holder != null)
		{
			if(holder instanceof EntityNameable)
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
