package io.xol.engine.misc;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class ByteBufferPool
{
	ByteBuffer[] pool;
	boolean avaible[];
	int size;

	public ByteBufferPool(int poolSize, int buffersSize)
	{
		size = poolSize;
		pool = new ByteBuffer[size];
		avaible = new boolean[size];
		for (int i = 0; i < size; i++)
		{
			pool[i] = BufferUtils.createByteBuffer(buffersSize);
			avaible[i] = true;
		}
	}

	public ByteBuffer accessByteBuffer(int id)
	{
		return pool[id];
	}

	public int requestByteBuffer()
	{
		for (int i = 0; i < size; i++)
		{
			if (avaible[i])
			{
				avaible[i] = false;
				pool[i].clear();
				return i;
			}
		}
		return -1;
	}

	public void releaseByteBuffer(int id)
	{
		avaible[id] = true;
	}
}