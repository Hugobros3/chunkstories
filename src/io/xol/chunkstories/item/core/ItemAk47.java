package io.xol.chunkstories.item.core;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.MouseClick;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.particules.ParticleBlood;
import io.xol.chunkstories.physics.particules.ParticleMuzzleFlash;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemAk47 extends Item
{
	public ItemAk47(ItemType type)
	{
		super(type);
		itemRenderer = new Ak47ViewModelRenderer(this);
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		return "./res/items/icons/ak47.png";
	}
	
	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input, Controller controller)
	{
		if (/*user.getWorld() instanceof WorldMaster && */input instanceof MouseClick)
		{
			if(user instanceof EntityLiving)
			{
				EntityLiving shooter = (EntityLiving)user;
				if(shooter.getWorld() instanceof WorldClient)
					shooter.getWorld().playSoundEffect("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f);
				if(shooter.getWorld() instanceof WorldMaster)
					((WorldMaster) shooter.getWorld()).playSoundEffectExcluding("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f, controller);
				
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if(shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));
				if (input.equals(MouseClick.LEFT))
				{
					Iterator<Entity> shotEntities = user.getWorld().rayTraceEntities(eyeLocation, shooter.getDirectionLookingAt(), 256f);
					while(shotEntities.hasNext())
					{
						Entity shotEntity = shotEntities.next();
						//Don't shoot itself
						if(!shotEntity.equals(shooter))
						{
							Vector3d hitPoint = shotEntity.collidesWith(eyeLocation, shooter.getDirectionLookingAt());
						
							Vector3d bloodDir = shooter.getDirectionLookingAt().normalize().scale(0.25);
							for(int i = 0; i < 25; i++)
							{
								Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
								random.scale(0.25);
								random.add(bloodDir);
								shooter.getWorld().addParticle(
										new ParticleBlood(shooter.getWorld(), 
												hitPoint, 
												random));
							}
							
						}
					}
				}
				
				shooter.getWorld().addParticle(new ParticleMuzzleFlash(shooter.getWorld(), eyeLocation));
			}
		}
		return false;
	}
}
