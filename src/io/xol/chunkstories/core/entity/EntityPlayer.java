package io.xol.chunkstories.core.entity;

import io.xol.engine.misc.ColorsTools;
import io.xol.engine.model.ModelLibrary;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.PlayerClient;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.interfaces.ItemOverlay;
import io.xol.chunkstories.api.item.interfaces.ItemZoom;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.EntityArmorInventory.EntityWithArmor;
import io.xol.chunkstories.core.entity.components.EntityComponentController;
import io.xol.chunkstories.core.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.core.entity.components.EntityComponentFlying;
import io.xol.chunkstories.core.entity.components.EntityComponentFoodLevel;
import io.xol.chunkstories.core.entity.components.EntityComponentInventory;
import io.xol.chunkstories.core.entity.components.EntityComponentName;
import io.xol.chunkstories.core.entity.components.EntityComponentSelectedItem;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.core.item.armor.ItemArmor;
import io.xol.chunkstories.core.voxel.VoxelClimbable;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.item.inventory.InventoryAllVoxels;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.WorldRenderer.RenderingPass;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;

import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Core/Vanilla player, has all the functionality you'd want from it
 */
public class EntityPlayer extends EntityHumanoid implements EntityControllable, EntityOverlay, EntityNameable, EntityWithInventory, EntityWithSelectedItem, EntityCreative, EntityFlying, EntityWithArmor
{
	//Add the controller component to whatever else the superclass may have
	final EntityComponentController controllerComponent;

	final EntityComponentInventory inventoryComponent;
	final EntityComponentSelectedItem selectedItemComponent;

	final EntityComponentName name;
	final EntityComponentCreativeMode creativeMode;
	final EntityComponentFlying flying;
	
	final EntityArmorInventory armor;

	//FOOD
	EntityComponentFoodLevel foodLevel;
	
	protected boolean onLadder = false;
	
	protected boolean noclip = true;
	
	//Nasty bullshit
	float lastPX = -1f;
	float lastPY = -1f;

	Location lastCameraLocation;

	int variant;
	
	public EntityPlayer(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		
		controllerComponent = new EntityComponentController(this, this.getComponents().getLastComponent());
		
		inventoryComponent = new EntityComponentInventory(this, 10, 4);
		selectedItemComponent = new EntityComponentSelectedItem(this, inventoryComponent);
		
		name = new EntityComponentName(this, this.getComponents().getLastComponent());
		creativeMode = new EntityComponentCreativeMode(this, this.getComponents().getLastComponent());
		flying = new EntityComponentFlying(this, this.getComponents().getLastComponent());
		
		foodLevel = new EntityComponentFoodLevel(this, 100);
		
		armor = new EntityArmorInventory(this, 4, 1);
	}

	public EntityPlayer(WorldImplementation w, double x, double y, double z, String name)
	{
		this(w, x, y, z);
		this.name.setName(name);

		variant = ColorsTools.getUniqueColorCode(name) % 6;
	}

	protected void moveCamera(PlayerClient controller)
	{
		if (isDead())
			return;
		float cPX = controller.getInputsManager().getMouseCursorX();
		float cPY = controller.getInputsManager().getMouseCursorY();
		
		float dx = 0, dy = 0;
		if (lastPX != -1f)
		{
			dx = cPX - controller.getWindow().getWidth() / 2;
			dy = cPY - controller.getWindow().getHeight() / 2;
		}
		lastPX = cPX;
		lastPY = cPY;
		
		float rotH = this.getEntityRotationComponent().getHorizontalRotation();
		float rotV = this.getEntityRotationComponent().getVerticalRotation();

		float modifier = 1.0f;
		if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemZoom)
		{
			ItemZoom item = (ItemZoom) this.getSelectedItemComponent().getSelectedItem().getItem();
			modifier = 1.0f / item.getZoomFactor();
		}
		
		rotH += dx * modifier / 3f * RenderingConfig.mouseSensitivity;
		rotV -= dy * modifier / 3f * RenderingConfig.mouseSensitivity;
		this.getEntityRotationComponent().setRotation(rotH, rotV);
		
