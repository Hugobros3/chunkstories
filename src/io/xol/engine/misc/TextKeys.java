package io.xol.engine.misc;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class TextKeys
{

	static boolean keys[] = new boolean[256];

	public static int keysTime[] = new int[256];

	public static void init()
	{
		int[] badkeys = { 1, 42, 59, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68,
				69, 70, 87, 88, 100, 101, 102, 103, 104, 105 };
		for (int key : badkeys)
			keys[key] = true;
	}

	public static boolean isTextKey(int k)
	{
		return !keys[k];
	}
}
