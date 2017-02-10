package io.xol.chunkstories.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.Content.ItemsTypes;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.exceptions.content.IllegalItemDeclarationException;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.chunkstories.materials.GenericNamedConfigurable;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemTypeImpl extends GenericNamedConfigurable implements ItemType
{
	private final int id;
	private final ItemTypesStore store;

	private final int slotsWidth;
	private final int slotsHeight;
	private final int maxStackSize;

	private final Constructor<? extends Item> itemConstructor;
	private final ItemRenderer itemRenderer;

	public ItemTypeImpl(ItemTypesStore store, String name, int id, BufferedReader reader) throws IllegalItemDeclarationException, IOException
	{
		super(name, reader);
		this.store = store;
		this.id = id;

		//Loads the items properties
		try
		{
			this.slotsWidth = Integer.parseInt(this.resolveProperty("width", "1"));
			this.slotsHeight = Integer.parseInt(this.resolveProperty("height", "1"));

			this.maxStackSize = Integer.parseInt(this.resolveProperty("maxStackSize", "100"));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalItemDeclarationException("Item " + this.getName() + " has a misformed number.");
		}

		//Loads up a custom class if one is defined
		String className = this.resolveProperty("customClass", "io.xol.chunkstories.api.item.Item");
		try
		{
			Class<?> rawClass = store.parent().modsManager().getClassByName(className);
			if (rawClass == null)
			{
				//ChunkStoriesLogger.getInstance().warning("Item class " + className + " does not exist in codebase.");
				throw new IllegalItemDeclarationException("Item " + this.getName() + " does not exist in codebase.");
			}
			else if (!(Item.class.isAssignableFrom(rawClass)))
			{
				//ChunkStoriesLogger.getInstance().warning("Item class " + className + " is not extending the Item class.");
				throw new IllegalItemDeclarationException("Item " + this.getName() + " is not extending the Item class.");
			}
			else
			{
				@SuppressWarnings("unchecked")
				Class<? extends Item> itemClass = (Class<? extends Item>) rawClass;
				Class<?>[] types = { ItemType.class };
				Constructor<? extends Item> constructor = itemClass.getConstructor(types);

				if (constructor == null)
				{
					throw new IllegalItemDeclarationException("Item " + this.getName() + " does not provide a valid constructor.");
				}

				this.itemConstructor = constructor;
			}
		}
		catch (NoSuchMethodException | SecurityException | IllegalArgumentException e)
		{
			e.printStackTrace();
			throw new IllegalItemDeclarationException("Item " + this.getName() + " has an issue with it's constructor: " + e.getMessage());
		}

		if (store.parent() instanceof ClientContent)
		{
			ItemRenderer defaultItemRenderer = new DefaultItemRenderer(this);
			
			Item sampleItem = this.newItem();
			ItemRenderer customItemRenderer = sampleItem.getCustomItemRenderer(defaultItemRenderer);
			
			this.itemRenderer = customItemRenderer == null ? defaultItemRenderer : customItemRenderer;
			System.out.println("Initialized itemRenderer to " + this.itemRenderer);
		}
		//There are no Item renderers on a server !
		else
			this.itemRenderer = null;
	}
	
	@Override
	public ItemRenderer getRenderer()
	{
		return itemRenderer;
	}

	@Override
	public int getID()
	{
		return id;
	}

	@Override
	public String getInternalName()
	{
		return super.getName();
	}

	@Override
	public int getSlotsWidth()
	{
		return slotsWidth;
	}

	/*public final void setSlotsWidth(int slotsWidth)
	{
		this.slotsWidth = slotsWidth;
	}*/

	@Override
	public int getSlotsHeight()
	{
		return slotsHeight;
	}

	/*public final void setSlotsHeight(int slotsHeight)
	{
		this.slotsHeight = slotsHeight;
	}*/

	@Override
	public int getMaxStackSize()
	{
		return maxStackSize;
	}

	/*public final void setMaxStackSize(int maxStackSize)
	{
		this.maxStackSize = maxStackSize;
	}*/

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
			ChunkStoriesLogger.getInstance().warning("Could not spawn : " + this);
			e.printStackTrace();
			return null;
		}
	}

	/*public final void setConstructor(Constructor<? extends Item> constructor)
	{
		this.itemConstructor = constructor;
	}*/

	public String toString()
	{
		return "[ItemType id:" + id + " name:" + getInternalName() + " w:" + this.getSlotsWidth() + " h:" + this.getSlotsHeight() + " max:" + this.getMaxStackSize() + "]";
	}

	public boolean equals(ItemType type)
	{
		return type.getID() == this.getID();
	}

	@Override
	public String resolveProperty(String propertyName, String defaultValue)
	{
		String r = resolveProperty(propertyName);
		return r != null ? r : defaultValue;
	}

	/*public void setup(String propertyName, String value)
	{
		customProperties.put(propertyName, value);
	}*/

	@Override
	public ItemsTypes store()
	{
		return store;
	}
}
