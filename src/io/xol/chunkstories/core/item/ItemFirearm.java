package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.core.events.ClientInputPressedEvent;
import io.xol.chunkstories.core.item.renderers.ObjViewModelRenderer;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.LegacyDogeZItemRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemFirearm extends Item implements DamageCause, ItemOverlay
{
	final boolean automatic;
	final double rpm;
	final String soundName;
	final double damage;
	final double accuracy;
	final double range;
	final double soundRange;
	final int shots;
	final double shake;
	final long reloadCooldown;

	final boolean scopedWeapon;
	final float scopeZoom;
	final float scopeSlow;
	final String scopeTexture;

	private boolean wasTriggerPressedLastTick = false;
	private long lastShot = 0L;

	private long cooldownEnd = 0L;

	private boolean isScoped = false;

	private ItemPile currentMagazine;

	public ItemFirearm(ItemType type)
	{
		super(type);

		automatic = type.getProperty("fireMode", "semiauto").equals("fullauto");
		rpm = Double.parseDouble(type.getProperty("roundsPerMinute", "60.0"));
		soundName = type.getProperty("fireSound", "sounds/sfx/shoot.ogg");

		damage = Double.parseDouble(type.getProperty("damage", "1.0"));
		accuracy = Double.parseDouble(type.getProperty("accuracy", "0.0"));
		range = Double.parseDouble(type.getProperty("range", "1000.0"));
		soundRange = Double.parseDouble(type.getProperty("soundRange", "1000.0"));

		reloadCooldown = Long.parseLong(type.getProperty("reloadCooldown", "150"));

		shots = Integer.parseInt(type.getProperty("shots", "1"));
		shake = Double.parseDouble(type.getProperty("shake", accuracy / 4.0 + ""));

		scopedWeapon = type.getProperty("scoped", "false").equals("true");
		scopeZoom = Float.parseFloat(type.getProperty("scopeZoom", "2.0"));
		scopeSlow = Float.parseFloat(type.getProperty("scopeSlow", "2.0"));

		scopeTexture = type.getProperty("scopeTexture", "./textures/gui/scope.png");

		String modelName = type.getProperty("modelObj", "none");
		if (!modelName.equals("none"))
		{
			itemRenderer = new ObjViewModelRenderer(this, modelName, type.getProperty("modelDiffuse", "none"));
		}
		else
			itemRenderer = new LegacyDogeZItemRenderer(this);

		if (scopedWeapon)
			itemRenderer = new ScopedWeaponItemRenderer(itemRenderer);
	}

	class ScopedWeaponItemRenderer implements ItemRenderer
	{

		ItemRenderer itemRenderer;

		public ScopedWeaponItemRenderer(ItemRenderer itemRenderer)
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
			if (pile.getInventory() != null)
			{
				if (pile.getInventory().getHolder() != null)
				{
					Entity clientEntity = Client.getInstance().getClientSideController().getControlledEntity();
					if (isScoped() && clientEntity.equals(pile.getInventory().getHolder()))
						return;
				}
			}
			itemRenderer.renderItemInWorld(renderingInterface, pile, world, location, handTransformation);
		}
	}

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
				if (controller.getInputsManager().getInputByName("mouse.left").isPressed())
				{
					//Check for bullet presence (or creative mode)
					boolean bulletPresence = (owner instanceof EntityCreative && ((EntityCreative) owner).isCreativeMode()) || checkBullet(itemPile);
					if (!bulletPresence && !wasTriggerPressedLastTick)
					{
						//Play sounds
						if (controller != null)
							controller.getSoundManager().playSoundEffect("sounds/dogez/weapon/default/dry.ogg", owner.getLocation(), 1.0f, 1.0f).setAttenuationEnd((float) soundRange);
						//Dry.ogg
						//return;
					}
					else if ((automatic || !wasTriggerPressedLastTick) && (System.currentTimeMillis() - lastShot) / 1000.0d > 1.0 / (rpm / 60.0))
					{
						//Fire virtual input
						ClientInputPressedEvent event = new ClientInputPressedEvent(controller.getInputsManager().getInputByName("shootGun"));
						Client.getInstance().getPluginsManager().fireEvent(event);
						lastShot = System.currentTimeMillis();
					}
				}

				isScoped = this.isScopedWeapon() && controller.getInputsManager().getInputByName("mouse.right").isPressed();

				wasTriggerPressedLastTick = controller.getInputsManager().getInputByName("mouse.left").isPressed();
			}
		}
	}

	@Override
	public boolean handleInteraction(Entity user, ItemPile pile, Input input, Controller controller)
	{
		//Don't do anything with the left mouse click
		if (input.getName().startsWith("mouse."))
		{
			return true;
		}
		if (input.getName().equals("shootGun"))
		{
			if (user instanceof EntityLiving)
			{
				EntityLiving shooter = (EntityLiving) user;

				//Serverside checks
				//if (user.getWorld() instanceof WorldMaster)
				{
					//Is the reload cooldown done
					if (cooldownEnd > System.currentTimeMillis())
						return false;

					//Do we have any bullets to shoot
					boolean bulletPresence = (user instanceof EntityCreative && ((EntityCreative) user).isCreativeMode()) || checkBullet(pile);
					if (!bulletPresence)
					{
						//Dry.ogg
						return true;
					}
					else if(!(user instanceof EntityCreative && ((EntityCreative) user).isCreativeMode()))
					{
						consumeBullet(pile);
					}
				}

				//Jerk client view a bit
				if (shooter.getWorld() instanceof WorldClient)
				{
					EntityComponentRotation rot = ((EntityLiving) user).getEntityRotationComponent();
					rot.applyInpulse(shake * (Math.random() - 0.5) * 3.0, shake * -(Math.random() - 0.25) * 5.0);
				}

				//Play sounds
				if (controller != null)
					controller.getSoundManager().playSoundEffect(this.soundName, user.getLocation(), 1.0f, 1.0f).setAttenuationEnd((float) soundRange);

				//Raytrace shot
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				if (shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

				//For each shot
				for (int ss = 0; ss < shots; ss++)
				{
					Vector3d direction = shooter.getDirectionLookingAt();
					direction.add(new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().scale(accuracy / 100d));
					direction.normalize();

					//Find wall collision
					Location shotBlock = user.getWorld().raytraceSolid(eyeLocation, direction, range);

					Vector3d nearestLocation = null;

					if (shotBlock != null)
					{
						Location shotBlockOuter = user.getWorld().raytraceSolidOuter(eyeLocation, direction, range);
						if (shotBlockOuter != null)
						{
							Vector3d normal = shotBlockOuter.sub(shotBlock);

							double NbyI2x = 2.0 * Vector3d.dot(direction, normal);
							Vector3d NxNbyI2x = new Vector3d(normal);
							NxNbyI2x.scale(NbyI2x);

							Vector3d reflected = new Vector3d();
							Vector3d.sub(direction, NxNbyI2x, reflected);

							//shotBlock.setX(shotBlock.getX() + 1);
							int data = user.getWorld().getVoxelData(shotBlock);
							Voxel voxel = Voxels.get(data);

							//This seems fine

							for (CollisionBox box : voxel.getTranslatedCollisionBoxes(user.getWorld(), (int) shotBlock.getX(), (int) shotBlock.getY(), (int) shotBlock.getZ()))
							{
								Vector3d thisLocation = box.collidesWith(eyeLocation, direction);
								if (thisLocation != null)
								{
									if (nearestLocation == null || nearestLocation.distanceTo(eyeLocation) > thisLocation.distanceTo(eyeLocation))
										nearestLocation = thisLocation;
								}
							}

							//Position adjustements so shot blocks always shoot proper particles
							if (shotBlock.getX() - nearestLocation.getX() <= -1.0)
								nearestLocation.add(-0.01, 0, 0);
							if (shotBlock.getY() - nearestLocation.getY() <= -1.0)
								nearestLocation.add(0, -0.01, 0);
							if (shotBlock.getZ() - nearestLocation.getZ() <= -1.0)
								nearestLocation.add(0, 0, -0.01);

							for (int i = 0; i < 25; i++)
							{
								Vector3d untouchedReflection = new Vector3d(reflected);

								Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
								random.scale(0.5);
								untouchedReflection.add(random);
								untouchedReflection.normalize();

								untouchedReflection.scale(0.25);

								Vector3d ppos = new Vector3d(nearestLocation);
								controller.getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", ppos, untouchedReflection);
							}

							controller.getDecalsManager().drawDecal(nearestLocation, normal.negate(), new Vector3d(0.5), "bullethole");
						}
					}

					//Hitreg takes place on server bois
					if (shooter.getWorld() instanceof WorldMaster)
					{
						//Iterate over each found entities
						Iterator<Entity> shotEntities = user.getWorld().rayTraceEntities(eyeLocation, direction, 256f);
						while (shotEntities.hasNext())
						{
							Entity shotEntity = shotEntities.next();
							//Don't shoot itself & only living things get shot
							if (!shotEntity.equals(shooter) && shotEntity instanceof EntityLiving)
							{
								//Get hit location
								Vector3d hitPoint = shotEntity.collidesWith(eyeLocation, direction);

								//Deal damage
								((EntityLiving) shotEntity).damage(shooter, (float) damage);

								//Spawn blood particles
								Vector3d bloodDir = direction.normalize().scale(0.25);
								for (int i = 0; i < 250; i++)
								{
									Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
									random.scale(0.25);
									random.add(bloodDir);

									shooter.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
								}

								//Spawn blood on walls
								if (nearestLocation != null)
									shooter.getWorld().getDecalsManager().drawDecal(nearestLocation, bloodDir, new Vector3d(3.0), "blood");
							}
						}
					}

				}

				controller.getParticlesManager().spawnParticleAtPosition("muzzle", eyeLocation);
				return (shooter.getWorld() instanceof WorldMaster);
			}
		}
		return false;
	}

	private boolean checkBullet(ItemPile weaponInstance)
	{
		if (currentMagazine == null)
			if (!findMagazine(weaponInstance))
				return false;

		if (currentMagazine.getAmount() <= 0)
		{
			currentMagazine = null;
			return false;
		}

		return true;

	}

	private void consumeBullet(ItemPile weaponInstance)
	{
		assert currentMagazine != null;

		currentMagazine.setAmount(currentMagazine.getAmount() - 1);

		if (currentMagazine.getAmount() <= 0)
		{
			currentMagazine.getInventory().setItemPileAt(currentMagazine.getX(), currentMagazine.getY(), null);
			currentMagazine = null;

			//Set reload cooldown
			if (findMagazine(weaponInstance))
				cooldownEnd = System.currentTimeMillis() + this.reloadCooldown;
		}
	}

	private boolean findMagazine(ItemPile weaponInstance)
	{
		Inventory inventory = weaponInstance.getInventory();
		for (ItemPile pile : inventory)
		{
			if (pile != null && pile.getItem() instanceof ItemFirearmMagazine)
			{
				ItemFirearmMagazine magazineItem = (ItemFirearmMagazine) pile.getItem();
				if (magazineItem.isSuitableFor(this) && pile.getAmount() > 0)
				{
					currentMagazine = pile;
					break;
				}
			}
		}
		return currentMagazine != null;
	}

	@Override
	public void drawItemOverlay(RenderingInterface renderingInterface, ItemPile pile)
	{
		EntityLiving clientControlledEntity = (EntityLiving) Client.getInstance().getClientSideController().getControlledEntity();
		if (clientControlledEntity != null && pile.getInventory() != null && pile.getInventory().getHolder() != null && pile.getInventory().getHolder().equals(clientControlledEntity))
		{
			if (isScoped())
				drawScope(renderingInterface);

			Vector3d eyeLocation = new Vector3d(clientControlledEntity.getLocation());
			if (clientControlledEntity instanceof EntityPlayer)
				eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) clientControlledEntity).eyePosition, 0.0));

			Vector3d direction = clientControlledEntity.getDirectionLookingAt();
			direction.add(new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().scale(accuracy / 100d));
			direction.normalize();

			Location shotBlock = clientControlledEntity.getWorld().raytraceSolid(eyeLocation, direction, 5000);

			String dist = "-1m";
			if (shotBlock != null)
				dist = Math.floor(shotBlock.distanceTo(clientControlledEntity.getLocation())) + "m";

			//renderingInterface.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial11px, GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, dist, 1);

			//display reload cooldownt text
			if (cooldownEnd > System.currentTimeMillis())
			{
				String reloadText = "Reloading weapon, please wait";
				int cooldownLength = TrueTypeFont.arial11px.getWidth(reloadText);
				renderingInterface.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial11px, -cooldownLength + GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, reloadText, 2);
			}
		}
	}

	private void drawScope(RenderingInterface renderingInterface)
	{
		//Temp, rendering interface should provide us
		int min = Math.min(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		int max = Math.max(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);

		int bandwidth = (max - min) / 2;
		int x = 0;

		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += bandwidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, false, new Vector4f(0.0, 0.0, 0.0, 1.0));
		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += min, GameWindowOpenGL.windowHeight, 0, 1, 1, 0, TexturesHandler.getTexture(scopeTexture), false, false, null);
		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += bandwidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, false, new Vector4f(0.0, 0.0, 0.0, 1.0));
	}

	public boolean isScoped()
	{
		return isScoped;
	}

	public boolean isScopedWeapon()
	{
		return scopedWeapon;
	}

	public float getScopeZoom()
	{
		return scopeZoom;
	}

	public float getScopeSlow()
	{
		return scopeSlow;
	}
}
