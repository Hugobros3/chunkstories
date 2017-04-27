package io.xol.chunkstories.core.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import io.xol.chunkstories.api.events.voxel.VoxelModificationCause;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.item.renderers.VoxelItemRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An item that contains voxels
 */
public class ItemVoxel extends Item implements VoxelModificationCause
{
	public Voxel voxel = null;
	public int voxelMeta = 0;

	public ItemVoxel(ItemType type)
	{
		super(type);
	}
	
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer)
	{
		return new VoxelItemRenderer(fallbackRenderer);
	}

	/*@Override
	public void onCreate(ItemPile pile, String[] info)
	{
		//ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (info != null && info.length > 0)
			voxel = Voxels.get(Integer.parseInt(info[0]));
		if (info != null && info.length > 1)
			voxelMeta = Integer.parseInt(info[1]) % 16;
	}
*/
	@Override
	public String getTextureName(ItemPile pile)
	{
		//ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (voxel != null)
			return "./items/icons/" + voxel.getName() + ".png";
		return "./items/icons/notex.png";
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
	public boolean onControllerInput(Entity entity, ItemPile pile, Input input, Controller controller)
	{
		if (entity.getWorld() instanceof WorldMaster && input.getName().equals("mouse.right"))
		{
			//TODO here we assumme a player, that's not correct
			EntityPlayer playerEntity = (EntityPlayer) entity;
			int voxelID = voxel.getId();
			
			boolean isEntityCreativeMode = (entity instanceof EntityCreative) && (((EntityCreative) entity).isCreativeMode());

			Location blockLocation = null;
			blockLocation = playerEntity.getBlockLookingAt(false);
			int data2write = VoxelFormat.format(voxelID, voxelMeta, 0, 0);
			
			if (blockLocation != null)
			{
				//int selectedBlockPreviousData = user.getWorld().getDataAt(selectedBlock);
				//Adding blocks should not erase light if the block's not opaque
				if (VoxelsStore.get().getVoxelById(data2write).getType().isOpaque())
				{
					data2write = VoxelFormat.changeSunlight(data2write, 0);
					data2write = VoxelFormat.changeBlocklight(data2write, 0);
				}
				
				//Glowy stuff should glow
				if(VoxelsStore.get().getVoxelById(data2write).getLightLevel(data2write) > 0)
					data2write = VoxelFormat.changeBlocklight(data2write, VoxelsStore.get().getVoxelById(data2write).getLightLevel(data2write));
					
				// Player events mod
				if(controller instanceof Player) {
					Player player = (Player)controller;
					VoxelContext ctx = entity.getWorld().peek(blockLocation);
					PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(ctx, data2write, isEntityCreativeMode ? EntityCreative.CREATIVE_MODE : this, player);
					
					//Anyone has objections ?
					entity.getWorld().getGameContext().getPluginManager().fireEvent(event);
					
					if(event.isCancelled())
						return true;
				}
				
				entity.getWorld().setVoxelData(blockLocation, data2write, entity);
				
				// Decrease stack size
				if(!isEntityCreativeMode) {
					int currentAmount = pile.getAmount();
					currentAmount--;
					pile.setAmount(currentAmount);
				}
			}
			else
			{
				//No space found :/
				return true;
			}
		}
		return false;
	}

	@Override
	public void load(DataInputStream stream) throws IOException
	{
		voxel = VoxelsStore.get().getVoxelById(stream.readInt());
		voxelMeta = stream.readByte();
		//((ItemDataVoxel) itemPile.data).voxel = VoxelTypes.get(stream.readInt());
		//((ItemDataVoxel) itemPile.data).voxelMeta = stream.readByte();
		
		//System.out.println("loaded my bits");
	}

	@Override
	public void save(DataOutputStream stream) throws IOException
	{
		/*if(((ItemDataVoxel) itemPile.data).voxel != null)
			stream.writeInt(((ItemDataVoxel) itemPile.data).voxel.getId());
		else
			stream.writeInt(1);
		stream.writeByte((byte) ((ItemDataVoxel) itemPile.data).voxelMeta);*/
		
		//System.out.println("saved my bits");
		
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
	
	@Override
	public String getName()
	{
		if(voxel != null)
		{
			return voxel.getName();
		}
		return "novoxel!";
	}
}
