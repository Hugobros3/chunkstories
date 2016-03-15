package io.xol.chunkstories.world.io;

import io.xol.chunkstories.world.chunk.ChunkHolder;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IORequiringTask
{
	public boolean run(ChunkHolder holder)
	{
		System.out.println("Unparameteized IORequiringTask was ran !");
		return true;
	}
}
