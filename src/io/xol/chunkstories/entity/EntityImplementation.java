package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.core.components.EntityComponentController;
import io.xol.chunkstories.entity.core.components.EntityComponentExistence;
import io.xol.chunkstories.entity.core.components.EntityComponentPosition;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.Camera;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.exceptions.IllegalUUIDChangeException;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class EntityImplementation implements Entity
{
	public long entityUUID = -1;
	
	Set<Subscriber> subscribers = new HashSet<Subscriber>();

	protected EntityComponentExistence existence = new EntityComponentExistence(this, null);
	protected EntityComponentPosition position = new EntityComponentPosition(this, existence);

	public World world;
	
	//public Vector3d pos;
	
	public Vector3d vel;
	public Vector3d acc;
	
	protected boolean flying = false;

	//public double pos.x, pos.y, pos.z;
	//public double vel.x, vel.y, vel.z;
	public float rotH, rotV;

	public boolean collision_top = false;
	public boolean collision_bot = false;
	public boolean collision_left = false;
	public boolean collision_right = false;
	public boolean collision_north = false;
	public boolean collision_south = false;

	public Vector3d blockedMomentum = new Vector3d();

	//public boolean inWater = false;
	public Voxel voxelIn;
	public Inventory inventory;

	//Flag set when deleted from world entities list ( to report to other refering places )
	
	//AtomicBoolean removed = new AtomicBoolean(false);
	// public boolean mpSendDeletePacket = false;

	public EntityImplementation(World w, double x, double y, double z)
	{
		world = w;
		
		position.setPositionXYZ(x, y, z);
		//pos = new Vector3d(x, y, z);
		vel = new Vector3d();
		acc = new Vector3d();
		//pos.x = x;
		//pos.y = y;
		//pos.z = z;
		//checkPositionAndUpdateHolder();
		//To avoid NPEs
		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt(position.getLocation())));
	}

	/**
	 * Returns the location of the entity
	 * 
	 * @return
	 */
	@Override
	public Location getLocation()
	{
		return position.getLocation();
	}

	/**
	 * Sets the location of the entity
	 * 
	 * @param loc
	 */
	@Override
	public void setLocation(Location loc)
	{
		position.setLocation(loc);
		/*this.pos.x = loc.x;
		this.pos.y = loc.y;
		this.pos.z = loc.z;*/

		//checkPositionAndUpdateHolder();
		//if (this instanceof EntityControllable && ((EntityControllable) this).getController() != null)
		//	((EntityControllable) this).getController().notifyTeleport(this);
	}

	@Override
	public WorldInterface getWorld()
	{
		return world;
	}

	@Override
	public ChunkHolder getChunkHolder()
	{
		return null;
	}

	public void setVelocity(double x, double y, double z)
	{
		vel.x = x;
		vel.y = y;
		vel.z = z;
	}

	public void applyExternalForce(double x, double y, double z)
	{
		vel.x += x;
		vel.y += y;
		vel.z += z;
	}

	// Ran each tick
	@Override
	public void tick()
	{
		//Irrelevant.
		//pos.x %= world.getWorldSize();
		//pos.z %= world.getWorldSize();

		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt(position.getLocation())));
		boolean inWater = voxelIn.isVoxelLiquid();

		// vel.z=Math.cos(a)*hSpeed*0.1;
		if (collision_left || collision_right)
			vel.x = 0;
		if (collision_north || collision_south)
			vel.z = 0;
		// Stap it
		if (collision_bot && vel.y < 0)
			vel.y = 0;
		else if (collision_top)
			vel.y = 0;

		// Gravity
		if (!isFlying())
		{
			double terminalVelocity = inWater ? -0.02 : -0.5;
			if (vel.y > terminalVelocity)
				vel.y -= 0.008;
			if (vel.y < terminalVelocity)
				vel.y = terminalVelocity;
		}

		// Acceleration
		vel.x += acc.x;
		vel.y += acc.y;
		vel.z += acc.z;

		//TODO ugly
		if (!world.isChunkLoaded((int) position.getLocation().x / 32, (int) position.getLocation().y / 32, (int) position.getLocation().z / 32))
		{
			vel.zero();
		}

		//TODO use vector3d there
		blockedMomentum = moveWithCollisionRestrain(vel.x, vel.y, vel.z, true);

		//checkPositionAndUpdateHolder();
	}

	@Override
	public void moveWithoutCollisionRestrain(double mx, double my, double mz)
	{
		Vector3d pos = new Vector3d(position.getLocation());
		pos.x += mx;
		pos.y += my;
		pos.z += mz;
		position.setPosition(pos);
	}

	@Override
	public String toString()
	{
		return "['" + this.getClass().getName() + "'] pos : " + position.getLocation() + " UUID : " + entityUUID + " EID : " + this.getEID() + " Holder:" + "WIP" + "Inventory : "
				+ this.inventory;
	}

	double clampDouble(double d)
	{
		d *= 100;
		d = Math.floor(d);
		d /= 100.0;
		return d;
	}

	@Override
	public Vector3d moveWithCollisionRestrain(Vector3d vec)
	{
		return moveWithCollisionRestrain(vec.x, vec.y, vec.z, false);
	}

	// Convinience method
	@Override
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions)
	{
		int id, data;

		boolean collision = false;
		if (writeCollisions)
		{
			collision_top = false;
			collision_bot = false;
			collision_left = false;
			collision_right = false;
			collision_north = false;
			collision_south = false;
		}
		// Make a normalized double vector and keep the original length
		Vector3d vec = new Vector3d(mx, my, mz);
		Vector3d distanceToTravel = new Vector3d(mx, my, mz);
		double len = vec.length();
		vec.normalize();
		vec.scale(0.25d);
		// Do it block per block, face per face
		double distanceTraveled = 0;
		// CollisionBox checker = getCollisionBox().translate(pos.x, pos.y, pos.z);

		//Extract the current position
		Vector3d pos = new Vector3d(position.getLocation());
		
		CollisionBox checkerX = getCollisionBox().translate(pos.x, pos.y, pos.z);
		CollisionBox checkerY = getCollisionBox().translate(pos.x, pos.y, pos.z);
		CollisionBox checkerZ = getCollisionBox().translate(pos.x, pos.y, pos.z);

		double pmx, pmy, pmz;

		while (distanceTraveled < len)
		{
			if (len - distanceTraveled > 0.25)
			{
				distanceTraveled += 0.25;
			}
			else
			{
				vec = new Vector3d(mx, my, mz);
				vec.normalize();
				vec.scale(len - distanceTraveled);
				distanceTraveled = len;
			}

			pmx = vec.x;
			pmy = vec.y;
			pmz = vec.z;

			int radius = 2;

			// checkerX = getCollisionBox().translate(pos.x+pmx, pos.y, pos.z);
			Voxel vox;
			checkerZ = getCollisionBox().translate(pos.x, pos.y, pos.z + pmz);
			// Z part
			for (int i = ((int) pos.x) - radius; i <= ((int) pos.x) + radius; i++)
				for (int j = ((int) pos.y - 1); j <= ((int) pos.y) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) pos.z) - radius; k <= ((int) pos.z) + radius; k++)
					{
						data = this.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);
									if (mz != 0.0)
									{
										if (checkerZ.collidesWith(b))
										{
											collision = true;
											if (collision == false)
												break;
											pmz = 0;
											if (mz < 0)
											{
												double south = Math.min((b.zpos + b.zw / 2.0 + checkerZ.zw / 2.0) - (pos.z), 0.0d);
												// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
												pmz = south;
												if (writeCollisions)
													collision_south = true;
											}
											else
											{
												double north = Math.max((b.zpos - b.zw / 2.0 - checkerZ.zw / 2.0) - (pos.z), 0.0d);
												// System.out.println(right);
												pmz = north;
												if (writeCollisions)
													collision_north = true;
											}
											vec.z = 0;
											checkerZ = getCollisionBox().translate(pos.x, pos.y, pos.z + pmz);
										}
									}
								}
						}
					}
			distanceToTravel.z -= pmz;
			pos.z += pmz;
			checkerX = getCollisionBox().translate(pos.x + pmx, pos.y, pos.z);
			// X-part
			for (int i = ((int) pos.x) - radius; i <= ((int) pos.x) + radius; i++)
				for (int j = ((int) pos.y - 1); j <= ((int) pos.y) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) pos.z) - radius; k <= ((int) pos.z) + radius; k++)
					{
						data = this.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);

									if (mx != 0.0)
									{
										if (checkerX.collidesWith(b))
										{
											collision = true;
											if (collision == false)
												break;
											pmx = 0;
											if (mx < 0)
											{
												double left = Math.min((b.xpos + b.xw / 2.0 + checkerX.xw / 2.0) - (pos.x), 0.0d);
												// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
												pmx = left;
												if (writeCollisions)
													collision_left = true;
											}
											else
											{
												double right = Math.max((b.xpos - b.xw / 2.0 - checkerX.xw / 2.0) - (pos.x), 0.0d);
												// System.out.println(right);
												pmx = right;
												if (writeCollisions)
													collision_right = true;
											}
											vec.x = 0;
											checkerX = getCollisionBox().translate(pos.x + pmx, pos.y, pos.z);
										}
									}
								}
						}
					}
			pos.x += pmx;
			distanceToTravel.x -= pmx;

			checkerY = getCollisionBox().translate(pos.x, pos.y + pmy, pos.z);
			for (int i = ((int) pos.x) - radius; i <= ((int) pos.x) + radius; i++)
				for (int j = ((int) pos.y) - 1; j <= ((int) pos.y) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) pos.z) - radius; k <= ((int) pos.z) + radius; k++)
					{
						data = this.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);
									if (my != 0.0)
									{
										if (checkerY.collidesWith(b))
										{
											collision = true;
											pmy = 0;
											if (my < 0)
											{
												double top = Math.min((b.ypos + b.h) - pos.y, 0.0d);
												// System.out.println(top);
												pmy = top;
												if (writeCollisions)
													collision_bot = true;
											}
											else
											{
												double bot = Math.max((b.ypos) - (pos.y + checkerY.h), 0.0d);
												// System.out.println(bot);
												pmy = bot;
												if (writeCollisions)
													collision_top = true;
											}
											vec.y = 0;
											checkerY = getCollisionBox().translate(pos.x, pos.y + pmy, pos.z);
										}
									}

								}
						}
					}
			pos.y += pmy;
			distanceToTravel.y -= pmy;
		}
		//Set the new position after computations have been done
		this.position.setPosition(pos);
		
		return distanceToTravel;
	}

	@Override
	public CollisionBox[] getTranslatedCollisionBoxes()
	{
		return new CollisionBox[] { getCollisionBox().translate(position.getLocation()) };
	}

	private CollisionBox getCollisionBox()
	{
		return new CollisionBox(0.75, 1.80, 0.75);
	}

	public void render()
	{
		// Do nothing.
	}

	@Override
	public void debugDraw()
	{
		// Do nothing.
	}

	@Override
	public void setupCamera(Camera camera)
	{
		synchronized (this)
		{
			camera.pos = new Vector3d(position.getLocation()).negate();
			//camera.pos.x = -pos.x;
			//camera.pos.y = -pos.y;
			//camera.pos.z = -pos.z;

			camera.rotationX = rotV;
			camera.rotationY = rotH;

			camera.fov = FastConfig.fov;

			camera.alUpdate();
		}
	}

	@Override
	public short getEID()
	{
		return EntitiesList.getIdForClass(getClass().getName());
	}

	public static short allocatedID = 0;

	@Override
	public long getUUID()
	{
		return entityUUID;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Entity))
			return false;
		return ((Entity) o).getUUID() == entityUUID;
	}

	@Override
	public void delete()
	{
		existence.destroyEntity();
	}

	@Override
	public Inventory getInventory()
	{
		//TODO wip
		return new Inventory(this, 5, 5, "ok");
		//return inventory;
	}

	@Override
	public void setInventory(Inventory inventory)
	{
		this.inventory = inventory;
		inventory.holder = this;
	}

	@Override
	public boolean shouldBeTrackedBy(Player player)
	{
		return !exists();
	}

	/**
	 * Loads the object state from the stream
	 */
	public void loadCSF(DataInputStream stream) throws IOException
	{

	}

	/**
	 * Writes the object state to a stream
	 */
	public void saveCSF(DataOutputStream stream) throws IOException
	{

	}
	
	public boolean exists()
	{
		return existence.exists();
	}

	public boolean isFlying()
	{
		return flying;
	}

	public void setFlying(boolean flying)
	{
		this.flying = flying;

		//if (this instanceof EntityControllable && ((EntityControllable) this).getController() != null)
		//	((EntityControllable) this).getController().notifyFlyingStateChange(this);
	}

	@Override
	public void setUUID(long uuid)
	{
		if(entityUUID == -1)
			this.entityUUID = uuid;
		else
			throw new IllegalUUIDChangeException();
	}

	@Override
	public Iterator<Subscriber> getAllSubscribers()
	{
		return subscribers.iterator();
	}

	@Override
	public boolean subscribe(Subscriber subscriber)
	{
		//If it didn't already contain the subscriber ...
		if(subscribers.add(subscriber))
		{
			//TODO send complete info about subscribed entity ... or do it in ServerPlayer ?
			//Re : let's try
			this.getComponents().pushAllComponents(subscriber);
			System.out.println(subscriber + " subscribed to "+this);
			return true;
		}
		return false;
	}

	@Override
	public boolean unsubscribe(Subscriber subscriber)
	{
		//If it did contain the subscriber
		if(subscribers.remove(subscriber))
		{
			//Push an update to the subscriber telling him to forget about the entity :
			this.existence.pushComponent(subscriber);
			//The existence component checks for the subscriber being present in the subscribees of the entity and if it doesn't find it it will
			//say the entity no longer exists
			return true;
		}
		return false;
	}

	@Override
	/**
	 * Returns first component : existence, all other components are linked to it via a chained list
	 */
	public EntityComponent getComponents()
	{
		return existence;
	}
}
