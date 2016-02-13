package io.xol.chunkstories.item.inventory;

import java.util.Set;

import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.voxel.VoxelTypes;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class InventoryAllVoxels extends Inventory
{
	public InventoryAllVoxels()
	{
		super(null, 0, 0, "All voxels");
		Set<Integer> allIds = VoxelTypes.getAllLoadedVoxelIds();
		this.height = (int)Math.ceil(allIds.size() / 10.0);
		this.width = 10;
		this.contents = new ItemPile[width][height];
		for(int id : allIds)
		{
			this.addItemPile(new ItemPile("item_voxel", new String[]{""+id}));
		}
	}
}
