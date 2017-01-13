package io.xol.chunkstories.api.utils;

import java.util.Iterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Java spec workarround
 */
public interface IterableIterator<T> extends Iterator<T>, Iterable<T>
{
	public default Iterator<T> iterator()
	{
		return this;
	}
}
