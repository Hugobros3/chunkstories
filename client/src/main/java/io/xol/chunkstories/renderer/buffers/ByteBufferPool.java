package io.xol.chunkstories.renderer.buffers;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.lang.ref.WeakReference;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;

public class ByteBufferPool
{
	ByteBuffer[] pool;
	WeakReference<PooledByteBuffer> trashCollector[];
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
	
	public class PooledByteBuffer implements RecyclableByteBuffer {
		
		int id;
		
		PooledByteBuffer(int id)
		{
			this.id = id;
		}
		
		@Override
		public ByteBuffer accessByteBuffer()
		{
			if(id < 0)
				throw new RuntimeException("Mishandling of a RecyclableByteBuffer, see documentation !");
			
			return pool[id];
		}
		
		@Override
		public void recycle()
		{
			if(id < 0)
				throw new RuntimeException("Mishandling of a RecyclableByteBuffer, see documentation !");
			
			trashCollector[id] = null;
			//avaible[id] = true;
			id = -1;
		}
	}

	public PooledByteBuffer requestByteBuffer()
	{
		for (int i = 0; i < size; i++)
		{
			WeakReference<PooledByteBuffer> safe = trashCollector[i];
			if (safe == null || safe.get() == null)
			{
				//avaible[i] = false;
				PooledByteBuffer r = new PooledByteBuffer(i);
				trashCollector[i] = new WeakReference<PooledByteBuffer>(r);
				pool[i].clear();
				return r;
			}
		}
		return null;
	}
}