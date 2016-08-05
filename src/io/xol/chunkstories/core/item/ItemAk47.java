package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.core.events.ClientInputPressedEvent;
import io.xol.chunkstories.core.item.renderers.Ak47ViewModelRenderer;
import io.xol.chunkstories.core.particles.ParticleBlood.BloodData;
import io.xol.chunkstories.core.particles.ParticleVoxelFragment.FragmentData;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.particles.ParticleTypes;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.VoxelTypes;
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

					ClientInputPressedEvent event = new ClientInputPressedEvent(controller.getInputsManager().getInputByName("shootGun"));
					Client.pluginsManager.fireEvent(event);
					//owner2.handleInteraction(controller.getInputsManager().getInputByName("shootGun"), controller);
					lastShot = System.currentTimeMillis();
				}
			}
		}
	}

	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input, Controller controller)
	{
		//Don't do anything with the left mouse click
		if(input.getName().startsWith("mouse."))
		{
			//System.out.println(input);
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
				if(controller != null)
					controller.getSoundManager().playSoundEffect("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f);
				
				/*if (shooter.getWorld() instanceof WorldClient)
					shooter.getWorld().playSoundEffect("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f);
				if (shooter.getWorld() instanceof WorldMaster)
					((WorldMaster) shooter.getWorld()).playSoundEffectExcluding("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f, controller);*/

				//Raytrace shot
				if(shooter.getWorld() instanceof WorldMaster)
					System.out.println("Raytracing on server");
				else
					System.out.println("Raytracing on client");
				
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if (shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

				//Iterate over each found entities
				Iterator<Entity> shotEntities = user.getWorld().rayTraceEntities(eyeLocation, shooter.getDirectionLookingAt(), 256f);
				while (shotEntities.hasNext())
				{
					Entity shotEntity = shotEntities.next();
					//Don't shoot itself & only living things get shot
					if (!shotEntity.equals(shooter) && shotEntity instanceof EntityLiving)
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

							((BloodData) shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("blood"), hitPoint)).setVelocity(random);
						}

						
						/*if(shooter.getWorld() instanceof WorldClient)
						{
							WorldClient world = (WorldClient)shooter.getWorld();
							world.getWorldRenderer().getDecalsRenderer().drawDecal(hitPoint, random, new Vector3d(2.0), "blood");
						}*/
					}
				}
				
				//Find wall collision
				Location shotBlock = user.getWorld().raytraceSolid(eyeLocation, shooter.getDirectionLookingAt(), 256f);
				if(shotBlock != null)
				{
					Location shotBlockOuter = user.getWorld().raytraceSolidOuter(eyeLocation, shooter.getDirectionLookingAt(), 256f);
					if(shotBlockOuter != null)
					{
						Vector3d normal = shotBlockOuter.sub(shotBlock);
						
						double NbyI2x = 2.0 * Vector3d.dot(shooter.getDirectionLookingAt(), normal);
						Vector3d NxNbyI2x = new Vector3d(normal);
						NxNbyI2x.scale(NbyI2x);
						
						Vector3d reflected = new Vector3d();
						Vector3d.sub(shooter.getDirectionLookingAt(), NxNbyI2x, reflected);
						
						//System.out.println("normal: "+normal);
						//System.out.println("reflected: "+reflected);
						
						int data = user.getWorld().getVoxelData(shotBlock);
						Voxel voxel = VoxelTypes.get(data);
						
						Vector3d nearestLocation = null;
						
						for(CollisionBox box : voxel.getTranslatedCollisionBoxes(user.getWorld(), (int)shotBlock.getX(), (int)shotBlock.getY(), (int)shotBlock.getZ()))
						{
							Vector3d thisLocation = box.collidesWith(eyeLocation, shooter.getDirectionLookingAt());
							if(thisLocation != null)
							{
								if(nearestLocation == null || nearestLocation.distanceTo(eyeLocation) > thisLocation.distanceTo(eyeLocation))
									nearestLocation = thisLocation;
							}
						}
						
						for (int i = 0; i < 25; i++)
						{
							//System.out.println("pp");
							Vector3d untouchedReflection = new Vector3d(reflected);
							
							Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
							random.scale(0.5);
							untouchedReflection.add(random);
							untouchedReflection.normalize();
							
							untouchedReflection.scale(0.25);
							
							Vector3d ppos = new Vector3d(nearestLocation);
							ppos.add(untouchedReflection);
							FragmentData fragParticule = ((FragmentData)shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("voxel_frag"), ppos));
							
							fragParticule.setVelocity(untouchedReflection);
							fragParticule.setData(data);
						}
						
						if(shooter.getWorld() instanceof WorldClient)
						{
							WorldClient world = (WorldClient)shooter.getWorld();
							//System.out.println("normal"+normal);
							world.getWorldRenderer().getDecalsRenderer().drawDecal(nearestLocation, normal.negate(), new Vector3d(0.5), "bullethole");
						}
					}
				}

				shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("muzzle"), eyeLocation);
				//shooter.getWorld().addParticle(new ParticleMuzzleFlash(shooter.getWorld(), eyeLocation));
				return (shooter.getWorld() instanceof WorldMaster);
			}
		}
		return false;
	}
}
