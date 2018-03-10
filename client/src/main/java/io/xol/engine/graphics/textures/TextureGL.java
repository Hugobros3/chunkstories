//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.textures;

import static org.lwjgl.opengl.GL11.*;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;

public abstract class TextureGL implements Texture
{
	private WeakReference<TextureGL> selfReference;

	protected final TextureFormat type;
	protected int glId = -1;

	private static final Logger logger = LoggerFactory.getLogger("rendering.textures");
	public static Logger logger() {
		return logger;
	}
	
	public TextureGL(TextureFormat type)
	{
		this.type = type;

		totalTextureObjects++;

		selfReference = new WeakReference<TextureGL>(this);
		allTextureObjects.add(selfReference);
	}

	@Override
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

	@Override
	public abstract void bind();

	@Override
	public boolean destroy()
	{
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			allTextureObjects.remove(selfReference);

			if (glId >= 0)
			{
				glDeleteTextures(glId);
				//System.out.println("Disallocated glId "+glId+" for "+this);
				allocatedIds.remove(glId);

				totalTextureObjects--;

				//Set id to -2 to prevent texture being used again
				glId = -2;
				return true;
			}
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

	@Override
	public abstract long getVramUsage();

	protected static BlockingQueue<TextureGL> objectsToDestroy = new LinkedBlockingQueue<TextureGL>();
	protected static BlockingQueue<TextureRunnable> scheduled = new LinkedBlockingQueue<TextureRunnable>();
	
	protected interface TextureRunnable extends Runnable {
		public TextureGL getTexture();
	}
	
	public static void updateTextureObjects() {
		destroyPendingTextureObjects();
		
		Iterator<TextureRunnable> i = scheduled.iterator();
		while (i.hasNext())
		{
			TextureRunnable todo = i.next();
			
			if(todo.getTexture().glId != -2)
				todo.run();

			i.remove();
		}
	}
	
	private static int destroyPendingTextureObjects()
	{
		int destroyedVerticesObjects = 0;

		synchronized (objectsToDestroy)
		{
			Iterator<TextureGL> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				Texture object = i.next();

				if (object.destroy())
					destroyedVerticesObjects++;

				i.remove();
			}
		}

		Iterator<Entry<Integer, WeakReference<TextureGL>>> i = allocatedIds.entrySet().iterator();
		while(i.hasNext())
		{
			Entry<Integer, WeakReference<TextureGL>> entry = i.next();
			int id = entry.getKey();
			WeakReference<TextureGL> weakReference = entry.getValue();
			Texture texture = weakReference.get();
			if(texture == null)
			{
				logger.info("Disallocated orphan openGL texture ID "+id);
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
		Iterator<WeakReference<TextureGL>> i = allTextureObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<TextureGL> reference = i.next();

			Texture object = reference.get();
			if (object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}

		return vram;
	}

	protected static int totalTextureObjects = 0;
	protected static BlockingQueue<WeakReference<TextureGL>> allTextureObjects = new LinkedBlockingQueue<WeakReference<TextureGL>>();
	
	protected static Map<Integer, WeakReference<TextureGL> > allocatedIds = new ConcurrentHashMap<Integer, WeakReference<TextureGL>>();
}
