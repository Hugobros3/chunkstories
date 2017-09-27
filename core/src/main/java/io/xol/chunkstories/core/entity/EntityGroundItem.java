package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityGroundItem extends EntityBase implements EntityRenderable
{
	private long spawnTime;
	private final EntityGroundItemPileComponent itemPileWithin;
	
	public EntityGroundItem(EntityType t, World world, double x, double y, double z)
	{
		super(t, world, x, y, z);
		itemPileWithin = new EntityGroundItemPileComponent(this);
	}
	
	public EntityGroundItem(EntityType t, World world, double x, double y, double z, ItemPile itemPile)
	{
		super(t, world, x, y, z);
		itemPileWithin = new EntityGroundItemPileComponent(this, itemPile);
		spawnTime = System.currentTimeMillis();
	}

	public ItemPile getItemPile()
	{
		return itemPileWithin.itemPile;
	}
	
	public void setItemPile(ItemPile itemPile)
	{
		itemPileWithin.setItemPile(itemPile);
		spawnTime = System.currentTimeMillis();
	}
	
	public boolean canBePickedUpYet()
	{
		return System.currentTimeMillis() - spawnTime > 2000L;
	}
	
	@Override
	public void tick()
	{
		this.moveWithCollisionRestrain(0, -0.05, 0);
		super.tick();
	}
	
	
	static EntityRenderer<EntityGroundItem> entityRenderer = new EntityGroundItemRenderer();
	
	static class EntityGroundItemRenderer implements EntityRenderer<EntityGroundItem> {
		
		@Override
		public int renderEntities(RenderingInterface renderingInterface, RenderingIterator<EntityGroundItem> renderableEntitiesIterator)
		{
			int i = 0;
			
			while(renderableEntitiesIterator.hasNext())
			{
				EntityGroundItem e = renderableEntitiesIterator.next();
				
				ItemPile within = e.itemPileWithin.getItemPile();
				if(within != null)
				{
					VoxelContext context = e.getWorld().peek(e.getLocation());
					int modelBlockData = context.getData();

					int lightSky = VoxelFormat.sunlight(modelBlockData);
					int lightBlock = VoxelFormat.blocklight(modelBlockData);
					renderingInterface.currentShader().setUniform2f("worldLightIn", lightBlock, lightSky );
					
					Matrix4f matrix = new Matrix4f();
					
					Vector3d loc = e.getLocation().add(0.0, 0.25, 0.0);
					matrix.translate((float)loc.x, (float)loc.y, (float)loc.z);
					matrix.rotate((float)Math.PI/2, new Vector3f(1,0 ,0));
					within.getItem().getType().getRenderer().renderItemInWorld(renderingInterface, within, e.getWorld(), e.getLocation(), matrix);
					//renderingInterface.flush();
				}
				else
				{
					System.out.println("EntityGroundItem: Not within any inventory ???");
				}
				
				i++;
			}
			
			return i;
		}

		@Override
		public void freeRessources()
		{
			//Not much either
		}
		
	}
	
	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return entityRenderer;
	}
}
