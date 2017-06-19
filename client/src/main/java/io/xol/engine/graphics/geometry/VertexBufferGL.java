package io.xol.engine.graphics.geometry;

import io.xol.chunkstories.api.rendering.vertex.AttributeSource;
import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool.PooledByteBuffer;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Holds and abstracts vertex buffers from OpenGL
 */
public class VertexBufferGL implements VertexBuffer
{
	private int openGLID = -1;

	private boolean isDataPresent = false;
	private long dataSize = 0L;

	private Object waitingToUploadMainThread;
	private Object waitingToUploadDeffered;
	
	private final WeakReference<VertexBufferGL> selfReference;

	private final UploadRegime uploadRegimeHint;
	
	public enum UploadRegime {
		FAST(GL_STREAM_DRAW),
		ONCE(GL_STATIC_DRAW);
		
		UploadRegime(int glId)
		{
			this.glId = glId;
		}
		int glId;
	}
	
	public VertexBufferGL()
	{
		this(UploadRegime.ONCE);
	}

	public VertexBufferGL(UploadRegime uploadRegimeHint)
	{
		this.uploadRegimeHint = uploadRegimeHint;
		
		//Increment counter and create weak reference to this object
		totalVerticesObjects++;
		selfReference = new WeakReference<VertexBufferGL>(this);
		allVerticesObjects.add(selfReference);

		//Assign a buffer ID if we're in the right thread
		if (Client.getInstance().getGameWindow().isMainGLWindow())
			aquireID();
	}

	public final synchronized void aquireID()
	{
		//If texture was already assignated we discard this call
		if (openGLID == -2 || openGLID >= 0)
			return;

		openGLID = glGenBuffers();
		//Keep the reference for this allocated id
		allocatedIds.put(openGLID, selfReference);
	}

	/**
	 * <i>Implementation internals, don't mess with this !</i><br/>
	 * Binds this VerticesObject to the opengl GL_ARRAY_BUFFER bind point.
	 */
	public void bind()
	{
		if (openGLID == -2)
		{
			ChunkStoriesLoggerImplementation.getInstance().log("Critical mess-up: Tried to bind a destroyed VerticesObject. Terminating process immediately.", LogLevel.CRITICAL);
			ChunkStoriesLoggerImplementation.getInstance().save();
			Thread.dumpStack();
			System.exit(-800);
			//throw new RuntimeException("Tryed to bind a destroyed VerticesBuffer");
		}

		//Check for and if needed create the buffer
		if (openGLID == -1)
			aquireID();

		bind(openGLID);
	}

	/**
	 * <i>Implementation internals, don't mess with this !</i><br/>
	 */
	@Deprecated
	public static void unbind()
	{
		bind(0);
	}

	static void bind(int arrayBufferId)
	{
		if (arrayBufferId == currentlyBoundArrayBuffer)
			return;

		glBindBuffer(GL_ARRAY_BUFFER, arrayBufferId);
		currentlyBoundArrayBuffer = arrayBufferId;
	}

	static int currentlyBoundArrayBuffer = 0;

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(java.nio.ByteBuffer)
	 */
	@Override
	public boolean uploadData(ByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit();
			return true;
		}

		//This is a deffered call
		waitingToUploadDeffered = dataToUpload;
		return false;
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(java.nio.FloatBuffer)
	 */
	@Override
	public boolean uploadData(FloatBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit() * 4;
			return true;
		}

		//This is a deffered call
		waitingToUploadDeffered = dataToUpload;
		return false;
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(io.xol.chunkstories.renderer.buffers.ByteBufferPool.RecyclableByteBuffer)
	 */
	@Override
	public boolean uploadData(RecyclableByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			Object replacing = waitingToUploadMainThread;
			waitingToUploadMainThread = dataToUpload;
			if(replacing != null && replacing != dataToUpload && replacing instanceof PooledByteBuffer)
			{
				System.out.println("Watch out, uploading two RecyclableByteBuffer in a row, the first one is getting recycled early to prevent locks");
				RecyclableByteBuffer rcb = (RecyclableByteBuffer)replacing;
				rcb.recycle();
			}
			dataSize = dataToUpload.accessByteBuffer().limit();
			return true;
		}

