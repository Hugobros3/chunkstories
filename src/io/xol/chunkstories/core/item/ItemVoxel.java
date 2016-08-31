package io.xol.chunkstories.core.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.item.renderers.VoxelItemRenderer;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.voxel.Voxels;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An item that contains voxels
 */
public class ItemVoxel extends Item
{
	public Voxel voxel = null;
	public int voxelMeta = 0;

	public ItemVoxel(ItemType type)
	{
		super(type);
		itemRenderer = new VoxelItemRenderer(this);
	}

	@Override
	public void onCreate(ItemPile pile, String[] info)
	{
		//ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (info != null && info.length > 0)
			voxel = Voxels.get(Integer.parseInt(info[0]));
		if (info != null && info.length > 1)
			voxelMeta = Integer.parseInt(info[1]) % 16;
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		//ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (voxel != null)
			return "res/items/icons/" + voxel.getName() + ".png";
		return "res/items/icons/notex.png";
	}

	public Voxel getVoxel()
	{
		return voxel;
		//((ItemDataVoxel) pile.getData()).voxel;
	}

	public int getVoxelMeta()
	{
		return voxelMeta;
		//((ItemDataVoxel) pile.getData()).voxelMeta;
	}

	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input, Controller controller)
	{
		if (user.getWorld() instanceof WorldMaster && input.getName().equals("mouse.right"))
		{
			//TODO here we assumme a player, that's not correct
			EntityPlayer player = (EntityPlayer) user;
			int voxelID = voxel.getId();// ((ItemDataVoxel) pile.getData()).voxel.getId();
			//int voxelMeta = ((ItemDataVoxel) pile.getData()).voxelMeta;

			int data2write = -1;
			Location selectedBlock = null;
			
			selectedBlock = player.getBlockLookingAt(false);
			data2write = VoxelFormat.format(voxelID, voxelMeta, 0, 0);
			
			if (selectedBlock != null && data2write != -1)
			{
				//int selectedBlockPreviousData = user.getWorld().getDataAt(selectedBlock);
				//Adding blocks should not erase light if the block's not opaque
				if (Voxels.get(data2write).isVoxelOpaque())
				{
					data2write = VoxelFormat.changeSunlight(data2write, 0);
					data2write = VoxelFormat.changeBlocklight(data2write, 0);
				}
				if(Voxels.get(data2write).getLightLevel(data2write) > 0)
					data2write = VoxelFormat.changeBlocklight(data2write, Voxels.get(data2write).getLightLevel(data2write));
					
				user.getWorld().setVoxelData(selectedBlock, data2write, user);
			}
			return true;
		}
		return false;
	}

	@Override
	public void load(DataInputStream stream) throws IOException
	{
		voxel = Voxels.get(stream.readInt());
		voxelMeta = stream.readByte();
		//((ItemDataVoxel) itemPile.data).voxel = VoxelTypes.get(stream.readInt());
		//((ItemDataVoxel) itemPile.data).voxelMeta = stream.readByte();
	}

	@Override
	public void save(DataOutputStream stream) throws IOException
	{
		/*if(((ItemDataVoxel) itemPile.data).voxel != null)
			stream.writeInt(((ItemDataVoxel) itemPile.data).voxel.getId());
		else
			stream.writeInt(1);
		stream.writeByte((byte) ((ItemDataVoxel) itemPile.data).voxelMeta);*/
		if(voxel != null)
			stream.writeInt(voxel.getId());
		else
			stream.writeInt(1);
		stream.writeByte(voxelMeta);
	}

	@Override
	public boolean canMergeWith(Item item)
	{
		if(item instanceof ItemVoxel)
		{
			ItemVoxel itemVoxel = (ItemVoxel)item;
			return super.canMergeWith(itemVoxel) && itemVoxel.getVoxel().getId() == this.getVoxel().getId() && itemVoxel.getVoxelMeta() == this.getVoxelMeta();
		}
		return false;
	}
}
