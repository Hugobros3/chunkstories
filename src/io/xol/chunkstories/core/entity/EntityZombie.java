package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityZombie extends EntityHumanoid
{
	int i = 0;

	public EntityZombie(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		//anim = new BVHAnimation(new File("res/models/human.bvh"));
	}

	public boolean renderable()
	{
		return true;
	}

	//BVHAnimation anim;

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

		renderingContext.sendTransformationMatrix(null);
		ModelLibrary.getRenderableMesh("./res/models/human.obj").render(
				renderingContext,  this.getAnimatedSkeleton(), (int)System.currentTimeMillis() % 1000000);
		
		//ModelLibrary.getRenderableMesh("./res/models/human.obj").render(
		//		renderingContext, BVHLibrary.getAnimation("res/animations/human/ded.bvh"), (int)System.currentTimeMillis() % 30000);
	}
}
