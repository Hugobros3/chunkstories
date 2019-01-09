//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util.concurrency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A list that has only unique elements
 */
public class UniqueList<K> implements List<K>, Collection<K> {
	Set<K> set = new HashSet<K>();
	List<K> list = new LinkedList<K>();

	@Override
	public boolean add(K e) {
		if (set.add(e))
			return list.add(e);
		return false;
	}

	@Override
	public void add(int index, K element) {
		if (set.add(element))
			list.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		if (set.addAll(c))
			return list.addAll(c);
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends K> c) {
		if (set.addAll(c))
			return list.addAll(index, c);
		return false;
	}

	@Override
	public void clear() {
		set.clear();
		list.clear();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public K get(int index) {
		return list.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<K> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<K> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<K> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		if (set.remove(o))
			return list.remove(o);
		return false;
	}

	@Override
	public K remove(int index) {
		K removed = list.remove(index);
		if (removed != null)
			set.remove(removed);
		return removed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		set.removeAll(c);
		return set.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public K set(int index, K element) {
		K p = null;
		if ((p = list.set(index, element)) != null)
			set.add(element);
		return p;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public List<K> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

}
