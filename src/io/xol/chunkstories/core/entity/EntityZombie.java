package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.ai.AI;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.core.entity.ai.GenericLivingAI;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityZombie extends EntityHumanoid
{
	int i = 0;
	AI zombieAi;

	public EntityZombie(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		zombieAi = new GenericLivingAI(this);
	}

	public boolean renderable()
	{
		return true;
	}
	
	public void tick()
	{
		zombieAi.tick();
		super.tick();
		
		//System.out.println(this.getVelocityComponent().getVelocity());

		if(Double.isNaN(this.getEntityRotationComponent().getHorizontalRotation()))
		{
			System.out.println("nan !" + this);
			this.getEntityRotationComponent().setRotation(0.0, 0.0);
			//this.setLocation(new Location(entity.getWorld(), entity.getLocation().clone().add(new Vector3d(Math.random() * 0.5, 0.0, Math.random() * 0.5))));
		}
	}

	class EntityZombieRenderer<H extends EntityHumanoid> extends EntityHumanoidRenderer<H> {
		
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
			
			//Player textures
			Texture2D playerTexture = TexturesHandler.getTexture("./models/zombie_s3.png");
			playerTexture.setLinearFiltering(false);
			
			renderingContext.bindAlbedoTexture(playerTexture);
		}
	}
	
	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityZombieRenderer<EntityZombie>();
	}
}
