package io.xol.chunkstories.api.mesh;

import io.xol.chunkstories.api.Content;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface MeshLibrary {
	
	public Mesh getMeshByName(String meshName);
	
	public MultiPartMesh getMultiPartMeshByName(String meshName);
	
	public Content parent();
}