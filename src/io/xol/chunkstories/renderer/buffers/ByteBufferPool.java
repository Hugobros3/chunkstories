package io.xol.chunkstories.renderer.buffers;

import java.lang.ref.WeakReference;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class ByteBufferPool
{
	ByteBuffer[] pool;
	WeakReference<RecyclableByteBuffer> trashCollector[];
	//boolean avaible[];
	int size;

	public ByteBufferPool(int poolSize, int buffersSize)
	{
		size = poolSize;
		pool = new ByteBuffer[size];
		//avaible = new boolean[size];
		trashCollector = new WeakReference[size];
		for (int i = 0; i < size; i++)
		{
			pool[i] = BufferUtils.createByteBuffer(buffersSize);
			//avaible[i] = true;
		}
	}
	
	public class RecyclableByteBuffer {
		
		int id;
		
		RecyclableByteBuffer(int id)
		{
			this.id = id;
		}
		
		public ByteBuffer accessByteBuffer()
		{
			if(id < 0)
				throw new RuntimeException("Mishandling of a RecyclableByteBuffer, see documentation !");
			
			return pool[id];
		}
		
		public void recycle()
		{
			if(id < 0)
				throw new RuntimeException("Mishandling of a RecyclableByteBuffer, see documentation !");
			
			trashCollector[id] = null;
			//avaible[id] = true;
			id = -1;
		}
	}

	public RecyclableByteBuffer requestByteBuffer()
	{
		for (int i = 0; i < size; i++)
		{
			WeakReference<RecyclableByteBuffer> safe = trashCollector[i];
			if (safe == null || safe.get() == null)
			{
				//avaible[i] = false;
				RecyclableByteBuffer r = new RecyclableByteBuffer(i);
				trashCollector[i] = new WeakReference<RecyclableByteBuffer>(r);
				pool[i].clear();
				return r;
			}
		}
		return null;
	}
}