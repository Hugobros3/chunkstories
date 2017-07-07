package io.xol.engine.concurrency;

import java.util.LinkedList;

import io.xol.chunkstories.api.util.concurrency.Fence;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class CompoundFence extends LinkedList<Fence> implements Fence {

	private static final long serialVersionUID = 1770973697744619763L;

	@Override
	/** Traverse-all :) */
	public void traverse() {
		for(Fence f : this) {
			f.traverse();
		}
	}

}
