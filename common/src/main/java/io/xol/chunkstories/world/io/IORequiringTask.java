//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.world.chunk.Region;



public class IORequiringTask
{
	public boolean run(Region holder)
	{
		System.out.println("Unparameteized IORequiringTask was ran !");
		return true;
	}
}
