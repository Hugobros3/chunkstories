package io.xol.chunkstories.api.mesh;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.FloatBuffer;

/** Describes a standardized in-engine way of handling meshes */
public class Mesh {

	public Mesh(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals) {
		this.vertices = vertices;
		this.textureCoordinates = textureCoordinates;
		this.normals = normals;
	}

	protected final FloatBuffer vertices; //Stored as triplets of coordinates
	protected final FloatBuffer textureCoordinates; //Stored as couples of coordinates
	protected final FloatBuffer normals; //Stored as normalized triplets
	
	public int getVerticesCount() {
		return vertices.capacity() / 3;
	}
	
	public FloatBuffer getVertices() {
		return vertices;
	}
	
	public FloatBuffer getTextureCoordinates() {
		return textureCoordinates;
	}
	
	public FloatBuffer getNormals() {
		return normals;
	}
	
	
}
