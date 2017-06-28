package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.rendering.text.TextMesh;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;

import io.xol.chunkstories.core.entity.components.EntityComponentSignText;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntitySign extends EntityBase implements EntityVoxel, EntityRenderable
{
	EntityComponentSignText signText = new EntityComponentSignText(this, this.getComponents().getLastComponent());

	String cachedText = null;
	TextMesh renderData = null;

	public EntitySign(World w, double x, double y, double z)
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

		void setupRender(RenderingInterface renderingContext)
		{
			renderingContext.setObjectMatrix(null);

			Texture2D diffuse = renderingContext.textures().getTexture("./models/sign.png");
			diffuse.setLinearFiltering(false);
			renderingContext.bindAlbedoTexture(diffuse);
			renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
			renderingContext.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));
		}

		@Override
		public int renderEntities(RenderingInterface renderingContext, RenderingIterator<EntitySign> renderableEntitiesIterator)
		{
			setupRender(renderingContext);
			
			int e = 0;
			
			renderingContext.setObjectMatrix(null);

			for (EntitySign entitySign : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				if (renderingContext.getCamera().getCameraPosition().distanceTo(entitySign.getLocation()) > 32)
					continue;
				
				e++;
				
				Texture2D diffuse = renderingContext.textures().getTexture("./models/sign.png");
				diffuse.setLinearFiltering(false);
				renderingContext.bindAlbedoTexture(diffuse);
				renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
				renderingContext.currentShader().setUniform3f("objectPosition", new Vector3fm(0));

				int modelBlockData = entitySign.getWorld().getVoxelData(entitySign.getLocation());

				Voxel voxel = VoxelsStore.get().getVoxelById(modelBlockData);
				boolean isPost = voxel.getName().endsWith("_post");

				int lightSky = VoxelFormat.sunlight(modelBlockData);
				int lightBlock = VoxelFormat.blocklight(modelBlockData);
				renderingContext.currentShader().setUniform3f("givenLightmapCoords", lightBlock / 15f, lightSky / 15f, 0f);

				int facing = VoxelFormat.meta(modelBlockData);

				Matrix4f mutrix = new Matrix4f();
				mutrix.translate(new Vector3fm(0.5f, 0.0f, 0.5f));
				mutrix.translate(entitySign.getLocation().castToSinglePrecision());
				mutrix.rotate((float) Math.PI * 2.0f * (-facing) / 16f, new Vector3fm(0, 1, 0));
				if (isPost)
					mutrix.translate(new Vector3fm(0.0f, 0.0f, -0.5f));
				renderingContext.setObjectMatrix(mutrix);

				//glDisable(GL_CULL_FACE);
				if (isPost)
					renderingContext.meshes().getRenderableMeshByName("./models/sign_post.obj").render(renderingContext);
				else
					renderingContext.meshes().getRenderableMeshByName("./models/sign.obj").render(renderingContext);
				//signText.setSignText("The #FF0000cuckiest man on earth #FFFF20 rises again to bring you A E S T H E T I C signs");

				// bake sign mesh
				if (entitySign.cachedText == null || !entitySign.cachedText.equals(entitySign.signText.getSignText()))
				{
					//entitySign.renderData = new TextMeshObject(entitySign.signText.getSignText());
					entitySign.cachedText = entitySign.signText.getSignText();
					entitySign.renderData = renderingContext.getFontRenderer().newTextMeshObject(renderingContext.getFontRenderer().defaultFont(), entitySign.cachedText);
				}
				// Display it
				mutrix.translate(new Vector3fm(0.0f, 1.15f, 0.055f));
				renderingContext.setObjectMatrix(mutrix);
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
	public CollisionBox getBoundingBox()
	{
		return new CollisionBox(1.0, 1.0, 1.0);
	}
}
