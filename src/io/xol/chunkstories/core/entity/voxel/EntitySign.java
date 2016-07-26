package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.components.EntityComponentSignText;
import io.xol.chunkstories.entity.EntityImplementation;
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

	@Override
	public void render(RenderingContext renderingContext)
	{
		
		Texture2D diffuse = TexturesHandler.getTexture("res/models/sign.png");
		diffuse.setLinearFiltering(false);
		renderingContext.setDiffuseTexture(diffuse.getId());
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		renderingContext.getCurrentShader().setUniformFloat3("objectPosition", getLocation());
		
		int modelBlockData = world.getVoxelData(getLocation());
		int lightSky = VoxelFormat.sunlight(modelBlockData);
		int lightBlock = VoxelFormat.blocklight(modelBlockData);
		renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);
		//world.particlesHolder.addParticle(new ParticleSmoke(world, pos.x+0.8+(Math.random()-0.5)*0.2, pos.y+0.5, pos.z- 3.0f));
	
		int facing = VoxelFormat.meta(world.getVoxelData(getLocation()));
		
		Matrix4f mutrix = new Matrix4f();
		mutrix.translate(new Vector3f(0.5f, 0.0f, 0.5f));
		mutrix.rotate((float)Math.PI * 2.0f * (facing + 8) / 16f, new Vector3f(0, 1, 0));
		renderingContext.sendTransformationMatrix(mutrix);
		
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
