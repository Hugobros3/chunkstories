package io.xol.chunkstories.api.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.core.entity.components.EntityComponentPosition;
import io.xol.chunkstories.core.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.item.inventory.CSFSerializable;
import io.xol.chunkstories.physics.Collidable;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity extends Collidable, CSFSerializable
{
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
	public void tick();

	public void moveWithoutCollisionRestrain(Vector3d delta);
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3d moveWithCollisionRestrain(Vector3d vec);
	
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions);
	
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
	public boolean removeFromWorld();

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
	
	/**
	 * Loads the object state from the stream, implying the ID has already been read in the stream.
	 * If you're initializing an entity from a stream, first create the proper entity type
	 * @param stream
	 * @throws IOException
	 */
	public void loadCSF(DataInputStream stream) throws IOException;

	/**
	 * Writes the entity's description, including ID.
	 * @param stream
	 * @throws IOException
	 */
	public void saveCSF(DataOutputStream stream) throws IOException;
	
	public Iterator<Subscriber> getAllSubscribers();
	
	public boolean subscribe(Subscriber subscriber);
	public boolean unsubscribe(Subscriber subscriber);
	
	public EntityComponent getComponents();
	
}