		//This is a deffered call
		waitingToUploadDeffered = dataToUpload;
		return false;
	}

	private boolean uploadDataActual(Object dataToUpload)
	{
		//Are we clear to execute openGL calls ?
		assert Client.getInstance().getGameWindow().isMainGLWindow();

		bind();

		if (openGLID == -2)
		{
			System.out.println("There we fucking go");
			Runtime.getRuntime().exit(-555);
		}

		if (dataToUpload instanceof PooledByteBuffer)
		{
			boolean returnCode = uploadDataActual(((RecyclableByteBuffer) dataToUpload).accessByteBuffer());
			((RecyclableByteBuffer) dataToUpload).recycle();
			return returnCode;
		}

		if (dataToUpload instanceof ByteBuffer)
		{
			dataSize = ((ByteBuffer) dataToUpload).limit();

			glBufferData(GL_ARRAY_BUFFER, (ByteBuffer) dataToUpload, uploadRegimeHint.glId);
			isDataPresent = true;
			return true;
		}
		else if (dataToUpload instanceof FloatBuffer)
		{
			dataSize = ((FloatBuffer) dataToUpload).limit() * 4;

			glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) dataToUpload, uploadRegimeHint.glId);
			isDataPresent = true;
			return true;
		}
		else if (dataToUpload instanceof IntBuffer)
		{
			dataSize = ((IntBuffer) dataToUpload).limit() * 4;

			glBufferData(GL_ARRAY_BUFFER, (IntBuffer) dataToUpload, uploadRegimeHint.glId);
			isDataPresent = true;
			return true;
		}
		else if (dataToUpload instanceof DoubleBuffer)
		{
			dataSize = ((DoubleBuffer) dataToUpload).limit() * 8;

			glBufferData(GL_ARRAY_BUFFER, (DoubleBuffer) dataToUpload, uploadRegimeHint.glId);
			isDataPresent = true;
			return true;
		}
		else if (dataToUpload instanceof ShortBuffer)
		{
			dataSize = ((ShortBuffer) dataToUpload).limit() * 2;

			glBufferData(GL_ARRAY_BUFFER, (ShortBuffer) dataToUpload, uploadRegimeHint.glId);
			isDataPresent = true;
			return true;
		}
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#isDataPresent()
	 */
	@Override
	public boolean isDataPresent()
	{
		if (openGLID == -2)
			return false;

		return isDataPresent || waitingToUploadMainThread != null;
	}

	private boolean uploadPendingDefferedData()
	{
		//Upload pending stuff
		Object atomicReference = waitingToUploadDeffered;
		if (atomicReference != null)
		{
			//System.out.println("oh shit waddup");

			bind();
			waitingToUploadDeffered = null;
			return uploadDataActual(atomicReference);
		}

		//Clear to draw stuff
		return false;
	}

	private boolean checkForPendingMainThreadData()
	{
		if (waitingToUploadMainThread == null)
			return false;

		//Take pending object, remove reference
		Object waitingToUploadMainThread = this.waitingToUploadMainThread;
		this.waitingToUploadMainThread = null;
		//And upload it
		return uploadDataActual(waitingToUploadMainThread);
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
			//Ensure it's bound
			bind();
			//Ensure it's up-to-date
			checkForPendingMainThreadData();
			if(!isDataPresent())
				throw new RuntimeException("No VBO data uploaded | "+GameWindowOpenGL.getInstance().renderingContext);
			//Set pointer
			glVertexAttribPointer(gl_AttributeLocation, dimensions, format.glId, format.normalized, stride, offset);
		}

	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#asAttributeSource(io.xol.engine.graphics.geometry.VertexFormat, int)
	 */
	@Override
	public AttributeSource asAttributeSource(VertexFormat format, int dimensions)
	{
		return new VerticesObjectAsAttribute(format, dimensions, 0, 0);
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#asAttributeSource(io.xol.engine.graphics.geometry.VertexFormat, int, int, long)
	 */
	@Override
	public AttributeSource asAttributeSource(VertexFormat format, int dimensions, int stride, long offset)
	{
		return new VerticesObjectAsAttribute(format, dimensions, stride, offset);
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#getVramUsage()
	 */
	@Override
	public long getVramUsage()
	{
		return dataSize;
	}

	public String toString()
	{
		return "[VerticeObjcect glId = " + this.openGLID + "]";
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#destroy()
	 */
	@Override
	public synchronized boolean destroy()
	{
		//If it was already destroyed
		if (openGLID == -2)
		{
			System.out.println("Tried to delete already destroyed verticesObject");
			Thread.dumpStack();
		}

		//If it wasn't allocated an id
		if (openGLID == -1)
		{
			//Mark it for unable to receive data, decrease counter
			openGLID = -2;
			totalVerticesObjects--;
			return true;
		}

		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			isDataPresent = false;

			//System.out.println("Deleting Buffer "+openglBufferId);
			allocatedIds.remove(openGLID);
			glDeleteBuffers(openGLID);
			openGLID = -2;
			dataSize = 0;

			totalVerticesObjects--;

			return true;
		}
		else
		{
			//synchronized (objectsToDestroy)
			{
				objectsToDestroy.add(this);
			}
			return false;
		}
	}

	private static Queue<VertexBufferGL> objectsToDestroy = new ConcurrentLinkedQueue<VertexBufferGL>();

	public static long updateVerticesObjects()
	{
		long vram = 0;

		//synchronized (objectsToDestroy)

		//Destroys unused objects
		{
			Iterator<VertexBufferGL> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				VertexBuffer object = i.next();

				if (object.destroy())
				{
				}

				i.remove();
			}
		}

		Iterator<Entry<Integer, WeakReference<VertexBufferGL>>> i = allocatedIds.entrySet().iterator();
		while (i.hasNext())
		{
			Entry<Integer, WeakReference<VertexBufferGL>> entry = i.next();
			int openGLID = entry.getKey();
			WeakReference<VertexBufferGL> weakReference = entry.getValue();
			VertexBuffer verticesObject = weakReference.get();

			if (verticesObject == null)
			{
				//Gives back orphan buffers
				glDeleteBuffers(openGLID);
				totalVerticesObjects--;
				//System.out.println("Destroyed orphan VerticesObject id #"+openGLID);

				i.remove();
			}
		}

		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<VertexBufferGL>> i2 = allVerticesObjects.iterator();
		while (i2.hasNext())
		{
			WeakReference<VertexBufferGL> reference = i2.next();

			VertexBufferGL verticesObject = reference.get();
			if (verticesObject != null && verticesObject.openGLID != -2)
			{
				//Send deffered uploads
				verticesObject.uploadPendingDefferedData();
					
				vram += verticesObject.getVramUsage();
			}
			//Remove null objects from the list
			else
				i2.remove();
		}

		return vram;
	}

	public static int getTotalNumberOfVerticesObjects()
	{
		return totalVerticesObjects;
	}

	/*
	public static long updateVerticesObjects()
	{
		long vram = 0;
	
		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<VerticesObject>> i = allVerticesObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<VerticesObject> reference = i.next();
	
			VerticesObject object = reference.get();
			if (object != null)
			{
				vram += object.getVramUsage();
			}
			//Remove null objects from the list
			else
				i.remove();
		}
	
		return vram;
	}*/

	private static int totalVerticesObjects = 0;
	private static Queue<WeakReference<VertexBufferGL>> allVerticesObjects = new ConcurrentLinkedQueue<WeakReference<VertexBufferGL>>();

	protected static Map<Integer, WeakReference<VertexBufferGL>> allocatedIds = new ConcurrentHashMap<Integer, WeakReference<VertexBufferGL>>();
}
