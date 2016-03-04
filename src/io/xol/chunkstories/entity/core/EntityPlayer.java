package io.xol.chunkstories.entity.core;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import io.xol.chunkstories.api.entity.ClientController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityHUD;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.entity.EntityNameable;
import io.xol.chunkstories.entity.EntityRotateable;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.net.packets.Packet04Entity;
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
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityPlayer extends EntityImplementation implements EntityControllable, EntityHUD, EntityNameable, EntityRotateable
{
	boolean noclip = true;

	float lastPX = -1f;
	float lastPY = -1f;

	boolean running = false;

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
			velY = jump;
			jump = 0;
		}

		boolean l = collision_bot;

		accelerationVector = new Vector3d(targetVectorX - velX, 0, targetVectorZ - velZ);

		double modifySpd = collision_bot ? 0.010 : 0.005;

		if (accelerationVector.length() > modifySpd)
		{
			accelerationVector.normalize();
			accelerationVector.scale(modifySpd);
		}

		if (flying)
		{
			this.velX = 0;
			this.velY = 0;
			this.velZ = 0;
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
		eyePosition = 1.6 + Math.sin(walked * 5d) * 0.035d;
	}

	// client-side method for updating the player movement
	public void tick(ClientController controller)
	{
		// super.changeChunk();
		// Null-out acceleration, until modified by controls
		if (flying)
			flyMove(controller.hasFocus());
		else
			normalMove(controller.hasFocus());
		//System.out.println("focus");
		super.updatePosition();

		if (Client.connection != null)
		{
			Packet04Entity packet = new Packet04Entity(true);
			packet.includeRotation = true;
			packet.applyFromEntity(this);
			Client.connection.sendPacket(packet);
			// Client.connection.sendTextMessage("player/position:"+posX+":"+posY+":"+posZ);
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

	public void normalMove(boolean focus)
	{
		//voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (posX), (int) (posY + 1), (int) (posZ))));
		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
		boolean onLadder = voxelIn instanceof VoxelClimbable;
		if (onLadder)
		{
			onLadder = false;
			CollisionBox[] boxes = voxelIn.getTranslatedCollisionBoxes(world, (int) (posX), (int) (posY), (int) (posZ));
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
			Client.getSoundManager().playSoundEffect("footsteps/jump.ogg", posX, posY, posZ, (float) (0.9f + Math.sqrt(velX * velX + velY * velY) * 0.1f), 1f);
		}
		if (landed)
		{
			landed = false;
			Client.getSoundManager().playSoundEffect("footsteps/jump.ogg", posX, posY, posZ, (float) (0.9f + Math.sqrt(velX * velX + velY * velY) * 0.1f), 1f);
		}

		if (walked > 0.2 * Math.PI * 2)
		{
			walked %= 0.2 * Math.PI * 2;
			Client.getSoundManager().playSoundEffect("footsteps/generic" + ((int) (1 + Math.floor(Math.random() * 3))) + ".ogg", posX, posY, posZ, (float) (0.9f + Math.sqrt(velX * velX + velY * velY) * 0.1f), 1f);
			// System.out.println("footstep");
		}

		if (focus && !inWater && Keyboard.isKeyDown(FastConfig.JUMP_KEY) && collision_bot)
		{
			// System.out.println("jumpin");
			jump = 0.15;
		}
		else if (focus && inWater && Keyboard.isKeyDown(FastConfig.JUMP_KEY))
			jump = 0.05;
		else
			jump = 0.0;

		// Movement
		// Run ?
		if (focus && Keyboard.isKeyDown(FastConfig.FORWARD_KEY))
		{
			if (Keyboard.isKeyDown(FastConfig.RUN_KEY))
				running = true;
		}
		else
			running = false;

		double modif = 0;
		if (focus)
		{
			if (Keyboard.isKeyDown(FastConfig.FORWARD_KEY) || Keyboard.isKeyDown(FastConfig.LEFT_KEY) || Keyboard.isKeyDown(FastConfig.RIGHT_KEY))
				hSpeed = (running ? 0.09 : 0.06);
			else if (Keyboard.isKeyDown(FastConfig.BACK_KEY))
				hSpeed = -0.05;
			else
				hSpeed = 0.0;
		}
		else
			hSpeed = 0.0;
		// Water slows you down
		if (inWater)
			hSpeed *= 0.45;

		if (Keyboard.isKeyDown(FastConfig.LEFT_KEY))
			modif += 90 * (Keyboard.isKeyDown(FastConfig.FORWARD_KEY) ? 0.5 : 1);
		if (Keyboard.isKeyDown(FastConfig.RIGHT_KEY))
			modif += -90 * (Keyboard.isKeyDown(FastConfig.FORWARD_KEY) ? 0.5 : 1);

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
				this.velY = (float)(Math.sin((-(rotV) / 180f * Math.PI)) * hSpeed);
			}
		}

		targetVectorX = Math.sin((180 - rotH + modif) / 180f * Math.PI) * hSpeed;
		targetVectorZ = Math.cos((180 - rotH + modif) / 180f * Math.PI) * hSpeed;

	}

	public void flyMove(boolean focus)
	{
		if (!focus)
			return;
		velX = velY = velZ = 0;
		eyePosition = 1.6;
		float camspeed = 0.125f;
		if (Keyboard.isKeyDown(42))
			camspeed = 1f;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
			camspeed = 5f;
		if (Keyboard.isKeyDown(FastConfig.BACK_KEY))
		{
			float a = (float) ((-rotH) / 180f * Math.PI);
			float b = (float) ((rotV) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (Keyboard.isKeyDown(FastConfig.FORWARD_KEY))
		{
			float a = (float) ((180 - rotH) / 180f * Math.PI);
			float b = (float) ((-rotV) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b), true);
		}
		if (Keyboard.isKeyDown(32))
		{
			float a = (float) ((-rotH - 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed, true);
		}
		if (Keyboard.isKeyDown(FastConfig.LEFT_KEY))
		{
			float a = (float) ((-rotH + 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed, true);
		}
	}

	public void setupCamera(Camera camera)
	{
		synchronized (this)
		{
			camera.camPosX = (float) -posX;
			camera.camPosY = (float) -(posY + eyePosition);
			camera.camPosZ = (float) -posZ;

			camera.view_rotx = rotV;
			camera.view_roty = rotH;

			camera.fov = (float) (FastConfig.fov + ((velX * velX + velZ * velZ) > 0.07 * 0.07 ? ((velX * velX + velZ * velZ) - 0.07 * 0.07) * 500 : 0));

			camera.alUpdate();
		}
	}

	public int[] rayTraceSelectedBlock(boolean inside)
	{
		double rayX = posX;
		double rayY = posY + 1 + (eyePosition);
		double rayZ = posZ;

		double step = 0.1f;

		Vector3d direction = new Vector3d();

		float a = (float) ((-rotH) / 360f * 2 * Math.PI);
		float b = (float) ((rotV) / 360f * 2 * Math.PI);

		direction.x = -(float) Math.sin(a) * (float) Math.cos(b);
		direction.y = -(float) Math.sin(b);
		direction.z = -(float) Math.cos(a) * (float) Math.cos(b);

		direction.normalize();
		direction.scale(step);

		float distance = 0f;
		int i = 0;
		while (distance < 1024)
		{
			int x = (int) rayX;
			int y = (int) rayY;
			int z = (int) rayZ;

			// Distance of current point from cube center
			double bModX = rayX % 1.0 - 0.5;
			double bModY = rayY % 1.0 - 0.5;
			double bModZ = rayZ % 1.0 - 0.5;

			// System.out.println("not skippin");
			int id = world.getDataAt(x, y, z);
			if (VoxelTypes.get(id).isVoxelSelectable())
			{
				if (!inside)
				{
					double length = Math.sqrt(bModX * bModX + bModY * bModY + bModZ * bModZ);

					double modifier = 1.0 / length;

					length *= modifier;
					bModX *= modifier;
					bModY *= modifier;
					bModZ *= modifier;

					if (Math.abs(bModX) > Math.abs(bModY))
					{
						if (Math.abs(bModX) > Math.abs(bModZ))
						{
							if (bModX > 0)
								x++;
							else
								x--;
						}
						else
						{
							if (bModZ > 0)
								z++;
							else
								z--;
						}
					}
					else
					{
						if (Math.abs(bModZ) > Math.abs(bModY))
						{
							if (bModZ > 0)
								z++;
							else
								z--;
						}
						else
						{
							if (bModY > 0)
								y++;
							else
								y--;
						}
					}
				}
				return new int[] { x, y, z };
			}
			rayX += direction.x * (i % 3 == 0 ? 1 : 0);
			rayY += direction.y * (i % 3 == 1 ? 1 : 0);
			rayZ += direction.z * (i % 3 == 2 ? 1 : 0);
			i++;
			distance += step;
		}
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
		Vector3f posOnScreen = camera.transform3DCoordinate(new Vector3f((float) posX, (float) posY + 2.5f, (float) posZ));

		float scale = posOnScreen.z;
		String txt = name;// + rotH;
		float dekal = TrueTypeFont.arial12.getWidth(txt) * 16 * scale;
		if (scale > 0)
			TrueTypeFont.arial12.drawStringWithShadow(posOnScreen.x - dekal / 2, posOnScreen.y, txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	public void render(RenderingContext renderingContext)
	{
		ItemPile selectedItemPile = this.getInventory().getSelectedItem();
		if(selectedItemPile != null)
		{
			Matrix4f transformation = new Matrix4f();
			transformation.setIdentity();
			selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, getWorld(), transformation);
		}
		
		if (this.equals(Client.controlledEntity))
			return; // Don't render yourself

		renderingContext.setDiffuseTexture(TexturesHandler.getTextureID("models/hogubrus3.png"));
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("textures/normalnormal.png"));
		renderingContext.renderingShader.setUniformFloat3("borderShift", (float) posX, (float) posY, (float) posZ);
		int modelBlockData = world.getDataAt((int) posX, (int) posY + 1, (int) posZ);
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.renderingShader.setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		Matrix4f mutrix = new Matrix4f();
		mutrix.rotate((90 - rotH) / 180 * 3.14159f, new Vector3f(0, 1, 0));

		renderingContext.renderingShader.setUniformMatrix4f("localTransform", mutrix);
		//debugDraw();
		ModelLibrary.getMesh("res/models/human.obj").render(renderingContext);
		//ModelLibrary.loadAndRenderAnimatedMesh("res/models/human.obj", "res/models/human-fixed-standstill.bvh", 0);
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
}
