package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.core.item.renderers.Ak47ViewModelRenderer;
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

	long lastShot = 0L;
	double rpm = 600.0;

	/**
	 * Should be called when the owner has this item selected
	 * 
	 * @param owner
	 */
	public void tickInHand(Entity owner, ItemPile itemPile)
	{
		if (owner instanceof EntityControllable && ((EntityControllable) owner).getController() != null)
		{
			EntityControllable owner2 = ((EntityControllable) owner);
			Controller controller = owner2.getController();

			//For now only client-side players can trigger shooting actions
			if (controller instanceof ClientSideController)
			{
				if (!((ClientSideController) controller).hasFocus())
					return;
				if (controller.getInputsManager().getInputByName("mouse.left").isPressed() && (System.currentTimeMillis() - lastShot) / 1000.0d > 1.0 / (rpm / 60.0))
				{
					owner2.handleInteraction(controller.getInputsManager().getInputByName("shootGun"), controller);
					lastShot = System.currentTimeMillis();
				}
			}
		}
	}

	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input, Controller controller)
	{
		if(input.getName().startsWith("mouse."))
		{
			System.out.println(input);
			return true;
		}
		if (input.getName().equals("shootGun"))
		{
			if (user instanceof EntityLiving)
			{
				EntityLiving shooter = (EntityLiving) user;
				//Jerk client view a bit
				if(shooter.getWorld() instanceof WorldClient)
				{
					EntityComponentRotation rot = ((EntityLiving) user).getEntityRotationComponent();
					
					rot.applyInpulse((Math.random() - 0.5) * 3.0, -(Math.random() - 0.25) * 5.0);
					
					//rot.setRotation(rot.getHorizontalRotation() + (Math.random() - 0.5) * 3.0, rot.getVerticalRotation() - (Math.random() - 0.25) * 5.0);
				}
				
				//Play sounds
				if (shooter.getWorld() instanceof WorldClient)
					shooter.getWorld().playSoundEffect("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f);
				if (shooter.getWorld() instanceof WorldMaster)
					((WorldMaster) shooter.getWorld()).playSoundEffectExcluding("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f, controller);

				//Raytrace shot
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if (shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

				//Iterate over each found entities
				Iterator<Entity> shotEntities = user.getWorld().rayTraceEntities(eyeLocation, shooter.getDirectionLookingAt(), 256f);
				while (shotEntities.hasNext())
				{
					Entity shotEntity = shotEntities.next();
					//Don't shoot itself
					if (!shotEntity.equals(shooter))
					{
						//Get hit location
						Vector3d hitPoint = shotEntity.collidesWith(eyeLocation, shooter.getDirectionLookingAt());

						//Spawn blood
						Vector3d bloodDir = shooter.getDirectionLookingAt().normalize().scale(0.25);
						for (int i = 0; i < 25; i++)
						{
							Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
							random.scale(0.25);
							random.add(bloodDir);
							shooter.getWorld().addParticle(new ParticleBlood(shooter.getWorld(), hitPoint, random));
						}

					}
				}

				shooter.getWorld().addParticle(new ParticleMuzzleFlash(shooter.getWorld(), eyeLocation));
				return true;
			}
		}
		return false;
	}
}
