package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.Camera;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.api.rendering.Light;
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
	public long entityID;

	public World world;
	public Vector3d pos;
	public Vector3d vel;
	public Vector3d acc;
	
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
	public ChunkHolder parentHolder;

	protected boolean flying = false;

	//Flag set when deleted from world entities list ( to report to other refering places )
	public boolean mpSendDeletePacket = false;

	public EntityImplementation(World w, double x, double y, double z)
	{
		world = w;
		pos = new Vector3d(x, y, z);
		vel = new Vector3d();
		acc = new Vector3d();
		//pos.x = x;
		//pos.y = y;
		//pos.z = z;
		updatePosition();
		//To avoid NPEs
		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y), (int) (pos.z))));
	}


	/**
	 * Returns the location of the entity
	 * 
	 * @return
	 */
	@Override
	public Location getLocation()
	{
		return new Location(world, pos);
	}

	/**
	 * Sets the location of the entity
	 * 
	 * @param loc
	 */
	@Override
	public void setLocation(Location loc)
	{
		this.pos.x = loc.x;
		this.pos.y = loc.y;
		this.pos.z = loc.z;
		
		updatePosition();
		if (this instanceof EntityControllable && ((EntityControllable) this).getController() != null)
			((EntityControllable) this).getController().notifyTeleport(this);
	}

	@Override
	public WorldInterface getWorld()
	{
		return world;
	}

	@Override
	public ChunkHolder getChunkHolder()
	{
		return parentHolder;
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
		pos.x %= world.getSizeSide();
		pos.z %= world.getSizeSide();

		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (pos.x), (int) (pos.y), (int) (pos.z))));
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
		if (!flying)
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

		if (!world.isChunkLoaded((int) pos.x / 32, (int) pos.y / 32, (int) pos.z / 32))
		{
			vel.zero();
		}

		//TODO use vector3d there
		blockedMomentum = moveWithCollisionRestrain(vel.x, vel.y, vel.z, true);

		updatePosition();
	}

	@Override
	/**
	 * Prevents entities from going outside the world area and updates the parentHolder reference
	 */
	public boolean updatePosition()
	{
		pos.x %= world.getSizeSide();
		pos.z %= world.getSizeSide();
		if (pos.x < 0)
			pos.x += world.getSizeSide();
		if (pos.z < 0)
			pos.z += world.getSizeSide();
		int regionX = (int) (pos.x / (32 * 8));
		int regionY = (int) (pos.y / (32 * 8));
		if (regionY < 0)
			regionY = 0;
		if (regionY > world.getMaxHeight() / (32 * 8))
			regionY = world.getMaxHeight() / (32 * 8);
		int regionZ = (int) (pos.z / (32 * 8));
		if (parentHolder != null && parentHolder.regionX == regionX && parentHolder.regionY == regionY && parentHolder.regionZ == regionZ)
		{
			return false; // Nothing to do !
		}
		else
		{
			//if(parentHolder != null)
			//	parentHolder.removeEntity(this);
			parentHolder = world.chunksHolder.getChunkHolder(regionX * 8, regionY * 8, regionZ * 8, true);
			//parentHolder.addEntity(this);
			/*System.out.println("Had to move entity "+this+" to a new holder :");
			System.out.println("RegionX : "+regionX+" PH: "+parentHolder.regionX);
			System.out.println("RegionY : "+regionY+" PH: "+parentHolder.regionY);
			System.out.println("RegionZ : "+regionZ+" PH: "+parentHolder.regionZ);*/
			return true;
		}
	}

	@Override
	public void moveWithoutCollisionRestrain(double mx, double my, double mz)
	{
		pos.x += mx;
		pos.y += my;
		pos.z += mz;
	}

	@Override
	public String toString()
	{
		return "['" + this.getClass().getName() + "'] pos.x : " + clampDouble(pos.x) + " pos.y" + clampDouble(pos.y) + " pos.z" + clampDouble(pos.z) + " UUID : " + entityID + " EID : " + this.getEID() + " Holder:" + this.parentHolder;
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
		return distanceToTravel;
	}

	@Override
	public Light[] getLights()
	{
		return null;
	}

	@Override
	public CollisionBox[] getTranslatedCollisionBoxes()
	{
		return new CollisionBox[] { getCollisionBox().translate(pos.x, pos.y, pos.z) };
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
			camera.pos = new Vector3d(pos).negate();
			//camera.pos.x = -pos.x;
			//camera.pos.y = -pos.y;
			//camera.pos.z = -pos.z;

			camera.view_rotx = rotV;
			camera.view_roty = rotH;

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
		return entityID;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Entity))
			return false;
		return ((Entity) o).getUUID() == entityID;
	}

	@Override
	public void delete()
	{
		mpSendDeletePacket = true;
	}

	@Override
	public Inventory getInventory()
	{
		return inventory;
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
		return !mpSendDeletePacket;
	}
	

	/**
	 * Loads the object state from the stream
	 * @param stream
	 * @throws IOException
	 */
	public void load(DataInputStream stream) throws IOException
	{
		
	}

	/**
	 * Writes the object state to a stream
	 * @param stream
	 * @throws IOException
	 */
	public void save(DataOutputStream stream) throws IOException
	{
		
	}
	
}
