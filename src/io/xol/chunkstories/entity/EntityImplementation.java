package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentExistence;
import io.xol.chunkstories.core.entity.components.EntityComponentPosition;
import io.xol.chunkstories.core.entity.components.EntityComponentVelocity;
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
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
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
	private EntityComponentVelocity velocity = new EntityComponentVelocity(this, position);

	public WorldImplementation world;
	
	//public Vector3d velocity;
	public Vector3d acceleration;

	public boolean collision_top = false;
	public boolean collision_bot = false;
	public boolean collision_left = false;
	public boolean collision_right = false;
	public boolean collision_north = false;
	public boolean collision_south = false;

	public Vector3d blockedMomentum = new Vector3d();

	//public boolean inWater = false;
	public Voxel voxelIn;
	
	private boolean hasSpawned = false;

	public EntityImplementation(WorldImplementation w, double x, double y, double z)
	{
		world = w;

		position.setWorld(w);
		position.setPositionXYZ(x, y, z);
		
		//velocity = new Vector3d();
		acceleration = new Vector3d();
		//checkPositionAndUpdateHolder();
		//To avoid NPEs
		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getVoxelData(position.getLocation(), false)));
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

	protected EntityComponentVelocity getVelocityComponent()
	{
		return velocity;
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
	}

	@Override
	public World getWorld()
	{
		return world;
	}

	@Override
	public Region getChunkHolder()
	{
		return position.getRegionWithin();
	}

	public void setVelocity(double x, double y, double z)
	{
		/*
		velocity.x = x;
		velocity.y = y;
		velocity.z = z;*/
	}

	public void applyExternalForce(double x, double y, double z)
	{/*
		velocity.x += x;
		velocity.y += y;
		velocity.z += z;*/
	}

	// Ran each tick
	@Override
	public void tick()
	{
		
	}

	@Override
	public void moveWithoutCollisionRestrain(double mx, double my, double mz)
	{
		Vector3d pos = new Vector3d(position.getLocation());
		pos.setX(pos.getX() + mx);
		pos.setY(pos.getY() + my);
		pos.setZ(pos.getZ() + mz);
		position.setPosition(pos);
	}

	@Override
	public void moveWithoutCollisionRestrain(Vector3d delta)
	{
		Vector3d pos = new Vector3d(position.getLocation());
		pos.add(delta);
		position.setPosition(pos);
	}

	@Override
	public String toString()
	{
		return "[" + this.getClass().getSimpleName() + " holder: "+position.getRegionWithin()+" pos : " + position.getLocation() + " UUID : " + entityUUID + " EID : " + this.getEID() + " Region:" + this.position.getRegionWithin() + " ]";
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
		return moveWithCollisionRestrain(vec, false);
	}

	public Vector3d moveWithCollisionRestrain(Vector3d vec, boolean writeCollisions)
	{
		return moveWithCollisionRestrain(this.getLocation(), vec, writeCollisions, false);
	}

	@Override
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions)
	{
		return moveWithCollisionRestrain(new Vector3d(mx, my, mz), writeCollisions);
	}

	/**
	 * Does the hitboxes computations to determine if that entity could move that delta
	 * 
	 * @return The remaining distance in each dimension if he got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can move without colliding with anything )
	 */
	public Vector3d canMoveWithCollisionRestrain(Vector3d delta)
	{
		return moveWithCollisionRestrain(this.getLocation(), delta, false, true);
	}

	/**
	 * Does the hitboxes computations to determine if that entity could move that delta
	 * 
	 * @param from
	 *            Change the origin of the movement from the default ( current entity position )
	 * @return The remaining distance in each dimension if he got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can move without colliding with anything )
	 */
	public Vector3d canMoveWithCollisionRestrain(Vector3d from, Vector3d delta)
	{
		return moveWithCollisionRestrain(from, delta, false, true);
	}

	// Convinience method
	private Vector3d moveWithCollisionRestrain(Vector3d from, Vector3d delta, boolean writeCollisions, boolean onlyTest)
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
		//Extract the current position
		Vector3d pos = new Vector3d(from);

		//Keep biggest distanceToTravel in each dimension
		Vector3d maxDistanceToTravel = new Vector3d(0.0);
		
		//Iterate over every box
		//CollisionBox[] translatedBoxes = getCollisionBoxes();
		for (int r = 0; r < getCollisionBoxes().length; r++)
		{
			// Make a normalized double vector and keep the original length
			Vector3d vec = new Vector3d(delta);
			Vector3d distanceToTravel = new Vector3d(delta);
			double len = vec.length();
			vec.normalize();
			vec.scale(0.25d);
			// Do it block per block, face per face
			double distanceTraveled = 0;
			
			CollisionBox checkerX = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());
			CollisionBox checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());
			CollisionBox checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());

			//translateAll(checkerX, pos);
			//translateAll(checkerY, pos);
			//translateAll(checkerZ, pos);

			double pmx, pmy, pmz;

			while (distanceTraveled < len)
			{
				if (len - distanceTraveled > 0.25)
				{
					distanceTraveled += 0.25;
				}
				else
				{
					vec = new Vector3d(delta);
					vec.normalize();
					vec.scale(len - distanceTraveled);
					distanceTraveled = len;
				}

				pmx = vec.getX();
				pmy = vec.getY();
				pmz = vec.getZ();

				int radius = 2;

				// checkerX = getCollisionBox().translate(pos.x+pmx, pos.y, pos.z);
				Voxel vox;
				checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + pmz);
				// Z part
				for (int i = ((int) pos.getX()) - radius; i <= ((int) pos.getX()) + radius; i++)
					for (int j = ((int) pos.getY() - 1); j <= ((int) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int) pos.getZ()) - radius; k <= ((int) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelTypes.get(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
								if (boxes != null)
									for (CollisionBox b : boxes)
									{
										b.translate(i, j, k);
										if (delta.getZ() != 0.0)
										{
											if (checkerZ.collidesWith(b))
											{
												collision = true;
												if (collision == false)
													break;
												pmz = 0;
												if (delta.getZ() < 0)
												{
													double south = Math.min((b.zpos + b.zw / 2.0 + checkerZ.zw / 2.0) - (pos.getZ()), 0.0d);
													// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
													pmz = south;
													if (writeCollisions)
														collision_south = true;
												}
												else
												{
													double north = Math.max((b.zpos - b.zw / 2.0 - checkerZ.zw / 2.0) - (pos.getZ()), 0.0d);
													// System.out.println(right);
													pmz = north;
													if (writeCollisions)
														collision_north = true;
												}
												vec.setZ(0);
												checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + pmz);
											}
										}
									}
							}
						}
				distanceToTravel.setZ(distanceToTravel.getZ() - pmz);
				pos.setZ(pos.getZ() + pmz);
				checkerX = getCollisionBoxes()[r].translate(pos.getX() + pmx, pos.getY(), pos.getZ());
				// X-part
				for (int i = ((int) pos.getX()) - radius; i <= ((int) pos.getX()) + radius; i++)
					for (int j = ((int) pos.getY() - 1); j <= ((int) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int) pos.getZ()) - radius; k <= ((int) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelTypes.get(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
								if (boxes != null)
									for (CollisionBox b : boxes)
									{
										b.translate(i, j, k);

										if (delta.getX() != 0.0)
										{
											if (checkerX.collidesWith(b))
											{
												collision = true;
												if (collision == false)
													break;
												pmx = 0;
												if (delta.getX() < 0)
												{
													double left = Math.min((b.xpos + b.xw / 2.0 + checkerX.xw / 2.0) - (pos.getX()), 0.0d);
													// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
													pmx = left;
													if (writeCollisions)
														collision_left = true;
												}
												else
												{
													double right = Math.max((b.xpos - b.xw / 2.0 - checkerX.xw / 2.0) - (pos.getX()), 0.0d);
													// System.out.println(right);
													pmx = right;
													if (writeCollisions)
														collision_right = true;
												}
												vec.setX(0);
												checkerX = getCollisionBoxes()[r].translate(pos.getX() + pmx, pos.getY(), pos.getZ());
											}
										}
									}
							}
						}
				pos.setX(pos.getX() + pmx);
				distanceToTravel.setX(distanceToTravel.getX() - pmx);

				checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + pmy, pos.getZ());
				for (int i = ((int) pos.getX()) - radius; i <= ((int) pos.getX()) + radius; i++)
					for (int j = ((int) pos.getY()) - 1; j <= ((int) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int) pos.getZ()) - radius; k <= ((int) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelTypes.get(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
								if (boxes != null)
									for (CollisionBox b : boxes)
									{
										b.translate(i, j, k);
										if (delta.getY() != 0.0)
										{
											if (checkerY.collidesWith(b))
											{
												collision = true;
												pmy = 0;
												if (delta.getY() < 0)
												{
													double top = Math.min((b.ypos + b.h) - pos.getY(), 0.0d);
													// System.out.println(top);
													pmy = top;
													if (writeCollisions)
														collision_bot = true;
												}
												else
												{
													double bot = Math.max((b.ypos) - (pos.getY() + checkerY.h), 0.0d);
													// System.out.println(bot);
													pmy = bot;
													if (writeCollisions)
														collision_top = true;
												}
												vec.setY(0);
												checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + pmy, pos.getZ());
											}
										}

									}
							}
						}
				pos.setY(pos.getY() + pmy);
				distanceToTravel.setY(distanceToTravel.getY() - pmy);
			}

			if(Math.abs(distanceToTravel.getX()) > Math.abs(maxDistanceToTravel.getX()))
				maxDistanceToTravel.setX(distanceToTravel.getX());

			if(Math.abs(distanceToTravel.getY()) > Math.abs(maxDistanceToTravel.getY()))
				maxDistanceToTravel.setY(distanceToTravel.getY());

			if(Math.abs(distanceToTravel.getZ()) > Math.abs(maxDistanceToTravel.getZ()))
				maxDistanceToTravel.setZ(distanceToTravel.getZ());
			
			//System.out.println("cuck'd"+distanceToTravel);
		}
		//Set the new position after computations have been done
		
		if (!onlyTest)
			this.moveWithoutCollisionRestrain(delta.getX() - maxDistanceToTravel.getX(), delta.getY() - maxDistanceToTravel.getY(), delta.getZ() - maxDistanceToTravel.getZ());
			//this.position.setPosition(pos);

		//System.out.println("cuck'd"+maxDistanceToTravel);
		return maxDistanceToTravel;
	}

	public boolean collidesWith(CollisionBox box)
	{
		return box.collidesWith(this);
	}
	
	public boolean collidesWith(Entity entity)
	{
		for(CollisionBox box : this.getTranslatedCollisionBoxes())
		{
			if(box.collidesWith(entity))
				return true;
		}
		return false;
	}
	
	public Vector3d collidesWith(Vector3d lineStart, Vector3d lineDirection)
	{
		for(CollisionBox box : this.getTranslatedCollisionBoxes())
		{
			Vector3d collides = box.collidesWith(lineStart, lineDirection);
			if(collides != null)
				return collides;
		}
		return null;
	}

	@Override
	public CollisionBox[] getTranslatedCollisionBoxes()
	{
		CollisionBox[] boxes = getCollisionBoxes();
		for (CollisionBox box : boxes)
			box.translate(getLocation());
		return boxes;
		//return new CollisionBox[] { getCollisionBox().translate(position.getLocation()) };
	}

	@Override
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(1.0, 1.0, 1.0) };
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

			//camera.rotationX = rotV;
			//camera.rotationY = rotH;

			//Default FOV
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
	public boolean removeFromWorld()
	{
		//Only once
		if(existence.exists())
		{
			//Destroys it
			existence.destroyEntity();
		
			//Removes it's reference within the region
			if(this.position.getRegionWithin() != null)
				this.position.getRegionWithin().removeEntity(this);
			
			//Actually removes it from the world list
			if(this.world != null)
				this.world.removeEntityFromList(this);
			
			return true;
		}
		return false;
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
	
	public boolean hasSpawned()
	{
		return hasSpawned;
	}
	
	public void markHasSpawned()
	{
		hasSpawned = true;
	}

	@Override
	public void setUUID(long uuid)
	{
		//Don't allow UUID changes once spawned !
		if(entityUUID != -1 && this.hasSpawned())
			throw new IllegalUUIDChangeException();
		
		this.entityUUID = uuid;
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
		if (subscribers.add(subscriber))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean unsubscribe(Subscriber subscriber)
	{
		//If it did contain the subscriber
		if (subscribers.remove(subscriber))
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
