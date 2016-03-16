package io.xol.chunkstories.world.chunk;

import java.util.Arrays;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunksData
{
	// This is the big memory eater : this class holds 32x32x32 blobs of data
	public static int CACHE_SIZE = 2048+256;
	private int[][] data;
	private boolean[] used;
	private int size = 0;

	public ChunksData()
	{
		System.gc();
		data = new int[CACHE_SIZE][32 * 32 * 32];// [32][32];
		used = new boolean[CACHE_SIZE];
		System.out.println("Preloading chunk cache");
		for (int i = 0; i < CACHE_SIZE; i++)
			data[i] = new int[32 * 32 * 32];
		System.out.println("Initialized chunk cache, size =" + CACHE_SIZE);
	}

	public int malloc()
	{
		for (int i = 0; i < CACHE_SIZE; i++)
		{
			if (!used[i])
			{
				// Clear data
				// data[i] = new int[32][32][32];
				used[i] = true;
				size++;
				return i;
			}
		}
		System.out.println("Out of memory :c");
		Runtime.getRuntime().exit(-1);
		return -1;
	}

	public int size()
	{
		return size;
	}

	public int[] grab(int i)
	{
		return data[i];
	}

	public void free(int i)
	{
		// data[i] = new int[32][32][32];
		// System.out.println("freeing ["+i+"] leaving "+(Runtime.getRuntime().freeMemory()/1024/1024)+"Mb of ram avaible");
		if (!used[i])
		{
			System.out.println("Why would you free a free chunkData ?");
			return;
		}

		Arrays.fill(data[i], 0);
		//for (int a = 0; a < 32; a++)
		//	for (int b = 0; b < 32; b++)
		//		for (int c = 0; c < 32; c++)
		//			data[i][a * 32 * 32 + b * 32 + c] = 0;
		// System.out.println("freed ["+i+"] leaving "+(Runtime.getRuntime().freeMemory()/1024/1024)+"Mb of ram avaible");
		used[i] = false;
		size--;
	}

	public int free()
	{
		return CACHE_SIZE - size;
	}

	public void destroy()
	{
		data = null;
	}
}
