package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.item.inventory.InventoryHolder;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.world.ChunkHolder;
import io.xol.chunkstories.world.World;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Entity extends InventoryHolder
{
	public void setPosition(double x, double y, double z);

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
	
	public World getWorld();

	public void tick();

	public boolean updatePosition();
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz);
	
	public Vector3d moveWithCollisionRestrain(Vector3d vec);
	
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions);

	public DefferedLight[] getLights();
	
	public CollisionBox[] getTranslatedCollisionBoxes();

	public boolean renderable();

	public void render(RenderingContext context);

	public void debugDraw();
	
	public void setupCamera(Camera camera);
	
	public short getEID();

	public long getUUID();
	
	public void delete();
}
