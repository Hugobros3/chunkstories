package io.xol.chunkstories.core.entity.voxel;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;

import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.components.EntityComponentSignText;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.TextMeshObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntitySign extends EntityImplementation implements EntityVoxel
{
	EntityComponentSignText signText = new EntityComponentSignText(this, this.getComponents().getLastComponent());
	
	String cachedText = null;
	TextMeshObject renderData = null;
	
	public EntitySign(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
	}
	
	public void setText(String text)
	{
		signText.setSignText(text);
	}

	@Override
	public void render(RenderingContext renderingContext)
	{
		//System.out.println();
		if(renderingContext.getCamera().getCameraPosition().sub(this.getLocation()).length() > 32)
			return;
		
		renderingContext.sendBoneTransformationMatrix(null);
		
		Texture2D diffuse = TexturesHandler.getTexture("res/models/sign.png");
		diffuse.setLinearFiltering(false);
		renderingContext.setDiffuseTexture(diffuse.getId());
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		renderingContext.getCurrentShader().setUniformFloat3("objectPosition", getLocation());
		
		int modelBlockData = world.getVoxelData(getLocation());
		
		Voxel voxel = VoxelTypes.get(modelBlockData);
		boolean isPost = voxel.getName().endsWith("_post");
		
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, pos.x+0.8+(Math.random()-0.5)*0.2, pos.y+0.5, pos.z- 3.0f));
	
		int facing = VoxelFormat.meta(modelBlockData);
		
		Matrix4f mutrix = new Matrix4f();
		mutrix.translate(new Vector3f(0.5f, 0.0f, 0.5f));
		mutrix.rotate((float)Math.PI * 2.0f * (-facing) / 16f, new Vector3f(0, 1, 0));
		if(isPost)
			mutrix.translate(new Vector3f(0.0f, 0.0f, -0.5f));
		renderingContext.sendTransformationMatrix(mutrix);


		renderingContext.enableVertexAttribute("colorIn");
		renderingContext.enableVertexAttribute("normalIn");
		glDisable(GL_CULL_FACE);
		if(isPost)
			ModelLibrary.getMesh("res/models/sign_post.obj").render(renderingContext);
		else
			ModelLibrary.getMesh("res/models/sign.obj").render(renderingContext);
		//signText.setSignText("The #FF0000cuckiest man on earth #FFFF20 rises again to bring you A E S T H E T I C signs");
		
		// bake sign mesh
		if(cachedText == null || !cachedText.equals(signText.getSignText()))
		{
			renderData = new TextMeshObject(signText.getSignText());
			cachedText = signText.getSignText();
		}
		// Display it
		mutrix.translate(new Vector3f(0.0f, 1.15f, 0.055f));
		renderingContext.sendTransformationMatrix(mutrix);
		renderData.render(renderingContext);
	}
}
