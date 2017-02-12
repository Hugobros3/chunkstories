package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentExistence;
import io.xol.chunkstories.core.entity.components.EntityComponentPosition;
import io.xol.chunkstories.core.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.Camera;

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

import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class EntityImplementation implements Entity
{
	//The entity UUID is set to -1 so when added to a World the World assigns it a proper one
	private long entityUUID = -1;
	private boolean hasSpawned = false;
	//The eID is just a cache to speed up classname<->serialized id resolution
	private final short eID;
	
	protected World world;

	//Multiplayer players or other agents that chose to be notified when components of the entity are changed
	protected Set<Subscriber> subscribers = new HashSet<Subscriber>();

	//Basic components every entity should have
	final protected EntityComponentExistence existenceComponent;
	protected EntityComponentPosition positionComponent;
	private EntityComponentVelocity velocityComponent;

	//Hacky bullshit
	protected Voxel voxelIn;
	
	public EntityImplementation(World world, double x, double y, double z)
	{
		this.world = world;

		//Components have to be initialized in constructor explicitly
		existenceComponent = new EntityComponentExistence(this, null);
		positionComponent = new EntityComponentPosition(this, existenceComponent);
		velocityComponent = new EntityComponentVelocity(this, positionComponent);
		
		positionComponent.setWorld(world);
		positionComponent.setPositionXYZ(x, y, z);

		//To avoid NPEs
		voxelIn = VoxelsStore.get().getVoxelById(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
		
		//@see: eID field declaration
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
		return moveWithCollisionRestrain(this.getLocation(), vec, false);
	}

	@Override
	public Vector3dm moveWithCollisionRestrain(double mx, double my, double mz)
	{
		return moveWithCollisionRestrain(new Vector3dm(mx, my, mz));
	}

	/**
	 * Does the hitboxes computations to determine if that entity could move that delta
	 * 
	 * @return The remaining distance in each dimension if he got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can move without colliding with anything )
	 */
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm delta)
	{
		return moveWithCollisionRestrain(this.getLocation(), delta, true);
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
		return moveWithCollisionRestrain(from, delta, true);
	}

	// Convenience method, currently uses a dirty step mechanism and snaps to bounding boxes
	private Vector3dm moveWithCollisionRestrain(Vector3dm from, Vector3dm delta, boolean onlyTest)
	{
		int id, data;

		boolean collision = false;
		
		//Extract the current position
		Vector3dm pos = new Vector3dm(from);

		//Keep biggest distanceToTravel for each dimension collisionBox of our entity
		Vector3dm maxDistanceToTravel = new Vector3dm(0.0);

		Vector3dm direction = new Vector3dm(delta);
		direction.normalize();

		//Iterate over every box
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

			double stepDistanceX, stepDistanceY, stepDistanceZ;

			while (distanceTraveled < len)
			{
				if (len - distanceTraveled > 0.25)
				{
					//DistanceTraveled is incremented no matter what, for momentum loss while sliding on walls
					distanceTraveled += 0.25;
				}
				else
				{
					vec = new Vector3dm(delta);
					vec.normalize();
					vec.scale(len - distanceTraveled);
					distanceTraveled = len;
				}

				stepDistanceX = vec.getX();
				stepDistanceY = vec.getY();
				stepDistanceZ = vec.getZ();

				Voxel vox;

				// Z part
				checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + stepDistanceZ);
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerX.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerX.h); j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerX.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);
										if (delta.getZ() != 0.0)
										{
											if (checkerZ.collidesWith(box))
											{
												collision = true;
												if (collision == false)
													break;
												stepDistanceZ = 0;
												if (delta.getZ() < 0)
												{
													double south = Math.min((box.zpos + box.zw + checkerZ.zw) - (pos.getZ()), 0.0d);
													// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
													//System.out.println("south:"+south);
													stepDistanceZ = south;
												}
												else
												{
													double north = Math.max((box.zpos) - (pos.getZ() + checkerZ.zw), 0.0d);
													//System.out.println("north:"+north);
													stepDistanceZ = north;
												}
												vec.setZ(0d);
												checkerZ = getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + stepDistanceZ);
											}
										}
									}
							}
						}
				distanceToTravel.setZ(distanceToTravel.getZ() - stepDistanceZ);
				pos.setZ(pos.getZ() + stepDistanceZ);

				// X-part
				checkerX = getCollisionBoxes()[r].translate(pos.getX() + stepDistanceX, pos.getY(), pos.getZ());
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerY.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerY.h); j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerY.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);

										if (delta.getX() != 0.0)
										{
											if (checkerX.collidesWith(box))
											{
												collision = true;
												if (collision == false)
													break;
												stepDistanceX = 0;
												if (delta.getX() < 0)
												{
													double left = Math.min((box.xpos + box.xw + checkerX.xw) - (pos.getX()), 0.0d);
													//System.out.println("left:"+left);
													stepDistanceX = left;
												}
												else
												{
													double right = Math.max((box.xpos) - (pos.getX() + checkerX.xw), 0.0d);
													//System.out.println("right"+right);
													stepDistanceX = right;
												}
												vec.setX(0d);
												checkerX = getCollisionBoxes()[r].translate(pos.getX() + stepDistanceX, pos.getY(), pos.getZ());
											}
										}
									}
							}
						}
				pos.setX(pos.getX() + stepDistanceX);
				distanceToTravel.setX(distanceToTravel.getX() - stepDistanceX);

				//Y-part
				checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + stepDistanceY, pos.getZ());
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerZ.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerZ.h) + 1; j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerZ.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContext(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);
										if (delta.getY() != 0.0)
										{
											if (checkerY.collidesWith(box))
											{
												collision = true;
												stepDistanceY = 0;
												if (delta.getY() < 0)
												{
													double top = Math.min((box.ypos + box.h) - pos.getY(), 0.0d);
													// System.out.println(top);
													stepDistanceY = top;
												}
												else
												{
													double bot = Math.max((box.ypos) - (pos.getY() + checkerY.h), 0.0d);
													// System.out.println(bot);
													stepDistanceY = bot;
												}
												vec.setY(0d);
												checkerY = getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + stepDistanceY, pos.getZ());
											}
										}

									}
							}
						}
				pos.setY(pos.getY() + stepDistanceY);
				distanceToTravel.setY(distanceToTravel.getY() - stepDistanceY);
			}

			if (Math.abs(distanceToTravel.getX()) > Math.abs(maxDistanceToTravel.getX()))
				maxDistanceToTravel.setX(distanceToTravel.getX());

			if (Math.abs(distanceToTravel.getY()) > Math.abs(maxDistanceToTravel.getY()))
				maxDistanceToTravel.setY(distanceToTravel.getY());

			if (Math.abs(distanceToTravel.getZ()) > Math.abs(maxDistanceToTravel.getZ()))
				maxDistanceToTravel.setZ(distanceToTravel.getZ());
		}
		//Set the new position after computations have been done

		if (!onlyTest)
			this.moveWithoutCollisionRestrain(delta.getX() - maxDistanceToTravel.getX(), delta.getY() - maxDistanceToTravel.getY(), delta.getZ() - maxDistanceToTravel.getZ());
		
		return maxDistanceToTravel;
	}

	private static final Vector3dm onGroundTest_ = new Vector3dm(0.0, -0.01, 0.0);

	@Override
	public boolean isOnGround()
	{
		return canMoveWithCollisionRestrain(onGroundTest_).length() != 0.0d;
	}

	@Override
	public CollisionBox getTranslatedBoundingBox()
	{
		CollisionBox box = getBoundingBox();
		box.translate(getLocation());
		return box;
	}

	@Override
	public CollisionBox getBoundingBox()
	{
		return new CollisionBox(1.0, 1.0, 1.0).translate(-0.5, 0, -0.5);
	}

	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[]{ getBoundingBox() };
	}

	@Override
	public void setupCamera(Camera camera)
	{
		//camera.pos = new Vector3dm(positionComponent.getLocation()).negate();
		camera.setCameraPosition(new Vector3dm(positionComponent.getLocation()));
		
		//Default FOV
		camera.fov = RenderingConfig.fov;

		camera.alUpdate();
	}

	@Override
	public short getEID()
	{
		return eID;
	}

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
}
