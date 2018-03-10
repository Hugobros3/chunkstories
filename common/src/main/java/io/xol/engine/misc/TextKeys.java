//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.misc;



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
