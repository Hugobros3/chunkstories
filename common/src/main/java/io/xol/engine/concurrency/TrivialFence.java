package io.xol.engine.concurrency;

import io.xol.chunkstories.api.util.concurrency.Fence;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TrivialFence implements Fence {

	@Override
	public void traverse() {
		//Do absolutely nothing
	}

}
