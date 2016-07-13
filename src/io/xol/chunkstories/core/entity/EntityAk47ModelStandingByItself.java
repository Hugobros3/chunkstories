package io.xol.chunkstories.core.entity;

import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityAk47ModelStandingByItself extends EntityImplementation
{

	int i = 0;

	public EntityAk47ModelStandingByItself(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
	}

	public boolean renderable()
	{
		return true;
	}

	//BVHAnimation anim;

	@Override
	public void render(RenderingContext renderingContext)
	{
		renderingContext.setDiffuseTexture(TexturesHandler.getTextureID("res/models/ak47.hq.png"));
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		renderingContext.getCurrentShader().setUniformFloat3("objectPosition", getLocation());
		int modelBlockData = world.getVoxelData(getLocation());
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, pos.x+0.8+(Math.random()-0.5)*0.2, pos.y+0.5, pos.z- 3.0f));
		Matrix4f mutrix = new Matrix4f();
		mutrix.translate(new Vector3f(0.0f, 1.0f, 0.0f));
		renderingContext.sendTransformationMatrix(mutrix);
		
		ModelLibrary.getMesh("res/models/ak47.hq.obj").render(renderingContext);;
	}
}
