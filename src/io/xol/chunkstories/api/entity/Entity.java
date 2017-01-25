package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.core.entity.components.EntityComponentExistence;
import io.xol.chunkstories.core.entity.components.EntityComponentPosition;
import io.xol.chunkstories.core.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.physics.Collidable;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity extends Collidable
{
	public EntityComponentExistence getComponentExistence();
	
	public EntityComponentPosition getEntityComponentPosition();
	
	/**
	 * Returns the location of the entity
	 * @return
	 */
	public Location getLocation();
	
	/**
	 * Sets the location of the entity
	 * @param loc
	 */
	public void setLocation(Location loc);
	
	/**
	 * Return the entity's current chunk holder
	 * @return
	 */
	public Region getRegion();
	
	/**
	 * Return the entity's world
	 * @return
	 */
	public World getWorld();

	/**
	 * Updates the entity, ran at 60Hz by default
	 */
	public void tick(WorldAuthority authorityType);

	//TODO refactor these properly
	public void moveWithoutCollisionRestrain(Vector3dm delta);
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3dm moveWithCollisionRestrain(Vector3dm vec);
	
	public Vector3dm moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions);
	
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm delta);
	
	public boolean isOnGround();
	
	public EntityComponentVelocity getVelocityComponent();
	
	/**
	 * Returns the entitie's AABBs to their position
	 * @return
	 */
	public CollisionBox[] getTranslatedCollisionBoxes();
	
	/**
	 * Returns the entitie's AABBs
	 * @return
	 */
	public CollisionBox[] getCollisionBoxes();
	
	/**
	 * Called when controlling/viewing an entity
	 * @param camera
	 */
	public void setupCamera(Camera camera);
	
	/**
	 * Get the EntityID of this entity
	 * ie : the number in .entities files
	 * @return
	 */
	public short getEID();

	/**
	 * Get the UUID of this entity.
	 * @return
	 */
	public long getUUID();
	
	/**
	 * Sets the UUID of the entity. Reserved for internals, trying to set/change the UUID after it's been results in an exception.
	 * @return
	 */
	public void setUUID(long uuid);
	
	/**
	 * Remove the entity from it's world and mark it for deletion (since Java requires to manually remove all references)
	 * @return false if already removed
	 */
	//public boolean removeFromWorld();

	/**
	 * Returns true unless it should be invisible to some players or all
	 * Exemple : dead/removed entity, invisible admin
	 * @return
	 */
	public boolean shouldBeTrackedBy(Player player);

	/**
	 * Returns false once the entity has been removed from the world
	 * @return
	 */
	public boolean exists();

	/**
	 * Returns true once the entity has been added into the world
	 * @return
	 */
	public boolean hasSpawned();
	
	public void markHasSpawned();
	
	public boolean isEntityOnGround();
		
	public IterableIterator<Subscriber> getAllSubscribers();
	
	public boolean subscribe(Subscriber subscriber);
	public boolean unsubscribe(Subscriber subscriber);
	
	public EntityComponent getComponents();
	
}
