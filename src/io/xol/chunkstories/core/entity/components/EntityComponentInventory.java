package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.Inventory;
import io.xol.chunkstories.api.item.InventoryHolder;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.item.inventory.BasicInventory;
import io.xol.chunkstories.net.packets.PacketInventoryPartialUpdate;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityComponentInventory extends EntityComponent implements Inventory
{
	private BasicInventory actualInventory;
	
	//What does this inventory belong to ?
	public EntityWithInventory holder;

	public EntityComponentInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, holder == null ? null : holder.getComponents().getLastComponent());
		this.holder = holder;
		
		this.actualInventory = new BasicInventory(width, height) {
			@Override
			public InventoryHolder getHolder()
			{
				return holder;
			}
		};
	}

	@Override
	public ItemPile getItemPileAt(int x, int y)
	{
		return actualInventory.getItemPileAt(x, y);
	}
	
	@Override
	public boolean canPlaceItemAt(int x, int y, ItemPile itemPile)
	{
		return actualInventory.canPlaceItemAt(x, y, itemPile);
	}

	@Override
	public ItemPile placeItemPileAt(int x, int y, ItemPile itemPile)
	{
		return actualInventory.placeItemPileAt(x, y, itemPile);
	}

	@Override
	public boolean setItemPileAt(int x, int y, ItemPile pile)
	{
		return actualInventory.setItemPileAt(x, y, pile);
	}

	@Override
	public ItemPile addItemPile(ItemPile pile)
	{
		return actualInventory.addItemPile(pile);
	}

	@Override
	public IterableIterator<ItemPile> iterator()
	{
		return actualInventory.iterator();
	}

	/**
	 * Copy the contents of another Inventory.
	 * 
	 * @param inventory
	 */
	/*public void load(EntityComponentInventory inventory)
	{
		this.width = inventory.width;
		this.height = inventory.height;
		contents = inventory.contents;
		//Update inventory references
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (contents[i][j] != null)
					contents[i][j].setInventory(this);
	}*/

	public void refreshCompleteInventory()
	{
		pushComponentController();
	}
	
	public enum UpdateMode
	{
		//MOVE_ITEM, 
		//CHANGE_ITEM, 
		REFRESH;
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

	@Override
	protected void push(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
		//Unused but keep
		stream.writeByte(UpdateMode.REFRESH.ordinal());
		
		actualInventory.pushInventory(destinator, stream);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream stream) throws IOException
	{
		//Unused
		stream.readByte();

		actualInventory.pullInventory(from, stream, entity.getWorld().getGameContext().getContent());
	}

	@Override
	public void clear()
	{
		actualInventory.clear();
	}

	@Override
	public int size()
	{
		return actualInventory.size();
	}

	@Override
	public String getInventoryName()
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
	public int getWidth()
	{
		return actualInventory.getWidth();
	}

	@Override
	public int getHeight()
	{
		return actualInventory.getHeight();
	}

	@Override
	public void refreshItemSlot(int x, int y)
	{
		actualInventory.refreshItemSlot(x, y);
	}

	@Override
	public InventoryHolder getHolder()
	{
		return holder;
	}

}
