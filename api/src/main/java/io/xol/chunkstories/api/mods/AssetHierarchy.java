package io.xol.chunkstories.api.mods;

import java.util.Iterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Some assets may get overloaded by mods, and in some cases you still want to read all versions, this interface allows just that
 */
public interface AssetHierarchy extends Iterable<Asset>
{
	/** Returns the name of the asset overloaded */
	public String getName();

	/** Returns the "top" asset ( the one the ModsManager returns if you ask for it by name */
	public Asset topInstance();

	/** Returns an iterator from higher to lower priority */
	public Iterator<Asset> iterator();
}