package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.EntityComponentExistence;
import io.xol.chunkstories.api.entity.components.EntityComponentPosition;
import io.xol.chunkstories.api.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Region;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity
{
	/**
	 * Return the object representing the declaration of this entity in a .entities file
	 */
	public EntityType getType();
	
	public EntityComponentExistence getComponentExistence();
	
	public EntityComponentPosition getEntityComponentPosition();
	
	/** @return the location of the entity */
	public Location getLocation();
	
	/** Sets the location of the entity */
	public void setLocation(Location loc);
	
	/** @return the entity's current chunk holder */
	public Region getRegion();
	
	/** @return the entity's world */
	public World getWorld();

	/** Updates the entity, ran at 60Hz by default */
	public void tick();

	/** Called when trying to interact with an entity. Returning 'true' will stop the interaction chain and won't allow anything further to interact with it. */
	public boolean handleInteraction(Entity entity, Input input);

	//TODO refactor these properly
	public void moveWithoutCollisionRestrain(Vector3dm delta);
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3dm moveWithCollisionRestrain(Vector3dm vec);
	
	public Vector3dm moveWithCollisionRestrain(double mx, double my, double mz);
	
	public Vector3dm canMoveWithCollisionRestrain(Vector3dm delta);
	
	public boolean isOnGround();
	
	//TODO Does all entities need that for real ?
	public EntityComponentVelocity getVelocityComponent();
	
	/** @return the entitie's AABBs to their position */
	public CollisionBox getTranslatedBoundingBox();
	
	/** @return the entitie's AABBs */
	public CollisionBox getBoundingBox();
	
	/**
	 * Called when controlling/viewing an entity
	 * @param renderer
	 */
	public void setupCamera(RenderingInterface renderer);

	/** @return the UUID of this entity. */
	public long getUUID();
	
	/** Sets the UUID of the entity. Reserved for internals, trying to set/change the UUID after it's been results in an exception. */
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

	/** @return false once the entity has been removed from the world */
	public boolean exists();

	/**
	 * Returns true once the entity has been added into the world
	 * @return
	 */
	public boolean hasSpawned();
	
	public void markHasSpawned();
		
	public IterableIterator<Subscriber> getAllSubscribers();
	
	public boolean subscribe(Subscriber subscriber);
	public boolean unsubscribe(Subscriber subscriber);
	
	public EntityComponent getComponents();

	public CollisionBox[] getCollisionBoxes();
	
}
