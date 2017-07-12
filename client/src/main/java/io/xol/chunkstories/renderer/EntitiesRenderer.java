package io.xol.chunkstories.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer.RenderingPass;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntitiesRenderer
{
	Map<Class<? extends EntityRenderable>, EntityRenderer<? extends EntityRenderable>> entityRenderers = new HashMap<Class<? extends EntityRenderable>, EntityRenderer<? extends EntityRenderable>>();

	World world;

	public EntitiesRenderer(World world)
	{
		this.world = world;
	}

	public void clearLoadedEntitiesRenderers()
	{
		for (EntityRenderer<? extends EntityRenderable> entityRenderer : entityRenderers.values())
			if (entityRenderer != null)
				entityRenderer.freeRessources();

		entityRenderers.clear();
	}

	public int renderEntities(RenderingInterface renderingContext)
	{
		((WorldImplementation) world).entitiesLock.readLock().lock();

		//Sort them by type
		Map<Class<? extends EntityRenderable>, List<EntityRenderable>> renderableEntitiesTypes = new HashMap<Class<? extends EntityRenderable>, List<EntityRenderable>>();
		for (Entity entity : world.getAllLoadedEntities())
		{
			if (entity instanceof EntityRenderable)
			{
				EntityRenderable entityRenderable = (EntityRenderable) entity;
				List<EntityRenderable> entitiesOfThisType = renderableEntitiesTypes.get(entityRenderable.getClass());
				if (entitiesOfThisType == null)
				{
					renderableEntitiesTypes.put(entityRenderable.getClass(), new ArrayList<EntityRenderable>());
					entitiesOfThisType = renderableEntitiesTypes.get(entityRenderable.getClass());
				}
				entitiesOfThisType.add(entityRenderable);
			}
		}

		int entitiesRendered = 0;

		for (Entry<Class<? extends EntityRenderable>, List<EntityRenderable>> entry : renderableEntitiesTypes.entrySet())
		{
			List<EntityRenderable> entities = entry.getValue();

			//Caches entity renderers until we f12
			if (!entityRenderers.containsKey(entry.getKey()))
				entityRenderers.put(entry.getKey(), entities.get(0).getEntityRenderer());

			EntityRenderer<? extends EntityRenderable> entityRenderer = entityRenderers.get(entry.getKey());
			//EntityRenderer<? extends EntityRenderable> entityRenderer = entities.get(0).getEntityRenderer();

			if (entityRenderer == null)
				continue;

			//entityRenderer.setupRender(renderingContext);

			try
			{
				int e = entityRenderer.renderEntities(renderingContext, new EntitiesRendererIterator<>(renderingContext, entities));
				
				entitiesRendered+=e;
				
				//renderingContext.flush();
			}
			catch (Throwable e)
			{
				System.out.println("Exception rendering entities "+entities.get(0).getClass().getSimpleName()+" using "+entityRenderer.getClass().getSimpleName());
				e.printStackTrace();
			}
		}

		((WorldImplementation) world).entitiesLock.readLock().unlock();

		return entitiesRendered;
	}

	@SuppressWarnings("unchecked")
	private class EntitiesRendererIterator<E extends EntityRenderable> implements RenderingIterator<E>
	{
		private RenderingInterface renderingContext;

		private List<EntityRenderable> entities;
		protected Iterator<EntityRenderable> iterator;
		protected EntityRenderable currentEntity;

		public EntitiesRendererIterator(RenderingInterface renderingContext, List<EntityRenderable> entities)
		{
			this.renderingContext = renderingContext;
			this.entities = entities;
			this.iterator = entities.iterator();
		}

		@Override
		public boolean hasNext()
		{
			if (currentEntity != null)
				return true;
			else if (iterator.hasNext())
			{
				if (currentEntity == null)
					currentEntity = iterator.next();
				return true;
			}
			else
				return false;
		}

		@Override
		public E next()
		{
			E cache = (E) currentEntity;

			//Here fancy rendering tech
			if (isCurrentElementInViewFrustrum())
			{
				//TODO instancing friendly way of providing those
				
				//renderingContext.currentShader().setUniform3f("objectPosition", currentEntity.getLocation());
				renderingContext.currentShader().setUniform2f("worldLightIn", world.getBlocklightLevelLocation(currentEntity.getLocation()), world.getSunlightLevelLocation(currentEntity.getLocation()));
			}

			currentEntity = null;
			return cache;
		}

		@Override
		public boolean isCurrentElementInViewFrustrum()
		{
			if (currentEntity == null)
				return false;

			CollisionBox box = currentEntity.getTranslatedBoundingBox();

			if (renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW || renderingContext.getCamera().isBoxInFrustrum(box))//new Vector3f(box.xpos - box.xw, box.ypos - box.h, box.zpos - box.zw), new Vector3f(box.xw, box.h, box.zw)))
			{
				return true;
			}
			
			return false;
		}

		@Override
		public RenderingIterator<E> getElementsInFrustrumOnly()
		{
			return new FrustrumCulledRenderingIterator<E>(renderingContext, entities);
		}

		public EntitiesRendererIterator<E> clone()
		{
			return new EntitiesRendererIterator<E>(renderingContext, entities);
		}

		private class FrustrumCulledRenderingIterator<S extends E> extends EntitiesRendererIterator<S>
		{
			EntityRenderable currentRenderableEntity = null;

			public FrustrumCulledRenderingIterator(RenderingInterface renderingContext, List<EntityRenderable> entities)
			{
				super(renderingContext, entities);
			}

			@Override
			public RenderingIterator<S> getElementsInFrustrumOnly()
			{
				return this;
			}

			@Override
			public boolean hasNext()
			{
				//If a cull-checked entity is already present just return null
				if (currentRenderableEntity != null)
					return true;

				//Else loop until we find one
				while (iterator.hasNext())
				{
					currentEntity = iterator.next();
					if (isCurrentElementInViewFrustrum())
					{
						currentRenderableEntity = currentEntity;
						return true;
					}
				}

				//We failed.
				return false;
			}

			@Override
			public S next()
			{
				currentEntity = currentRenderableEntity;
				S s = super.next();

				//Null-out reference for future call
				currentRenderableEntity = null;
				return s;
			}

			public FrustrumCulledRenderingIterator<S> clone()
			{
				return new FrustrumCulledRenderingIterator<S>(renderingContext, entities);
			}
		}

	}
}
