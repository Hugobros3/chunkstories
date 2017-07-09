package io.xol.chunkstories.api.entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.EntityComponentExistence;
import io.xol.chunkstories.api.entity.components.EntityComponentPosition;
import io.xol.chunkstories.api.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.exceptions.IllegalUUIDChangeException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Region;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class EntityBase implements Entity
{
	final protected World world;
	
	//The entity UUID is set to -1 so when added to a World the World assigns it a proper one
	private long entityUUID = -1;
	private boolean hasSpawned = false;
	
	//The eID is just a cache to speed up classname<->serialized id resolution
	//private final short eID;
	private final EntityType entityType;
	
	//Multiplayer players or other agents that chose to be notified when components of the entity are changed
	private final Set<Subscriber> subscribers = new HashSet<Subscriber>();

	//Basic components every entity should have
	final protected EntityComponentExistence existenceComponent;
	final protected EntityComponentPosition positionComponent;
	final protected EntityComponentVelocity velocityComponent;
	
	public EntityBase(EntityType entityType, World world, double x, double y, double z)
	{
		this.world = world;

		//Components have to be initialized in constructor explicitly
		existenceComponent = new EntityComponentExistence(this, null);
		positionComponent = new EntityComponentPosition(this, existenceComponent);
		velocityComponent = new EntityComponentVelocity(this, positionComponent);
		
		positionComponent.setWorld(world);
		positionComponent.setPosition(x, y, z);
		
		//@see: eID field declaration
		this.entityType = entityType;//world.getGameContext().getContent().entities().getEntityIdByClassname(this.getClass().getName());
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
	public void tick()
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
		return "[" + this.getClass().getSimpleName() + ": holderExists: " + (positionComponent.getRegionWithin() != null) + " ,position : " + positionComponent.getLocation() + " UUID : " + entityUUID + " Type: " + this.getType().getName() + " Region:"
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
	public Vector3dm moveWithCollisionRestrain(Vector3dm delta)
	{
		Vector3dm movementLeft = world.collisionsManager().runEntityAgainstWorldVoxels(this, this.getLocation(), delta);
		this.moveWithoutCollisionRestrain(delta.getX() - movementLeft.getX(), delta.getY() - movementLeft.getY(), delta.getZ() - movementLeft.getZ());
		return movementLeft;
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
		return world.collisionsManager().runEntityAgainstWorldVoxels(this, this.getLocation(), delta);
	}

	/**
	 * Does the hitboxes computations to determine if that entity could move that delta
	 * @param from Change the origin of the movement from the default ( current entity position )
	 * @return The remaining distance in each dimension if he got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can move without colliding with anything )
	 */
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm from, Vector3dm delta)
	{
		return world.collisionsManager().runEntityAgainstWorldVoxels(this, from, delta);
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
	public void setupCamera(RenderingInterface renderingInterface)
	{
		renderingInterface.getCamera().setCameraPosition(new Vector3dm(positionComponent.getLocation()));
		
		//Default FOV
		renderingInterface.getCamera().setFOV(renderingInterface.renderingConfig().getFov());
	}

	@Override
	public final EntityType getType()
	{
		return entityType;
	}

	@Override
	public final long getUUID()
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

	public final boolean exists()
	{
		return existenceComponent.exists();
	}

	public final boolean hasSpawned()
	{
		return hasSpawned;
	}

	public final void markHasSpawned()
	{
		hasSpawned = true;
	}

	@Override
	public final void setUUID(long uuid)
	{
		//Don't allow UUID changes once spawned !
		if (entityUUID != -1 && this.hasSpawned())
			throw new IllegalUUIDChangeException();

		this.entityUUID = uuid;
	}

	@Override
	public final IterableIterator<Subscriber> getAllSubscribers()
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
	public final boolean subscribe(Subscriber subscriber)
	{
		//If it didn't already contain the subscriber ...
		if (subscribers.add(subscriber))
		{
			return true;
		}
		return false;
	}

	@Override
	public final boolean unsubscribe(Subscriber subscriber)
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
	public final EntityComponent getComponents()
	{
		return existenceComponent;
	}
	
	public boolean handleInteraction(Entity entity, Input input) {
		return false;
	}
}
