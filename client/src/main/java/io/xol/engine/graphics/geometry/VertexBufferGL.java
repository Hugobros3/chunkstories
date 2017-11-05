package io.xol.engine.graphics.geometry;

import io.xol.chunkstories.api.rendering.vertex.AttributeSource;
import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.Client;
import io.xol.engine.base.GameWindowOpenGL_LWJGL3;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.concurrency.TrivialFence;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

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
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private Semaphore asynchUploadLock = new Semaphore(1);
	private PendingUpload pendingAsyncUpload;
	
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

		//Assign a buffer ID if we're in the right thread/fly
		if (GameWindowOpenGL_LWJGL3.getInstance().isMainGLWindow())
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
			logger().error("Critical mess-up: Tried to bind a destroyed VerticesObject. Terminating process immediately.");
			//logger().save();
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

	private Fence setAsyncDataPendingUpload(Object o) {
		
		PendingUpload pendingUpload = new PendingUpload(o);
		asynchUploadLock.acquireUninterruptibly();
		
		//Check if there is already pending data not yet uploaded
		PendingUpload alreadyPendingUpload = this.pendingAsyncUpload;
		if(alreadyPendingUpload != null) {
			Object trashedData = alreadyPendingUpload.data;
			
			//Recycle any pooled byte buffer that won't be uploaded ( nice guys always loose )
			if(trashedData != null && trashedData instanceof RecyclableByteBuffer) {
				((RecyclableByteBuffer)trashedData).recycle();
			}
		}
		
		this.pendingAsyncUpload = pendingUpload;
		
		asynchUploadLock.release();
		return pendingUpload;
	}
	
	class PendingUpload extends SimpleFence {
		final Object data;
		
		PendingUpload(Object o) {
			this.data = o;
		}
	}
	
	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(java.nio.ByteBuffer)
	 */
	@Override
	public Fence uploadData(ByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit();
			return new TrivialFence();
		}

		//This is a deffered call
		
		return setAsyncDataPendingUpload(dataToUpload);
		//pendingAsyncUpload = dataToUpload;
		//return false;
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(java.nio.FloatBuffer)
	 */
	@Override
	public Fence uploadData(FloatBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			waitingToUploadMainThread = dataToUpload;
			dataSize = dataToUpload.limit() * 4;
			return new TrivialFence();
		}

		//This is a deffered call
		return setAsyncDataPendingUpload(dataToUpload);
		//pendingAsyncUpload = dataToUpload;
		//return false;
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.graphics.geometry.VertexBuffer#uploadData(io.xol.chunkstories.renderer.buffers.ByteBufferPool.RecyclableByteBuffer)
	 */
	@Override
	public Fence uploadData(RecyclableByteBuffer dataToUpload)
	{
		if (openGLID == -2)
			throw new RuntimeException("Illegal operation : Attempted to upload data to a destroyed VerticesObject !");

		//Queue for immediate upload
		if (Client.getInstance().getGameWindow().isMainGLWindow())
		{
			Object replacing = waitingToUploadMainThread;
			waitingToUploadMainThread = dataToUpload;
			if(replacing != null && replacing != dataToUpload && replacing instanceof RecyclableByteBuffer)
			{
				//System.out.println("Watch out, uploading two RecyclableByteBuffer in a row, the first one is getting recycled early to prevent locks");
				RecyclableByteBuffer rcb = (RecyclableByteBuffer)replacing;
				rcb.recycle();
			}
			dataSize = dataToUpload.accessByteBuffer().limit();
			return new TrivialFence();
		}

		//This is a deffered call
		return setAsyncDataPendingUpload(dataToUpload);
		//pendingAsyncUpload = dataToUpload;
		//return false;
	}

	private boolean uploadDataActual(Object dataToUpload)
	{
		//Are we clear to execute openGL calls ?
		assert Client.getInstance().getGameWindow().isMainGLWindow();

		bind();

		if (openGLID == -2)
		{
			System.out.println("FATAL: Attempted to upload data to a destroy()ed VertexBuffer. Terminating immediately.");
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

	private void uploadPendingDefferedData()
	{
		//Quickly check if anything is worth noting
		PendingUpload quickCheck = pendingAsyncUpload;
		if (quickCheck != null)
		{
			//We do enter locked mode to retreive the data.
			this.asynchUploadLock.acquireUninterruptibly();
			
			PendingUpload pendingUpload = this.pendingAsyncUpload;
			
			//this should NEVER be null, the only code path able to null this out is this very block, and it's only called from a single thread ever
			//(the graphics thread). If this fails, we might as well crash and burn the entire world
			assert pendingUpload != null;
			
			//Access the data
			Object dataWithin = pendingUpload.data;
			
			assert dataWithin != null;
			
			//Null-out the original reference
			pendingAsyncUpload = null;
			
			this.asynchUploadLock.release();

			//We don't need mutex to bind GL objects and give back pooled ressources.
			bind();
			uploadDataActual(dataWithin);
			
			//Signal the fence as done
			pendingUpload.signal();
		}

		//Clear to draw stuff
		//return false;
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
		int dimensions, stride, divisor;
		long offset;
		boolean asIntAttribute;

		public VerticesObjectAsAttribute(VertexFormat format, int dimensions, int stride, long offset, int divisor, boolean asIntAttribute)
		{
			this.format = format;
			this.dimensions = dimensions;
			this.stride = stride;
			this.offset = offset;
			this.divisor = divisor;
			this.asIntAttribute = asIntAttribute;
		}

		@Override
		public void setup(int gl_AttributeLocation)
		{
			//Ensure it's bound
			bind();
			//Ensure it's up-to-date
			checkForPendingMainThreadData();
			if(!isDataPresent())
				throw new RuntimeException("No VBO data uploaded | "+Client.getInstance().getGameWindow().getRenderingContext());
			//Set pointer
			if(asIntAttribute)
				glVertexAttribIPointer(gl_AttributeLocation, dimensions, format.glId, stride, offset);
			else
				glVertexAttribPointer(gl_AttributeLocation, dimensions, format.glId, format.normalized, stride, offset);
			glVertexAttribDivisor(gl_AttributeLocation, divisor);
		}

	}

	@Override
	public AttributeSource asAttributeSource(VertexFormat format, int dimensions)
	{
		return new VerticesObjectAsAttribute(format, dimensions, 0, 0, 0, false);
	}

	@Override
	public AttributeSource asAttributeSource(VertexFormat format, int dimensions, int stride, long offset)
	{
		return new VerticesObjectAsAttribute(format, dimensions, stride, offset, 0, false);
	}

	@Override
	public AttributeSource asAttributeSource(VertexFormat format, int dimensions, int stride, long offset, int divisor)
	{
		return new VerticesObjectAsAttribute(format, dimensions, stride, offset, divisor, false);
	}

	@Override
	public AttributeSource asIntegerAttributeSource(VertexFormat format, int dimensions, int stride, long offset, int divisor)
	{
		return new VerticesObjectAsAttribute(format, dimensions, stride, offset, divisor, true);
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

	private static int totalVerticesObjects = 0;
	private static Queue<WeakReference<VertexBufferGL>> allVerticesObjects = new ConcurrentLinkedQueue<WeakReference<VertexBufferGL>>();

	protected static Map<Integer, WeakReference<VertexBufferGL>> allocatedIds = new ConcurrentHashMap<Integer, WeakReference<VertexBufferGL>>();
	
	private static final Logger logger = LoggerFactory.getLogger("rendering.vertexBuffers");
	public Logger logger() {
		return logger;
	}
}
