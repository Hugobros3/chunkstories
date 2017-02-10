package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.PlayerClient;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityLiving.HitBox;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.item.renderers.ObjViewModelRenderer;
import io.xol.chunkstories.item.renderer.LegacyDogeZItemRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemMeleeWeapon extends Item implements DamageCause
{
	final long swingDuration;
	final long hitTime;
	final double range;

	final float damage;

	final float itemRenderScale;

	long currentSwingStart = 0L;
	boolean hasHitYet = false;
	long cooldownEnd = 0L;

	public ItemMeleeWeapon(ItemType type)
	{
		super(type);

		swingDuration = Integer.parseInt(type.resolveProperty("swingDuration", "100"));
		hitTime = Integer.parseInt(type.resolveProperty("hitTime", "100"));

		range = Double.parseDouble(type.resolveProperty("range", "3"));
		damage = Float.parseFloat(type.resolveProperty("damage", "100"));

		itemRenderScale = Float.parseFloat(type.resolveProperty("itemRenderScale", "2"));

		String modelName = type.resolveProperty("modelObj", "none");
		if (!modelName.equals("none"))
		{
			itemRenderer = new ObjViewModelRenderer(this, modelName, type.resolveProperty("modelDiffuse", "none"));
		}
		else
			itemRenderer = new LegacyDogeZItemRenderer(this);

		itemRenderer = new MeleeWeaponRenderer(itemRenderer);
	}

	@Override
	public void tickInHand(WorldAuthority authority, Entity owner, ItemPile itemPile)
	{
		//Only happening server-side
		//if (owner.getWorld() instanceof WorldMaster)
		{
			if (currentSwingStart != 0 && !hasHitYet && (System.currentTimeMillis() - currentSwingStart > hitTime))
			{
				if (owner instanceof EntityControllable && ((EntityControllable) owner).getController() != null)
				{
					EntityControllable owner2 = ((EntityControllable) owner);
					Controller controller = owner2.getController();

					//For now only client-side players can trigger shooting actions
					if (controller instanceof PlayerClient)
					{
						if (!((PlayerClient) controller).hasFocus())
							return;

						//ClientInputPressedEvent event = new ClientInputPressedEvent(controller.getInputsManager().getInputByName("shootGun"));
						//Client.getInstance().getPluginManager().fireEvent(event);

						Client.getInstance().getInputsManager().onInputPressed(controller.getInputsManager().getInputByName("shootGun"));
						hasHitYet = true;
					}
				}

			}
		}
	}

	@Override
	public boolean handleInteraction(Entity owner, ItemPile pile, Input input, Controller controller)
	{
		if (input.getName().startsWith("mouse.left"))
		{
			//Checks current swing is done
			if (System.currentTimeMillis() - currentSwingStart > swingDuration)
			{
				currentSwingStart = System.currentTimeMillis();
				hasHitYet = false;
			}

			return true;
		}
		else if (input.getName().equals("shootGun") && owner.getWorld() instanceof WorldMaster)
		{
			//Actually hits
			EntityLiving shooter = (EntityLiving) owner;
			Vector3dm direction = shooter.getDirectionLookingAt();

			Vector3dm eyeLocation = new Vector3dm(shooter.getLocation());
			if (shooter instanceof EntityPlayer)
				eyeLocation.add(new Vector3dm(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

			//Find wall collision
			Location shotBlock = owner.getWorld().raytraceSolid(eyeLocation, direction, range);

			Vector3dm nearestLocation = null;

			if (shotBlock != null)
			{
				Location shotBlockOuter = owner.getWorld().raytraceSolidOuter(eyeLocation, direction, range);
				if (shotBlockOuter != null)
				{
					Vector3dm normal = shotBlockOuter.sub(shotBlock);

					double NbyI2x = 2.0 * direction.dot(normal);
					Vector3dm NxNbyI2x = new Vector3dm(normal);
					NxNbyI2x.scale(NbyI2x);

					Vector3dm reflected = new Vector3dm(direction);
					reflected.sub(NxNbyI2x);
					//Vector3dm.sub(direction, NxNbyI2x, reflected);

					//shotBlock.setX(shotBlock.getX() + 1);
					int data = owner.getWorld().getVoxelData(shotBlock);
					Voxel voxel = VoxelsStore.get().getVoxelById(data);

					//This seems fine

					for (CollisionBox box : voxel.getTranslatedCollisionBoxes(owner.getWorld(), (int) (double) shotBlock.getX(), (int) (double) shotBlock.getY(), (int) (double) shotBlock.getZ()))
					{
						Vector3dm thisLocation = box.lineIntersection(eyeLocation, direction);
						if (thisLocation != null)
						{
							if (nearestLocation == null || nearestLocation.distanceTo(eyeLocation) > thisLocation.distanceTo(eyeLocation))
								nearestLocation = thisLocation;
						}
					}

					//Position adjustements so shot blocks always shoot proper particles
					if (shotBlock.getX() - nearestLocation.getX() <= -1.0)
						nearestLocation.add(-0.01, 0.0, 0.0);
					if (shotBlock.getY() - nearestLocation.getY() <= -1.0)
						nearestLocation.add(0.0, -0.01, 0.0);
					if (shotBlock.getZ() - nearestLocation.getZ() <= -1.0)
						nearestLocation.add(0.0, 0.0, -0.01);

					for (int i = 0; i < 25; i++)
					{
						Vector3dm untouchedReflection = new Vector3dm(reflected);

						Vector3dm random = new Vector3dm(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
						random.scale(0.5);
						untouchedReflection.add(random);
						untouchedReflection.normalize();

						untouchedReflection.scale(0.25);

						Vector3dm ppos = new Vector3dm(nearestLocation);
						owner.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", ppos, untouchedReflection);

						owner.getWorld().getSoundManager().playSoundEffect(VoxelsStore.get().getVoxelById(shotBlock.getVoxelDataAtLocation()).getMaterial().resolveProperty("landingSounds"), ppos, 1, 0.25f);
					}

					owner.getWorld().getDecalsManager().drawDecal(nearestLocation, normal.negate(), new Vector3dm(0.5), "bullethole");
				}
			}

			//Hitreg takes place on server bois
			if (shooter.getWorld() instanceof WorldMaster)
			{
				//Iterate over each found entities
				Iterator<Entity> shotEntities = owner.getWorld().rayTraceEntities(eyeLocation, direction, range);
				while (shotEntities.hasNext())
				{
					Entity shotEntity = shotEntities.next();
					//Don't shoot itself & only living things get shot
					if (!shotEntity.equals(shooter) && shotEntity instanceof EntityLiving)
					{
						//Get hit location
						for (HitBox hitBox : ((EntityLiving) shotEntity).getHitBoxes())
						{
							Vector3dm hitPoint = hitBox.lineIntersection(eyeLocation, direction);

							if (hitPoint == null)
								continue;

							//Deal damage
							((EntityLiving) shotEntity).damage(shooter, hitBox, (float) damage);

							//Spawn blood particles
							Vector3dm bloodDir = direction.normalize().scale(0.25);
							for (int i = 0; i < 250; i++)
							{
								Vector3dm random = new Vector3dm(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
								random.scale(0.25);
								random.add(bloodDir);

								shooter.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
							}

							//Spawn blood on walls
							if (nearestLocation != null)
								shooter.getWorld().getDecalsManager().drawDecal(nearestLocation, bloodDir, new Vector3dm(3.0), "blood");
						}
					}
				}
			}

		}
		return false;
	}

	class MeleeWeaponRenderer implements ItemRenderer
	{
		private final ItemRenderer itemRenderer;

		MeleeWeaponRenderer(ItemRenderer itemRenderer)
		{
			this.itemRenderer = itemRenderer;
		}

		@Override
		public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
		{
			itemRenderer.renderItemInInventory(renderingInterface, pile, screenPositionX, screenPositionY, scaling);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
		{
			Matrix4f matrixed = new Matrix4f(handTransformation);

			float rot = 0;

			if (System.currentTimeMillis() - currentSwingStart < swingDuration)
			{
				if (hitTime == swingDuration)
				{
					//Whole thing over the same duration
					rot = (float) (0 - Math.PI / 4f * (float) (System.currentTimeMillis() - currentSwingStart) / hitTime);
				}
				else
				{
					//We didn't hit yet
					if (System.currentTimeMillis() - currentSwingStart < hitTime)
						rot = (float) (0 - Math.PI / 4f * (float) (System.currentTimeMillis() - currentSwingStart) / hitTime);
					//We did
					else
						rot = (float) (0 - Math.PI / 4f + Math.PI / 4f * (float) (System.currentTimeMillis() - currentSwingStart - hitTime) / (swingDuration - hitTime));

				}
			}

			float dekal = -0.45f;
			matrixed.translate(new Vector3fm(0, dekal, 0));
			matrixed.rotate(rot, new Vector3fm(0, 0, 1));
			matrixed.translate(new Vector3fm(0, 0.25 - dekal, 0));

			matrixed.scale(new Vector3fm(itemRenderScale));

			itemRenderer.renderItemInWorld(renderingInterface, pile, world, location, matrixed);
		}
	}
}
