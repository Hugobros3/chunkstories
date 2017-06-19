package io.xol.engine.misc;

import java.util.Iterator;

import io.xol.chunkstories.api.util.IterableIterator;

/** Because Java lacks stupid things sometimes */
public class CasterIterator<T, V extends T> implements IterableIterator<T>
{
	final Iterator<V> i;
	
	public CasterIterator(Iterator<V> iterator)
	{
		i = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return i.hasNext();
	}

	@Override
	public T next()
	{
		return (T)i.next();
	}

}
