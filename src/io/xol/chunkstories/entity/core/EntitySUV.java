package io.xol.chunkstories.entity.core;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Matrix4f;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.world.World;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntitySUV extends EntityImplementation
{
	int i = 0;

	public EntitySUV(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		//anim = new BVHAnimation(new File("res/models/human.bvh"));
	}

	public boolean renderable()
	{
		return true;
	}

	//BVHAnimation anim;

	public void render(RenderingContext renderingContext)
	{
		// if(Math.random() > 0.9)
		i++;
		i %= 80;
		// System.out.println("rendering entity test");
		renderingContext.setDiffuseTexture(TexturesHandler.getTextureID("res/models/rookie.png"));
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		renderingContext.renderingShader.setUniformFloat3("borderShift", (float) posX, (float) posY + 0.4f, (float) posZ);
		int modelBlockData = world.getDataAt((int) posX, (int) posY + 1, (int) posZ);
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.renderingShader.setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, posX+0.8+(Math.random()-0.5)*0.2, posY+0.5, posZ- 3.0f));
		Matrix4f mutrix = new Matrix4f();
		mutrix.translate(new Vector3f(0.0f, 1.0f, 0.0f));
		renderingContext.renderingShader.setUniformMatrix4f("localTransform", mutrix);
		//debugDraw();
		ModelLibrary.getMesh("res/models/rookie.obj").render(renderingContext);
		//ModelLibrary.loadAndRenderAnimatedMesh("res/models/human.obj", "res/models/human-fixed-standstill.bvh", i);

	}
	
	public DefferedLight[] getLights()
	{
		return new DefferedLight[] {
				
				new DefferedLight(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ - 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ - 3.5f), 2f),

				new DefferedLight(new Vector3f(2.0f, 2.0f, 2.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ + 1.5f), 35f, 30f, new Vector3f(0,0,-1)),
				
				new DefferedLight(new Vector3f(2.0f, 2.0f, 2.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 1.5f), 35f, 30f, new Vector3f(0,0,-1)),
				
				//new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 5f, 30f)
				};
	}
}
