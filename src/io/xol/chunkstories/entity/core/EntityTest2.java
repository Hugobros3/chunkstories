package io.xol.chunkstories.entity.core;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.World;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityTest2 extends EntityImplementation
{

	int i = 0;

	public EntityTest2(World w, double x, double y, double z)
	{
		super(w, x, y, z);
	}

	public boolean renderable()
	{
		return true;
	}

	//BVHAnimation anim;

	public void render(RenderingContext renderingContext)
	{
		renderingContext.setDiffuseTexture(TexturesHandler.getTextureID("res/models/ak47.hq.png"));
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		renderingContext.getCurrentShader().setUniformFloat3("borderShift", (float) posX, (float) posY + 1.4f, (float) posZ);
		int modelBlockData = world.getDataAt((int) posX, (int) posY + 1, (int) posZ);
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, posX+0.8+(Math.random()-0.5)*0.2, posY+0.5, posZ- 3.0f));
		Matrix4f mutrix = new Matrix4f();
		mutrix.translate(new Vector3f(0.0f, 1.0f, 0.0f));
		renderingContext.sendTransformationMatrix(mutrix);
		
		ModelLibrary.getMesh("res/models/ak47.hq.obj").render(renderingContext);;
	}
}
