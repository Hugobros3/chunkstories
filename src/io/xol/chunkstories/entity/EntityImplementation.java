package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentExistence;
import io.xol.chunkstories.core.entity.components.EntityComponentPosition;
import io.xol.chunkstories.core.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
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
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class EntityImplementation implements Entity
{
	//Crucial stuff
	private long entityUUID = -1;
	private boolean hasSpawned = false;
	protected WorldImplementation world;

	//Multiplayer-related
	protected Set<Subscriber> subscribers = new HashSet<Subscriber>();

	//Basic components
	final protected EntityComponentExistence existenceComponent;
	protected EntityComponentPosition positionComponent;
	private EntityComponentVelocity velocityComponent;

	//Physics system info
	//TODO: refactor this out
	public boolean collision_top = false;
	public boolean collision_bot = false;
	public boolean collision_left = false;
	public boolean collision_right = false;
	public boolean collision_north = false;
	public boolean collision_south = false;

	public Vector3dm blockedMomentum = new Vector3dm();

	//Hacky bullshit
	protected Voxel voxelIn;

	private final short eID;
	
	public EntityImplementation(WorldImplementation world, double x, double y, double z)
	{
		this.world = world;

		existenceComponent = new EntityComponentExistence(this, null);
		positionComponent = new EntityComponentPosition(this, existenceComponent);
		velocityComponent = new EntityComponentVelocity(this, positionComponent);
		
		positionComponent.setWorld(world);
		positionComponent.setPositionXYZ(x, y, z);

		//To avoid NPEs
		voxelIn = VoxelsStore.get().getVoxelById(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
		
		eID = world.getGameContext().getContent().entities().getEntityIdByClassname(this.getClass().getName());
	}
	
	public EntityComponentExistence getComponentExistence()
	{
		return this.existenceComponent;
	}

	public EntityComponentPosition getEntityComponentPosition()
	{
		return positionComponent;
	}

	@Override
	public Location getLocation()
	{
		return positionComponent.getLocation();
	}

	public EntityComponentVelocity getVelocityComponent()
	{
		return velocityComponent;
	}

	/**
	 * Sets the location of the entity
	 * 
	 * @param loc
	 */
	@Override
	public void setLocation(Location loc)
	{
		positionComponent.setLocation(loc);
	}

	@Override
	public World getWorld()
	{
		return world;
	}

	@Override
	public Region getRegion()
	{
		return positionComponent.getRegionWithin();
	}

	// Ran each tick
	@Override
	public void tick(WorldAuthority authority)
	{
		//Don't do much
	}

	@Override
	public void moveWithoutCollisionRestrain(double mx, double my, double mz)
	{
		Vector3dm pos = new Vector3dm(positionComponent.getLocation());
		pos.setX(pos.getX() + mx);
		pos.setY(pos.getY() + my);
		pos.setZ(pos.getZ() + mz);
		positionComponent.setPosition(pos);
	}

	@Override
	public void moveWithoutCollisionRestrain(Vector3dm delta)
	{
		Vector3dm pos = new Vector3dm(positionComponent.getLocation());
		pos.add(delta);
		positionComponent.setPosition(pos);
	}

	@Override
	public String toString()
	{
		return "[" + this.getClass().getSimpleName() + ": holderExists: " + (positionComponent.getRegionWithin() != null) + " ,position : " + positionComponent.getLocation() + " UUID : " + entityUUID + " EID : " + this.getEID() + " Region:"
				+ this.positionComponent.getRegionWithin() + " ]";
	}

	double clampDouble(double d)
	{
		d *= 100;
		d = Math.floor(d);
		d /= 100.0;
		return d;
	}

	@Override
	public Vector3dm moveWithCollisionRestrain(Vector3dm vec)
	{
		return moveWithCollisionRestrain(vec, false);
	}

	public Vector3dm moveWithCollisionRestrain(Vector3dm vec, boolean writeCollisions)
	{
		return moveWithCollisionRestrain(this.getLocation(), vec, writeCollisions, false);
	}

	@Override
	public Vector3dm moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions)
	{
		return moveWithCollisionRestrain(new Vector3dm(mx, my, mz), writeCollisions);
	}

	/**
	 * Does the hitboxes computations to determine if that entity could move that delta
	 * 
	 * @return The remaining distance in each dimension if he got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can move without colliding with anything )
	 */
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm delta)
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
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm from, Vector3dm delta)
	{
		return moveWithCollisionRestrain(from, delta, false, true);
	}

	// Convinience method
	private Vector3dm moveWithCollisionRestrain(Vector3dm from, Vector3dm delta, boolean writeCollisions, boolean onlyTest)
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
		Vector3dm pos = new Vector3dm(from);

		//Keep biggest distanceToTravel in each dimension
		Vector3dm maxDistanceToTravel = new Vector3dm(0.0);

		//Iterate over every box
		//CollisionBox[] translatedBoxes = getCollisionBoxes();
		for (int r = 0; r < getCollisionBoxes().length; r++)
		{
			// Make a normalized double vector and keep the original length
			Vector3dm vec = new Vector3dm(delta);
			Vector3dm distanceToTravel = new Vector3dm(delta);
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
					vec = new Vector3dm(delta);
					vec.normalize();
					vec.scale(len - distanceTraveled);
					distanceTraveled = len;
				}

				pmx = vec.getX();
				pmy = vec.getY();
				pmz = vec.getZ();

				int radius = 1;

				// checkerX = getCollisionBox().translate(pos.x+pmx, pos.y, pos.z);
				Voxel vox;
				checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + pmz);
				// Z part
				for (int i = ((int)(double) pos.getX()) - radius; i <= ((int)(double) pos.getX()) + radius; i++)
					for (int j = ((int)(double) pos.getY() - 1); j <= ((int)(double) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int)(double) pos.getZ()) - radius; k <= ((int)(double) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
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
												vec.setZ(0d);
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
				for (int i = ((int)(double) pos.getX()) - radius; i <= ((int)(double) pos.getX()) + radius; i++)
					for (int j = ((int)(double) pos.getY() - 1); j <= ((int)(double) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int)(double) pos.getZ()) - radius; k <= ((int)(double) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
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
												vec.setX(0d);
												checkerX = getCollisionBoxes()[r].translate(pos.getX() + pmx, pos.getY(), pos.getZ());
											}
										}
									}
							}
						}
				pos.setX(pos.getX() + pmx);
				distanceToTravel.setX(distanceToTravel.getX() - pmx);

				checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + pmy, pos.getZ());
				for (int i = ((int)(double) pos.getX()) - radius; i <= ((int)(double) pos.getX()) + radius; i++)
					for (int j = ((int)(double) pos.getY()) - 1; j <= ((int)(double) pos.getY()) + (int) Math.ceil(checkerY.h) + 1; j++)
						for (int k = ((int)(double) pos.getZ()) - radius; k <= ((int)(double) pos.getZ()) + radius; k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.isVoxelSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
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
												vec.setY(0d);
												checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + pmy, pos.getZ());
											}
										}

									}
							}
						}
				pos.setY(pos.getY() + pmy);
				distanceToTravel.setY(distanceToTravel.getY() - pmy);
			}

			if (Math.abs(distanceToTravel.getX()) > Math.abs(maxDistanceToTravel.getX()))
				maxDistanceToTravel.setX(distanceToTravel.getX());

			if (Math.abs(distanceToTravel.getY()) > Math.abs(maxDistanceToTravel.getY()))
				maxDistanceToTravel.setY(distanceToTravel.getY());

			if (Math.abs(distanceToTravel.getZ()) > Math.abs(maxDistanceToTravel.getZ()))
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
		for (CollisionBox box : this.getTranslatedCollisionBoxes())
		{
			if (box.collidesWith(entity))
				return true;
		}
		return false;
	}

	public Vector3dm collidesWith(Vector3dm lineStart, Vector3dm lineDirection)
	{
		for (CollisionBox box : this.getTranslatedCollisionBoxes())
		{
			Vector3dm collides = box.collidesWith(lineStart, lineDirection);
			if (collides != null)
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
	}

	@Override
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(1.0, 1.0, 1.0) };
	}

	@Override
	public void setupCamera(Camera camera)
	{
		camera.pos = new Vector3dm(positionComponent.getLocation()).negate();

		//Default FOV
		camera.fov = RenderingConfig.fov;

		camera.alUpdate();
	}

	@Override
	public short getEID()
	{
		return eID;
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
		if (existenceComponent.exists())
		{
			//Destroys it
			existenceComponent.destroyEntity();

			//Removes it's reference within the region
			if (this.positionComponent.getRegionWithin() != null)
				this.positionComponent.getRegionWithin().removeEntityFromRegion(this);

			//Actually removes it from the world list
			if (this.world != null)
				this.world.removeEntityFromList(this);

			return true;
		}
		return false;
	}

	@Override
	public boolean shouldBeTrackedBy(Player player)
	{
		//Note 05/09/2016 : Gobrosse read yourself properly you tardfuck
		return exists();
	}

	public boolean exists()
	{
		return existenceComponent.exists();
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
		if (entityUUID != -1 && this.hasSpawned())
			throw new IllegalUUIDChangeException();

		this.entityUUID = uuid;
	}

	@Override
	public IterableIterator<Subscriber> getAllSubscribers()
	{
		return new IterableIterator<Subscriber>() {

			Iterator<Subscriber> i = subscribers.iterator();
			
			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public Subscriber next()
			{
				return i.next();
			}

			@Override
			public Iterator<Subscriber> iterator()
			{
				return this;
			}};
		
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
			this.existenceComponent.pushComponent(subscriber);
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
		return existenceComponent;
	}

	@Override
	public boolean isEntityOnGround()
	{
		return this.canMoveWithCollisionRestrain(new Vector3dm(0.0, -0.01, 0.0)).length() == 0.01;
	}
}
