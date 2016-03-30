package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.item.inventory.InventoryHolder;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity extends InventoryHolder
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
	
	public ChunkHolder getChunkHolder();
	
	public WorldInterface getWorld();

	public void tick();

	public boolean updatePosition();
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3d moveWithCollisionRestrain(Vector3d vec);
	
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions);

	public Light[] getLights();
	
	public CollisionBox[] getTranslatedCollisionBoxes();

	public void render(RenderingContext context);

	public void debugDraw();
	
	public void setupCamera(Camera camera);
	
	public short getEID();

	public long getUUID();
	
	public void delete();

	/**
	 * Returns true unless it should be invisible to some players or all
	 * Exemple : dead/removed entity, invisible admin
	 * @return
	 */
	public boolean shouldBeTrackedBy(Player player);
}
