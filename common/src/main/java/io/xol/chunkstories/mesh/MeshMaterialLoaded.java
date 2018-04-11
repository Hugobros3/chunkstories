//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshMaterial;

public class MeshMaterialLoaded implements MeshMaterial {
	final String name;
	
	Mesh mesh;
	int firstVertex, lastVertex;
	
	String albedoTextureName, normalTextureName, specularTextureName;

	public MeshMaterialLoaded(Mesh mesh, String name, int firstVertex, int lastVertex, String albedoTextureName, String normalTextureName, String specularTextureName) {
		this.mesh = mesh;
		this.name = name;
		this.firstVertex = firstVertex;
		this.lastVertex = lastVertex;
		this.albedoTextureName = albedoTextureName;
		this.normalTextureName = normalTextureName;
		this.specularTextureName = specularTextureName;
	}

	public Mesh getMesh() {
		return mesh;
	}

	public String getName() {
		return name;
	}

	public int firstVertex() {
		return firstVertex;
	}

	public int lastVertex() {
		return lastVertex;
	}

	public String getAlbedoTextureName() {
		return albedoTextureName;
	}

	public String getNormalTextureName() {
		return normalTextureName;
	}

	public String getSpecularTextureName() {
		return specularTextureName;
	}
		
}
