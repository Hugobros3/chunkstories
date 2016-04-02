package io.xol.chunkstories.entity.core;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityHUD;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.entity.EntityNameable;
import io.xol.chunkstories.entity.EntityRotateable;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.core.ItemAk47;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.net.packets.PacketEntity;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.core.VoxelClimbable;
import io.xol.chunkstories.world.World;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.font.TrueTypeFont;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.model.animation.BVHAnimation;
import io.xol.engine.model.animation.BVHLibrary;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityPlayer extends EntityImplementation implements EntityControllable, EntityHUD, EntityNameable, EntityRotateable
{
	boolean noclip = true;
	boolean running = false;

	float lastPX = -1f;
	float lastPY = -1f;

	private String name;

	public EntityPlayer(World w, double x, double y, double z)
	{
		this(w, x, y, z, "");
	}

	public EntityPlayer(World w, double x, double y, double z, String name)
	{
		super(w, x, y, z);
		this.name = name;
		inventory = new Inventory(this, 10, 4, this.name + "'s Inventory");
		flying = false;
	}

	public void moveCamera()
	{
		float cPX = Mouse.getX();
		float cPY = Mouse.getY();
		if (lastPX != -1f)
		{
			rotH += (cPX - XolioWindow.frameW / 2) / 3f * FastConfig.mouseSensitivity;
			rotV -= (cPY - XolioWindow.frameH / 2) / 3f * FastConfig.mouseSensitivity;
		}
		if (rotV > 90)
			rotV = 90;
		if (rotV < -90)
			rotV = -90;
		lastPX = cPX;
		lastPY = cPY;
		Mouse.setCursorPosition(XolioWindow.frameW / 2, XolioWindow.frameH / 2);
	}

	// Server-side updating
	@Override
	public void tick()
	{
		if (jump > 0)
		{
			jumped = true;
			walked = 0;
			vel.y = jump;
			jump = 0;
		}

		boolean l = collision_bot;

		acc = new Vector3d(targetVectorX - vel.x, 0, targetVectorZ - vel.z);

		double modifySpd = collision_bot ? 0.010 : 0.005;

		if (acc.length() > modifySpd)
		{
			acc.normalize();
			acc.scale(modifySpd);
		}

		if (flying)
		{
			this.vel.x = 0;
			this.vel.y = 0;
			this.vel.z = 0;
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
			walked += Math.abs(hSpeed);
	}

	// client-side method for updating the player movement
	@Override
	public void tick(ClientController controller)
	{
		// Null-out acceleration, until modified by controls
		synchronized (this)
		{
			if (flying)
				flyMove(controller);
			else
				normalMove(controller);
		}

		super.updatePosition();
		if (Client.connection != null)
		{
			PacketEntity packet = new PacketEntity(true);
			packet.includeRotation = true;
			packet.applyFromEntity(this);
			Client.connection.sendPacket(packet);
		}
	}

	public double maxSpeedRunning = 0.25;
	public double maxSpeed = 0.15;

	public double hSpeed = 0;

	public double eyePosition = 1.6;
	public double walked = 0d;

	double jump = 0;
	double targetVectorX;
	double targetVectorZ;

	boolean jumped = false;
	boolean landed = false;

	public void normalMove(ClientController controller)
	{
		WorldClient worldClient = (WorldClient)world;
		boolean focus = controller.hasFocus();
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y + 1), (int) (pos.z))));
		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
		boolean onLadder = voxelIn instanceof VoxelClimbable;
		if (onLadder)
		{
			onLadder = false;
			CollisionBox[] boxes = voxelIn.getTranslatedCollisionBoxes(world, (int) (pos.x), (int) (pos.y), (int) (pos.z));
			if (boxes != null)
				for (CollisionBox box : boxes)
				{
					if (box.collidesWith(this))
						onLadder = true;
				}
		}

		if (jumped)
		{
			jumped = false;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/jump.ogg", pos.x, pos.y, pos.z, (float) (0.9f + Math.sqrt(vel.x * vel.x + vel.y * vel.y) * 0.1f), 1f);
		}
		if (landed)
		{
			landed = false;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/jump.ogg", pos.x, pos.y, pos.z, (float) (0.9f + Math.sqrt(vel.x * vel.x + vel.y * vel.y) * 0.1f), 1f);
		}

		if (walked > 0.2 * Math.PI * 2)
		{
			walked %= 0.2 * Math.PI * 2;
			worldClient.getClient().getSoundManager().playSoundEffect("footsteps/generic" + ((int) (1 + Math.floor(Math.random() * 3))) + ".ogg", pos.x, pos.y, pos.z, (float) (0.9f + Math.sqrt(vel.x * vel.x + vel.y * vel.y) * 0.1f), 1f);
			// System.out.println("footstep");
		}

		if (focus && !inWater && controller.getKeyBind("jump").isPressed() && collision_bot)
		{
			// System.out.println("jumpin");
			jump = 0.15;
		}
		else if (focus && inWater && controller.getKeyBind("jump").isPressed())
			jump = 0.05;
		else
			jump = 0.0;

		// Movement
		// Run ?
		if (focus && controller.getKeyBind("forward").isPressed())
		{
			if (controller.getKeyBind("run").isPressed())
				running = true;
		}
		else
			running = false;

		double modif = 0;
		if (focus)
		{
			if (controller.getKeyBind("forward").isPressed() || controller.getKeyBind("left").isPressed() || controller.getKeyBind("right").isPressed())
				hSpeed = (running ? 0.09 : 0.06);
			else if (controller.getKeyBind("back").isPressed())
				hSpeed = -0.05;
			else
				hSpeed = 0.0;
		}
		else
			hSpeed = 0.0;
		// Water slows you down
		if (inWater)
			hSpeed *= 0.45;

		if (controller.getKeyBind("left").isPressed())
			modif += 90 * (controller.getKeyBind("forward").isPressed() ? 0.5 : 1);
		if (controller.getKeyBind("right").isPressed())
			modif += -90 * (controller.getKeyBind("forward").isPressed() ? 0.5 : 1);

		synchronized (this)
		{
			if (collision_bot && (Math.abs(this.blockedMomentum.x) > 0.0005d || Math.abs(this.blockedMomentum.z) > 0.0005d))
			{
				blockedMomentum.y = 0;
				if (blockedMomentum.length() > 0.20d)
				{
					blockedMomentum.normalize();
					blockedMomentum.scale(0.20);
				}
				// System.out.println("trying 2 complete"+blockedMomentum);
				this.moveWithCollisionRestrain(0, 0.55, 0, false);
				this.moveWithCollisionRestrain(blockedMomentum);
				this.moveWithCollisionRestrain(0, -0.55, 0, false);
			}
			if (onLadder)
			{
				//moveWithCollisionRestrain(0, (float)(Math.sin(((rotV) / 180f * Math.PI)) * hSpeed), 0, false);
				this.vel.y = (float) (Math.sin((-(rotV) / 180f * Math.PI)) * hSpeed);
			}
		}

		targetVectorX = Math.sin((180 - rotH + modif) / 180f * Math.PI) * hSpeed;
		targetVectorZ = Math.cos((180 - rotH + modif) / 180f * Math.PI) * hSpeed;

		eyePosition = 1.65 + Math.sin(walked * 5d) * 0.035d;
	}

	public void flyMove(ClientController controller)
	{
		if (!controller.hasFocus())
			return;
		vel.zero();
		eyePosition = 1.65;
		float camspeed = 0.125f;
		if (Keyboard.isKeyDown(42))
			camspeed = 1f;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
			camspeed = 5f;
		if (controller.getKeyBind("back").isPressed())
		{
			float a = (float) ((-rotH) / 180f * Math.PI);
			float b = (float) ((rotV) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (controller.getKeyBind("forward").isPressed())
		{
			float a = (float) ((180 - rotH) / 180f * Math.PI);
			float b = (float) ((-rotV) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (controller.getKeyBind("right").isPressed())
		{
			float a = (float) ((-rotH - 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed, true);
		}
		if (controller.getKeyBind("left").isPressed())
		{
			float a = (float) ((-rotH + 90) / 180f * Math.PI);
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
			//camera.campos.x = -pos.x;
			//camera.campos.y = -(pos.y + eyePosition);
			//camera.campos.z = -pos.z;

			camera.pos = new Vector3d(pos).negate();
			camera.pos.add(0d, -eyePosition, 0d);
			
			camera.view_rotx = rotV;
			camera.view_roty = rotH;

			camera.fov = (float) (FastConfig.fov + ((vel.x * vel.x + vel.z * vel.z) > 0.07 * 0.07 ? ((vel.x * vel.x + vel.z * vel.z) - 0.07 * 0.07) * 500 : 0));
			camera.alUpdate();
		}
	}

	@Override
	public Location getBlockLookingAt(boolean inside)
	{
		Vector3d initialPosition = new Vector3d(pos);
		initialPosition.add(new Vector3d(0, eyePosition, 0));
		//Vector3d position = new Vector3d(pos.x, pos.y + eyePosition, pos.z);
		Vector3d direction = new Vector3d();

		float a = (float) ((-rotH) / 360f * 2 * Math.PI);
		float b = (float) ((rotV) / 360f * 2 * Math.PI);
		direction.x = -(float) Math.sin(a) * (float) Math.cos(b);
		direction.y = -(float) Math.sin(b);
		direction.z = -(float) Math.cos(a) * (float) Math.cos(b);

		direction.normalize();
		//direction.scale(0.02);

		float distance = 0f;
		Voxel vox;
		int x, y, z;
		x = (int) Math.floor(initialPosition.x);
		y = (int) Math.floor(initialPosition.y);
		z = (int) Math.floor(initialPosition.z);

		//DDA algorithm

		//It requires double arrays because it works using loops over each dimension
		double[] rayOrigin = new double[3];
		double[] rayDirection = new double[3];
		rayOrigin[0] = initialPosition.x;
		rayOrigin[1] = initialPosition.y;
		rayOrigin[2] = initialPosition.z;
		rayDirection[0] = direction.x;
		rayDirection[1] = direction.y;
		rayDirection[2] = direction.z;
		int voxelCoords[] = new int[] { x, y, z };
		double[] deltaDist = new double[3];
		double[] next = new double[3];
		int step[] = new int[3];

		int side = 0;
		//Prepare distances
		for (int i = 0; i < 3; ++i)
		{
			double deltaX = rayDirection[0] / rayDirection[i];
			double deltaY = rayDirection[1] / rayDirection[i];
			double deltaZ = rayDirection[2] / rayDirection[i];
			deltaDist[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			if (rayDirection[i] < 0.f)
			{
				step[i] = -1;
				next[i] = (rayOrigin[i] - voxelCoords[i]) * deltaDist[i];
			}
			else
			{
				step[i] = 1;
				next[i] = (voxelCoords[i] + 1.f - rayOrigin[i]) * deltaDist[i];
			}
		}

		do
		{
			x = voxelCoords[0];
			y = voxelCoords[1];
			z = voxelCoords[2];
			vox = VoxelTypes.get(world.getDataAt(x, y, z));
			if (vox.isVoxelSolid() || vox.isVoxelSelectable())
			{
				boolean collides = false;
				for (CollisionBox box : vox.getTranslatedCollisionBoxes(world, x, y, z))
				{
					//System.out.println(box);
					Vector3d collisionPoint = box.collidesWith(initialPosition, direction);
					if (collisionPoint != null)
					{
						collides = true;
						//System.out.println("collides @ "+collisionPoint);
					}
				}
				if (collides)
				{
					if (inside)
						return new Location(world, x, y, z);
					else
					{
						//Back off a bit
						switch (side)
						{
						case 0:
							x -= step[side];
							break;
						case 1:
							y -= step[side];
							break;
						case 2:
							z -= step[side];
							break;
						}
						return new Location(world, x, y, z);
					}
				}
			}
			//DDA steps
			side = 0;
			for (int i = 1; i < 3; ++i)
			{
				if (next[side] > next[i])
				{
					side = i;
				}
			}
			next[side] += deltaDist[side];
			voxelCoords[side] += step[side];

			distance += 1;
		}
		while (distance < 256);
		return null;
	}

	public void toogleFly()
	{
		flying = !flying;
	}

	public void toggleNoclip()
	{
		noclip = !noclip;
	}

	@Override
	public void drawHUD(Camera camera)
	{
		if (this.equals(Client.controlledEntity))
			return; // Don't render yourself
		Vector3f posOnScreen = camera.transform3DCoordinate(new Vector3f((float) pos.x, (float) pos.y + 2.5f, (float) pos.z));

		float scale = posOnScreen.z;
		String txt = name;// + rotH;
		float dekal = TrueTypeFont.arial12.getWidth(txt) * 16 * scale;
		if (scale > 0)
			TrueTypeFont.arial12.drawStringWithShadow(posOnScreen.x - dekal / 2, posOnScreen.y, txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	@Override
	public void render(RenderingContext renderingContext)
	{
		Camera cam = renderingContext.getCamera();
		ItemPile selectedItemPile = this.getInventory().getSelectedItem();
		BVHAnimation animation = BVHLibrary.getAnimation("res/models/human-standstill.bvh");
		if (selectedItemPile != null)
		{
			if (selectedItemPile.getItem() instanceof ItemAk47)
				animation = BVHLibrary.getAnimation("res/models/human-rifle-holding.bvh");
			else
				animation = BVHLibrary.getAnimation("res/models/human-holding.bvh");
		}
		//Player textures
		Texture playerTexture = TexturesHandler.getTexture("models/hogubrus3.png");
		playerTexture.setLinearFiltering(false);
		renderingContext.setDiffuseTexture(playerTexture.getID());
		//Players models have no normal mapping
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("textures/normalnormal.png"));

		renderingContext.getCurrentShader().setUniformFloat3("borderShift", pos.castToSP());
		//Prevents laggy behaviour
		if (this.equals(Client.controlledEntity))
			renderingContext.getCurrentShader().setUniformFloat3("borderShift", -(float) cam.pos.x, -(float) cam.pos.y, -(float) cam.pos.z);

		//TODO use some function in World
		int modelBlockData = world.getDataAt((int) pos.x, (int) pos.y, (int) pos.z);
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//Player rotations to the viewmodel
		Matrix4f playerRotationMatrix = new Matrix4f();
		playerRotationMatrix.rotate((90 - rotH) / 180f * 3.14159f, new Vector3f(0, 1, 0));
		if (this.equals(Client.controlledEntity) && !renderingContext.shadow)
			playerRotationMatrix.rotate((-rotV) / 180f * 3.14159f, new Vector3f(0, 0, 1));
		playerRotationMatrix.translate(new Vector3f(0f, -(float) this.eyePosition, 0f), playerRotationMatrix);
		renderingContext.sendTransformationMatrix(playerRotationMatrix);
		//Render parts of the body
		if (!renderingContext.shadow && this.equals(Client.controlledEntity))
			ModelLibrary.getMesh("res/models/human.obj").render(renderingContext, fp_elements, animation, 0);
		else
			ModelLibrary.getMesh("res/models/human.obj").render(renderingContext);

		//Matrix to itemInHand bone in the player's bvh
		Matrix4f itemMatrix = new Matrix4f();
		itemMatrix = animation.getTransformationForBone("boneItemInHand", 0);
		Matrix4f.mul(playerRotationMatrix, itemMatrix, itemMatrix);

		if (selectedItemPile != null)
			selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, itemMatrix);
	}

	static Set<String> fp_elements = new HashSet<String>();

	static
	{
		fp_elements.add("boneArmLU");
		fp_elements.add("boneArmRU");
		fp_elements.add("boneArmLD");
		fp_elements.add("boneArmRD");
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String n)
	{
		this.inventory.name = this.name + "'s Inventory";
		name = n;
	}

	Controller controller;

	@Override
	public Controller getController()
	{
		return controller;
	}

	@Override
	public void setController(Controller controller)
	{
		this.controller = controller;
	}

	@Override
	public void moveCamera(ClientController controller)
	{
		if (controller.hasFocus())
		{
			moveCamera();
		}
	}

	@Override
	public boolean handleInteraction(Input input)
	{
		ItemPile itemSelected = this.getInventory().getSelectedItem();
		if (itemSelected != null)
		{
			//See if the item handles the interaction
			if(itemSelected.getItem().handleInteraction(this, itemSelected, input))
				return true;
		}
		//Here goes generic entity response to interaction
		
		//Then we check if the world minds being interacted with
		Location blockLocation = this.getBlockLookingAt(true);
		world.handleInteraction(this, blockLocation, input);
		return false;
	}
}
