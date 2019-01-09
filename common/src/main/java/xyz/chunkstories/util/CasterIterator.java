//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util;

import java.util.Iterator;

import xyz.chunkstories.api.util.IterableIterator;

/** Because Java lacks stupid things sometimes */
public class CasterIterator<T, V extends T> implements IterableIterator<T> {
	final Iterator<V> i;

	public CasterIterator(Iterator<V> iterator) {
		i = iterator;
	}

	@Override
	public boolean hasNext() {
		return i.hasNext();
	}

	@Override
	public T next() {
		return (T) i.next();
	}

}
