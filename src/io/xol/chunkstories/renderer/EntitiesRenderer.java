package io.xol.chunkstories.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
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
			if(entityRenderer != null)
				entityRenderer.freeRessources();

		entityRenderers.clear();
	}

	public int renderEntities(RenderingContext renderingContext)
	{
		//Lock l = ((WorldImplementation)world).entitiesLock.readLock();
		//l.lock();
		
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

		@SuppressWarnings("unused")
		int entitiesRendered = 0;
		
		for (Entry<Class<? extends EntityRenderable>, List<EntityRenderable>> entry : renderableEntitiesTypes.entrySet())
		{
			List<EntityRenderable> entities = entry.getValue();
			
			//Caches entity renderers until we f12
			if(!entityRenderers.containsKey(entry.getKey()))
				entityRenderers.put(entry.getKey(), entities.get(0).getEntityRenderer());
			
			EntityRenderer<? extends EntityRenderable> entityRenderer = entityRenderers.get(entry.getKey());
			//EntityRenderer<? extends EntityRenderable> entityRenderer = entities.get(0).getEntityRenderer();

			if(entityRenderer == null)
				continue;
			
			entityRenderer.setupRender(renderingContext);

			entitiesRendered += entityRenderer.forEach(renderingContext, new EntitiesRendererIterator<>(renderingContext, entities));
		}
		
		//l.unlock();
		
		return entitiesRendered;
	}

	@SuppressWarnings("unchecked")
	private class EntitiesRendererIterator<E extends EntityRenderable> implements RenderingIterator<E>
	{
		private RenderingContext renderingContext;
		
		private List<EntityRenderable> entities;
		protected Iterator<EntityRenderable> iterator;
		protected EntityRenderable currentEntity;

		public EntitiesRendererIterator(RenderingContext renderingContext, List<EntityRenderable> entities)
		{
			this.renderingContext = renderingContext;
			this.entities = entities;
			this.iterator = entities.iterator();
		}

		@Override
		public boolean hasNext()
		{
			if(currentEntity != null)
				return true;
			else if (iterator.hasNext())
			{
				if(currentEntity == null)
					currentEntity = iterator.next();
				return true;
			}
			else
				return false;
		}

		@Override
		public E next()
		{
			E cache = (E)currentEntity;
			
			//Here fancy rendering tech
			if(isCurrentElementInViewFrustrum())
			{
				//TODO instancing friendly
				//renderingContext.currentShader().setUniform3f("objectPosition", currentEntity.getLocation());
				renderingContext.currentShader().setUniform2f("worldLight", world.getBlocklightLevelLocation(currentEntity.getLocation()), world.getSunlightLevelLocation(currentEntity.getLocation()));
			
				//Reset animations transformations
				renderingContext.currentShader().setUniformMatrix4f("localTansform", new Matrix4f());
				renderingContext.currentShader().setUniformMatrix3f("localTransformNormal", new Matrix3f());
			}

			currentEntity = null;
			return cache;
		}

		@Override
		public boolean isCurrentElementInViewFrustrum()
		{
			if(currentEntity == null)
				return false;
			
			for (CollisionBox box : currentEntity.getTranslatedCollisionBoxes())
			{
				if (renderingContext.isThisAShadowPass() || renderingContext.getCamera().isBoxInFrustrum(new Vector3f(box.xpos, box.ypos + box.h / 2, box.zpos), new Vector3f(box.xw, box.h, box.zw)))
				{
					return true;
				}
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
			
			public FrustrumCulledRenderingIterator(RenderingContext renderingContext, List<EntityRenderable> entities)
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
				if(currentRenderableEntity != null)
					return true;
				
				//Else loop until we find one
				while(iterator.hasNext())
				{
					currentEntity = iterator.next();
					if(isCurrentElementInViewFrustrum())
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
