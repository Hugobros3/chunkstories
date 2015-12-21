package io.xol.chunkstories.entity.core;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityHUD;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.voxel.VoxelFormat;
import io.xol.chunkstories.world.World;
import io.xol.engine.base.TexturesHandler;
import io.xol.engine.base.font.TrueTypeFont;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityTest2 extends Entity implements EntityHUD
{

	int i = 0;

	public EntityTest2(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		//anim = new BVHAnimation(new File("res/models/human.bvh"));
	}

	public boolean renderable()
	{
		return true;
	}

	//BVHAnimation anim;

	public void render()
	{
		// if(Math.random() > 0.9)
		i++;
		i %= 80;
		// System.out.println("rendering entity test");
		RenderingContext.setDiffuseTexture(TexturesHandler.idTexture("res/models/rookie.png"));
		RenderingContext.setNormalTexture(TexturesHandler.idTexture("res/textures/normalnormal.png"));
		RenderingContext.renderingShader.setUniformFloat3("borderShift", (float) posX, (float) posY + 0.4f, (float) posZ);
		int modelBlockData = world.getDataAt((int) posX, (int) posY + 1, (int) posZ);
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		RenderingContext.renderingShader.setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, posX+0.8+(Math.random()-0.5)*0.2, posY+0.5, posZ- 3.0f));
		RenderingContext.renderingShader.setUniformMatrix4f("localTransform", new Matrix4f());
		//debugDraw();
		ModelLibrary.loadAndRenderMesh("res/models/rookie.obj");
		//ModelLibrary.loadAndRenderAnimatedMesh("res/models/human.obj", "res/models/human-fixed-standstill.bvh", i);

	}
	
	public DefferedLight[] getLights()
	{
		return new DefferedLight[] {
				new DefferedLight(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ - 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ - 3.5f), 2f),

				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX - 1.0f, (float)posY + 1.1f, (float)posZ + 1.5f), 35f, 30f, new Vector3f(0,0,-1)),
				
				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 2f),
				new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 1.5f), 35f, 30f, new Vector3f(0,0,-1)),
				
				//new DefferedLight(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f((float)posX + 1.0f, (float)posY + 1.1f, (float)posZ + 3.5f), 5f, 30f)
				};
	}

	public void debugDraw()
	{
		// Debug this shit
		// System.out.println("Debug draw");
		
	}

	@Override
	public void drawHUD(Camera camera)
	{
		// TODO Auto-generated method stub
		Vector3f posOnScreen = camera.transform3DCoordinate(new Vector3f((float)posX, (float)posY + 2.5f, (float)posZ));
		
		float scale = posOnScreen.z;
		float dekal = TrueTypeFont.arial12.getWidth("Player")*16*scale;
		if(scale > 0)
			TrueTypeFont.arial12.drawStringWithShadow(posOnScreen.x-dekal/2, posOnScreen.y, "Player", 16*scale, 16*scale, new Vector4f(1,1,1,1));
	}
}
