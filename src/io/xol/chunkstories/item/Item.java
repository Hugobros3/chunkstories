package io.xol.chunkstories.item;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Item
{
	public int slotsWidth = 1;
	public int slotsHeight = 1;

	public Item()
	{
		this(1, 1);
	}

	public Item(int slotsWidth, int slotsHeight)
	{
		this.slotsWidth = slotsWidth;
		this.slotsHeight = slotsHeight;
	}

	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	public abstract String getTextureName();
}
