package io.xol.chunkstories.renderer.buffers;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class FloatBufferPool
{
	FloatBuffer[] pool;
	boolean avaible[];
	int size;

	public FloatBufferPool(int poolSize, int buffersSize)
	{
		size = poolSize;
		pool = new FloatBuffer[size];
		avaible = new boolean[size];
		for (int i = 0; i < size; i++)
		{
			pool[i] = BufferUtils.createFloatBuffer(buffersSize);
			avaible[i] = true;
		}
	}

	public FloatBuffer accessFloatBuffer(int id)
	{
		return pool[id];
	}

	public int requestFloatBuffer()
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
		//System.out.println("Out of avaible floatBuffers :/");
		return -1;
	}

	public void releaseFloatBuffer(int id)
	{
		avaible[id] = true;
	}
}