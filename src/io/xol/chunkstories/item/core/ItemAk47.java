package io.xol.chunkstories.item.core;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.MouseClick;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.item.ItemPile;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemAk47 extends Item
{
	
	public ItemAk47(int id)
	{
		super(id);
		itemRenderer = new Ak47ViewModelRenderer(this);
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		return "./res/items/icons/ak47.png";
	}

	public boolean handleInteraction(Entity user, ItemPile pile, Input input)
	{
		if (/*user.getWorld() instanceof WorldMaster && */input instanceof MouseClick)
		{
			if(user instanceof EntityLiving)
			{
				EntityLiving shooter = (EntityLiving)user;
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if(shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));
				if (input.equals(MouseClick.LEFT))
				{
					Iterator<Entity> shotEntities = user.getWorld().rayTraceEntities(eyeLocation, shooter.getDirectionLookingAt(), 256f);
					System.out.println("Intersections : ");
					while(shotEntities.hasNext())
					{
						Entity shotEntity = shotEntities.next();
						System.out.println("Shot : "+shotEntity);
					}
				}
			}
		}
		return false;
	}
}
