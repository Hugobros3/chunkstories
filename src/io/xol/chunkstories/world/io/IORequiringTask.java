package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.world.Region;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IORequiringTask
{
	public boolean run(Region holder)
	{
		System.out.println("Unparameteized IORequiringTask was ran !");
		return true;
	}
}
