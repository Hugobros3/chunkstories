package io.xol.chunkstories.core.entity;

import org.lwjgl.input.Mouse;

import io.xol.engine.math.lalgb.Vector4f;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.model.ModelLibrary;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentController;
import io.xol.chunkstories.core.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.core.entity.components.EntityComponentFlying;
import io.xol.chunkstories.core.entity.components.EntityComponentFoodLevel;
import io.xol.chunkstories.core.entity.components.EntityComponentInventory;
import io.xol.chunkstories.core.entity.components.EntityComponentName;
import io.xol.chunkstories.core.entity.components.EntityComponentSelectedItem;
import io.xol.chunkstories.core.item.ItemFirearm;
import io.xol.chunkstories.core.item.ItemOverlay;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.core.voxel.VoxelClimbable;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Core/Vanilla player, has all the functionality you'd want from it
 */
public class EntityPlayer extends EntityHumanoid implements EntityControllable, EntityOverlay, EntityNameable, EntityWithInventory, EntityWithSelectedItem, EntityCreative, EntityFlying
{
	//Add the controller component to whatever else the superclass may have
	EntityComponentController controllerComponent = new EntityComponentController(this, this.getComponents().getLastComponent());

	//Declared in constructor, makes no difference at the end of the day
	EntityComponentInventory inventoryComponent;
	EntityComponentSelectedItem selectedItemComponent;

	EntityComponentName name = new EntityComponentName(this, this.getComponents().getLastComponent());
	EntityComponentCreativeMode creativeMode = new EntityComponentCreativeMode(this, this.getComponents().getLastComponent());
	EntityComponentFlying flying = new EntityComponentFlying(this, this.getComponents().getLastComponent());

	//FOOD
	EntityComponentFoodLevel foodLevel = new EntityComponentFoodLevel(this, 100);

	protected boolean noclip = true;

	//Nasty bullshit
	float lastPX = -1f;
	float lastPY = -1f;

	Location lastCameraLocation;

	int variant;

	public EntityPlayer(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		inventoryComponent = new EntityComponentInventory(this, 10, 4);
		selectedItemComponent = new EntityComponentSelectedItem(this, inventoryComponent);
	}

	public EntityPlayer(WorldImplementation w, double x, double y, double z, String name)
	{
		super(w, x, y, z);
		this.name.setName(name);

		variant = ColorsTools.getUniqueColorCode(name) % 6;

		inventoryComponent = new EntityComponentInventory(this, 10, 4);
		selectedItemComponent = new EntityComponentSelectedItem(this, inventoryComponent);
	}

	//TODO Don't use fucking Mouse class
	public void moveCamera()
	{
		if (isDead())
			return;

		float cPX = Mouse.getX();
		float cPY = Mouse.getY();

		float rotH = this.getEntityRotationComponent().getHorizontalRotation();
		float rotV = this.getEntityRotationComponent().getVerticalRotation();

		float modifier = 1.0f;
		if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemFirearm)
		{
			ItemFirearm item = (ItemFirearm) this.getSelectedItemComponent().getSelectedItem().getItem();
			if (item.isScoped())
				modifier = 1.0f / item.getScopeSlow();
		}

		if (lastPX != -1f)
		{
			rotH += modifier * (cPX - GameWindowOpenGL.windowWidth / 2) / 3f * RenderingConfig.mouseSensitivity;
			rotV -= modifier * (cPY - GameWindowOpenGL.windowHeight / 2) / 3f * RenderingConfig.mouseSensitivity;
		}

		lastPX = cPX;
		lastPY = cPY;

		this.getEntityRotationComponent().setRotation(rotH, rotV);
		Mouse.setCursorPosition(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
	}

