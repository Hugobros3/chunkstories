package io.xol.chunkstories.api.mesh;

import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mods.Asset;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface MeshLoader {
	
	/** What extension does this loader provides support for ? (ie: .obj .col .dae .h3d ) */
	public String getExtension();
	
	/** Returns a loaded Mesh or MultiPartMesh */
	public Mesh loadMeshFromAsset(Asset a) throws MeshLoadException;
}
