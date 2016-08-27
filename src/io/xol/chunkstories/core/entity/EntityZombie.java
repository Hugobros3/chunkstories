package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.ai.AI;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.ai.GenericLivingAI;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.ModelLibrary;

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

	@Override
	public void render(RenderingContext renderingContext)
	{
		i++;
		i %= 80;
		
		Texture2D playerTexture = TexturesHandler.getTexture("models/zombie_s3.png");
		playerTexture.setLinearFiltering(false);
		
		renderingContext.setDiffuseTexture(playerTexture.getId());
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("textures/normalnormal.png"));
		renderingContext.getCurrentShader().setUniformFloat3("objectPosition", getLocation().castToSP());
		int modelBlockData = world.getVoxelData(getLocation());
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);

		Matrix4f matrix = new Matrix4f();
		matrix.rotate((90 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
		
		renderingContext.sendTransformationMatrix(matrix);
		ModelLibrary.getRenderableMesh("./res/models/human.obj").render(renderingContext,  this.getAnimatedSkeleton(), (int)(System.currentTimeMillis() % 1000000));
	}
}