	// Server-side updating
	@Override
	public void tick(WorldAuthority authority)
	{
		//This is a controllable entity, take care of controlling
		if (authority.isClient())
			tickClientController(Client.getInstance().getClientSideController());

		//Tick item in hand if one such exists
		ItemPile pileSelected = getSelectedItemComponent().getSelectedItem();
		if (pileSelected != null)
			pileSelected.getItem().tickInHand(authority, this, pileSelected);

		//Auto-pickups items on the ground
		if (authority.isMaster() && (world.getTicksElapsed() % 60L) == 0L)
		{
			//TODO localize
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
						ItemPile left = this.inventoryComponent.addItemPile(pile);
						if (left == null)
							eg.removeFromWorld();
						else
							eg.setItemPile(left);
					}
				}
			}
		}

		//Food/health decrease over time
		if (authority.isMaster())
		{
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

			//else
			//	System.out.println("prout"+(world.getTicksElapsed() % 10L));

			//System.out.println(this.getVelocityComponent().getVelocity().length());
		}

		super.tick(authority);

	}

	// client-side method for updating the player movement
	@Override
	public void tickClientController(ClientSideController controller)
	{
		// Null-out acceleration, until modified by controls
		synchronized (this)
		{
			if (this.getFlyingComponent().isFlying())
				tickFlyMove(controller);
			else
				tickNormalMove(controller);
		}

		//TODO check if this is needed
		//Instead of creating a packet and dealing with it ourselves, we instead push the relevant components
		this.positionComponent.pushComponentEveryoneButController();
		//In that case that means pushing to the server.
	}

	public void tickNormalMove(ClientSideController controller)
	{
		if (isDead())
			return;

		boolean focus = controller.hasFocus();
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y + 1), (int) (pos.z))));
		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
		boolean onLadder = false;
		if (voxelIn instanceof VoxelClimbable)
		{
			CollisionBox[] boxes = voxelIn.getTranslatedCollisionBoxes(world, getLocation());
			if (boxes != null)
				for (CollisionBox box : boxes)
				{
					if (box.collidesWith(this))
						onLadder = true;
				}
		}

		if (focus && !inWater && controller.getInputsManager().getInputByName("jump").isPressed() && collision_bot)
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
				horizontalSpeed = (running ? 0.09 : 0.06);
			else if (controller.getInputsManager().getInputByName("back").isPressed())
				horizontalSpeed = -0.05;
			else
				horizontalSpeed = 0.0;
		}
		else
			horizontalSpeed = 0.0;
		// Water slows you down

		//if (inWater)
		//	horizontalSpeed *= 0.45;

		if (controller.getInputsManager().getInputByName("left").isPressed())
			modif += 90 * (controller.getInputsManager().getInputByName("forward").isPressed() ? 0.5 : 1);
		if (controller.getInputsManager().getInputByName("right").isPressed())
			modif += -90 * (controller.getInputsManager().getInputByName("forward").isPressed() ? 0.5 : 1);

		//Auto-step logic
		if (collision_bot && (Math.abs(this.blockedMomentum.getX()) > 0.0005d || Math.abs(this.blockedMomentum.getZ()) > 0.0005d))
		{
			blockedMomentum.setY(0);
			if (blockedMomentum.length() > 0.20d)
			{
				blockedMomentum.normalize();
				blockedMomentum.scale(0.20);
			}

			for (double d = 0.25; d < 0.5; d += 0.05)
			{
				//I don't want any of this to reflect on the object, because it causes ugly jumps in the animation
				Vector3d canMoveUp = this.canMoveWithCollisionRestrain(new Vector3d(0.0, d, 0.0));
				//It can go up that bit
				if (canMoveUp.length() == 0.0f)
				{
					//Would it help with being stuck ?
					Vector3d tryFromHigher = new Vector3d(this.getLocation());
					tryFromHigher.add(new Vector3d(0.0, d, 0.0));
					Vector3d blockedMomentumRemaining = this.canMoveWithCollisionRestrain(tryFromHigher, blockedMomentum);
					//If length of remaining momentum < of what we requested it to do, that means it *did* go a bit further away
					if (blockedMomentumRemaining.length() < blockedMomentum.length())
					{
						//Where would this land ?
						Vector3d afterJump = new Vector3d(tryFromHigher);
						afterJump.add(blockedMomentum);
						afterJump.sub(blockedMomentumRemaining);

						//land distance = whatever is left of our -0.55 delta when it hits the ground
						Vector3d landDistance = this.canMoveWithCollisionRestrain(afterJump, new Vector3d(0.0, -d, 0.0));
						afterJump.add(new Vector3d(0.0, -d, 0.0));
						afterJump.sub(landDistance);

						this.setLocation(new Location(world, afterJump));
						break;
					}
				}
			}
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

		eyePosition = 1.65;// + Math.sin(metersWalked * 5d) * 0.035d;
	}

	public static float flySpeed = 0.125f;

	public void tickFlyMove(ClientSideController controller)
	{
		if (!controller.hasFocus())
			return;
		getVelocityComponent().setVelocity(0, 0, 0);
		eyePosition = 1.65;
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
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (controller.getInputsManager().getInputByName("forward").isPressed())
		{
			float a = (float) ((180 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * Math.PI);
			float b = (float) ((-this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (controller.getInputsManager().getInputByName("right").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() - 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed, true);
		}
		if (controller.getInputsManager().getInputByName("left").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() + 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed, true);
		}
	}

	@Override
	public void setupCamera(Camera camera)
	{
		synchronized (this)
		{
			lastCameraLocation = getLocation();

			camera.pos = lastCameraLocation.clone().negate();
			camera.pos.add(0d, -eyePosition, 0d);

			camera.rotationX = this.getEntityRotationComponent().getVerticalRotation();
			camera.rotationY = this.getEntityRotationComponent().getHorizontalRotation();

			float modifier = 1.0f;
			if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemFirearm)
			{
				ItemFirearm item = (ItemFirearm) this.getSelectedItemComponent().getSelectedItem().getItem();
				if (item.isScoped())
					modifier = 1.0f / item.getScopeZoom();
			}

			camera.fov = modifier * (float) (RenderingConfig.fov
					+ ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) > 0.07 * 0.07
							? ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) - 0.07 * 0.07) * 500 : 0));
			camera.alUpdate();
		}
	}

	@Override
	public Location getBlockLookingAt(boolean inside)
	{
		Vector3d initialPosition = new Vector3d(getLocation());
		initialPosition.add(new Vector3d(0, eyePosition, 0));

		Vector3d direction = getDirectionLookingAt();

		if (inside)
			return world.raytraceSelectable(new Location(world, initialPosition), direction, 256.0);
		else
			return world.raytraceSolidOuter(new Location(world, initialPosition), direction, 256.0);
	}

	@Override
	public void drawEntityOverlay(RenderingInterface renderingContext)
	{
		if (this.equals(Client.getInstance().getClientSideController().getControlledEntity()))
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
		Vector3d pos = getLocation();

		//don't render tags too far out
		if (pos.distanceTo(renderingContext.getCamera().getCameraPosition().negate()) > 32f)
			return;

		//Don't render a dead player tag
		if (this.getHealth() <= 0)
			return;

		Vector3fm posOnScreen = renderingContext.getCamera().transform3DCoordinate(new Vector3fm((float) pos.getX(), (float) pos.getY() + 2.0f, (float) pos.getZ()));

		float scale = posOnScreen.getZ();
		String txt = name.getName();// + rotH;
		float dekal = TrueTypeFont.arial11px.getWidth(txt) * 16 * scale;
		//System.out.println("dekal"+dekal);
		if (scale > 0)
			renderingContext.getTrueTypeFontRenderer().drawStringWithShadow(TrueTypeFont.arial11px, posOnScreen.getX() - dekal / 2, posOnScreen.getY(), txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	class EntityPlayerRenderer<H extends EntityPlayer> extends EntityHumanoidRenderer<H>
	{

		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
		}

		@Override
		public int forEach(RenderingInterface renderingContext, RenderingIterator<H> renderableEntitiesIterator)
		{
			int e = 0;

			for (EntityPlayer entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();

				if (!(renderingContext.isThisAShadowPass() && location.distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f))
				{
					entity.cachedSkeleton.lodUpdate(renderingContext);

					Matrix4f matrix = new Matrix4f();
					matrix.translate(location.castToSimplePrecision());
					renderingContext.setObjectMatrix(matrix);

					variant = ColorsTools.getUniqueColorCode(entity.getName()) % 6;

					//Player textures
					Texture2D playerTexture = TexturesHandler.getTexture("./models/variant" + variant + ".png");
					playerTexture.setLinearFiltering(false);

					renderingContext.bindAlbedoTexture(playerTexture);

					ModelLibrary.getRenderableMesh("./models/human.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
					//animationsData.add(new AnimatableData(location.castToSimplePrecision(), entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, bl, sl));
			
					ItemPile selectedItemPile = null;

					if (entity instanceof EntityWithSelectedItem)
						selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItemComponent().getSelectedItem();

					renderingContext.currentShader().setUniform3f("objectPosition", new Vector3fm(0));

					if (selectedItemPile != null)
					{
						Matrix4f itemMatrix = new Matrix4f();
						itemMatrix.translate(entity.getPredictedLocation().castToSimplePrecision());

						Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

						selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
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
	public void setupCamera(ClientSideController controller)
	{
		if (controller.hasFocus())
		{
			moveCamera();
		}
	}

	@Override
	public boolean handleInteraction(Input input, Controller controller)
	{
		Location blockLocation = this.getBlockLookingAt(true);
		ItemPile itemSelected = getSelectedItemComponent().getSelectedItem();
		if (itemSelected != null)
		{
			//See if the item handles the interaction
			if (itemSelected.getItem().handleInteraction(this, itemSelected, input, controller))
				return true;
		}
		if (getWorld() instanceof WorldMaster)
		{
			//Creative mode features building and picking.
			if (this.getCreativeModeComponent().isCreativeMode())
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
							ItemVoxel item = (ItemVoxel) ItemTypes.getItemTypeByName("item_voxel").newItem();
							item.voxel = Voxels.get(voxelID);
							item.voxelMeta = voxelMeta;

							ItemPile itemVoxel = new ItemPile(item);
							this.inventoryComponent.setItemPileAt(getSelectedItemComponent().getSelectedSlot(), 0, itemVoxel);
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

	@Override
	public EntityComponentController getControllerComponent()
	{
		return this.controllerComponent;
	}

	@Override
	public EntityComponentInventory getInventory()
	{
		return inventoryComponent;
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
		// TODO Auto-generated method stub
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
}
