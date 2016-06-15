package io.xol.chunkstories.item.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.MouseClick;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.item.ItemData;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.VoxelItemRenderer;
import io.xol.chunkstories.voxel.VoxelTypes;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An item that contains voxels
 */
public class ItemVoxel extends Item
{
	public class ItemDataVoxel implements ItemData
	{
		public Voxel voxel = null;
		public int voxelMeta = 0;
	}

	public ItemVoxel(int id)
	{
		super(id);
		itemRenderer = new VoxelItemRenderer(this);
	}

	@Override
	public ItemDataVoxel getItemData()
	{
		return new ItemDataVoxel();
	}

	@Override
	public void onCreate(ItemPile pile, String[] info)
	{
		ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (info != null && info.length > 0)
			idv.voxel = VoxelTypes.get(Integer.parseInt(info[0]));
		if (info != null && info.length > 1)
			idv.voxelMeta = Integer.parseInt(info[1]) % 16;
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (idv.voxel != null)
			return "res/voxels/textures/" + idv.voxel.getName() + ".png";
		return "res/items/icons/notex.png";
	}

	public static Voxel getVoxel(ItemPile pile)
	{
		return ((ItemDataVoxel) pile.getData()).voxel;
	}

	public static int getVoxelMeta(ItemPile pile)
	{
		return ((ItemDataVoxel) pile.getData()).voxelMeta;
	}

	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input)
	{
		if (user.getWorld() instanceof WorldMaster && input instanceof MouseClick)
		{
			//TODO here we assumme a player, that's not correct
			EntityPlayer player = (EntityPlayer) user;
			int voxelID = ((ItemDataVoxel) pile.getData()).voxel.getId();
			int voxelMeta = ((ItemDataVoxel) pile.getData()).voxelMeta;

			int data2write = -1;
			Location selectedBlock = null;
			if (input.equals(MouseClick.RIGHT))
			{
				selectedBlock = player.getBlockLookingAt(false);
				data2write = VoxelFormat.format(voxelID, voxelMeta, 0, 0);
			}
			
			else
				return false;
			if (selectedBlock != null && data2write != -1)
			{
				//int selectedBlockPreviousData = user.getWorld().getDataAt(selectedBlock);
				//Adding blocks should not erase light if the block's not opaque
				if (VoxelTypes.get(data2write).isVoxelOpaque())
				{
					data2write = VoxelFormat.changeSunlight(data2write, 0);
					data2write = VoxelFormat.changeBlocklight(data2write, 0);
				}
				if(VoxelTypes.get(data2write).getLightLevel(data2write) > 0)
					data2write = VoxelFormat.changeBlocklight(data2write, VoxelTypes.get(data2write).getLightLevel(data2write));
					
				//System.out.println(VoxelFormat.blocklight(data2write));
				user.getWorld().setDataAt(selectedBlock, data2write, true);
			}
			return true;
		}
		return false;
	}

	@Override
	public void load(ItemPile itemPile, DataInputStream stream) throws IOException
	{
		((ItemDataVoxel) itemPile.data).voxel = VoxelTypes.get(stream.readInt());
		((ItemDataVoxel) itemPile.data).voxelMeta = stream.readByte();
	}

	@Override
	public void save(ItemPile itemPile, DataOutputStream stream) throws IOException
	{
		if(((ItemDataVoxel) itemPile.data).voxel != null)
			stream.writeInt(((ItemDataVoxel) itemPile.data).voxel.getId());
		else
			stream.writeInt(1);
		stream.writeByte((byte) ((ItemDataVoxel) itemPile.data).voxelMeta);
	}

}
