package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.xol.engine.base.GameWindowOpenGL;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Texture
{
	private WeakReference<Texture> selfReference;

	protected final TextureFormat type;
	protected int glId = -1;

	public Texture(TextureFormat type)
	{
		this.type = type;

		totalTextureObjects++;

		selfReference = new WeakReference<Texture>(this);
		allTextureObjects.add(selfReference);
	}

	public TextureFormat getType()
	{
		return type;
	}

	public final void aquireID()
	{
		//If texture was already assignated we discard this call
		if (glId == -2 || glId >= 0)
			return;

		glId = glGenTextures();
		//Keep the reference for this allocated id
		allocatedIds.put(glId, selfReference);
	}

	public abstract void bind();

	public boolean destroy()
	{
		if (GameWindowOpenGL.isMainGLWindow())
		{
			allTextureObjects.remove(selfReference);

			if (glId >= 0)
			{
				glDeleteTextures(glId);
				allocatedIds.remove(glId);

				totalTextureObjects--;
				return true;
			}

			//Set id to -2 to prevent texture being used again
			glId = -2;
			return false;
		}
		else
		{
			synchronized (objectsToDestroy)
			{
				objectsToDestroy.add(this);
			}
			return false;
		}
	}

	public abstract long getVramUsage();

	protected static BlockingQueue<Texture> objectsToDestroy = new LinkedBlockingQueue<Texture>();

	public static int destroyPendingTextureObjects()
	{
		int destroyedVerticesObjects = 0;

		synchronized (objectsToDestroy)
		{
			Iterator<Texture> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				Texture object = i.next();

				if (object.destroy())
					destroyedVerticesObjects++;

				i.remove();
			}
		}

		Iterator<Entry<Integer, WeakReference<Texture>>> i = allocatedIds.entrySet().iterator();
		while(i.hasNext())
		{
			Entry<Integer, WeakReference<Texture>> entry = i.next();
			int id = entry.getKey();
			WeakReference<Texture> weakReference = entry.getValue();
			Texture texture = weakReference.get();
			if(texture == null)
			{
				System.out.println("Destroyed orphan texture id #"+id);
				glDeleteTextures(id);
				destroyedVerticesObjects++;
				
				i.remove();
			}
		}
		
		return destroyedVerticesObjects;
	}

	public static int getTotalNumberOfTextureObjects()
	{
		return totalTextureObjects;
	}

	public static long getTotalVramUsage()
	{
		long vram = 0;

		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<Texture>> i = allTextureObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<Texture> reference = i.next();

			Texture object = reference.get();
			if (object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}

		return vram;
	}

	protected static int totalTextureObjects = 0;
	protected static BlockingQueue<WeakReference<Texture>> allTextureObjects = new LinkedBlockingQueue<WeakReference<Texture>>();
	
	protected static Map<Integer, WeakReference<Texture> > allocatedIds = new ConcurrentHashMap<Integer, WeakReference<Texture>>();
}
