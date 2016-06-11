package io.xol.chunkstories.api.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.item.inventory.CSFSerializable;
import io.xol.chunkstories.item.inventory.InventoryHolder;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity extends InventoryHolder, CSFSerializable
{
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
	public ChunkHolder getChunkHolder();
	
	/**
	 * Return the entity's world
	 * @return
	 */
	public WorldInterface getWorld();

	/**
	 * Updates the entity, ran at 60Hz by default
	 */
	public void tick();
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3d moveWithCollisionRestrain(Vector3d vec);
	
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions);
	
	public boolean isFlying();

	public void setFlying(boolean flying);
	
	/**
	 * Returns the entitie's AABBs
	 * @return
	 */
	public CollisionBox[] getTranslatedCollisionBoxes();

	/**
	 * Renders the entity using the context
	 * @param context
	 */
	public void render(RenderingContext context);

	public void debugDraw();
	
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
	 * Remove the entity from it's world and mark it for deletion (since Java requires to manually remove all references)
	 */
	public void delete();

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
}
