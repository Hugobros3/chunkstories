package io.xol.engine.graphics.geometry;

import io.xol.engine.base.GameWindowOpenGL;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Holds and abstracts vertex buffers from OpenGL
 */
public class VerticesObject
{
	//private static int boundBuffer = -1;

	private int openglBufferId = -1;

	private boolean isDataPresent = false;
	private long dataSize = 0L;

	private ByteBuffer dataPendingUpload = null;
	
	private final WeakReference<VerticesObject> self;

	public VerticesObject()
	{
		//Assign a buffer ID if we're in the right thread
		if (GameWindowOpenGL.isMainGLWindow())
			openglBufferId = glGenBuffers();

		//Increment counter and create weak reference to this object
		totalVerticesObjects++;
		self = new WeakReference<VerticesObject>(this);
		allVerticesObjects.add(self);
	}

	public void bind()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
			throw new IllegalRenderingThreadException();

		//Check for and if needed create the buffer
		if (openglBufferId == -1)
			openglBufferId = glGenBuffers();

		glBindBuffer(GL_ARRAY_BUFFER, openglBufferId);
	}

	/**
	 * @return True if the data was immediatly uploaded
	 */
	public boolean uploadData(ByteBuffer dataToUpload)
	{
		//Are we clear to execute openGL calls ?
		if (GameWindowOpenGL.isMainGLWindow())
		{
			bind();
			dataSize = dataToUpload.remaining();
			glBufferData(GL_ARRAY_BUFFER, dataToUpload, GL_STATIC_DRAW);
			isDataPresent = true;
			return true;
		}
		else
		{
			//Mark data for pending uploading.
			dataPendingUpload = dataToUpload;

			return false;
		}
	}

	/**
	 * Notice : there is no risk of synchronisation issues with an object suddently being destroyed during because actual destruction of the objects only occur at the end of the frame !
	 * 
	 * @return True if data is present and the verticesObject can be drawn
	 */
	public boolean isDataPresent()
	{
		return isDataPresent;
	}

	//Mututal stuff of drawElements* functions
	private boolean prepareDraw()
	{
		//Check for context
		if (!GameWindowOpenGL.isMainGLWindow())
			throw new IllegalRenderingThreadException();

		//Upload pending stuff
		ByteBuffer atomicReference = dataPendingUpload;
		if (atomicReference != null)
		{
			uploadData(atomicReference);
			dataPendingUpload = null;
		}

		//Check for data presence
		if (!isDataPresent())
			return false;

		//Clear to draw stuff
		return true;
	}

	public boolean drawElementsPoints(int elementsToDraw)
	{
		if (!prepareDraw())
			return false;
		glDrawArrays(GL_POINTS, 0, elementsToDraw);
		return true;
	}

	public boolean drawElementsLines(int elementsToDraw)
	{
		if (!prepareDraw())
			return false;
		glDrawArrays(GL_LINES, 0, elementsToDraw);
		return true;
	}

	public boolean drawElementsTriangles(int elementsToDraw)
	{
		if (!prepareDraw())
			return false;
		glDrawArrays(GL_TRIANGLES, 0, elementsToDraw);
		return true;
	}

	public boolean drawElementsQuads(int elementsToDraw)
	{
		if (!prepareDraw())
			return false;
		glDrawArrays(GL_QUADS, 0, elementsToDraw);
		return true;
	}

	public long getVramUsage()
	{
		return dataSize;
	}
	
	/**
	 * Synchronized, returns true only when it actually deletes the gl buffer
	 */
	public synchronized boolean destroy()
	{
		if (openglBufferId == -1)
		{
			//Mark it for unable to receive data, decrease counter
			openglBufferId = -2;
			totalVerticesObjects--;
			return false;
		}

		if (GameWindowOpenGL.isMainGLWindow())
		{
			isDataPresent = false;

			glDeleteBuffers(openglBufferId);
			openglBufferId = -2;
			dataSize = 0;
			
			totalVerticesObjects--;

			return true;
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

	private static BlockingQueue<VerticesObject> objectsToDestroy = new LinkedBlockingQueue<VerticesObject>();

	public static int destroyPendingVerticesObjects()
	{
		int destroyedVerticesObjects = 0;

		synchronized (objectsToDestroy)
		{
			Iterator<VerticesObject> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				VerticesObject object = i.next();

				if (object.destroy())
					destroyedVerticesObjects++;

				i.remove();
			}
		}

		return destroyedVerticesObjects;
	}

	public static int getTotalNumberOfVerticesObjects()
	{
		return totalVerticesObjects;
	}

	public static long getTotalVramUsage()
	{
		long vram = 0;

		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<VerticesObject>> i = allVerticesObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<VerticesObject> reference = i.next();

			VerticesObject object = reference.get();
			if(object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}
		
		return vram;
	}

	private static int totalVerticesObjects = 0;
	private static BlockingQueue<WeakReference<VerticesObject>> allVerticesObjects = new LinkedBlockingQueue<WeakReference<VerticesObject>>();
}
