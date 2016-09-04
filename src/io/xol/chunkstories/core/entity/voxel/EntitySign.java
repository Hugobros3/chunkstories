package io.xol.chunkstories.core.entity.voxel;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;

import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.components.EntityComponentSignText;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.Voxels;
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

public class EntitySign extends EntityImplementation implements EntityVoxel, EntityRenderable
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
	
	public String getText()
	{
		return signText.getSignText();
	}

	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntitySignRenderer();
	}

	static class EntitySignRenderer implements EntityRenderer<EntitySign>
	{

		@Override
		public void setupRender(RenderingContext renderingContext)
		{
			renderingContext.sendBoneTransformationMatrix(null);

			Texture2D diffuse = TexturesHandler.getTexture("./models/sign.png");
			diffuse.setLinearFiltering(false);
			renderingContext.setDiffuseTexture(diffuse.getId());
			renderingContext.setNormalTexture(TexturesHandler.getTextureID("./textures/normalnormal.png"));
		}

		@Override
		public int forEach(RenderingContext renderingContext, RenderingIterator<EntitySign> renderableEntitiesIterator)
		{
			int e = 0;
			
			renderingContext.sendBoneTransformationMatrix(null);

			for (EntitySign entitySign : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				if (renderingContext.getCamera().getCameraPosition().add(entitySign.getLocation()).length() > 32)
					continue;
				
				e++;
				
				Texture2D diffuse = TexturesHandler.getTexture("./models/sign.png");
				diffuse.setLinearFiltering(false);
				renderingContext.setDiffuseTexture(diffuse.getId());
				renderingContext.setNormalTexture(TexturesHandler.getTextureID("./textures/normalnormal.png"));
				renderingContext.getCurrentShader().setUniformFloat3("objectPosition", entitySign.getLocation());

				int modelBlockData = entitySign.getWorld().getVoxelData(entitySign.getLocation());

				Voxel voxel = Voxels.get(modelBlockData);
				boolean isPost = voxel.getName().endsWith("_post");

				int lightSky = VoxelFormat.sunlight(modelBlockData);
				int lightBlock = VoxelFormat.blocklight(modelBlockData);
				renderingContext.getCurrentShader().setUniformFloat3("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);

				int facing = VoxelFormat.meta(modelBlockData);

				Matrix4f mutrix = new Matrix4f();
				mutrix.translate(new Vector3f(0.5f, 0.0f, 0.5f));
				mutrix.rotate((float) Math.PI * 2.0f * (-facing) / 16f, new Vector3f(0, 1, 0));
				if (isPost)
					mutrix.translate(new Vector3f(0.0f, 0.0f, -0.5f));
				renderingContext.sendTransformationMatrix(mutrix);

				renderingContext.enableVertexAttribute("colorIn");
				renderingContext.enableVertexAttribute("normalIn");
				glDisable(GL_CULL_FACE);
				if (isPost)
					ModelLibrary.getRenderableMesh("./models/sign_post.obj").render(renderingContext);
				else
					ModelLibrary.getRenderableMesh("./models/sign.obj").render(renderingContext);
				//signText.setSignText("The #FF0000cuckiest man on earth #FFFF20 rises again to bring you A E S T H E T I C signs");

				// bake sign mesh
				if (entitySign.cachedText == null || !entitySign.cachedText.equals(entitySign.signText.getSignText()))
				{
					entitySign.renderData = new TextMeshObject(entitySign.signText.getSignText());
					entitySign.cachedText = entitySign.signText.getSignText();
				}
				// Display it
				mutrix.translate(new Vector3f(0.0f, 1.15f, 0.055f));
				renderingContext.sendTransformationMatrix(mutrix);
				entitySign.renderData.render(renderingContext);
			}
			
			return e;
		}

		@Override
		public void freeRessources()
		{

		}

	}

	@Override
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(1.0, 1.0, 1.0).translate(0.5, 0.0, 0.5) };
	}
}
