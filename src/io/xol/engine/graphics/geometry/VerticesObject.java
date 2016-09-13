package io.xol.engine.graphics.geometry;

import io.xol.chunkstories.api.rendering.AttributeSource;
import io.xol.engine.base.GameWindowOpenGL;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Holds and abstracts vertex buffers from OpenGL
 */
public class VerticesObject
{
	private int glId = -1;

	private boolean isDataPresent = false;
	private long dataSize = 0L;

	private Object dataPendingUpload = null;

	private final WeakReference<VerticesObject> selfReference;

	public VerticesObject()
	{
		//Increment counter and create weak reference to this object
		totalVerticesObjects++;
		selfReference = new WeakReference<VerticesObject>(this);
		allVerticesObjects.add(selfReference);

		//Assign a buffer ID if we're in the right thread
		if (GameWindowOpenGL.isMainGLWindow())
			aquireID();
	}

	public final synchronized void aquireID()
	{
		//If texture was already assignated we discard this call
		if (glId == -2 || glId >= 0)
			return;

		glId = glGenBuffers();
		//Keep the reference for this allocated id
		allocatedIds.put(glId, selfReference);
	}

	public void bind()
	{
		if(glId == -2)
			throw new RuntimeException("Tryed to bind a destroyed VerticesBuffer");
		
		//Check for and if needed create the buffer
		if (glId == -1)
			aquireID();

		bind(glId);

		checkForPendingUploadData();
	}

	public static void unbind()
	{
		bind(0);
	}

	static void bind(int arrayBufferId)
	{
		if(arrayBufferId == bound)
			return;

		//When no updates are supposed to be required
		/*if (arrayBufferId == bound)
		{
			int boundAccordingToGL = glGetInteger( GL_ARRAY_BUFFER_BINDING );
			if (boundAccordingToGL != bound)
			{
				System.out.println("WOW : " + boundAccordingToGL + " != " + bound);
				WeakReference<VerticesObject> ref = allocatedIds.get(boundAccordingToGL);
				if (ref != null)
				{
					VerticesObject object = ref.get();
					if (object != null)
						System.out.println(object);
				}
			}
		}*/

		glBindBuffer(GL_ARRAY_BUFFER, arrayBufferId);
		bound = arrayBufferId;
	}

	static int bound = 0;

	/**
	 * @return True if the data was immediatly uploaded
	 */
	public boolean uploadData(ByteBuffer dataToUpload)
	{
		if(glId == -2)
			throw new RuntimeException("Tryed to upload to a destroyed VerticesBuffer");
		
		return uploadDataActual(dataToUpload);
	}

	/**
	 * @return True if the data was immediatly uploaded
	 */
	public boolean uploadData(FloatBuffer dataToUpload)
	{
		if(glId == -2)
			throw new RuntimeException("Tryed to upload to a destroyed VerticesBuffer");
		
		return uploadDataActual(dataToUpload);
	}

