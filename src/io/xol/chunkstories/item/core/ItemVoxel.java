package io.xol.chunkstories.item.core;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.item.Item;
import io.xol.chunkstories.item.ItemData;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.chunkstories.voxel.VoxelTypes;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An item that contains voxels
 *
 */
public class ItemVoxel extends Item
{
	class ItemDataVoxel implements ItemData
	{
		public Voxel voxel = null;
		public int voxelMeta = 0;
	}
	
	public ItemVoxel(int id)
	{
		super(id);
		itemRenderer = new VoxelItemRenderer(this);
	}
	
	public ItemData getItemData()
	{
		return new ItemDataVoxel();
	}
	
	public void onCreate(ItemPile pile, String[] info)
	{
		ItemDataVoxel idv = (ItemDataVoxel)pile.data;
		if(info != null && info.length > 0)
			idv.voxel = VoxelTypes.get(Integer.parseInt(info[0]));
	}
	
	@Override
	public String getTextureName(ItemPile pile)
	{
		ItemDataVoxel idv = (ItemDataVoxel)pile.data;
		if(idv.voxel != null)
			return "res/voxels/textures/"+idv.voxel.getName()+".png";
		return "res/items/icons/notex.png";
	}

}
