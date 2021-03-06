//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity;

import java.util.Iterator;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.util.IterableIterator;

public class EntityWorldIterator implements IterableIterator<Entity> {
	Iterator<Entity> ie;
	Entity currentEntity;

	public EntityWorldIterator(Iterator<Entity> ie) {
		this.ie = ie;
	}

	@Override
	public boolean hasNext() {
		return ie.hasNext();
	}

	@Override
	public Entity next() {
		currentEntity = ie.next();
		// System.out.println(currentEntity);
		return currentEntity;
	}

	@Override
	public void remove() {
		// Remove it from the world set
		ie.remove();
	}
}