		controller.getInputsManager().setMouseCursorLocation(controller.getWindow().getWidth() / 2, controller.getWindow().getHeight() / 2);
	}

	// Server-side updating
	@Override
	public void tick()
	{
		//This is a controllable entity, take care of controlling
		if (world instanceof WorldClient && Client.getInstance().getPlayer().getControlledEntity() == this)
			tickClientController(Client.getInstance().getPlayer());

		//Tick item in hand if one such exists
		ItemPile pileSelected = getSelectedItemComponent().getSelectedItem();
		if (pileSelected != null)
			pileSelected.getItem().tickInHand(this, pileSelected);

		//Auto-pickups items on the ground
		if (world instanceof WorldMaster && (world.getTicksElapsed() % 60L) == 0L)
		{
			//TODO Use more precise, regional functions to not iterate over the entire world like a retard
			for (Entity e : world.getAllLoadedEntities())
			{
				if (e instanceof EntityGroundItem && e.getLocation().distanceTo(this.getLocation()) < 3.0f)
				{
					EntityGroundItem eg = (EntityGroundItem) e;
					if (!eg.canBePickedUpYet())
						continue;

					ItemPile pile = eg.getItemPile();
					if (pile != null)
					{
						ItemPile left = this.inventoryComponent.getInventory().addItemPile(pile);
						if (left == null)
							world.removeEntity(eg);
						else
							eg.setItemPile(left);
					}
				}
			}
		}

		if (world instanceof WorldMaster)
		{
			//Food/health subsystem hanled here decrease over time
			
			//Take damage when starving
			if ((world.getTicksElapsed() % 100L) == 0L)
			{
				if (this.getFoodLevel() == 0)
					this.damage(EntityComponentFoodLevel.HUNGER_DAMAGE_CAUSE, 1);
				else
				{
					//27 minutes to start starving at 0.1 starveFactor
					//Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

					//Starve slowly if inactive
					float starve = 0.03f;

					//Walking drains you
					if (this.getVelocityComponent().getVelocity().length() > 0.3)
					{
						starve = 0.06f;
						//Running is even worse
						if (this.getVelocityComponent().getVelocity().length() > 0.7)
							starve = 0.15f;
					}

					float newfoodLevel = this.getFoodLevel() - starve;
					this.setFoodLevel(newfoodLevel);
					//System.out.println("new food level:"+newfoodLevel);
				}
			}

			//It restores hp
			if (getFoodLevel() > 20 && !this.isDead())
			{
				if (this.getHealth() < this.getMaxHealth())
				{
					this.setHealth(this.getHealth() + 0.01f);

					float newfoodLevel = this.getFoodLevel() - 0.01f;
					this.setFoodLevel(newfoodLevel);
				}
			}
			
			//Being on a ladder resets your jump height
			if(onLadder)
				lastStandingHeight = this.getEntityComponentPosition().getLocation().getY();
			if(this.getFlyingComponent().get())
				lastStandingHeight = Double.NaN;
			
			//else
			//	System.out.println("prout"+(world.getTicksElapsed() % 10L));

			//System.out.println(this.getVelocityComponent().getVelocity().length());
		}

		super.tick();

	}

	// client-side method for updating the player movement
	@Override
	public void tickClientController(PlayerClient controller)
	{
		// Null-out acceleration, until modified by controls
		synchronized (this)
		{
			if (this.getFlyingComponent().get())
				tickFlyMove(controller);
			else
				tickNormalMove(controller);
		}

		//TODO check if this is needed
		//Instead of creating a packet and dealing with it ourselves, we instead push the relevant components
		this.positionComponent.pushComponentEveryoneButController();
		//In that case that means pushing to the server.
	}

	public void tickNormalMove(PlayerClient controller)
	{
		if (isDead())
			return;

		boolean focus = controller.hasFocus();
		
		if (focus && isOnGround())
		{
			if(controller.getInputsManager().getInputByName("crouch").isPressed())
				this.stance.set(EntityHumanoidStance.CROUCHING);
			else
				this.stance.set(EntityHumanoidStance.STANDING);
		}
		
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y + 1), (int) (pos.z))));
		boolean inWater = voxelIn != null && voxelIn.getType().isLiquid();
		
		onLadder = false;
		if (voxelIn instanceof VoxelClimbable)
		{
			CollisionBox[] boxes = voxelIn.getTranslatedCollisionBoxes(world, getLocation());
			if (boxes != null)
				for (CollisionBox box : boxes)
				{
					if (box.collidesWith(this.getTranslatedBoundingBox()))
						onLadder = true;
				}
		}

		if (focus && !inWater && controller.getInputsManager().getInputByName("jump").isPressed() && isOnGround())
		{
			// System.out.println("jumpin");
			jumpForce = 0.15;
		}
		else if (focus && inWater && controller.getInputsManager().getInputByName("jump").isPressed())
			jumpForce = 0.05;
		else
			jumpForce = 0.0;

		// Movement
		// Run ?
		if (focus && controller.getInputsManager().getInputByName("forward").isPressed())
		{
			if (controller.getInputsManager().getInputByName("run").isPressed())
				running = true;
		}
		else
			running = false;

		double modif = 0;
		if (focus)
		{
			if (controller.getInputsManager().getInputByName("forward").isPressed() || controller.getInputsManager().getInputByName("left").isPressed() || controller.getInputsManager().getInputByName("right").isPressed())
				horizontalSpeed = ((running && this.stance.get() == EntityHumanoidStance.STANDING) ? 0.09 : 0.06);
			else if (controller.getInputsManager().getInputByName("back").isPressed())
				horizontalSpeed = -0.05;
			else
				horizontalSpeed = 0.0;
			
			if(this.stance.get() == EntityHumanoidStance.CROUCHING)
				horizontalSpeed *= 0.85f;
		}
		else
			horizontalSpeed = 0.0;
		// Water slows you down

		//Strafing
		if(controller.getInputsManager().getInputByName("forward").isPressed())
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 45;
		}
		else if(controller.getInputsManager().getInputByName("back").isPressed())
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 180 - 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 180 - 45;
		}
		else
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 90;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 90;
		}
		
		if (onLadder)
		{
			//moveWithCollisionRestrain(0, (float)(Math.sin(((rotV) / 180f * Math.PI)) * hSpeed), 0, false);
			this.getVelocityComponent().setVelocityY((float) (Math.sin((-(this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI)) * horizontalSpeed));
		}

		targetVelocity.setX(Math.sin((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed);
		targetVelocity.setZ(Math.cos((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed);

		//targetVelocityX = Math.sin((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;
		//targetVelocityZ = Math.cos((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;
	}

	public static float flySpeed = 0.125f;

	public void tickFlyMove(PlayerClient controller)
	{
		if (!controller.hasFocus())
			return;
		
		//Flying means we're standing
		this.stance.set(EntityHumanoidStance.STANDING);
		
		getVelocityComponent().setVelocity(0, 0, 0);
		
		float camspeed = flySpeed;
		if (controller.getInputsManager().getInputByName("flyReallyFast").isPressed())
			camspeed *= 8 * 5f;
		else if (controller.getInputsManager().getInputByName("flyFast").isPressed())
			camspeed *= 8f;

		if (controller.getInputsManager().getInputByName("back").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation()) / 180f * Math.PI);
			float b = (float) ((this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
		}
		if (controller.getInputsManager().getInputByName("forward").isPressed())
		{
			float a = (float) ((180 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * Math.PI);
			float b = (float) ((-this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
		}
		if (controller.getInputsManager().getInputByName("right").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() - 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
		}
		if (controller.getInputsManager().getInputByName("left").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() + 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
		}
	}

	@Override
	public void setupCamera(CameraInterface camera)
	{
		synchronized (this)
		{
			lastCameraLocation = getLocation();

			camera.setCameraPosition(new Vector3dm(positionComponent.getLocation().add(0.0, eyePosition, 0.0)));
			//camera.pos = lastCameraLocation.clone().negate();
			//camera.pos.add(0d, -eyePosition, 0d);

			camera.setRotationX(this.getEntityRotationComponent().getVerticalRotation());
			camera.setRotationY(this.getEntityRotationComponent().getHorizontalRotation());

			float modifier = 1.0f;
			if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemZoom)
			{
				ItemZoom item = (ItemZoom) this.getSelectedItemComponent().getSelectedItem().getItem();
				modifier = 1.0f / item.getZoomFactor();
			}

			camera.setFOV(modifier * (float) (RenderingConfig.fov
					+ ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) > 0.07 * 0.07
							? ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) - 0.07 * 0.07) * 500 : 0)));
			
		}
	}

	@Override
	public Location getBlockLookingAt(boolean inside)
	{
		Vector3dm initialPosition = new Vector3dm(getLocation());
		initialPosition.add(new Vector3dm(0, eyePosition, 0));

		Vector3dm direction = getDirectionLookingAt();

		if (inside)
			return world.collisionsManager().raytraceSelectable(new Location(world, initialPosition), direction, 256.0);
		else
			return world.collisionsManager().raytraceSolidOuter(new Location(world, initialPosition), direction, 256.0);
	}

	@Override
	public void drawEntityOverlay(RenderingInterface renderingContext)
	{
		if (this.equals(Client.getInstance().getPlayer().getControlledEntity()))
		{
			//If we're using an item that can render an overlay
			if (this.getSelectedItemComponent().getSelectedItem() != null)
			{
				ItemPile pile = this.getSelectedItemComponent().getSelectedItem();
				if (pile.getItem() instanceof ItemOverlay)
					((ItemOverlay) pile.getItem()).drawItemOverlay(renderingContext, pile);
			}

			//We don't want to render our own tag do we ?
			return;
		}

		//Renders the nametag above the player heads
		Vector3dm pos = getLocation();

		//don't render tags too far out
		if (pos.distanceTo(renderingContext.getCamera().getCameraPosition()) > 32f)
			return;

		//Don't render a dead player tag
		if (this.getHealth() <= 0)
			return;

		Vector3fm posOnScreen = renderingContext.getCamera().transform3DCoordinate(new Vector3fm((float)(double) pos.getX(), (float)(double) pos.getY() + 2.0f, (float)(double) pos.getZ()));

		float scale = posOnScreen.getZ();
		String txt = name.getName();// + rotH;
		float dekal = TrueTypeFont.arial11px.getWidth(txt) * 16 * scale;
		//System.out.println("dekal"+dekal);
		if (scale > 0)
			renderingContext.getTrueTypeFontRenderer().drawStringWithShadow(TrueTypeFont.arial11px, posOnScreen.getX() - dekal / 2, posOnScreen.getY(), txt, 16 * scale, 16 * scale, new Vector4fm(1, 1, 1, 1));
	}

	class EntityPlayerRenderer<H extends EntityPlayer> extends EntityHumanoidRenderer<H>
	{
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
		}

		@Override
		public int renderEntities(RenderingInterface renderingContext, RenderingIterator<H> renderableEntitiesIterator)
		{
			setupRender(renderingContext);
			
			int e = 0;

			for (EntityPlayer entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();

				if (!(renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW && location.distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f))
				{
					entity.cachedSkeleton.lodUpdate(renderingContext);

					Matrix4f matrix = new Matrix4f();
					matrix.translate(location.castToSinglePrecision());
					renderingContext.setObjectMatrix(matrix);

					variant = ColorsTools.getUniqueColorCode(entity.getName()) % 6;

					//Player textures
					Texture2D playerTexture = TexturesHandler.getTexture("./models/variant" + variant + ".png");
					playerTexture.setLinearFiltering(false);

					renderingContext.bindAlbedoTexture(playerTexture);
					renderingContext.bindNormalTexture(TexturesHandler.getTexture("./textures/normalnormal.png"));
					renderingContext.bindMaterialTexture(TexturesHandler.getTexture("./textures/defaultmaterial.png"));

					ModelLibrary.getRenderableMesh("./models/human.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
					//animationsData.add(new AnimatableData(location.castToSinglePrecision(), entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, bl, sl));
			
					for(ItemPile aip : entity.armor.getInventory().iterator())
					{
						ItemArmor ia = (ItemArmor)aip.getItem();
						
						renderingContext.bindAlbedoTexture(TexturesHandler.getTexture(ia.getOverlayTextureName()));
						TexturesHandler.getTexture(ia.getOverlayTextureName()).setLinearFiltering(false);
						ModelLibrary.getRenderableMesh("./models/human_overlay.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, ia.bodyPartsAffected());
					}
					
					ItemPile selectedItemPile = null;

					if (entity instanceof EntityWithSelectedItem)
						selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItemComponent().getSelectedItem();

					renderingContext.currentShader().setUniform3f("objectPosition", new Vector3fm(0));

					if (selectedItemPile != null)
					{
						Matrix4f itemMatrix = new Matrix4f();
						itemMatrix.translate(entity.getPredictedLocation().castToSinglePrecision());

						Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

						selectedItemPile.getItem().getType().getRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
					}
				}
				e++;
			}

			return e;
		}
	}

	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityPlayerRenderer<EntityPlayer>();
	}

	@Override
	public String getName()
	{
		return name.getName();
	}

	@Override
	public void setName(String n)
	{
		name.setName(n);
	}

	@Override
	public void onEachFrame(PlayerClient controller)
	{
		if (controller.hasFocus())
		{
			moveCamera(controller);
		}
	}

	@Override
	public boolean onControllerInput(Input input, Controller controller)
	{
		//We are moving inventory bringup here !
		if(input.getName().equals("inventory") && world instanceof WorldClient)
		{
			
			if(creativeMode.get()) {
				Client.getInstance().openInventories(this.inventoryComponent.getInventory(), new InventoryAllVoxels());
			}
			else {
				Client.getInstance().openInventories(this.inventoryComponent.getInventory(), this.armor.getInventory());
			}
			
			return true;
		}
		
		Location blockLocation = this.getBlockLookingAt(true);
		
		double maxLen = 1024;
		
		if(blockLocation != null ) {
			Vector3dm diff = blockLocation.clone().sub(this.getLocation());
			//Vector3dm dir = diff.clone().normalize();
			maxLen = diff.length();
		}
		
		Vector3dm initialPosition = new Vector3dm(getLocation());
		initialPosition.add(new Vector3dm(0, eyePosition, 0));
		
		Vector3dm direction = getDirectionLookingAt();
		
		Iterator<Entity> i = world.collisionsManager().rayTraceEntities(initialPosition, direction, maxLen);
		while(i.hasNext()) {
			Entity e = i.next();
			if(e.handleInteraction(this, input))
				return true;
		}
		
		ItemPile itemSelected = getSelectedItemComponent().getSelectedItem();
		if (itemSelected != null)
		{
			//See if the item handles the interaction
			if (itemSelected.getItem().onControllerInput(this, itemSelected, input, controller))
				return true;
		}
		if (getWorld() instanceof WorldMaster)
		{
			//Creative mode features building and picking.
			if (this.getCreativeModeComponent().get())
			{
				if (input.getName().equals("mouse.left"))
				{
					if (blockLocation != null)
					{
						world.setVoxelData(blockLocation, 0, this);
						return true;
					}
				}
				else if (input.getName().equals("mouse.middle"))
				{
					if (blockLocation != null)
					{
						int data = this.getWorld().getVoxelData(blockLocation);

						int voxelID = VoxelFormat.id(data);
						int voxelMeta = VoxelFormat.meta(data);

						if (voxelID > 0)
						{
							//Spawn new itemPile in his inventory
							ItemVoxel item = (ItemVoxel) world.getGameContext().getContent().items().getItemTypeByName("item_voxel").newItem();
							item.voxel = VoxelsStore.get().getVoxelById(voxelID);
							item.voxelMeta = voxelMeta;

							ItemPile itemVoxel = new ItemPile(item);
							this.getInventory().setItemPileAt(getSelectedItemComponent().getSelectedSlot(), 0, itemVoxel);
							return true;
						}
					}
				}
			}
		}
		//Here goes generic entity response to interaction

		//				n/a

		//Then we check if the world minds being interacted with
		return world.handleInteraction(this, blockLocation, input);
	}
	
	
	public boolean handleInteraction(Entity entity, Input input) {
		if(isDead() && input.getName().equals("mouse.right") && entity instanceof EntityControllable) {
			EntityControllable ctrla = (EntityControllable)entity;
			
			Controller ctrlr = ctrla.getController();
			if(ctrlr != null && ctrlr instanceof Player) {
				Player p = (Player)ctrlr;
				
				p.openInventory(this.getInventory());
				//p.sendMessage("HELLO THIS MY CADAVERER, PLZ FUCK OFF");
				return true;
			}
		}
		return false;
	}

	@Override
	public EntityComponentController getControllerComponent()
	{
		return this.controllerComponent;
	}

	@Override
	public Inventory getInventory()
	{
		return inventoryComponent.getInventory();
	}

	@Override
	public EntityComponentSelectedItem getSelectedItemComponent()
	{
		return this.selectedItemComponent;
	}

	@Override
	public EntityComponentName getNameComponent()
	{
		return name;
	}

	@Override
	public EntityComponentFlying getFlyingComponent()
	{
		return flying;
	}

	@Override
	public EntityComponentCreativeMode getCreativeModeComponent()
	{
		return creativeMode;
	}

	@Override
	public boolean shouldSaveIntoRegion()
	{
		//Player entities are handled their own way.
		return false;
	}

	@Override
	public Location getPredictedLocation()
	{
		//System.out.println("predict");
		return lastCameraLocation != null ? lastCameraLocation : getLocation();
	}

	public float getFoodLevel()
	{
		return foodLevel.getValue();
	}

	public void setFoodLevel(float level)
	{
		foodLevel.setValue(level);
	}
	
	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if(!isDead())
		{
			int i = 1 + (int) Math.random() * 3;
			world.getSoundManager().playSoundEffect("sounds/sfx/entities/human/hurt"+i+".ogg", this.getLocation(), (float)Math.random() * 0.4f + 0.8f, 5.0f);
		}
		
		return super.damage(cause, osef, damage);
	}

	@Override
	public EntityArmorInventory getArmor()
	{
		return armor;
	}
}
