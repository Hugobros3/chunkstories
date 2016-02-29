package io.xol.chunkstories.item.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.api.voxel.Voxel;
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
		List<ItemPile> allItems = new ArrayList<ItemPile>();
		Set<Integer> allIds = VoxelTypes.getAllLoadedVoxelIds();
		for(int id : allIds)
		{
			Voxel vox = VoxelTypes.get(id);
			for(ItemPile item : vox.getItems())
			{
				allItems.add(item);
			}
			//this.addItemPile(new ItemPile("item_voxel", new String[]{""+id}));
		}
		this.height = (int)Math.ceil(allItems.size() / 10.0);
		this.width = 10;
		this.contents = new ItemPile[width][height];
		for(ItemPile pile : allItems)
			this.addItemPile(pile);
		
	}
}
