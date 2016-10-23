package io.xol.chunkstories.item;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemTypeImpl implements ItemType
{
	private final int id;
	private String internalName;
	private int slotsWidth = 1;
	private int slotsHeight = 1;
	private int maxStackSize = 100;
	
	private Constructor<? extends Item> itemConstructor;
	
	private Map<String, String> customProperties = new HashMap<String, String>();

	public ItemTypeImpl(int id)
	{
		this.id = id;
	}

	@Override
	public int getID()
	{
		return id;
	}
	
	@Override
	public String getInternalName()
	{
		return internalName;
	}

	public final void setInternalName(String internalName)
	{
		this.internalName = internalName;
	}

	@Override
	public int getSlotsWidth()
	{
		return slotsWidth;
	}

	public final void setSlotsWidth(int slotsWidth)
	{
		this.slotsWidth = slotsWidth;
	}

	@Override
	public int getSlotsHeight()
	{
		return slotsHeight;
	}

	public final void setSlotsHeight(int slotsHeight)
	{
		this.slotsHeight = slotsHeight;
	}

	@Override
	public int getMaxStackSize()
	{
		return maxStackSize;
	}

	public final void setMaxStackSize(int maxStackSize)
	{
		this.maxStackSize = maxStackSize;
	}

	@Override
	public Item newItem()
	{
		Object[] parameters = { this };
		try
		{
			return this.itemConstructor.newInstance(parameters);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			ChunkStoriesLogger.getInstance().warning("Could not spawn : "+this);
			e.printStackTrace();
			return null;
		}
	}

	public final void setConstructor(Constructor<? extends Item> constructor)
	{
		this.itemConstructor = constructor;
	}

	public String toString()
	{
		return "[ItemType id:"+id+" name:"+getInternalName()+" w:"+this.getSlotsWidth()+" h:"+this.getSlotsHeight()+" max:"+this.getMaxStackSize()+"]";
	}
	
	public boolean equals(ItemType type)
	{
		return type.getID() == this.getID();
	}

	@Override
	public String getProperty(String propertyName, String defaultValue)
	{
		String r = customProperties.get(propertyName);
		return r != null ? r : defaultValue;
	}
	
	public void setup(String propertyName, String value)
	{
		customProperties.put(propertyName, value);
	}
}