	private boolean uploadDataActual(Object dataToUpload)
	{
		//Are we clear to execute openGL calls ?
		if (GameWindowOpenGL.isMainGLWindow())
		{
			bind();
			
			if(glId == -2)
			{
				System.out.println("There we fucking go");
				Runtime.getRuntime().exit(-555);
			}

			if (dataToUpload instanceof ByteBuffer)
			{
				dataSize = ((ByteBuffer) dataToUpload).limit();

				glBufferData(GL_ARRAY_BUFFER, (ByteBuffer) dataToUpload, GL_STATIC_DRAW);
				isDataPresent = true;
				return true;
			}
			else if (dataToUpload instanceof FloatBuffer)
			{
				dataSize = ((FloatBuffer) dataToUpload).limit() * 4;

				glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) dataToUpload, GL_STATIC_DRAW);
				isDataPresent = true;
				return true;
			}
			else if (dataToUpload instanceof IntBuffer)
			{
				dataSize = ((IntBuffer) dataToUpload).limit() * 4;

				glBufferData(GL_ARRAY_BUFFER, (IntBuffer) dataToUpload, GL_STATIC_DRAW);
				isDataPresent = true;
				return true;
			}
			else if (dataToUpload instanceof DoubleBuffer)
			{
				dataSize = ((DoubleBuffer) dataToUpload).limit() * 8;

				glBufferData(GL_ARRAY_BUFFER, (DoubleBuffer) dataToUpload, GL_STATIC_DRAW);
				isDataPresent = true;
				return true;
			}
			else if (dataToUpload instanceof ShortBuffer)
			{
				dataSize = ((ShortBuffer) dataToUpload).limit() * 2;

				glBufferData(GL_ARRAY_BUFFER, (ShortBuffer) dataToUpload, GL_STATIC_DRAW);
				isDataPresent = true;
				return true;
			}
		}
		else
		{
			//Mark data for pending uploading.
			dataPendingUpload = dataToUpload;

			return false;
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Notice : there is no risk of synchronisation issues with an object suddently being destroyed during because actual destruction of the objects only occur at the end of the frame !
	 * 
	 * @return True if data is present and the verticesObject can be drawn
	 */
	public boolean isDataPresent()
	{
		if(glId == -2)
			return false;
		
		return isDataPresent || dataPendingUpload != null;
	}

	private boolean checkForPendingUploadData()
	{
		//Upload pending stuff
		Object atomicReference = dataPendingUpload;
		if (atomicReference != null)
		{
			//System.out.println("Uploading pending VerticesObject ... ");
			dataPendingUpload = null;
			uploadDataActual(atomicReference);
		}

		//Check for data presence
		if (!isDataPresent())
			return false;

		//Clear to draw stuff
		return true;
	}

	class VerticesObjectAsAttribute implements AttributeSource
	{
		VertexFormat format;
		int dimensions, stride;
		long offset;

		public VerticesObjectAsAttribute(VertexFormat format, int dimensions, int stride, long offset)
		{
			this.format = format;
			this.dimensions = dimensions;
			this.stride = stride;
			this.offset = offset;
		}

		@Override
		public void setup(int gl_AttributeLocation)
		{
			bind();
			glVertexAttribPointer(gl_AttributeLocation, dimensions, format.glId, format.normalized, stride, offset);
		}

	}

	public AttributeSource asAttributeSource(VertexFormat format, int dimensions)
	{
		return new VerticesObjectAsAttribute(format, dimensions, 0, 0);
	}

	public AttributeSource asAttributeSource(VertexFormat format, int dimensions, int stride, long offset)
	{
		return new VerticesObjectAsAttribute(format, dimensions, stride, offset);
	}

	public long getVramUsage()
	{
		return dataSize;
	}

	public String toString()
	{
		return "[VerticeObjcect glId = " + this.glId + "]";
	}

	/**
	 * Synchronized, returns true only when it actually deletes the gl buffer
	 */
	public synchronized boolean destroy()
	{
		//If it was already destroyed
		if(glId == -2)
		{
			System.out.println("Tried to delete already destroyed verticesObject");
			Thread.dumpStack();
		}
		
		//If it wasn't allocated an id
		if (glId == -1)
		{
			//Mark it for unable to receive data, decrease counter
			glId = -2;
			totalVerticesObjects--;
			return true;
		}

		if (GameWindowOpenGL.isMainGLWindow())
		{
			isDataPresent = false;

			//System.out.println("Deleting Buffer "+openglBufferId);
			glDeleteBuffers(glId);
			allocatedIds.remove(glId);
			glId = -2;
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

		Iterator<Entry<Integer, WeakReference<VerticesObject>>> i = allocatedIds.entrySet().iterator();
		while (i.hasNext())
		{
			Entry<Integer, WeakReference<VerticesObject>> entry = i.next();
			int id = entry.getKey();
			WeakReference<VerticesObject> weakReference = entry.getValue();
			VerticesObject verticesObject = weakReference.get();
			if (verticesObject == null)
			{
				//System.out.println("Destroyed orphan VerticesObject id #"+id);
				glDeleteBuffers(id);
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
			if (object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}

		return vram;
	}

	private static int totalVerticesObjects = 0;
	private static BlockingQueue<WeakReference<VerticesObject>> allVerticesObjects = new LinkedBlockingQueue<WeakReference<VerticesObject>>();

	protected static Map<Integer, WeakReference<VerticesObject>> allocatedIds = new ConcurrentHashMap<Integer, WeakReference<VerticesObject>>();
}
