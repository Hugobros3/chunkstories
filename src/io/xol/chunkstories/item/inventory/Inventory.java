package io.xol.chunkstories.item.inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.item.Item;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemsList;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Inventory implements Iterable<ItemPile>,  CSFSerializable
{
	public String name;

	public int width;
	public int height;
	
	//What does this inventory belong to ?
	public InventoryHolder holder;

	protected ItemPile[][] contents;

	public Inventory(InventoryHolder holder, int width, int height, String name)
	{
		this.holder = holder;
		this.width = width;
		this.height = height;
		this.name = name;
		contents = new ItemPile[width][height];
	}

	public ItemPile[][] getContents()
	{
		return contents;
	}
	
	public ItemPile getItem(int x, int y)
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

	public boolean canPlaceItemAt(int x, int y, ItemPile pile)
	{
		if (contents[x % width][y % height] != null)
			return false;
		else
		{
			ItemPile p;
			for (int i = 0; i < x + (pile.item.getSlotsWidth()); i++)
			{
				// If overflow in width
				if (i >= width)
					return false;
				for (int j = 0; j < y + (pile.item.getSlotsHeight()); j++)
				{
					// If overflow in height
					if (j >= height)
						return false;
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

	/**
	 * Returns null if the item was put in this inventory, the item if it wasn't
	 * 
	 * @param x
	 * @param y
	 * @param pile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <CE extends Entity & EntityControllable> ItemPile setItemPileAt(int x, int y, ItemPile pile)
	{
		if (pile == null)
		{
			contents[x % width][y % height] = null;
		}
		else if (canPlaceItemAt(x, y, pile))
		{
			pile.inventory = this;
			pile.x = x;
			pile.y = y;
			contents[x % width][y % height] = pile;
		}
		else
			return pile;
		if(this.holder != null && this.holder instanceof Entity && this.holder instanceof EntityControllable && ((EntityControllable)this.holder).getController() != null)
		{
			((EntityControllable)this.holder).getController().notifyInventoryChange((CE)this.holder);
		}
		return null;
	}
	
	/**
	 * Try to add a pile to this inventory.
	 * @param pile
	 * @return Null if it succeeds or the input pile if it fails
	 */
	public ItemPile addItemPile(ItemPile pile)
	{
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height; j++)
				if(setItemPileAt(i, j, pile) == null)
					return null;
		return pile;
	}

	@Override
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
	
	public Inventory(DataInputStream stream) throws IOException
	{
		load(stream);
	}

	@Override
	public void load(DataInputStream stream) throws IOException
	{
		this.width = stream.readInt();
		this.height = stream.readInt();
		boolean hasName = stream.readBoolean();
		if(hasName)
			name = stream.readUTF();
		contents = new ItemPile[width][height];
		int id;
		Item item;
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height ; j++)
			{
				id = stream.readInt() & 0x00FFFFFF;
				item = ItemsList.get(id);
				if(item != null)
				{
					contents[i][j] = new ItemPile(item, stream);
					contents[i][j].inventory = this;
					contents[i][j].x = i;
					contents[i][j].y = j;
				}
			}
	}

	@Override
	public void save(DataOutputStream stream) throws IOException
	{
		stream.writeInt(width);
		stream.writeInt(height);
		boolean hasName = name != null;
		stream.writeBoolean(hasName);
		if(hasName)
			stream.writeUTF(name);
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
					pile.save(stream);
				}
			}
	}

	/**
	 * Removes all itempiles in the inventory.
	 */
	@SuppressWarnings("unchecked")
	public <CE extends Entity & EntityControllable> void clear()
	{
		contents = new ItemPile[width][height];
		if(this.holder != null && this.holder instanceof Entity && this.holder instanceof EntityControllable && ((EntityControllable)this.holder).getController() != null)
		{
			((EntityControllable)this.holder).getController().notifyInventoryChange((CE)this.holder);
		}
	}

	/**
	 * Counts the ammount of stuff this inventory contains.
	 * @return
	 */
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
}
