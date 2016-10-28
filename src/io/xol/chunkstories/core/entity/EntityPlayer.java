package io.xol.chunkstories.core.entity;

import org.lwjgl.input.Mouse;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
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
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentController;
import io.xol.chunkstories.core.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.core.entity.components.EntityComponentFlying;
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

	protected boolean noclip = true;

	float lastPX = -1f;
	float lastPY = -1f;
	
	Location lastCameraLocation;

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
		inventoryComponent = new EntityComponentInventory(this, 10, 4);
		selectedItemComponent = new EntityComponentSelectedItem(this, inventoryComponent);
	}

	//TODO Don't use fucking Mouse class
	public void moveCamera()
	{
		if(isDead())
			return;
		
		float cPX = Mouse.getX();
		float cPY = Mouse.getY();

		float rotH = this.getEntityRotationComponent().getHorizontalRotation();
		float rotV = this.getEntityRotationComponent().getVerticalRotation();

		float modifier = 1.0f;
		if(this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemFirearm)
		{
			ItemFirearm item = (ItemFirearm) this.getSelectedItemComponent().getSelectedItem().getItem();
			if(item.isScoped())
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
	public void tick()
	{
		//Tick item in hand if one such exists
		ItemPile pileSelected = getSelectedItemComponent().getSelectedItem();
		if(pileSelected != null)
			pileSelected.getItem().tickInHand(this, pileSelected);
		
		super.tick();
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

		//Instead of creating a packet and dealing with it ourselves, we instead push the relevant components
		this.positionComponent.pushComponentEveryoneButController();
		//In that case that means pushing to the server.
	}

	public void tickNormalMove(ClientSideController controller)
	{
		if(isDead())
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

			for(double d = 0.25; d < 0.5; d+= 0.05)
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

		targetVectorX = Math.sin((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;
		targetVectorZ = Math.cos((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;

		eyePosition = 1.65;// + Math.sin(metersWalked * 5d) * 0.035d;
	}

	public void tickFlyMove(ClientSideController controller)
	{
		if (!controller.hasFocus())
			return;
		getVelocityComponent().setVelocity(0, 0, 0);
		eyePosition = 1.65;
		float camspeed = 0.125f;
		//if (Keyboard.isKeyDown(42))
		if (controller.getInputsManager().getInputByName("flyFast").isPressed())
			camspeed = 1f;
		//if (Keyboard.isKeyDown(Keyboard.KEY_LMENU)) //56
		if (controller.getInputsManager().getInputByName("flyReallyFast").isPressed())
			camspeed = 5f;
		
		//camspeed = 1f;
		
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
			if(this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemFirearm)
			{
				ItemFirearm item = (ItemFirearm) this.getSelectedItemComponent().getSelectedItem().getItem();
				if(item.isScoped())
					modifier = 1.0f / item.getScopeZoom();
			}
			
			camera.fov = modifier * (float) (RenderingConfig.fov + ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) > 0.07 * 0.07 ? ((getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getZ() * getVelocityComponent().getVelocity().getZ()) - 0.07 * 0.07) * 500 : 0));
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
			if(this.getSelectedItemComponent().getSelectedItem() != null)
			{
				ItemPile pile = this.getSelectedItemComponent().getSelectedItem();
				if(pile.getItem() instanceof ItemOverlay)
					((ItemOverlay) pile.getItem()).drawItemOverlay(renderingContext, pile);
			}
		
			//We don't want to render our own tag do we ?
			return;
		}
		
		//Renders the nametag above the player heads
		Vector3d pos = getLocation();
		
		//don't render tags too far out
		if(pos.distanceTo(renderingContext.getCamera().getCameraPosition()) > 32f)
			return;
		
		Vector3f posOnScreen = renderingContext.getCamera().transform3DCoordinate(new Vector3f((float) pos.getX(), (float) pos.getY() + 2.0f, (float) pos.getZ()));

		float scale = posOnScreen.z;
		String txt = name.getName();// + rotH;
		float dekal = TrueTypeFont.arial11px.getWidth(txt) * 16 * scale;
		//System.out.println("dekal"+dekal);
		if (scale > 0)
			renderingContext.getTrueTypeFontRenderer().drawStringWithShadow(TrueTypeFont.arial11px, posOnScreen.x - dekal / 2, posOnScreen.y, txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	class EntityPlayerRenderer<H extends EntityHumanoid> extends EntityHumanoidRenderer<H> {
		
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
			
			//Player textures
			Texture2D playerTexture = TexturesHandler.getTexture("./models/guyA.png");
			playerTexture.setLinearFiltering(false);
			
			renderingContext.bindAlbedoTexture(playerTexture);
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
							ItemVoxel item = (ItemVoxel)ItemTypes.getItemTypeByName("item_voxel").newItem();
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
}
