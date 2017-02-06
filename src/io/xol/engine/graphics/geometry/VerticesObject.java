package io.xol.engine.graphics.geometry;

import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool.RecyclableByteBuffer;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;
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
public class VerticesObject
{
	private int openGLID = -1;

	private boolean isDataPresent = false;
	private long dataSize = 0L;

	private Object waitingToUploadMainThread;
	private Object waitingToUploadDeffered;
	
	private final WeakReference<VerticesObject> selfReference;

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
	
	public VerticesObject()
	{
		this(UploadRegime.ONCE);
	}

	public VerticesObject(UploadRegime uploadRegimeHint)
	{
		this.uploadRegimeHint = uploadRegimeHint;
		
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
			ChunkStoriesLogger.getInstance().log("Critical mess-up: Tried to bind a destroyed VerticesObject. Terminating process immediately.", LogLevel.CRITICAL);
			ChunkStoriesLogger.getInstance().save();
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

	/**
	 * Uploads new data to this buffer, replacing former content.<br/>
	 * <u>IF THIS IS CALLED IN THE MAIN THREAD:</u><br/>
	 * * Uploading of data is queued for the next time this object is used in a drawcall ( an attribute source is setup, namely )<br/>
	 * * The apparent size of the object ( getVramUsage() ) is updated immediately<br/>
	 * * Multiple subsequent uploads in one frame can override former ones; given the right conditions a call to uploadData followed by another one then a draw call, the first upload will be ignored altogether and only the latest buffer content will be used<br/>
	 * * Changing the VerticesObject content does not trigger a flush() in the RenderingInterface, if you want to issue draw calls on this buffer with multiple data per frame you have to issue flush() commands between them to make sure the data is uploaded. More information is on the wiki on this.<br/>
	 * <u>IF THIS IS CALLED IN ANY OTHER THREAD:</u><br/>
	 * * The data is queued for upload on the <b>NEXT</b> frame.<br/>
	 * * Before each frame starts being drawed, the latest buffer content provided by a foreign thread, if such exists, is uploaded, replacing the VerticesObject content and updating it's size.
	 * 
	 * @return True if the data was uploaded ( or rather, queued for upload on use ), false if it was deffered to the next frame
	 */
	public boolean uploadData(ByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (GameWindowOpenGL.isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit();
			return true;
		}

		//This is a deffered call
		waitingToUploadDeffered = dataToUpload;
		return false;
	}

	/**
	 * Uploads new data to this buffer, replacing former content.<br/>
	 * <u>IF THIS IS CALLED IN THE MAIN THREAD:</u><br/>
	 * * Uploading of data is queued for the next time this object is used in a drawcall ( an attribute source is setup, namely )<br/>
	 * * The apparent size of the object ( getVramUsage() ) is updated immediately<br/>
	 * * Multiple subsequent uploads in one frame can override former ones; given the right conditions a call to uploadData followed by another one then a draw call, the first upload will be ignored altogether and only the latest buffer content will be used<br/>
	 * * Changing the VerticesObject content does not trigger a flush() in the RenderingInterface, if you want to issue draw calls on this buffer with multiple data per frame you have to issue flush() commands between them to make sure the data is uploaded. More information is on the wiki on this.<br/>
	 * <u>IF THIS IS CALLED IN ANY OTHER THREAD:</u><br/>
	 * * The data is queued for upload on the <b>NEXT</b> frame.<br/>
	 * * Before each frame starts being drawed, the latest buffer content provided by a foreign thread, if such exists, is uploaded, replacing the VerticesObject content and updating it's size.
	 * 
	 * @return True if the data was uploaded ( or rather, queued for upload on use ), false if it was deffered to the next frame
	 */
	public boolean uploadData(FloatBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (GameWindowOpenGL.isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit() * 4;
			return true;
		}

		//This is a deffered call
		waitingToUploadDeffered = dataToUpload;
		return false;
	}

	public boolean uploadData(RecyclableByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (GameWindowOpenGL.isMainGLWindow())
		{
			Object replacing = waitingToUploadMainThread;
			waitingToUploadMainThread = dataToUpload;
			if(replacing != null && replacing != dataToUpload && replacing instanceof RecyclableByteBuffer)
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
		assert GameWindowOpenGL.isMainGLWindow();

		bind();

		if (openGLID == -2)
		{
			System.out.println("There we fucking go");
			Runtime.getRuntime().exit(-555);
		}

		if (dataToUpload instanceof RecyclableByteBuffer)
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

	/**
	 * Notice : there is no risk of synchronisation issues with an object suddently being destroyed during because actual destruction of the objects only occur at the end of the frame !
	 * 
	 * @return True if data is present and the verticesObject can be drawn
	 */
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
				throw new RuntimeException("No VBO data uploaded | "+GameWindowOpenGL.instance.renderingContext);
			//Set pointer
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
		return "[VerticeObjcect glId = " + this.openGLID + "]";
	}

	/**
	 * Synchronized, returns true only when it actually deletes the gl buffer
	 */
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

		if (GameWindowOpenGL.isMainGLWindow())
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

	private static Queue<VerticesObject> objectsToDestroy = new ConcurrentLinkedQueue<VerticesObject>();

	public static long updateVerticesObjects()
	{
		long vram = 0;

		//synchronized (objectsToDestroy)

		//Destroys unused objects
		{
			Iterator<VerticesObject> i = objectsToDestroy.iterator();
			while (i.hasNext())
			{
				VerticesObject object = i.next();

				if (object.destroy())
				{
				}

				i.remove();
			}
		}

		Iterator<Entry<Integer, WeakReference<VerticesObject>>> i = allocatedIds.entrySet().iterator();
		while (i.hasNext())
		{
			Entry<Integer, WeakReference<VerticesObject>> entry = i.next();
			int openGLID = entry.getKey();
			WeakReference<VerticesObject> weakReference = entry.getValue();
			VerticesObject verticesObject = weakReference.get();

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
		Iterator<WeakReference<VerticesObject>> i2 = allVerticesObjects.iterator();
		while (i2.hasNext())
		{
			WeakReference<VerticesObject> reference = i2.next();

			VerticesObject verticesObject = reference.get();
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
	private static Queue<WeakReference<VerticesObject>> allVerticesObjects = new ConcurrentLinkedQueue<WeakReference<VerticesObject>>();

	protected static Map<Integer, WeakReference<VerticesObject>> allocatedIds = new ConcurrentHashMap<Integer, WeakReference<VerticesObject>>();
}
