package io.xol.chunkstories.core.item;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityLiving.HitBox;
import io.xol.chunkstories.api.entity.components.EntityComponentRotation;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.item.interfaces.ItemCustomHoldingAnimation;
import io.xol.chunkstories.api.item.interfaces.ItemOverlay;
import io.xol.chunkstories.api.item.interfaces.ItemZoom;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.item.renderer.ItemRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.item.renderer.FlatIconItemRenderer;
import io.xol.chunkstories.item.renderer.ObjViewModelRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemFirearm extends ItemWeapon implements ItemOverlay, ItemZoom, ItemCustomHoldingAnimation
{
	public final boolean automatic;
	public final double rpm;
	public final String soundName;
	public final double damage;
	public final double accuracy;
	public final double range;
	public final double soundRange;
	public final int shots;
	public final double shake;
	public final long reloadCooldown;

	public final boolean scopedWeapon;
	public final float scopeZoom;
	public final float scopeSlow;
	public final String scopeTexture;
	
	public final String holdingAnimationName;
	public final String shootingAnimationName;
	public final long shootingAnimationDuration;

	private boolean wasTriggerPressedLastTick = false;
	private long lastShot = 0L;

	private long cooldownEnd = 0L;
	private long animationStart = 0L;
	private long animationCooldownEnd = 0L;

	private boolean isScoped = false;

	private ItemPile currentMagazine;

	public ItemFirearm(ItemType type)
	{
		super(type);

		automatic = type.resolveProperty("fireMode", "semiauto").equals("fullauto");
		rpm = Double.parseDouble(type.resolveProperty("roundsPerMinute", "60.0"));
		soundName = type.resolveProperty("fireSound", "sounds/sfx/shoot.ogg");

		damage = Double.parseDouble(type.resolveProperty("damage", "1.0"));
		accuracy = Double.parseDouble(type.resolveProperty("accuracy", "0.0"));
		range = Double.parseDouble(type.resolveProperty("range", "1000.0"));
		soundRange = Double.parseDouble(type.resolveProperty("soundRange", "1000.0"));

		reloadCooldown = Long.parseLong(type.resolveProperty("reloadCooldown", "150"));

		shots = Integer.parseInt(type.resolveProperty("shots", "1"));
		shake = Double.parseDouble(type.resolveProperty("shake", accuracy / 4.0 + ""));

		scopedWeapon = type.resolveProperty("scoped", "false").equals("true");
		scopeZoom = Float.parseFloat(type.resolveProperty("scopeZoom", "2.0"));
		scopeSlow = Float.parseFloat(type.resolveProperty("scopeSlow", "2.0"));

		scopeTexture = type.resolveProperty("scopeTexture", "./textures/gui/scope.png");
		
		holdingAnimationName = type.resolveProperty("holdingAnimationName", "./animations/human/holding-rifle.bvh");
		shootingAnimationName = type.resolveProperty("shootingAnimationName", "./animations/human/human_shoot_pistol.bvh");
		
		shootingAnimationDuration = Long.parseLong(type.resolveProperty("shootingAnimationDuration", "200"));
	}
	
	/** Some weapons have fancy renderers */
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer)
	{
		ItemRenderer itemRenderer;
		
		String modelName = getType().resolveProperty("modelObj", "none");
		if (!modelName.equals("none"))
			itemRenderer = new ObjViewModelRenderer(this, fallbackRenderer, modelName, getType().resolveProperty("modelDiffuse", "none"));
		else
			itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getType());

		if (scopedWeapon)
			itemRenderer = new ScopedWeaponItemRenderer(itemRenderer);
		
		return itemRenderer;
	}
	
	/** Displays a scope sometimes */
	class ScopedWeaponItemRenderer extends ItemRenderer
	{
		ItemRenderer actualRenderer;
		public ScopedWeaponItemRenderer(ItemRenderer itemRenderer)
		{
			super(null);
			this.actualRenderer = itemRenderer;
		}

		@Override
		public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, float screenPositionX, float screenPositionY, int scaling)
		{
			actualRenderer.renderItemInInventory(renderingInterface, pile, screenPositionX, screenPositionY, scaling);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
		{
			if (pile.getInventory() != null)
			{
				if (pile.getInventory().getHolder() != null)
				{
					Entity clientEntity = renderingInterface.getClient().getPlayer().getControlledEntity();
					ItemFirearm item = (ItemFirearm) pile.getItem();
					
					if (item.isScoped() && clientEntity.equals(pile.getInventory().getHolder()))
						return;
				}
			}
			actualRenderer.renderItemInWorld(renderingInterface, pile, world, location, handTransformation);
		}
	}

	/**
	 * Should be called when the owner has this item selected
	 * 
	 * @param owner
	 */
	@Override
	public void tickInHand(Entity owner, ItemPile itemPile)
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
				
				PlayerClient playerClient = (PlayerClient)controller;
				
				if (playerClient.getInputsManager().getInputByName("mouse.left").isPressed())
				{
					//Check for bullet presence (or creative mode)
					boolean bulletPresence = (owner instanceof EntityCreative && ((EntityCreative) owner).isCreativeMode()) || checkBullet(itemPile);
					if (!bulletPresence && !wasTriggerPressedLastTick)
					{
						//Play sounds
						if (playerClient != null)
							playerClient.getSoundManager().playSoundEffect("sounds/dogez/weapon/default/dry.ogg", owner.getLocation(), 1.0f, 1.0f, 1f, (float) soundRange);
						//Dry.ogg
						//return;
					}
					else if ((automatic || !wasTriggerPressedLastTick) && (System.currentTimeMillis() - lastShot) / 1000.0d > 1.0 / (rpm / 60.0))
					{
						//Fire virtual input
						//ClientInputPressedEvent event = new ClientInputPressedEvent(controller.getInputsManager().getInputByName("shootGun"));
						//Client.getInstance().getPluginManager().fireEvent(event);

						playerClient.getInputsManager().onInputPressed(controller.getInputsManager().getInputByName("shootGun"));

						lastShot = System.currentTimeMillis();
					}
				}

				isScoped = this.isScopedWeapon() && controller.getInputsManager().getInputByName("mouse.right").isPressed();

				wasTriggerPressedLastTick = controller.getInputsManager().getInputByName("mouse.left").isPressed();
			}
		}
	}

	@Override
	public boolean onControllerInput(Entity user, ItemPile pile, Input input, Controller controller)
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
					else if (!(user instanceof EntityCreative && ((EntityCreative) user).isCreativeMode()))
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
				{
					controller.getSoundManager().playSoundEffect(this.soundName, (float) (double) user.getLocation().x(), (float) (double) user.getLocation().y(), (float) (double) user.getLocation().z(), 1.0f, 1.0f, 1.0f,
							(float) soundRange);
				}
				
				playAnimation();

				//Raytrace shot
				Vector3d eyeLocation = new Vector3d(shooter.getLocation());
				//eyeLocation.add(0.25, 0.0, 0.25);

				if (shooter instanceof EntityPlayer)
					eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) shooter).eyePosition, 0.0));

				//For each shot
				for (int ss = 0; ss < shots; ss++)
				{
					Vector3d direction = new Vector3d(shooter.getDirectionLookingAt());
					direction.add(new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().mul(accuracy / 100d));
					direction.normalize();

					//Find wall collision
					Location shotBlock = user.getWorld().collisionsManager().raytraceSolid(eyeLocation, direction, range);

					Vector3dc nearestLocation = null;

					//Loops to try and break blocks
					while(user.getWorld() instanceof WorldMaster && shotBlock != null)
					{
						int data = user.getWorld().getVoxelData(shotBlock);
						Voxel voxel = VoxelsStore.get().getVoxelById(data);
						
						if(voxel.getId() != 0 && voxel.getMaterial().resolveProperty("bulletBreakable") != null && voxel.getMaterial().resolveProperty("bulletBreakable").equals("true"))
						{
							//Spawn an event to check if it's okay
							
							//Destroy it
							user.getWorld().setVoxelData(shotBlock, 0);
							for(int i = 0; i < 25; i++)
							{
								Vector3d smashedVoxelParticleDirection = new Vector3d(direction);
								smashedVoxelParticleDirection.mul(2.0);
								smashedVoxelParticleDirection.add(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
								smashedVoxelParticleDirection.normalize();
								
								user.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", shotBlock, smashedVoxelParticleDirection);
							}
							user.getWorld().getSoundManager().playSoundEffect("sounds/sfx/glass.ogg", shotBlock, (float)Math.random() * 0.2f + 0.9f, 1.0f);
							
							//Re-raytrace the ray
							shotBlock = user.getWorld().collisionsManager().raytraceSolid(eyeLocation, direction, range);
						}
						else
							break;
					}
					
					if (shotBlock != null)
					{
						Location shotBlockOuter = user.getWorld().collisionsManager().raytraceSolidOuter(eyeLocation, direction, range);
						
						if (shotBlockOuter != null)
						{
							Vector3d normal = shotBlockOuter.sub(shotBlock);

							double NbyI2x = 2.0 * direction.dot(normal);
							Vector3d NxNbyI2x = new Vector3d(normal);
							NxNbyI2x.mul(NbyI2x);

							Vector3d reflected = new Vector3d(direction);
							reflected.sub(NxNbyI2x);
							//Vector3d.sub(direction, NxNbyI2x, reflected);

							//shotBlock.setX(shotBlock.getX() + 1);
							int data = user.getWorld().getVoxelData(shotBlock);
							Voxel voxel = VoxelsStore.get().getVoxelById(data);

							//This seems fine

							for (CollisionBox box : voxel.getTranslatedCollisionBoxes(user.getWorld(), (int) (double) shotBlock.x(), (int) (double) shotBlock.y(), (int) (double) shotBlock.z()))
							{
								Vector3dc thisLocation = box.lineIntersection(eyeLocation, direction);
								if (thisLocation != null)
								{
									if (nearestLocation == null || nearestLocation.distance(eyeLocation) > thisLocation.distance(eyeLocation))
										nearestLocation = thisLocation;
								}
							}

							Vector3d particleSpawnPosition = new Vector3d(nearestLocation);
							
							//Position adjustements so shot blocks always shoot proper particles
							if (shotBlock.x() - particleSpawnPosition.x() <= -1.0)
								particleSpawnPosition.add(-0.01, 0d, 0d);
							if (shotBlock.y() - particleSpawnPosition.y() <= -1.0)
								particleSpawnPosition.add(0d, -0.01, 0d);
							if (shotBlock.z() - particleSpawnPosition.z() <= -1.0)
								particleSpawnPosition.add(0d, 0d, -0.01);

							for (int i = 0; i < 25; i++)
							{
								Vector3d untouchedReflection = new Vector3d(reflected);

								Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
								random.mul(0.5);
								untouchedReflection.add(random);
								untouchedReflection.normalize();

								untouchedReflection.mul(0.25);

								//Vector3d ppos = new Vector3d(particleSpawnPosition);
								controller.getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", particleSpawnPosition, untouchedReflection);

							}
							
							controller.getSoundManager().playSoundEffect(VoxelsStore.get().getVoxelById(shotBlock.getVoxelDataAtLocation()).getMaterial().resolveProperty("landingSounds"), particleSpawnPosition, 1, 0.05f);
							
							/*double bspeed = 5/60.0 * (1 + Math.random() * 3 * Math.random());
							Vector3d ppos = new Vector3d(reflected);
							ppos.normalize();
							ppos.scale(0.5);
							ppos.add(nearestLocation);
							WorldEffects.createFireball(shooter.getWorld(), ppos, 1f, damage*0.15*bspeed, (float) (0.0 + 0.05*damage));
							*/
							
							controller.getDecalsManager().drawDecal(nearestLocation, normal.negate(), new Vector3d(0.5), "bullethole");
						}
					}
					
					//Hitreg takes place on server bois
					if (shooter.getWorld() instanceof WorldMaster)
					{
						//Iterate over each found entities
						Iterator<Entity> shotEntities = user.getWorld().collisionsManager().rayTraceEntities(eyeLocation, direction, 256f);
						while (shotEntities.hasNext())
						{
							Entity shotEntity = shotEntities.next();
							//Don't shoot itself & only living things get shot
							if (!shotEntity.equals(shooter) && shotEntity instanceof EntityLiving)
							{
								
								//Get hit location
								for (HitBox hitBox : ((EntityLiving) shotEntity).getHitBoxes())
								{
									Vector3dc hitPoint = hitBox.lineIntersection(eyeLocation, direction);
									
									if(hitPoint == null)
										continue;

									//System.out.println("shot" + hitBox.getName());

									//Deal damage
									((EntityLiving) shotEntity).damage(pileAsDamageCause(pile), hitBox, (float) damage);

									//Spawn blood particles
									Vector3d bloodDir = direction.normalize().mul(0.75);
									for (int i = 0; i < 120; i++)
									{
										Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
										random.mul(0.25);
										random.add(bloodDir);

										shooter.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
									}

									//Spawn blood on walls
									if (nearestLocation != null)
										shooter.getWorld().getDecalsManager().drawDecal(nearestLocation, bloodDir, new Vector3d(Math.min(3, shots) * damage / 20f), "blood");
								}
							}
						}
					}

				}

				controller.getParticlesManager().spawnParticleAtPosition("muzzle", eyeLocation);
				
				FirearmShotEvent event = new FirearmShotEvent(this, shooter, controller);
				shooter.getWorld().getGameContext().getPluginManager().fireEvent(event);
				
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
		EntityLiving clientControlledEntity = (EntityLiving) renderingInterface.getClient().getPlayer().getControlledEntity();
		if (clientControlledEntity != null && pile.getInventory() != null && pile.getInventory().getHolder() != null && pile.getInventory().getHolder().equals(clientControlledEntity))
		{
			if (isScoped())
				drawScope(renderingInterface);

			Vector3d eyeLocation = new Vector3d(clientControlledEntity.getLocation());
			if (clientControlledEntity instanceof EntityPlayer)
				eyeLocation.add(new Vector3d(0.0, ((EntityPlayer) clientControlledEntity).eyePosition, 0.0));

			Vector3d direction = new Vector3d(clientControlledEntity.getDirectionLookingAt());
			direction.add(new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().mul(accuracy / 100d));
			direction.normalize();

			//Location shotBlock = clientControlledEntity.getWorld().raytraceSolid(eyeLocation, direction, 5000);

			//String dist = "-1m";
			//if (shotBlock != null)
			//	dist = Math.floor(shotBlock.distanceTo(clientControlledEntity.getLocation())) + "m";

			//renderingInterface.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial11px, renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, dist, 1);

			//display reload cooldownt text
			if (cooldownEnd > System.currentTimeMillis())
			{
				String reloadText = "Reloading weapon, please wait";
				
				Font font = renderingInterface.getFontRenderer().getFont("arial", 11);
				
				int cooldownLength = font.getWidth(reloadText);//TrueTypeFont.arial11px.getWidth(reloadText);
				renderingInterface.getFontRenderer().drawString(font, -cooldownLength + renderingInterface.getWindow().getWidth() / 2, renderingInterface.getWindow().getHeight() / 2, reloadText, 2);
			}
		}
	}

	private void drawScope(RenderingInterface renderingInterface)
	{
		//Temp, rendering interface should provide us
		int min = Math.min(renderingInterface.getWindow().getWidth(), renderingInterface.getWindow().getHeight());
		int max = Math.max(renderingInterface.getWindow().getWidth(), renderingInterface.getWindow().getHeight());

		int bandwidth = (max - min) / 2;
		int x = 0;

		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += bandwidth, renderingInterface.getWindow().getHeight(), 0, 0, 0, 0, null, false, false, new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += min, renderingInterface.getWindow().getHeight(), 0, 1, 1, 0, renderingInterface.textures().getTexture(scopeTexture), false, false, null);
		renderingInterface.getGuiRenderer().drawBoxWindowsSpace(x, 0, x += bandwidth, renderingInterface.getWindow().getHeight(), 0, 0, 0, 0, null, false, false, new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
	}

	public boolean isScoped()
	{
		return isScoped;
	}

	public boolean isScopedWeapon()
	{
		return scopedWeapon;
	}

	public float getZoomFactor()
	{
		return isScoped ? scopeZoom : 1f;
	}

	public float getScopeSlow()
	{
		return scopeSlow;
	}

	public void playAnimation() {
		this.animationStart = System.currentTimeMillis();
		this.animationCooldownEnd = this.animationStart + this.shootingAnimationDuration;
	}

	@Override
	public String getCustomAnimationName()
	{
		if(this.animationCooldownEnd > System.currentTimeMillis())
			return this.shootingAnimationName;
		return holdingAnimationName;
	}

	@Override
	public double transformAnimationTime(double originalTime) {
		if(this.animationCooldownEnd > System.currentTimeMillis()) {
			long delta = System.currentTimeMillis() - this.animationStart;
			return delta;
			/*
			double deltaD = delta;
			double durationD = 200;//this.shootingAnimationDuration;
			double time = deltaD / durationD;
			time *= 200;
			//System.out.println(time);
			return time;*/
		}
		
		return originalTime;
	}
}
