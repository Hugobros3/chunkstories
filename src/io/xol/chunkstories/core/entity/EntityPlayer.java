package io.xol.chunkstories.core.entity;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityHUD;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentController;
import io.xol.chunkstories.core.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.core.entity.components.EntityComponentFlying;
import io.xol.chunkstories.core.entity.components.EntityComponentInventory;
import io.xol.chunkstories.core.entity.components.EntityComponentName;
import io.xol.chunkstories.core.entity.components.EntityComponentSelectedItem;
import io.xol.chunkstories.core.item.ItemAk47;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.core.voxel.VoxelClimbable;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.lights.DefferedLight;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.model.animation.BVHAnimation;
import io.xol.engine.model.animation.BVHLibrary;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Core/Vanilla player, has all the functionality you'd want from it
 */
public class EntityPlayer extends EntityLivingImplentation implements EntityControllable, EntityHUD, EntityNameable, EntityWithInventory, EntityWithSelectedItem, EntityCreative, EntityFlying
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

	boolean running = false;

	float lastPX = -1f;
	float lastPY = -1f;

	//Body parts to render in first person
	static Set<String> fp_elements = new HashSet<String>();

	static
	{
		fp_elements.add("boneArmLU");
		fp_elements.add("boneArmRU");
		fp_elements.add("boneArmLD");
		fp_elements.add("boneArmRD");
	}

	//private Controller controller;

	public double maxSpeedRunning = 0.25;
	public double maxSpeed = 0.15;

	public double horizontalSpeed = 0;

	public double eyePosition = 1.6;
	public double walked = 0d;

	double jump = 0;
	double targetVectorX;
	double targetVectorZ;

	boolean jumped = false;
	boolean landed = false;

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

	public void moveCamera()
	{
		float cPX = Mouse.getX();
		float cPY = Mouse.getY();

		float rotH = this.getEntityRotationComponent().getHorizontalRotation();
		float rotV = this.getEntityRotationComponent().getVerticalRotation();

		if (lastPX != -1f)
		{
			rotH += (cPX - GameWindowOpenGL.windowWidth / 2) / 3f * FastConfig.mouseSensitivity;
			rotV -= (cPY - GameWindowOpenGL.windowHeight / 2) / 3f * FastConfig.mouseSensitivity;
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
		//Tick item
		ItemPile pileSelected = getSelectedItemComponent().getSelectedItem();
		if(pileSelected != null)
			pileSelected.getItem().tickInHand(this, pileSelected);
		
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y), (int) (pos.z))));
		if (jump > 0)
		{
			jumped = true;
			walked = 0;
			velocity.y = jump;
			jump = 0;
		}

		boolean l = collision_bot;

		acceleration = new Vector3d(targetVectorX - velocity.x, 0, targetVectorZ - velocity.z);

		double modifySpd = collision_bot ? 0.010 : 0.005;

		if (acceleration.length() > modifySpd)
		{
			acceleration.normalize();
			acceleration.scale(modifySpd);
		}
		super.tick();
		// Sound stuff
		if (collision_bot && !l)
		{
			landed = true;
			walked = 0;
		}
		//Bobbing
		if (collision_bot)
			walked += Math.abs(horizontalSpeed);
	}

	// client-side method for updating the player movement
	@Override
	public void tickClient(ClientSideController controller)
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
		this.position.pushComponentEveryoneButController();
		//In that case that means pushing to the server.
	}

	public void tickNormalMove(ClientSideController controller)
	{
		//System.out.println("tck");
		WorldClient worldClient = (WorldClient) world;
		boolean focus = controller.hasFocus();
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y + 1), (int) (pos.z))));
		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
		boolean onLadder = voxelIn instanceof VoxelClimbable;
		if (onLadder)
		{
			onLadder = false;
			CollisionBox[] boxes = voxelIn.getTranslatedCollisionBoxes(world, getLocation());
			if (boxes != null)
				for (CollisionBox box : boxes)
				{
					if (box.collidesWith(this))
						onLadder = true;
				}
		}

		if (jumped && !inWater)
		{
			jumped = false;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/jump.ogg", getLocation(), (float) (0.9f + Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * 0.1f), 1f);
		}
		if (landed)
		{
			landed = false;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/jump.ogg", getLocation(), (float) (0.9f + Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * 0.1f), 1f);
		}

		if (walked > 0.2 * Math.PI * 2)
		{
			walked %= 0.2 * Math.PI * 2;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/generic" + ((int) (1 + Math.floor(Math.random() * 3))) + ".ogg", getLocation(), (float) (0.9f + Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * 0.1f), 1f);
			// System.out.println("footstep");
		}

		if (focus && !inWater && controller.getInputsManager().getInputByName("jump").isPressed() && collision_bot)
		{
			// System.out.println("jumpin");
			jump = 0.15;
		}
		else if (focus && inWater && controller.getInputsManager().getInputByName("jump").isPressed())
			jump = 0.05;
		else
			jump = 0.0;

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
		if (inWater)
			horizontalSpeed *= 0.45;

		if (controller.getInputsManager().getInputByName("left").isPressed())
			modif += 90 * (controller.getInputsManager().getInputByName("forward").isPressed() ? 0.5 : 1);
		if (controller.getInputsManager().getInputByName("right").isPressed())
			modif += -90 * (controller.getInputsManager().getInputByName("forward").isPressed() ? 0.5 : 1);

		//Auto-step logic
		if (collision_bot && (Math.abs(this.blockedMomentum.x) > 0.0005d || Math.abs(this.blockedMomentum.z) > 0.0005d))
		{
			blockedMomentum.y = 0;
			if (blockedMomentum.length() > 0.20d)
			{
				blockedMomentum.normalize();
				blockedMomentum.scale(0.20);
			}

			//I don't want any of this to reflect on the object, because it causes ugly jumps in the animation
			Vector3d canMoveUp = this.canMoveWithCollisionRestrain(new Vector3d(0.0, 0.55, 0.0));
			//It can go up that bit
			if (canMoveUp.length() == 0.0f)
			{
				//Would it help with being stuck ?
				Vector3d tryFromHigher = new Vector3d(this.getLocation());
				tryFromHigher.add(new Vector3d(0.0, 0.55, 0.0));
				Vector3d blockedMomentumRemaining = this.canMoveWithCollisionRestrain(tryFromHigher, blockedMomentum);
				//If length of remaining momentum < of what we requested it to do, that means it *did* go a bit further away
				if (blockedMomentumRemaining.length() < blockedMomentum.length())
				{
					//Where would this land ?
					Vector3d afterJump = new Vector3d(tryFromHigher);
					afterJump.add(blockedMomentum);
					afterJump.sub(blockedMomentumRemaining);

					//land distance = whatever is left of our -0.55 delta when it hits the ground
					Vector3d landDistance = this.canMoveWithCollisionRestrain(afterJump, new Vector3d(0.0, -0.55, 0.0));
					afterJump.add(new Vector3d(0.0, -0.55, 0.0));
					afterJump.sub(landDistance);

					this.setLocation(new Location(world, afterJump));
				}
			}
		}
		if (onLadder)
		{
			//moveWithCollisionRestrain(0, (float)(Math.sin(((rotV) / 180f * Math.PI)) * hSpeed), 0, false);
			this.velocity.y = (float) (Math.sin((-(this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI)) * horizontalSpeed);
		}

		targetVectorX = Math.sin((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;
		targetVectorZ = Math.cos((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed;

		eyePosition = 1.65 + Math.sin(walked * 5d) * 0.035d;

		//System.out.println("nrml mv");
	}

	public void tickFlyMove(ClientSideController controller)
	{
		if (!controller.hasFocus())
			return;
		velocity.zero();
		eyePosition = 1.65;
		float camspeed = 0.125f;
		if (Keyboard.isKeyDown(42))
			camspeed = 1f;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
			camspeed = 5f;
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
		
		if (this.getFlyingComponent().isFlying())
		{
			this.velocity.x = 0;
			this.velocity.y = 0;
			this.velocity.z = 0;
		}
	}

	@Override
	public void setupCamera(Camera camera)
	{
		synchronized (this)
		{
			camera.pos = new Vector3d(getLocation()).negate();
			camera.pos.add(0d, -eyePosition, 0d);

			camera.rotationX = this.getEntityRotationComponent().getVerticalRotation();
			camera.rotationY = this.getEntityRotationComponent().getHorizontalRotation();

			camera.fov = (float) (FastConfig.fov + ((velocity.x * velocity.x + velocity.z * velocity.z) > 0.07 * 0.07 ? ((velocity.x * velocity.x + velocity.z * velocity.z) - 0.07 * 0.07) * 500 : 0));
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
	public void drawHUD(Camera camera)
	{
		if (this.equals(Client.controlledEntity))
			return; // Don't render yourself
		Vector3d pos = getLocation();
		Vector3f posOnScreen = camera.transform3DCoordinate(new Vector3f((float) pos.x, (float) pos.y + 2.0f, (float) pos.z));

		float scale = posOnScreen.z;
		String txt = name.getName();// + rotH;
		float dekal = TrueTypeFont.arial12.getWidth(txt) * 16 * scale;
		//System.out.println("dekal"+dekal);
		if (scale > 0)
			TrueTypeFont.arial12.drawStringWithShadow(posOnScreen.x - dekal / 2, posOnScreen.y, txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	@Override
	public void render(RenderingContext renderingContext)
	{
		Camera cam = renderingContext.getCamera();
		ItemPile selectedItemPile = getSelectedItemComponent().getSelectedItem();
		BVHAnimation animation = BVHLibrary.getAnimation("res/models/human-standstill.bvh");
		if (selectedItemPile != null)
		{
			if (selectedItemPile.getItem() instanceof ItemAk47)
				animation = BVHLibrary.getAnimation("res/models/human-rifle-holding.bvh");
			else
				animation = BVHLibrary.getAnimation("res/models/human-holding.bvh");
		}
		//Player textures
		Texture2D playerTexture = TexturesHandler.getTexture("models/guyA.png");
		playerTexture.setLinearFiltering(false);
		renderingContext.setDiffuseTexture(playerTexture.getId());
		//Players models have no normal mapping
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("textures/normalnormal.png"));

		//Prevents laggy behaviour
		if (this.equals(Client.controlledEntity))
			renderingContext.getCurrentShader().setUniformFloat3("objectPosition", -(float) cam.pos.x, -(float) cam.pos.y - eyePosition, -(float) cam.pos.z);

		//Renders normal limbs
		Matrix4f playerRotationMatrix = new Matrix4f();
		playerRotationMatrix.translate(new Vector3f(0f, (float) this.eyePosition, 0f));
		playerRotationMatrix.rotate((90 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
		
		playerRotationMatrix.translate(new Vector3f(0f, -(float) this.eyePosition, 0f));
		renderingContext.sendTransformationMatrix(playerRotationMatrix);
		//Except in fp 
		if (!this.equals(Client.controlledEntity) || renderingContext.shadow)
			ModelLibrary.getMesh("res/models/human.obj").renderBut(renderingContext, fp_elements, animation, 0);
		
		
		//Render rotated limbs
		playerRotationMatrix = new Matrix4f();
		playerRotationMatrix.translate(new Vector3f(0f, (float) this.eyePosition, 0f));
		playerRotationMatrix.rotate((90 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
		
		if(selectedItemPile != null)
			playerRotationMatrix.rotate((-this.getEntityRotationComponent().getVerticalRotation()) / 180f * 3.14159f, new Vector3f(0, 0, 1));
		
		playerRotationMatrix.translate(new Vector3f(0f, -(float) this.eyePosition, 0f));
		renderingContext.sendTransformationMatrix(playerRotationMatrix);

		if(selectedItemPile != null || !this.equals(Client.controlledEntity) || renderingContext.shadow)
		ModelLibrary.getMesh("res/models/human.obj").render(renderingContext, fp_elements, animation, 0);
	
		//Matrix to itemInHand bone in the player's bvh
		Matrix4f itemMatrix = new Matrix4f();
		itemMatrix = animation.getTransformationForBone("boneItemInHand", 0);
		Matrix4f.mul(playerRotationMatrix, itemMatrix, itemMatrix);

		if (selectedItemPile != null)
		{
			if (selectedItemPile.getItem() instanceof ItemVoxel)
			{
				if (((ItemVoxel) selectedItemPile.getItem()).getVoxel().getLightLevel(0x00) > 0)
				{
					Vector3d pos = getLocation();
					Light heldBlockLight = new DefferedLight(new Vector3f(0.5f, 0.45f, 0.4f), new Vector3f((float) pos.x, (float) pos.y + 1.6f, (float) pos.z), 15f);
					renderingContext.addLight(heldBlockLight);	
					
					//If we hold a light source, prepare the shader accordingly
					renderingContext.getCurrentShader().setUniformFloat2("worldLight", ((ItemVoxel) selectedItemPile.getItem()).getVoxel().getLightLevel(0x00), world.getSunlightLevel(this.getLocation()));
					
				}
			}
			selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, itemMatrix);
		}
	}

	@Override
	public String getName()
	{
		return name.getName();
	}

	@Override
	public void setName(String n)
	{
		//this.inventory.name = this.name + "'s Inventory";
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
							ItemPile itemVoxel = new ItemPile("item_voxel", new String[] { "" + voxelID, "" + voxelMeta });
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
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(0.75, 1.80, 0.75) };
	}
}
