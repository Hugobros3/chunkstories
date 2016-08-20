package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
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
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemAk47 extends Item implements DamageCause
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
					Client.getInstance().getPluginsManager().fireEvent(event);
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
		if (input.getName().startsWith("mouse."))
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
				if (shooter.getWorld() instanceof WorldClient)
				{
					EntityComponentRotation rot = ((EntityLiving) user).getEntityRotationComponent();

					rot.applyInpulse((Math.random() - 0.5) * 3.0, -(Math.random() - 0.25) * 5.0);

					//rot.setRotation(rot.getHorizontalRotation() + (Math.random() - 0.5) * 3.0, rot.getVerticalRotation() - (Math.random() - 0.25) * 5.0);
				}

				//Play sounds
				if (controller != null)
					controller.getSoundManager().playSoundEffect("weapons/ak47/shootNear.ogg", user.getLocation(), 1.0f, 1.0f).setAttenuationEnd(150f);

				/*if (shooter.getWorld() instanceof WorldClient)
					shooter.getWorld().playSoundEffect("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f);
				if (shooter.getWorld() instanceof WorldMaster)
					((WorldMaster) shooter.getWorld()).playSoundEffectExcluding("sfx/shoot.ogg", user.getLocation(), 1.0f, 1.0f, controller);*/

				//Raytrace shot

				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if (shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

				//Hitreg takes place on server bois
				if (shooter.getWorld() instanceof WorldMaster)
				{
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

							//Deal damage
							((EntityLiving) shotEntity).damage(this, 35f);
							
							//Spawn blood particles
							Vector3d bloodDir = shooter.getDirectionLookingAt().normalize().scale(0.25);
							for (int i = 0; i < 25; i++)
							{
								Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
								random.scale(0.25);
								random.add(bloodDir);

								shooter.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
								//((BloodData) shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("blood"), hitPoint)).setVelocity(random);
							}

							//Spawn blood on walls
							shooter.getWorld().getDecalsManager().drawDecal(hitPoint, bloodDir, new Vector3d(3.0), "blood");
						}
					}
				}

				//Find wall collision
				Location shotBlock = user.getWorld().raytraceSolid(eyeLocation, shooter.getDirectionLookingAt(), 256f);
				if (shotBlock != null)
				{
					Location shotBlockOuter = user.getWorld().raytraceSolidOuter(eyeLocation, shooter.getDirectionLookingAt(), 256f);
					if (shotBlockOuter != null)
					{
						Vector3d normal = shotBlockOuter.sub(shotBlock);

						double NbyI2x = 2.0 * Vector3d.dot(shooter.getDirectionLookingAt(), normal);
						Vector3d NxNbyI2x = new Vector3d(normal);
						NxNbyI2x.scale(NbyI2x);

						Vector3d reflected = new Vector3d();
						Vector3d.sub(shooter.getDirectionLookingAt(), NxNbyI2x, reflected);

						//System.out.println("normal: "+normal);
						//System.out.println("reflected: "+reflected);

						//shotBlock.setX(shotBlock.getX() + 1);
						int data = user.getWorld().getVoxelData(shotBlock);
						Voxel voxel = VoxelTypes.get(data);

						Vector3d nearestLocation = null;

						//This seems fine
						
						for (CollisionBox box : voxel.getTranslatedCollisionBoxes(user.getWorld(), (int) shotBlock.getX(), (int) shotBlock.getY(), (int) shotBlock.getZ()))
						{
							Vector3d thisLocation = box.collidesWith(eyeLocation, shooter.getDirectionLookingAt());
							if (thisLocation != null)
							{
								if (nearestLocation == null || nearestLocation.distanceTo(eyeLocation) > thisLocation.distanceTo(eyeLocation))
									nearestLocation = thisLocation;
							}
						}
						
						for (int i = 0; i < 25; i++)
						{
							Vector3d untouchedReflection = new Vector3d(reflected);

							Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
							random.scale(0.5);
							untouchedReflection.add(random);
							untouchedReflection.normalize();

							untouchedReflection.scale(0.25);

							Vector3d ppos = new Vector3d(nearestLocation);
							//ppos.add(untouchedReflection);
							//FragmentData fragParticule = ((FragmentData)shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("voxel_frag"), ppos));
							controller.getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", ppos, untouchedReflection);
							//fragParticule.setVelocity(untouchedReflection);
							//fragParticule.setData(data);
						}

						controller.getDecalsManager().drawDecal(nearestLocation, normal.negate(), new Vector3d(0.5), "bullethole");
					}
				}

				controller.getParticlesManager().spawnParticleAtPosition("muzzle", eyeLocation);
				//shooter.getWorld().addParticle(ParticleTypes.getParticleTypeByName("muzzle"), eyeLocation);
				//shooter.getWorld().addParticle(new ParticleMuzzleFlash(shooter.getWorld(), eyeLocation));
				return (shooter.getWorld() instanceof WorldMaster);
			}
		}
		return false;
	}
}
